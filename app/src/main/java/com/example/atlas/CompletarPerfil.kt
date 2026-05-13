package com.example.atlas

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.atlas.databinding.ActivityCompletarPerfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class CompletarPerfil : AppCompatActivity() {

    private lateinit var binding: ActivityCompletarPerfilBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompletarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        configurarBotones()
        configurarMenuGenero()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                progressDialog.setMessage("Cancelando registro...")
                progressDialog.show()

                firebaseAuth.currentUser?.delete()?.addOnCompleteListener {
                    firebaseAuth.signOut()
                    progressDialog.dismiss()
                    Toast.makeText(this@CompletarPerfil, "Registro cancelado", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@CompletarPerfil, OpcionesLogin::class.java))
                    finishAffinity()
                }
            }
        })
    }

    private fun configurarBotones() {
        binding.BtnGuardarDatos.setOnClickListener {
            validarDatos()
        }

        binding.EtFechaNac.setOnClickListener {
            mostrarCalendario()
        }
    }

    private fun configurarMenuGenero() {
        val generos = arrayOf("Masculino", "Femenino", "Otro", "Prefiero no decirlo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, generos)
        binding.EtGenero.setAdapter(adapter)
    }

    private fun mostrarCalendario() {
        val calendario = Calendar.getInstance()
        val año = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val fechaSeleccionada = "$dayOfMonth/${month + 1}/$year"
            binding.EtFechaNac.setText(fechaSeleccionada)
        }, año, mes, dia)

        datePickerDialog.show()
    }

    private fun validarDatos() {
        val nombre = binding.EtNombre.text.toString().trim()
        val peso = binding.EtPeso.text.toString().trim()
        val altura = binding.EtAltura.text.toString().trim()
        val fechaNac = binding.EtFechaNac.text.toString().trim()
        val genero = binding.EtGenero.text.toString().trim()

        if (nombre.isEmpty()) {
            binding.EtNombre.error = "Ingrese su nombre"
            return
        }
        if (fechaNac.isEmpty()) {
            Toast.makeText(this, "Seleccione su fecha de nacimiento", Toast.LENGTH_SHORT).show()
            return
        }
        if (genero.isEmpty()) {
            Toast.makeText(this, "Seleccione su género", Toast.LENGTH_SHORT).show()
            return
        }
        if (peso.isEmpty()) {
            binding.EtPeso.error = "Ingrese su peso"
            return
        }
        if (altura.isEmpty()) {
            binding.EtAltura.error = "Ingrese su altura"
            return
        }

        guardarEnFirebase()
    }

    private fun guardarEnFirebase() {
        progressDialog.setMessage("Guardando perfil...")
        progressDialog.show()

        val uidUsuario = firebaseAuth.uid
        val emailUsuario = firebaseAuth.currentUser?.email
        val tiempo = System.currentTimeMillis()

        val nombre = binding.EtNombre.text.toString().trim()
        val telefono = binding.EtTelefono.text.toString().trim()
        val fechaNac = binding.EtFechaNac.text.toString().trim()
        val genero = binding.EtGenero.text.toString().trim()
        val peso = binding.EtPeso.text.toString().toDoubleOrNull() ?: 0.0
        val altura = binding.EtAltura.text.toString().toDoubleOrNull() ?: 0.0

        val hashMap = HashMap<String, Any>()

        // --- DATOS BÁSICOS ---
        hashMap["uid"] = "$uidUsuario"
        hashMap["email"] = "$emailUsuario"
        hashMap["nombres"] = nombre
        hashMap["telefono"] = telefono
        hashMap["fecha_nacimiento"] = fechaNac
        hashMap["tiempo"] = tiempo

        // --- DATOS FÍSICOS ---
        hashMap["peso"] = peso
        hashMap["altura"] = altura
        hashMap["genero"] = genero

        // --- DATOS INICIALES DE APP FITNESS ---
        hashMap["contadorEntrenos"] = 0
        hashMap["contadorSeguidores"] = 0
        hashMap["contadorSiguiendo"] = 0
        hashMap["minutosEntrenadosSemana"] = 0

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uidUsuario!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "¡Bienvenido a Atlas!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}