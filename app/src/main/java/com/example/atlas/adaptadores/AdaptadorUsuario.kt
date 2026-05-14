package com.example.atlas.adaptadores

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.atlas.R
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.atlas.chat.ChatActivity
import com.example.atlas.modelos.Usuario

class AdaptadorUsuario (
    val context: Context, val
    listaUsuarios: List<Usuario>
) : RecyclerView.Adapter<AdaptadorUsuario.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.item_usuario, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaUsuarios.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usuario = listaUsuarios[position]
        holder.uid.text = usuario.uid
        holder.nombres.text = usuario.nombres
        holder.email.text = usuario.email

        Glide.with(context)
            .load(usuario.urlImagenPerfil)
            .placeholder(R.drawable.ic_imagen_perfil)
            .into(holder.imagen)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("uid", holder.uid.text)
            Toast.makeText(context,
                "Has seleccionado al usuario: ${holder.nombres.text}",
                Toast.LENGTH_SHORT).show()
            context.startActivity(intent)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var uid : TextView
        var email : TextView
        var nombres : TextView
        var imagen : ImageView

        init {
            uid = itemView.findViewById(R.id.item_uid)
            email = itemView.findViewById(R.id.item_email)
            nombres = itemView.findViewById(R.id.item_nombre)
            imagen = itemView.findViewById(R.id.item_imagen)
        }
    }
}