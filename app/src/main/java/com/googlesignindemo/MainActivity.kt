package com.googlesignindemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    val TAG = MainActivity::class.java.name
    val RC_SIGN_IN = 101
    lateinit var googleSignInOptions: GoogleSignInOptions
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firebaseAuth = FirebaseAuth.getInstance()
        googleSignInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.CLIENT_ID)
                .requestEmail()
                .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        sign_in_button.setSize(SignInButton.SIZE_WIDE)

        sign_in_button.setOnClickListener(this)
        btnLogOut.setOnClickListener(this)

        isUserSignIn()
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        if(idToken.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.strUnableToFetchToken), Toast.LENGTH_LONG).show()
        } else {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")
                        val user = firebaseAuth.currentUser
                        Toast.makeText(this, String.format("%s %s", getString(R.string.strHello), user?.displayName), Toast.LENGTH_LONG).show()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        Toast.makeText(this, getString(R.string.strAuthFailed), Toast.LENGTH_LONG).show()
                    }
                }
        }
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.sign_in_button -> signIn()
            R.id.btnLogOut -> signOut()
        }
    }

    private fun signIn() {
        val signIntent = googleSignInClient.signInIntent
        startActivityForResult(signIntent, RC_SIGN_IN)
    }

    private fun signOut() {
        googleSignInClient.let {
            googleSignInClient.signOut().addOnCompleteListener(this, OnCompleteListener {
                updateStatus(null)
            })
        }
    }

    private fun updateStatus(account: GoogleSignInAccount?) {
        if (account != null) {
            firebaseAuthWithGoogle(account.idToken)
            txtStatus.text = String.format("%s\n%s %s", getString(R.string.strLogin), getString(R.string.strHello), account.displayName)
            btnLogOut.visibility = View.VISIBLE
            sign_in_button.visibility = View.GONE
        } else {
            txtStatus.text = getString(R.string.strLoginOut)
            btnLogOut.visibility = View.GONE
            sign_in_button.visibility = View.VISIBLE
        }

    }

    private fun isUserSignIn() {
        updateStatus(GoogleSignIn.getLastSignedInAccount(this))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount? = completedTask.getResult(ApiException::class.java)
            updateStatus(account)
        } catch (e: ApiException) {
            Log.e(TAG, "signInResult:failed code=" + e.statusCode)
            updateStatus(null)
        }
    }
}