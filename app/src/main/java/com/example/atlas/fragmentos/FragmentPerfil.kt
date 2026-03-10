package com.example.atlas.fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.atlas.OpcionesLogin
import com.example.atlas.R
import com.example.atlas.databinding.FragmentPerfilBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FragmentPerfil : Fragment() {

    private lateinit var binding: FragmentPerfilBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mContext: Context

    // Variables de la vista
    private lateinit var txtUsername: TextView
    private lateinit var txtEntrenos: TextView
    private lateinit var txtSeguidores: TextView
    private lateinit var txtSiguiendo: TextView
    private lateinit var imgPerfil: ShapeableImageView

    // Nuevas variables para Gráfica y Filtros
    private lateinit var txtHorasSemana: TextView
    private lateinit var btnFiltroDuracion: MaterialButton
    private lateinit var btnFiltroVolumen: MaterialButton
    private lateinit var btnFiltroRepeticiones: MaterialButton

    // Menú inferior
    private lateinit var cardEstadisticas: MaterialCardView
    private lateinit var cardEjercicios: MaterialCardView
    private lateinit var cardMedidas: MaterialCardView
    private lateinit var cardCalendario: MaterialCardView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPerfilBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.BtnCerrarSesion.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(mContext, OpcionesLogin::class.java))
            activity?.finishAffinity()
        }

        inicializarVistas(view)
        cargarDatosDeUsuario()
        configurarBotones()
    }

    private fun inicializarVistas(view: View) {
        txtUsername = view.findViewById(R.id.txtUsername)
        txtEntrenos = view.findViewById(R.id.txtNumEntrenos)
        txtSeguidores = view.findViewById(R.id.txtNumSeguidores)
        txtSiguiendo = view.findViewById(R.id.txtNumSiguiendo)
        imgPerfil = view.findViewById(R.id.imgPerfil)
        imagenPerfilAleatoria()

        txtHorasSemana = view.findViewById(R.id.txtHorasSemana)
        btnFiltroDuracion = view.findViewById(R.id.btnFiltroDuracion)
        btnFiltroVolumen = view.findViewById(R.id.btnFiltroVolumen)
        btnFiltroRepeticiones = view.findViewById(R.id.btnFiltroRepeticiones)

        cardEstadisticas = view.findViewById(R.id.cardEstadisticas)
        cardEjercicios = view.findViewById(R.id.cardEjercicios)
        cardMedidas = view.findViewById(R.id.cardMedidas)
        cardCalendario = view.findViewById(R.id.cardCalendario)
    }

    private fun imagenPerfilAleatoria() {
        val imagenes = listOf(
            R.drawable.img_perfil_1,
            R.drawable.img_perfil_2,
            R.drawable.img_perfil_3)
        val imagenAleatoria = imagenes.random()
        imgPerfil.setImageResource(imagenAleatoria)
    }


    private fun cargarDatosDeUsuario() {
        val uidUsuario = firebaseAuth.uid ?: return

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios").child(uidUsuario)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val nombre = snapshot.child("nombre").value.toString()
                    val entrenos = snapshot.child("contadorEntrenos").value.toString()
                    val seguidores = snapshot.child("contadorSeguidores").value.toString()
                    val siguiendo = snapshot.child("contadorSiguiendo").value.toString()

                    val minutosString = snapshot.child("minutosEntrenadosSemana").value.toString()
                    val minutosTotales = if (minutosString != "null" && minutosString.isNotEmpty()) minutosString.toInt() else 0
                    actualizarTextoHoras(minutosTotales)

                    txtUsername.text = if (nombre.isNotEmpty() && nombre != "null") nombre else "Usuario"
                    txtEntrenos.text = if (entrenos != "null") entrenos else "0"
                    txtSeguidores.text = if (seguidores != "null") seguidores else "0"
                    txtSiguiendo.text = if (siguiendo != "null") siguiendo else "0"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(mContext, "Error al cargar el perfil", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun actualizarTextoHoras(minutosTotales: Int) {
        if (minutosTotales < 60) {
            txtHorasSemana.text = mContext.getString(R.string.formato_minutos, minutosTotales)
        } else {
            val horas = minutosTotales / 60
            val minutosSobrantes = minutosTotales % 60
            if (minutosSobrantes == 0) {
                txtHorasSemana.text = mContext.getString(R.string.formato_horas, horas)
            } else {
                txtHorasSemana.text = mContext.getString(R.string.formato_horas_minutos, horas, minutosSobrantes)
            }
        }
    }

    private fun configurarBotones() {
        btnFiltroDuracion.setOnClickListener {
            cambiarColorFiltros(btnFiltroDuracion)
        }
        btnFiltroVolumen.setOnClickListener {
            cambiarColorFiltros(btnFiltroVolumen)
        }
        btnFiltroRepeticiones.setOnClickListener {
            cambiarColorFiltros(btnFiltroRepeticiones)
        }

        cardEstadisticas.setOnClickListener { Toast.makeText(context, "Estadísticas", Toast.LENGTH_SHORT).show() }
        cardEjercicios.setOnClickListener { Toast.makeText(context, "Ejercicios", Toast.LENGTH_SHORT).show() }
        cardMedidas.setOnClickListener { Toast.makeText(context, "Medidas", Toast.LENGTH_SHORT).show() }
        cardCalendario.setOnClickListener { Toast.makeText(context, "Calendario", Toast.LENGTH_SHORT).show() }
    }

    private fun cambiarColorFiltros(botonSeleccionado: MaterialButton) {
        val colorInactivo = ContextCompat.getColor(mContext, R.color.gray)
        val textoInactivo = ContextCompat.getColor(mContext, R.color.text_primary)

        btnFiltroDuracion.setBackgroundColor(colorInactivo)
        btnFiltroDuracion.setTextColor(textoInactivo)

        btnFiltroVolumen.setBackgroundColor(colorInactivo)
        btnFiltroVolumen.setTextColor(textoInactivo)

        btnFiltroRepeticiones.setBackgroundColor(colorInactivo)
        btnFiltroRepeticiones.setTextColor(textoInactivo)

        val colorActivo = ContextCompat.getColor(mContext, R.color.red)
        val textoActivo = ContextCompat.getColor(mContext, R.color.white)

        botonSeleccionado.setBackgroundColor(colorActivo)
        botonSeleccionado.setTextColor(textoActivo)
    }
}