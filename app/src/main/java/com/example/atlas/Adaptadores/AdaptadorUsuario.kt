package com.example.atlas.Adaptadores

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView
import com.example.atlas.R
import androidx.recyclerview.widget.RecyclerView
import com.example.atlas.Modelos.Usuario

class AdaptadorUsuario (val context: Context, val listaUsuarios: List<Usuario>)
    : RecyclerView.Adapter<AdaptadorUsuario.ViewHolder>(){
        
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdaptadorUsuario.ViewHolder {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: AdaptadorUsuario.ViewHolder, position: Int) {
        TODO("Not yet implemented")
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