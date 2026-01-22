package com.example.atlas

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.atlas.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.BottomNV.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Item_Inicio -> {
                    true
                }
                R.id.Item_Rutinas -> {
                    true
                }
                R.id.Item_Progreso -> {
                    true
                }
                R.id.Item_Perfil -> {
                    true
                }
                else -> false
            }
        }

    }
}