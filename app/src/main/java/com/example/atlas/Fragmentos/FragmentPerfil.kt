package com.example.atlas.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.atlas.OpcionesLogin
import com.example.atlas.R
import com.example.atlas.databinding.FragmentPerfilBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth

class FragmentPerfil : Fragment() {

    private lateinit var binding: FragmentPerfilBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var mContext: Context

    private lateinit var txtUsername: TextView
    private lateinit var txtEntrenos: TextView
    private lateinit var txtSeguidores: TextView
    private lateinit var txtSiguiendo: TextView
    private lateinit var imgPerfil: ShapeableImageView

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
        // Buscamos los componentes por su ID
        txtUsername = view.findViewById(R.id.txtUsername)
        txtEntrenos = view.findViewById(R.id.txtNumEntrenos)
        txtSeguidores = view.findViewById(R.id.txtNumSeguidores)
        txtSiguiendo = view.findViewById(R.id.txtNumSiguiendo)
        imgPerfil = view.findViewById(R.id.imgPerfil)

        cardEstadisticas = view.findViewById(R.id.cardEstadisticas)
        cardEjercicios = view.findViewById(R.id.cardEjercicios)
        cardMedidas = view.findViewById(R.id.cardMedidas)
        cardCalendario = view.findViewById(R.id.cardCalendario)
    }

    private fun cargarDatosDeUsuario() {
        // DATOS SIMULADOS
        val nombreUsuario = "Maiky Ahoyo"
        val numEntrenos = 12
        val numSeguidores = 150
        val numSiguiendo = 45

        txtUsername.text = nombreUsuario
        txtEntrenos.text = numEntrenos.toString()
        txtSeguidores.text = numSeguidores.toString()
        txtSiguiendo.text = numSiguiendo.toString()

        // Imagen placeholder
        imgPerfil.setImageResource(R.drawable.ic_launcher_background)
    }

    private fun configurarBotones() {
        cardEstadisticas.setOnClickListener {
            Toast.makeText(context, "Abriendo Estadísticas...", Toast.LENGTH_SHORT).show()
        }

        cardEjercicios.setOnClickListener {
            Toast.makeText(context, "Abriendo Ejercicios...", Toast.LENGTH_SHORT).show()
        }

        cardMedidas.setOnClickListener {
            Toast.makeText(context, "Abriendo Medidas...", Toast.LENGTH_SHORT).show()
        }

        cardCalendario.setOnClickListener {
            Toast.makeText(context, "Abriendo Calendario...", Toast.LENGTH_SHORT).show()
        }
    }
}