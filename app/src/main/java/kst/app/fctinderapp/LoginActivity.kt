package kst.app.fctinderapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kst.app.fctinderapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        callbackManager = CallbackManager.Factory.create()
        initViews()
        initEmailPassword()
    }

    private fun initViews(){

        binding.loginBt.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val password = binding.passwordEt.text.toString()

            auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this){ task ->
                    if (task.isSuccessful){
                        handleSuccessLogin()
                    } else {
                        Toast.makeText(this,"로그인에 실패 하였습니다. 이메일또는 비밀번호를 확인해 주세요.",Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.signUpBt.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val password = binding.passwordEt.text.toString()

            auth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this){ task ->
                    if (task.isSuccessful){
                        Toast.makeText(this,"회원가입에 성공하였습니다. 로그인 버튼을 눌러 로그인 해주세",Toast.LENGTH_LONG).show()
                    }else{
                        Toast.makeText(this,"회원가입에 실패 하였습니다. 이메일또는 비밀번호를 확인해 주세요.",Toast.LENGTH_LONG).show()

                    }
                }
        }

        binding.faceBookLoginBt.setPermissions("email","public_profile")
        binding.faceBookLoginBt.registerCallback(callbackManager,object : FacebookCallback<LoginResult>{
            override fun onSuccess(result: LoginResult) {
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this@LoginActivity){ task ->
                        if (task.isSuccessful){
                            handleSuccessLogin()
                        } else{
                            Toast.makeText(this@LoginActivity,"페이스북 로그인이 실패 했습니다.",Toast.LENGTH_SHORT).show()
                        }
                    }
            }

            override fun onCancel() {
                TODO("Not yet implemented")
            }

            override fun onError(error: FacebookException?) {
                Toast.makeText(this@LoginActivity,"페이스북 로그인이 실패 했습니다.",Toast.LENGTH_SHORT).show()
            }

        })

    }

    private fun initEmailPassword(){
        binding.emailEt.addTextChangedListener {
            val enable = binding.emailEt.text.isNotEmpty() && binding.passwordEt.text.isNotEmpty()
            binding.loginBt.isEnabled = enable
            binding.signUpBt.isEnabled = enable
        }

        binding.passwordEt.addTextChangedListener {
            val enable = binding.emailEt.text.isNotEmpty() && binding.passwordEt.text.isNotEmpty()
            binding.loginBt.isEnabled = enable
            binding.signUpBt.isEnabled = enable
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleSuccessLogin(){
        if (auth.currentUser == null){
            Toast.makeText(this,"로그인에 실패하였습니다.",Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid.orEmpty()
        val currentUserDB = Firebase.database.reference.child("Users").child(userId)
        val user = mutableMapOf<String, Any>()
        user["userId"] = userId
        currentUserDB.updateChildren(user)

        finish()
    }


}