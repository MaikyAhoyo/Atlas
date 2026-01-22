package com.example.atlas

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.atlas.Fragmentos.FragmentInicio
import com.example.atlas.Fragmentos.FragmentPerfil
import com.example.atlas.Fragmentos.FragmentProgreso
import com.example.atlas.Fragmentos.FragmentRutinas
import com.example.atlas.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                R.id.Item_Progreso -> {
                    verFragmentProgreso()
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

    private fun verFragmentProgreso(){
        binding.TituloRL.text ="Progreso"
        val fragment = FragmentProgreso()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.FragmentL1.id, fragment, "FragmentProgreso")
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