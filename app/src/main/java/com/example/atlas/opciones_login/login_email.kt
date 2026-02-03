package com.example.atlas.opciones_login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.atlas.RegistroEmail
import com.example.atlas.databinding.ActivityLoginEmailBinding

class login_email : AppCompatActivity() {

    private lateinit var binding: ActivityLoginEmailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.TxtRegistrarme.setOnClickListener {
            startActivity(Intent(this@login_email, RegistroEmail::class.java))
        }
    }
}