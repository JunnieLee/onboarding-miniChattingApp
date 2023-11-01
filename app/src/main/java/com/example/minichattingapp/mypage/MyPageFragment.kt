package com.example.minichattingapp.mypage

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.minichattingapp.Key
import com.example.minichattingapp.LoginActivity
import com.example.minichattingapp.R
import com.example.minichattingapp.databinding.FragmentMypageBinding
import com.example.minichattingapp.userlist.UserItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MyPageFragment : Fragment(R.layout.fragment_mypage) {

    private lateinit var binding : FragmentMypageBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMypageBinding.bind(view)

        // firebase DB로부터 현재 유저의 유저네임과 상메를 가져옴 -> 가져와서 UI에 띄워줌
        val currentUserId = Firebase.auth.currentUser?.uid?:""
        val currentUserDB = Firebase.database.reference.child(Key.DB_USERS).child(currentUserId)
        currentUserDB.get().addOnSuccessListener {
            val currentUserItem = it.getValue(UserItem::class.java)?: return@addOnSuccessListener
            binding.usernameEditText.setText(currentUserItem.username)
            binding.descriptionEditText.setText(currentUserItem.description)
        }

        binding.applyButton.setOnClickListener {

            // (1) 입력값 확인
            val username = binding.usernameEditText.text.toString()
            val description = binding.descriptionEditText.text.toString()
            if (username.isEmpty()){
                Toast.makeText(context, "유저이름은 빈 값으로 두실 수 없습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // (2) firebase realtime database update

            val user = mutableMapOf<String,Any>()
            user["username"] = username
            user["description"] = description
            currentUserDB.updateChildren(user)
        }
        binding.signOutButton.setOnClickListener {
            Firebase.auth.signOut()
            startActivity(Intent(context, LoginActivity::class.java))
            activity?.finish()
        }
    }

}