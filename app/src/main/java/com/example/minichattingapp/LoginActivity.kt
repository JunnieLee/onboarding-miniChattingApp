package com.example.minichattingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.minichattingapp.Key.Companion.DB_URL
import com.example.minichattingapp.Key.Companion.DB_USERS
import com.example.minichattingapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

       // 회원가입
        binding.signUpButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "이메일 혹은 패스워드를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Firebase.auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) {task->
                    if (task.isSuccessful){
                        // 회원가입 성공
                        Toast.makeText(this, "회원가입에 성공했습니다. 로그인 해주세요.", Toast.LENGTH_SHORT).show()
                    } else {
                        // 회원가입 실패
                        Toast.makeText(this, "회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // 로그인
        binding.signInButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "이메일 혹은 패스워드를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Firebase.auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this){task->
                    val currentUser = Firebase.auth.currentUser
                    if (task.isSuccessful && currentUser != null){

                        // (1) DB에 유저 테이블을 만들고 데이터를 추가한다
                        val userId = currentUser.uid
                        Firebase.messaging.token.addOnCompleteListener{// 푸시알림 토큰
                            val token = it.result
                            val user = mutableMapOf<String,Any>()
                            user["userId"] = userId
                            user["username"] = email
                            user["fcmToken"] = token
                            Firebase.database(DB_URL).reference.child(DB_USERS).child(userId)
                                .updateChildren(user)

                            // (2) 로그인이 잘 되었으면 메인 페이지로 이동한다
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }

                    } else {
                        Log.e("LoginActivity", task.exception.toString())
                        Toast.makeText(this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}