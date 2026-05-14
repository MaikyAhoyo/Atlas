package com.example.atlas.opciones_login

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.atlas.MainActivity
import com.example.atlas.RegistroEmail
import com.example.atlas.databinding.ActivityLoginEmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging


class LoginEmail : AppCompatActivity() {

    private lateinit var binding: ActivityLoginEmailBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityLoginEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.BtnIngresar.setOnClickListener {
            validarInfo()
        }

        binding.TxtRegistrarme.setOnClickListener {
            startActivity(Intent(this, RegistroEmail::class.java))
        }
    }

    private var email = ""
    private var password = ""

    private fun validarInfo() {
        email = binding.EtEmail.text.toString().trim()
        password = binding.EtPassword.text.toString().trim()

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.EtEmail.error = "Email invalido"
            binding.EtEmail.requestFocus()

        }
        else if (email.isEmpty()) {
            binding.EtEmail.error = "Ingrese un email"
            binding.EtEmail.requestFocus()
        }
        else if (password.isEmpty()){
            binding.EtPassword.error = "Ingrese el password"
            binding.EtPassword.requestFocus()
        }else{
            loginUsuario()
        }
    }

    private fun loginUsuario() {
        progressDialog.setMessage("Iniciando sesión")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {

                val miUid = firebaseAuth.uid
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
                        ref.child(miUid!!).child("fcmToken").setValue(token)
                    }
                }

                progressDialog.dismiss()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
                Toast.makeText(this,
                    "Bienvenido",
                    Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this,
                    "No se pudo iniciar sesión debido a ${it.message}",
                    Toast.LENGTH_SHORT)
                    .show()
            }
    }
}