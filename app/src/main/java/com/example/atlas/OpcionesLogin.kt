package com.example.atlas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.atlas.databinding.ActivityOpcionesLoginBinding
import com.example.atlas.opciones_login.login_email

class OpcionesLogin : AppCompatActivity() {

    private lateinit var binding: ActivityOpcionesLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOpcionesLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.IngresarEmail.setOnClickListener {
            startActivity(Intent(this@OpcionesLogin, login_email::class.java))
        }
    }
}