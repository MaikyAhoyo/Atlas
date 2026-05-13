package com.example.atlas.fragmentos

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atlas.adaptadores.AdaptadorUsuario
import com.example.atlas.modelos.Usuario
import com.example.atlas.databinding.FragmentChatsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FragmentChats : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!

    private lateinit var mContext: Context
    private var usuarioAdaptador: AdaptadorUsuario? = null
    private val usuarioLista: MutableList<Usuario> = mutableListOf()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)

        binding.RVUsuarios.setHasFixedSize(true)
        binding.RVUsuarios.layoutManager = LinearLayoutManager(mContext)

        binding.EtBuscarUsuario.doOnTextChanged { texto, _, _, _ ->
            val busqueda = texto.toString().trim()
            if (busqueda.isNotEmpty()) {
                buscarUsuario(busqueda)
            } else {
                listarUsuarios()
            }
        }

        listarUsuarios()

        return binding.root
    }

    private fun listarUsuarios() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val firebaseUser = currentUser.uid
        val reference = FirebaseDatabase.getInstance().reference
            .child("Usuarios")
            .orderByChild("nombres")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (binding.EtBuscarUsuario.text.toString().trim().isNotEmpty()) {
                    return
                }

                usuarioLista.clear()

                for (sn in snapshot.children) {
                    val usuario: Usuario? = sn.getValue(Usuario::class.java)
                    if (usuario != null && usuario.uid != firebaseUser) {
                        usuarioLista.add(usuario)
                    }
                }

                actualizarVistaDeLista()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error al cargar usuarios: ${error.message}")
                Toast.makeText(mContext, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun buscarUsuario(usuario: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val firebaseUser = currentUser.uid
        val reference = FirebaseDatabase.getInstance().reference
            .child("Usuarios")
            .orderByChild("nombres")
            .startAt(usuario)
            .endAt(usuario + "\uf8ff")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usuarioLista.clear()

                for (ss in snapshot.children) {
                    val user: Usuario? = ss.getValue(Usuario::class.java)
                    if (user != null && user.uid != firebaseUser) {
                        usuarioLista.add(user)
                    }
                }

                actualizarVistaDeLista()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error al buscar usuarios: ${error.message}")
                Toast.makeText(mContext, "Error al buscar", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun actualizarVistaDeLista() {
        if (usuarioLista.isEmpty()) {
            binding.tvSinUsuarios.visibility = View.VISIBLE
            binding.RVUsuarios.visibility = View.GONE
        } else {
            binding.tvSinUsuarios.visibility = View.GONE
            binding.RVUsuarios.visibility = View.VISIBLE

            usuarioAdaptador = AdaptadorUsuario(requireContext(), usuarioLista)
            binding.RVUsuarios.adapter = usuarioAdaptador
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}