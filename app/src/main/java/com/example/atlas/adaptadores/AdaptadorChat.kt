package com.example.atlas.adaptadores

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.atlas.Constantes
import com.example.atlas.modelos.Chat
import com.example.atlas.R
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AdaptadorChat : RecyclerView.Adapter<AdaptadorChat.HolderChat> {

    private val context : Context
    private val chatArray : ArrayList<Chat>
    private val firebaseAuth : FirebaseAuth
    private var chatRuta = ""
    private var mediaPlayer: MediaPlayer? = null
    private var playingPosition: Int = -1 // Rastrea qué audio se está reproduciendo

    companion object {
        private const val MENSAJE_IZQUIERDO = 0
        private const val MENSAJE_DERECHO = 1
    }

    constructor(context: Context, chatArray: ArrayList<Chat>) {
        this.context = context
        this.chatArray = chatArray
        firebaseAuth = FirebaseAuth.getInstance()
    }

    inner class HolderChat(itemView : View) : RecyclerView.ViewHolder(itemView) {
        var Tv_mensaje : TextView = itemView.findViewById(R.id.Tv_mensaje)
        var Iv_mensaje : ShapeableImageView = itemView.findViewById(R.id.Iv_mensaje)
        var Tv_tiempo_mensaje : TextView = itemView.findViewById(R.id.Tv_tiempo_mensaje)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderChat {
        return if (viewType == MENSAJE_DERECHO) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_chat_derecho, parent, false)
            HolderChat(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_chat_izquierdo, parent, false)
            HolderChat(view)
        }
    }

    override fun getItemCount(): Int {
        return chatArray.size
    }

    override fun onBindViewHolder(holder: HolderChat, position: Int) {
        val modeloChat = chatArray[position]
        val mensaje = modeloChat.mensaje
        val tipoMensaje = modeloChat.tipoMensaje
        val tiempo = modeloChat.tiempo

        holder.Tv_tiempo_mensaje.text = Constantes.obtenerFechaHora(tiempo)

        when (tipoMensaje) {
            Constantes.MENSAJE_TIPO_TEXTO -> {
                holder.Tv_mensaje.visibility = View.VISIBLE
                holder.Iv_mensaje.visibility = View.GONE
                holder.Tv_mensaje.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                holder.Tv_mensaje.text = mensaje

                if (modeloChat.emisorUid == firebaseAuth.uid) {
                    holder.itemView.setOnClickListener {
                        mostrarDialogoEliminar(position, holder, modeloChat)
                    }
                }
            }
            Constantes.MENSAJE_TIPO_AUDIO -> {
                holder.Tv_mensaje.visibility = View.VISIBLE
                holder.Iv_mensaje.visibility = View.GONE
                holder.Tv_mensaje.text = "Mensaje de voz"

                // Cambiar ícono dinámicamente según si este ítem se está reproduciendo
                val iconRes = if (playingPosition == position) R.drawable.ic_pause else R.drawable.ic_play
                holder.Tv_mensaje.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
                holder.Tv_mensaje.compoundDrawablePadding = 15

                holder.itemView.setOnClickListener {
                    if (playingPosition == position) {
                        pausarAudio()
                    } else {
                        reproducirAudio(mensaje, position)
                    }
                }

                if (modeloChat.emisorUid == firebaseAuth.uid) {
                    holder.itemView.setOnLongClickListener {
                        mostrarDialogoEliminar(position, holder, modeloChat, true)
                        true
                    }
                }
            }
            else -> { // Tipo IMAGEN
                holder.Tv_mensaje.visibility = View.GONE
                holder.Iv_mensaje.visibility = View.VISIBLE
                holder.Tv_mensaje.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

                Glide.with(context)
                    .load(mensaje)
                    .placeholder(R.drawable.img_enviada)
                    .error(R.drawable.img_perfil)
                    .into(holder.Iv_mensaje)

                holder.itemView.setOnClickListener {
                    val opciones = if (modeloChat.emisorUid == firebaseAuth.uid) {
                        arrayOf<CharSequence>("Eliminar imagen", "Ver imagen", "Cancelar")
                    } else {
                        arrayOf<CharSequence>("Ver imagen", "Cancelar")
                    }

                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("¿Qué desea realizar?")
                    builder.setItems(opciones) { _, which ->
                        when {
                            opciones[which] == "Eliminar imagen" -> eliminarMensaje(position, holder, modeloChat)
                            opciones[which] == "Ver imagen" -> visualizadorImagen(mensaje)
                        }
                    }
                    builder.show()
                }
            }
        }
    }

    private fun reproducirAudio(url: String, position: Int) {
        try {
            val oldPosition = playingPosition
            playingPosition = position

            // Actualizar iconos de los elementos afectados
            if (oldPosition != -1) notifyItemChanged(oldPosition)
            notifyItemChanged(playingPosition)

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    val finishedPos = playingPosition
                    playingPosition = -1
                    notifyItemChanged(finishedPos)
                }
                setOnErrorListener { _, _, _ ->
                    playingPosition = -1
                    notifyItemChanged(position)
                    false
                }
            }
        } catch (e: Exception) {
            playingPosition = -1
            notifyItemChanged(position)
        }
    }

    private fun pausarAudio() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                val oldPos = playingPosition
                playingPosition = -1
                notifyItemChanged(oldPos)
            }
        } catch (e: Exception) {
            Log.e("Audio", "Error al pausar: ${e.message}")
        }
    }

    private fun mostrarDialogoEliminar(position: Int, holder: HolderChat, modeloChat: Chat, esAudio: Boolean = false) {
        val tipo = if (esAudio) "audio" else "mensaje"
        val opciones = arrayOf<CharSequence>("Eliminar $tipo", "Cancelar")
        val builder = AlertDialog.Builder(context)
        builder.setTitle("¿Qué deseas realizar?")
        builder.setItems(opciones) { _, which ->
            if (which == 0) eliminarMensaje(position, holder, modeloChat)
        }
        builder.show()
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatArray[position].emisorUid == firebaseAuth.uid) MENSAJE_DERECHO else MENSAJE_IZQUIERDO
    }

    private fun eliminarMensaje(position: Int, holder : HolderChat, modeloChat : Chat) {
        chatRuta = Constantes.rutaChat(modeloChat.receptorUid, modeloChat.emisorUid)
        FirebaseDatabase.getInstance().reference.child("Chats")
            .child(chatRuta).child(modeloChat.idMensaje)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Eliminado correctamente", Toast.LENGTH_SHORT).show()
            }
    }

    private fun visualizadorImagen(imagen : String) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.visualizador_img)
        val pv: PhotoView = dialog.findViewById(R.id.PV_img)
        val btnCerrar: MaterialButton = dialog.findViewById(R.id.BtnCerrarVisualizador)

        Glide.with(context).load(imagen).placeholder(R.drawable.img_enviada).into(pv)
        btnCerrar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}