package com.example.atlas

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.atlas.databinding.ActivityOpcionesLoginBinding
import com.example.atlas.opciones_login.LoginEmail
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class OpcionesLogin : AppCompatActivity() {

    private lateinit var binding: ActivityOpcionesLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        binding = ActivityOpcionesLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.IngresarGoogle.setOnClickListener {
            googleLogin()
        }

        binding.IngresarEmail.setOnClickListener {
            startActivity(Intent(this@OpcionesLogin, LoginEmail::class.java))
        }
    }

    private fun googleLogin(){
        progressDialog.setMessage("Conectando con Google...")
        progressDialog.show()
        val googleSignInIntent = mGoogleSignInClient.signInIntent
        googleSingInARL.launch(googleSignInIntent)
    }

    private val googleSingInARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ resultado ->
        if(resultado.resultCode == RESULT_OK){
            val data = resultado.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try{
                val cuenta = task.getResult(ApiException::class.java)
                autenticacionGoogle(cuenta.idToken)
            }catch (e: Exception){
                progressDialog.dismiss()
                Toast.makeText(this, "${e.message }", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Cancelado. Código: ${resultado.resultCode}", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
        }
    }

    private fun autenticacionGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener {
                progressDialog.dismiss()
                verificarPerfilEnBD()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun comprobarSesion(){
        if (firebaseAuth.currentUser != null){
            verificarPerfilEnBD()
        }
    }

    private fun verificarPerfilEnBD() {
        val uid = firebaseAuth.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    startActivity(Intent(this@OpcionesLogin, MainActivity::class.java))
                    finishAffinity()
                } else {
                    startActivity(Intent(this@OpcionesLogin, CompletarPerfil::class.java))
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@OpcionesLogin, "Error de base de datos", Toast.LENGTH_SHORT).show()
            }
        })
    }
}