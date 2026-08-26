package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSummary
import com.example.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("bd_chat_prefs", Context.MODE_PRIVATE)
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance("https://chat-app-79120-default-rtdb.firebaseio.com")
        } catch (_: Exception) {
            FirebaseDatabase.getInstance()
        }
    }

    private val usersRef: DatabaseReference get() = database.getReference("users")
    private val globalMessagesRef: DatabaseReference get() = database.getReference("global_messages")
    private val directChatsRef: DatabaseReference get() = database.getReference("direct_chats")
    private val userChatsRef: DatabaseReference get() = database.getReference("user_chats")

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _globalMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val globalMessages: StateFlow<List<ChatMessage>> = _globalMessages.asStateFlow()

    private val _activeDirectMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val activeDirectMessages: StateFlow<List<ChatMessage>> = _activeDirectMessages.asStateFlow()

    private val _chatSummaries = MutableStateFlow<List<ChatSummary>>(emptyList())
    val chatSummaries: StateFlow<List<ChatSummary>> = _chatSummaries.asStateFlow()

    private var currentActiveChatId: String? = null
    private var directChatValueListener: ValueEventListener? = null
    private var userChatsListener: ValueEventListener? = null

    init {
        loadSavedUser()
        listenToAllUsers()
        listenToGlobalMessages()
    }

    fun getDirectChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    private fun loadSavedUser() {
        val savedUid = prefs.getString("saved_uid", null)
        val savedUsername = prefs.getString("saved_username", null)
        val savedName = prefs.getString("saved_name", null)
        val savedPhoto = prefs.getString("saved_photo", null)
        val savedBio = prefs.getString("saved_bio", null)
        val savedEmail = prefs.getString("saved_email", null)

        if (!savedUid.isNullOrBlank() && !savedName.isNullOrBlank()) {
            val user = User(
                userId = savedUid,
                username = savedUsername ?: "@user",
                displayName = savedName,
                email = savedEmail ?: "",
                photoUrl = savedPhoto ?: "",
                bio = savedBio ?: "Hello! I am using BD Chat.",
                online = true
            )
            _currentUser.value = user
            setUserOnlinePresence(user.userId, true)
            listenToUserDirectChats(user.userId)
        }
    }

    private fun saveUserToLocal(user: User) {
        prefs.edit().apply {
            putString("saved_uid", user.userId)
            putString("saved_username", user.username)
            putString("saved_name", user.displayName)
            putString("saved_photo", user.photoUrl)
            putString("saved_bio", user.bio)
            putString("saved_email", user.email)
            apply()
        }
    }

    fun signUp(
        username: String,
        displayName: String,
        email: String,
        password: String,
        photoUrl: String = "",
        bio: String = "Hello! I am using BD Chat.",
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanUsername = if (username.startsWith("@")) username.trim() else "@${username.trim()}"
        val sanitizedUid = if (email.isNotBlank()) {
            email.replace(".", "_").replace("@", "_").replace("#", "").replace("$", "").replace("[", "").replace("]", "")
        } else {
            cleanUsername.replace("@", "").replace(".", "_")
        }

        // Check if user with this username or email already exists in local list or database
        val existing = _allUsers.value.find { 
            it.username.equals(cleanUsername, ignoreCase = true) || 
            (email.isNotBlank() && it.email.equals(email.trim(), ignoreCase = true))
        }

        if (existing != null) {
            onError("An account with this username or email already exists. Please log in.")
            return
        }

        val newUser = User(
            userId = sanitizedUid,
            username = cleanUsername,
            displayName = displayName.ifBlank { cleanUsername.removePrefix("@") },
            email = email.trim(),
            password = password,
            photoUrl = photoUrl,
            bio = bio.ifBlank { "Hello! I am using BD Chat." },
            online = true,
            createdAt = System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis()
        )

        usersRef.child(sanitizedUid).setValue(newUser)
            .addOnSuccessListener {
                _currentUser.value = newUser
                saveUserToLocal(newUser)
                setUserOnlinePresence(sanitizedUid, true)
                listenToUserDirectChats(sanitizedUid)
                onSuccess(newUser)
            }
            .addOnFailureListener { err ->
                // Local fallback
                _currentUser.value = newUser
                saveUserToLocal(newUser)
                onSuccess(newUser)
            }
    }

    fun login(
        identifier: String,
        password: String,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanIdentifier = identifier.trim()
        val withAt = if (cleanIdentifier.startsWith("@")) cleanIdentifier else "@$cleanIdentifier"
        val withoutAt = cleanIdentifier.removePrefix("@")

        // First check in _allUsers in-memory cache
        val userFromCache = _allUsers.value.find {
            it.username.equals(cleanIdentifier, ignoreCase = true) ||
            it.username.equals(withAt, ignoreCase = true) ||
            it.username.equals(withoutAt, ignoreCase = true) ||
            it.email.equals(cleanIdentifier, ignoreCase = true)
        }

        if (userFromCache != null) {
            if (userFromCache.password.isNotBlank() && userFromCache.password != password) {
                onError("Incorrect password! Please check and try again.")
                return
            }
            // Successful match
            _currentUser.value = userFromCache
            saveUserToLocal(userFromCache)
            setUserOnlinePresence(userFromCache.userId, true)
            listenToUserDirectChats(userFromCache.userId)
            onSuccess(userFromCache)
            return
        }

        // Query database directly in case user list hasn't loaded yet
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var foundUser: User? = null
                for (child in snapshot.children) {
                    val u = child.getValue(User::class.java)
                    if (u != null) {
                        if (u.username.equals(cleanIdentifier, ignoreCase = true) ||
                            u.username.equals(withAt, ignoreCase = true) ||
                            u.username.equals(withoutAt, ignoreCase = true) ||
                            u.email.equals(cleanIdentifier, ignoreCase = true)
                        ) {
                            foundUser = u
                            break
                        }
                    }
                }

                if (foundUser != null) {
                    if (foundUser.password.isNotBlank() && foundUser.password != password) {
                        onError("Incorrect password! Please check and try again.")
                    } else {
                        _currentUser.value = foundUser
                        saveUserToLocal(foundUser)
                        setUserOnlinePresence(foundUser.userId, true)
                        listenToUserDirectChats(foundUser.userId)
                        onSuccess(foundUser)
                    }
                } else {
                    onError("No user found with username or email '$cleanIdentifier'. Please Sign Up.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onError("Login failed: ${error.message}")
            }
        })
    }

    fun updateProfile(
        displayName: String,
        photoUrl: String,
        bio: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            displayName = displayName.ifBlank { current.displayName },
            photoUrl = photoUrl,
            bio = bio.ifBlank { current.bio }
        )

        usersRef.child(updated.userId).setValue(updated)
            .addOnSuccessListener {
                _currentUser.value = updated
                saveUserToLocal(updated)
                onSuccess()
            }
            .addOnFailureListener { err ->
                _currentUser.value = updated
                saveUserToLocal(updated)
                onSuccess()
            }
    }

    fun logout() {
        val current = _currentUser.value
        if (current != null) {
            setUserOnlinePresence(current.userId, false)
        }
        prefs.edit().clear().apply()
        _currentUser.value = null
        _chatSummaries.value = emptyList()
    }

    private fun setUserOnlinePresence(userId: String, isOnline: Boolean) {
        try {
            val userPresenceRef = usersRef.child(userId).child("online")
            val lastSeenRef = usersRef.child(userId).child("lastSeen")

            userPresenceRef.setValue(isOnline)
            lastSeenRef.setValue(System.currentTimeMillis())

            val connectedRef = database.getReference(".info/connected")
            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        userPresenceRef.onDisconnect().setValue(false)
                        lastSeenRef.onDisconnect().setValue(System.currentTimeMillis())
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (_: Exception) {}
    }

    private fun listenToAllUsers() {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<User>()
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java)
                    if (user != null) {
                        list.add(user)
                    }
                }
                _allUsers.value = list
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Users fetch cancelled: ${error.message}")
            }
        })
    }

    private fun listenToGlobalMessages() {
        globalMessagesRef.limitToLast(100).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val msg = child.getValue(ChatMessage::class.java)
                    if (msg != null) {
                        list.add(msg)
                    }
                }
                val sorted = list.sortedBy { it.timestamp }
                val previousLast = _globalMessages.value.lastOrNull()
                _globalMessages.value = sorted

                // Trigger notification for new message if from someone else
                val currentUserId = _currentUser.value?.userId
                val newLast = sorted.lastOrNull()
                if (newLast != null && newLast.id != previousLast?.id && currentUserId != null) {
                    if (newLast.senderId != currentUserId) {
                        NotificationHelper.showMessageNotification(
                            context = context,
                            notificationId = newLast.id.hashCode(),
                            senderName = newLast.senderName,
                            messageText = newLast.text,
                            chatTitle = "Global Group Chat"
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun sendGlobalMessage(text: String, replyTo: ChatMessage? = null) {
        val user = _currentUser.value ?: return
        if (text.isBlank()) return

        val msgId = globalMessagesRef.push().key ?: UUID.randomUUID().toString()
        val message = ChatMessage(
            id = msgId,
            senderId = user.userId,
            senderName = user.displayName,
            senderPhoto = user.photoUrl,
            receiverId = "",
            chatId = "global",
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            seen = true,
            replyToId = replyTo?.id ?: "",
            replyToSender = replyTo?.senderName ?: "",
            replyToText = replyTo?.text ?: ""
        )

        globalMessagesRef.child(msgId).setValue(message)
    }

    fun startListeningToDirectChat(chatId: String, otherUserId: String) {
        currentActiveChatId = chatId
        directChatValueListener?.let { directChatsRef.child(chatId).removeEventListener(it) }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                val msgSnap = snapshot.child("messages")
                for (child in msgSnap.children) {
                    val msg = child.getValue(ChatMessage::class.java)
                    if (msg != null) {
                        list.add(msg)
                    }
                }
                val sorted = list.sortedBy { it.timestamp }
                val previousLast = _activeDirectMessages.value.lastOrNull()
                _activeDirectMessages.value = sorted

                // Check for new incoming message and notify
                val currentUserId = _currentUser.value?.userId
                val newLast = sorted.lastOrNull()
                if (newLast != null && newLast.id != previousLast?.id && currentUserId != null) {
                    if (newLast.senderId != currentUserId) {
                        // Mark as seen immediately if user is on this screen
                        markMessageAsSeen(chatId, newLast.id)
                    }
                }

                // Automatically mark all messages from other user as seen
                if (currentUserId != null) {
                    markAllDirectMessagesAsSeen(chatId, currentUserId)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        directChatValueListener = listener
        directChatsRef.child(chatId).addValueEventListener(listener)
    }

    fun stopListeningToDirectChat() {
        currentActiveChatId?.let { chatId ->
            directChatValueListener?.let { directChatsRef.child(chatId).removeEventListener(it) }
        }
        currentActiveChatId = null
        directChatValueListener = null
        _activeDirectMessages.value = emptyList()
    }

    fun markMessageAsSeen(chatId: String, messageId: String) {
        directChatsRef.child(chatId).child("messages").child(messageId).child("seen").setValue(true)
    }

    fun markAllDirectMessagesAsSeen(chatId: String, currentUserId: String) {
        directChatsRef.child(chatId).child("messages").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val msg = child.getValue(ChatMessage::class.java)
                    if (msg != null && msg.senderId != currentUserId && !msg.seen) {
                        child.ref.child("seen").setValue(true)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun sendDirectMessage(
        receiver: User,
        text: String,
        replyTo: ChatMessage? = null
    ) {
        val user = _currentUser.value ?: return
        if (text.isBlank()) return

        val chatId = getDirectChatId(user.userId, receiver.userId)
        val msgRef = directChatsRef.child(chatId).child("messages").push()
        val msgId = msgRef.key ?: UUID.randomUUID().toString()

        val message = ChatMessage(
            id = msgId,
            senderId = user.userId,
            senderName = user.displayName,
            senderPhoto = user.photoUrl,
            receiverId = receiver.userId,
            chatId = chatId,
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            seen = false, // Starts as 1 single tick (✓), becomes 2 blue ticks (✓✓) when seen
            replyToId = replyTo?.id ?: "",
            replyToSender = replyTo?.senderName ?: "",
            replyToText = replyTo?.text ?: ""
        )

        msgRef.setValue(message)

        // Update user chat index for both sender & receiver
        val summaryData = mapOf(
            "chatId" to chatId,
            "otherUserId" to receiver.userId,
            "otherUserName" to receiver.displayName,
            "otherUserPhoto" to receiver.photoUrl,
            "lastMessage" to text.trim(),
            "lastTimestamp" to System.currentTimeMillis(),
            "lastSenderId" to user.userId
        )
        userChatsRef.child(user.userId).child(receiver.userId).setValue(summaryData)

        val receiverSummaryData = mapOf(
            "chatId" to chatId,
            "otherUserId" to user.userId,
            "otherUserName" to user.displayName,
            "otherUserPhoto" to user.photoUrl,
            "lastMessage" to text.trim(),
            "lastTimestamp" to System.currentTimeMillis(),
            "lastSenderId" to user.userId
        )
        userChatsRef.child(receiver.userId).child(user.userId).setValue(receiverSummaryData)
    }

    private fun listenToUserDirectChats(currentUserId: String) {
        userChatsListener?.let { userChatsRef.child(currentUserId).removeEventListener(it) }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatSummary>()
                for (child in snapshot.children) {
                    val otherUid = child.child("otherUserId").getValue(String::class.java) ?: child.key ?: ""
                    val chatId = child.child("chatId").getValue(String::class.java) ?: ""
                    val lastMsg = child.child("lastMessage").getValue(String::class.java) ?: ""
                    val lastTime = child.child("lastTimestamp").getValue(Long::class.java) ?: 0L
                    val lastSender = child.child("lastSenderId").getValue(String::class.java) ?: ""

                    val otherUser = _allUsers.value.find { it.userId == otherUid } ?: User(
                        userId = otherUid,
                        displayName = child.child("otherUserName").getValue(String::class.java) ?: "User",
                        photoUrl = child.child("otherUserPhoto").getValue(String::class.java) ?: ""
                    )

                    list.add(
                        ChatSummary(
                            chatId = chatId,
                            otherUser = otherUser,
                            lastMessage = lastMsg,
                            lastTimestamp = lastTime,
                            lastSenderId = lastSender
                        )
                    )
                }
                _chatSummaries.value = list.sortedByDescending { it.lastTimestamp }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        userChatsListener = listener
        userChatsRef.child(currentUserId).addValueEventListener(listener)
    }
}
