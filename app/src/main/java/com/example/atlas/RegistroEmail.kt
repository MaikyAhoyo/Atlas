package com.example.atlas

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.atlas.databinding.ActivityRegistroEmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegistroEmail : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroEmailBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.BtnRegistrar.setOnClickListener {
            validarInfo()
        }
    }

    private var email = ""
    private var password = ""
    private var r_password = ""

    private fun validarInfo() {
        email = binding.EtEmail.text.toString().trim()
        password = binding.EtPassword.text.toString().trim()
        r_password = binding.EtRPassword.text.toString().trim()

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.EtEmail.error = "Email invalido"
            binding.EtEmail.requestFocus()
        }
        else if(email.isEmpty()){
            binding.EtEmail.error = "Email requerido"
            binding.EtEmail.requestFocus()
        }
        else if(password.isEmpty()){
            binding.EtPassword.error = "Contraseña requerida"
            binding.EtPassword.requestFocus()
        }
        else if(r_password.isEmpty()){
            binding.EtRPassword.error = "Repetir contraseña requerida"
            binding.EtRPassword.requestFocus()
        }
        else if(password != r_password){
            binding.EtRPassword.error = "Contraseñas no coinciden"
            binding.EtRPassword.requestFocus()
        }
        else {
            registrarUsuario()
        }
    }

    private fun registrarUsuario() {
        progressDialog.setMessage("Creando cuenta")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                progressDialog.dismiss()

                EnviarCorreo.enviar(email, password)

                val intent = Intent(this, CompletarPerfil::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                progressDialog.dismiss()
                Toast.makeText(this,
                    "No se pudo crear el usuario debido a ${exception.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }
}


