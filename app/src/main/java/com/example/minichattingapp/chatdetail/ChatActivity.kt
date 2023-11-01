package com.example.minichattingapp.chatdetail

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.minichattingapp.Key
import com.example.minichattingapp.R
import com.example.minichattingapp.databinding.ActivityChatBinding
import com.example.minichattingapp.userlist.UserItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter : ChatDetailAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    private var chatRoomId:String=""
    private var otherUserId:String=""
    private var myUserId:String=""
    private var myUserName:String=""
    private var otherUserFcmToken:String = ""
    private var isInit:Boolean=false

    private val chatItemList = mutableListOf<ChatItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatRoomId = intent.getStringExtra(EXTRA_CHAT_ROOM_ID)?:return
        otherUserId = intent.getStringExtra(EXTRA_OTHER_USER_ID)?:return
        myUserId = Firebase.auth.currentUser?.uid?:""

        chatAdapter = ChatDetailAdapter()
        linearLayoutManager=LinearLayoutManager(applicationContext)

        Firebase.database.reference.child(Key.DB_USERS).child(myUserId).get()
            .addOnSuccessListener {
                val myUserItem = it.getValue(UserItem::class.java)
                myUserName = myUserItem?.username?:""
                getOtherUserData() // 내 정보 가져오는거 성공하면 상대방 데이터도 가져오기
            }


        binding.chatRecyclerView.apply{
            layoutManager = linearLayoutManager
            adapter = chatAdapter
        }

        // 가장 최신에 보낸 메시지 (맨 아래쪽)로 scroll된 상태로 보이도록 하기 위한 장치
        chatAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver(){
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) { // 새로운 메시지 아이템이 삽입될때마다
                super.onItemRangeInserted(positionStart, itemCount)

                linearLayoutManager.smoothScrollToPosition(
                    binding.chatRecyclerView,
                    null,
                    chatAdapter.itemCount
                )

            }
        })

        binding.sendButton.setOnClickListener {
            val message = binding.messageEditText.text.toString()

            if (!isInit){
                return@setOnClickListener
            }

            if (message.isEmpty()){
                Toast.makeText(applicationContext, "빈 메시지를 전송할수 없습니다.",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newChatItem = ChatItem(
                message = message,
                userId = myUserId,
            )
            Firebase.database.reference.child(Key.DB_CHATS).child(chatRoomId).push().apply {
                newChatItem.chatId = key
                setValue(newChatItem)
            }

            val updates:MutableMap<String,Any> = hashMapOf(
                "${Key.DB_CHAT_ROOMS}/$myUserId/$otherUserId/lastMessage" to message,
                "${Key.DB_CHAT_ROOMS}/$otherUserId/$myUserId/lastMessage" to message,
                "${Key.DB_CHAT_ROOMS}/$otherUserId/$myUserId/chatRoomId" to chatRoomId,
                "${Key.DB_CHAT_ROOMS}/$otherUserId/$myUserId/otherUserName" to myUserName,
            )
            Firebase.database.reference.updateChildren(updates)

            // 푸시 메세지 보내라고 서버에 알림
            val client = OkHttpClient()

            val root = JSONObject()

            val notification = JSONObject()
            notification.put("title", getString(R.string.app_name))
            notification.put("body",message)

            root.put("to",otherUserFcmToken)
            root.put("priority","high")
            root.put("notification",notification)

            val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().post(requestBody).url("https://fcm.googleapis.com/fcm/send")
                .header("Authorization","key=${getString(R.string.FCM_SERVER_KEY)}").build()
            client.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.stackTraceToString()
                }
                override fun onResponse(call: Call, response: Response) {
                    Log.e("ChatActivity", response.toString())
                }
            })

            // 다 보냈으면 메세지 보내는 창에 있는 텍스트는 싹 지워줌
            binding.messageEditText.text.clear()
        }
    }

    private fun getOtherUserData(){
        Firebase.database.reference.child(Key.DB_USERS).child(otherUserId).get()
            .addOnSuccessListener {
                val otherUserItem = it.getValue(UserItem::class.java)
                otherUserFcmToken = otherUserItem?.fcmToken.orEmpty()
                chatAdapter.otherUserItem = otherUserItem
                isInit = true
                getChatData() // 상대방 정보를 가지고 오는것에 성공했을떄, 채팅 내용 데이터를 가져오기
            }
    }

    private fun getChatData(){
        Firebase.database.reference.child(Key.DB_CHATS).child(chatRoomId)
            .addChildEventListener( object:ChildEventListener{
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val chatItem = snapshot.getValue(ChatItem::class.java)
                    chatItem?:return
                    chatItemList.add(chatItem)
                    chatAdapter.submitList(chatItemList.toMutableList()) // 새로운 주소값의 array를 넣어줘야 달라졌다는걸 앎
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            }
            )

    }

    companion object {
        const val EXTRA_CHAT_ROOM_ID = "CHAT_ROOM_ID"
        const val EXTRA_OTHER_USER_ID = "OTHER_USER_ID"
    }

}