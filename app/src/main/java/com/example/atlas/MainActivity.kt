package com.example.atlas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.atlas.Fragmentos.FragmentInicio
import com.example.atlas.Fragmentos.FragmentPerfil
import com.example.atlas.Fragmentos.FragmentChat
import com.example.atlas.Fragmentos.FragmentRutinas
import com.example.atlas.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        verFragmentInicio()

        binding.BottomNV.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Item_Inicio -> {
                    verFragmentInicio()
                    true
                }
                R.id.Item_Rutinas -> {
                    verFragmentRutinas()
                    true
                }
                R.id.Item_Chat -> {
                    verFragmentChat()
                    true
                }
                R.id.Item_Perfil -> {
                    verFragmentPerfil()
                    true
                }
                else -> false
            }
        }

    }

    private fun comprobarSesion(){
        if (firebaseAuth.currentUser == null){
            startActivity(Intent(this, OpcionesLogin::class.java))
            finishAffinity()
        }
    }

    private fun verFragmentInicio(){
        binding.TituloRL.text ="Inicio"
        val fragment = FragmentInicio()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentInicio")
        fragmentTransition.commit()
    }

    private fun verFragmentRutinas(){
        binding.TituloRL.text ="Rutinas"
        val fragment = FragmentRutinas()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentRutinas")
        fragmentTransition.commit()
    }

    private fun verFragmentChat(){
        binding.TituloRL.text ="Chat"
        val fragment = FragmentChat()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentChat")
        fragmentTransition.commit()
    }

    private fun verFragmentPerfil(){
        binding.TituloRL.text ="Perfil"
        val fragment = FragmentPerfil()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentPerfil")
        fragmentTransition.commit()
    }
}