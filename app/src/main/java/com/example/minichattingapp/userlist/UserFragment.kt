package com.example.minichattingapp.userlist

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.minichattingapp.Key.Companion.DB_CHAT_ROOMS
import com.example.minichattingapp.Key.Companion.DB_USERS
import com.example.minichattingapp.R
import com.example.minichattingapp.chatdetail.ChatActivity
import com.example.minichattingapp.chatdetail.ChatActivity.Companion.EXTRA_CHAT_ROOM_ID
import com.example.minichattingapp.chatdetail.ChatActivity.Companion.EXTRA_OTHER_USER_ID
import com.example.minichattingapp.chatlist.ChatRoomItem
import com.example.minichattingapp.databinding.FragmentUserlistBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.UUID

class UserFragment : Fragment(R.layout.fragment_userlist) { // 생성자에서 넘겨주게 되면 자동으로 bind를 함

    private lateinit var binding: FragmentUserlistBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentUserlistBinding.bind(view)

        val userListAdapter = UserAdapter{ otherUser ->
            // ChatActivity에 chatRoomId, otherUserId 넘겨줘야함
            val myUserId = Firebase.auth.currentUser?.uid?:""
            val chatRoomDB = Firebase.database.reference.child(DB_CHAT_ROOMS).child(myUserId).child(otherUser.userId?:"")

            chatRoomDB.get().addOnSuccessListener {
                var chatRoomId =""
                if (it.value != null){
                    // 데이터가 존재 - 이미 채팅방이 존재할 경우
                    val chatRoom = it.getValue(ChatRoomItem::class.java)
                    chatRoomId = chatRoom?.chatRoomId?:""
                } else { // 데이터 존재 X - 이전 채팅 내역이 없어 채팅방이 새로 시작되는 경우
                    chatRoomId = UUID.randomUUID().toString() // 새로 id 생성
                    val newChatRoom = ChatRoomItem(
                        chatRoomId = chatRoomId,
                        otherUserName = otherUser.username,
                        otherUserId = otherUser.userId,
                    )
                    chatRoomDB.setValue(newChatRoom)
                }

                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra(EXTRA_OTHER_USER_ID,otherUser.userId)
                intent.putExtra(EXTRA_CHAT_ROOM_ID,chatRoomId)
                startActivity(intent)
            }

        }
        binding.userListRecyclerView.apply{
            layoutManager = LinearLayoutManager(context)
            adapter = userListAdapter
        }

        val currentUserId = Firebase.auth.currentUser?.uid?:""

        Firebase.database.reference.child(DB_USERS)
            .addListenerForSingleValueEvent(object:ValueEventListener{

                override fun onDataChange(snapshot: DataSnapshot) {
                    val userItemList = mutableListOf<UserItem>()
                    snapshot.children.forEach {
                        val user = it.getValue(UserItem::class.java)
                        user?:return
                        if (user.userId!=currentUserId){ // 나를 제외한 나머지 유저들
                            userItemList.add(user)
                        }
                    }
                    userListAdapter.submitList(userItemList)
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

    }
}