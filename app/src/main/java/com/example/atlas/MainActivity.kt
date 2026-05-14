package com.example.atlas

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.atlas.fragmentos.FragmentInicio
import com.example.atlas.fragmentos.FragmentPerfil
import com.example.atlas.fragmentos.FragmentChats
import com.example.atlas.fragmentos.FragmentRutinas
import com.example.atlas.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        actualizarToken()

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
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
        val fragment = FragmentChats()
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

    private fun actualizarToken() {
        val miUid = firebaseAuth.uid
        if (miUid != null) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
                    ref.child(miUid).child("fcmToken").setValue(token)
                }
            }
        }
    }
}