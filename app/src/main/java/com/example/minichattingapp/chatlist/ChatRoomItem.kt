package com.example.minichattingapp.chatlist

data class ChatRoomItem(
    val chatRoomId:String?=null,
    val otherUserName:String?=null,
    val otherUserId:String?=null,
    val lastMessage:String?=null,
)
