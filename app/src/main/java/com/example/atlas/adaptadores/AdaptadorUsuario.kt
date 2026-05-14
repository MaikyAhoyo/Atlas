package com.example.atlas.adaptadores

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.atlas.Constantes
import com.example.atlas.R
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.atlas.chat.ChatActivity
import com.example.atlas.modelos.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdaptadorUsuario(
    val context: Context,
    val listaUsuarios: List<Usuario>
) : RecyclerView.Adapter<AdaptadorUsuario.ViewHolder>() {

    private val miUid = FirebaseAuth.getInstance().uid!!

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_usuario, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = listaUsuarios.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usuario = listaUsuarios[position]
        holder.uid.text = usuario.uid
        holder.nombres.text = usuario.nombres
        holder.email.text = usuario.email

        Glide.with(context)
            .load(usuario.urlImagenPerfil)
            .placeholder(R.drawable.ic_imagen_perfil)
            .into(holder.imagen)

        // Badge de mensajes no leídos
        val chatRuta = Constantes.rutaChat(usuario.uid, miUid)
        FirebaseDatabase.getInstance()
            .getReference("Chats")
            .child(chatRuta)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val noLeidos = snapshot.children.count { ds ->
                        val leido = ds.child("leido").getValue(Boolean::class.java) ?: true
                        val receptorUid = ds.child("receptorUid").getValue(String::class.java) ?: ""
                        !leido && receptorUid == miUid
                    }
                    if (noLeidos > 0) {
                        holder.badge.visibility = View.VISIBLE
                        holder.badge.text = if (noLeidos > 9) "9+" else "$noLeidos"
                    } else {
                        holder.badge.visibility = View.GONE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Verificar si el usuario está bloqueado
        FirebaseDatabase.getInstance()
            .getReference("Bloqueados")
            .child(miUid)
            .child(usuario.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    holder.bloqueado.visibility = if (snapshot.exists()) View.VISIBLE else View.GONE
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("uid", holder.uid.text)
            context.startActivity(intent)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val uid: TextView = itemView.findViewById(R.id.item_uid)
        val email: TextView = itemView.findViewById(R.id.item_email)
        val nombres: TextView = itemView.findViewById(R.id.item_nombre)
        val imagen: ImageView = itemView.findViewById(R.id.item_imagen)
        val badge: TextView = itemView.findViewById(R.id.item_badge)
        val bloqueado: ImageView = itemView.findViewById(R.id.item_bloqueado)
    }
}