package com.example.atlas

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.atlas.databinding.ActivityOpcionesLoginBinding
import com.example.atlas.opciones_login.login_email
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth

class OpcionesLogin : AppCompatActivity() {

    private lateinit var binding: ActivityOpcionesLoginBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var mGoogleSignInClient: GoogleSignInClient

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpcionesLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        binding.IngresarEmail.setOnClickListener {
            startActivity(Intent(this@OpcionesLogin, login_email::class.java))
        }
    }

    private fun comprobarSesion(){
        if (firebaseAuth.currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}