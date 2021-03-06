package com.example.kingbook
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.kingbook.model.Friend
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_login.*
import java.io.ByteArrayOutputStream

class LoginActivity: AppCompatActivity() {

    private var auth : FirebaseAuth? = null
    lateinit var database: DatabaseReference
    private var googleSignInClient : GoogleSignInClient? = null
    private var GOOGLE_LOGIN_CODE = 9001
    lateinit var intentMain: Intent
    var waitTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = Firebase.auth
        database = Firebase.database.reference

        var google_sign_in_button = findViewById<SignInButton>(R.id.login_button)

        login_button.setOnClickListener { googleLogin() }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)

    }

    fun googleLogin(){
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == GOOGLE_LOGIN_CODE){
            // ??????API??? ???????????? ??? ?????????
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)!!

            // ?????? ????????? ??????
            if(result.isSuccess)
            {
                var account = result.signInAccount
                var user = auth?.currentUser

                // ?????? ??????
                if(user != null)
                {
                    intentMain = Intent(this, MainActivity::class.java)
                    updateUI(user)
                    finish()
                    startActivity(intentMain)
                    Toast.makeText(this, "????????? ??????", Toast.LENGTH_SHORT).show()
                }
                // ?????? ??????
                else
                {
                    // ?????? ??????
                    firebaseAuthWithGoogle(account)
                    Toast.makeText(this, "????????? ?????????????????????.", Toast.LENGTH_SHORT).show()
                }
            }
            // ?????? ????????? ??????
            else
            {

                finish()
                Toast.makeText(this, "????????? ??????", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ????????????????????? ?????? ????????? ???????????? ??????
    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?){
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)

        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener{ task ->
                if(task.isSuccessful){
                    // ?????????, ???????????? ??????
                    val email= account?.email// ????????????
                    val name= account?.givenName
                    val imageUrl=account?.photoUrl
                    val user = Firebase.auth.currentUser
                    val userId = user?.uid
                    val userIdSt = userId.toString()
                    val user_uid = user?.uid
                    intentMain = Intent(this, MainActivity::class.java)

                    Log.d("g.email", "$email")
                    Log.d("g.photo", "$imageUrl")
                    Log.d("g.id", "$user_uid")

                   GlideApp.with(this)
                        .asBitmap()
                        .load(imageUrl)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                val bitmap = resource
                                val baos = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                                var data = baos.toByteArray()


                                FirebaseStorage.getInstance()
                                    .reference.child("userImages").child("$userIdSt/photo").putBytes(data!!).addOnSuccessListener {
                                        var userProfile: Uri? = null
                                        FirebaseStorage.getInstance().reference.child("userImages").child("$userIdSt/photo").downloadUrl
                                            .addOnSuccessListener {
                                                userProfile = it
                                                Log.d("userProfile", "$userProfile")
                                                val friend = Friend(email.toString(), name.toString(), userProfile.toString(), userIdSt)
                                                database.ref.child("users").child(userId.toString()).setValue(friend)
                                            }
                                            .addOnFailureListener{
                                                Toast.makeText(applicationContext, "fail", Toast.LENGTH_LONG).show()
                                            }

                                        updateUI(user)
                                        finish()
                                        startActivity(intentMain)
                                    }
                                Log.d("bytedata", "111111")

                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                Log.d("TODO","hi")
                            }
                        })

//                    moveMainPage(task.result?.user)

                   // Toast.makeText(LoginActivity.this, "success", Toast.LENGTH_SHORT).show(
                }
                else
                {
                    // ????????? ???
                    //moveMainPage(task.result?.user)
                    //test
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {

    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth?.currentUser
        if(currentUser != null){
            reload();
        }
    }

    private fun reload() {

    }
    override fun onBackPressed(){
        if(System.currentTimeMillis() - waitTime >= 1500){
            waitTime = System.currentTimeMillis()
            Toast.makeText(this, "???????????? ????????? ?????? ??? ????????? ???????????????", Toast.LENGTH_SHORT).show()
        }else{
            finish()
        }
    }
}

