package com.example.atlas.chat

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.atlas.Constantes
import com.example.atlas.R
import com.example.atlas.adaptadores.AdaptadorChat
import com.example.atlas.databinding.ActivityChatBinding
import com.example.atlas.modelos.Chat
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private var uid = ""
    private var tokenReceptor = ""
    private var miNombre = ""

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var miUid = ""

    private var chatRuta = ""
    private var imagenUri: Uri? = null

    // Grabación de audio
    private var mediaRecorder: MediaRecorder? = null
    private var audioPath: String = ""
    private var estaGrabando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        miUid = firebaseAuth.uid ?: ""
        uid = intent.getStringExtra("uid") ?: ""

        if (uid.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        chatRuta = Constantes.rutaChat(uid, miUid)

        progressDialog = ProgressDialog(this).apply {
            setTitle("Espere por favor")
            setCanceledOnTouchOutside(false)
        }

        obtenerMiNombre()
        configurarListeners()
        cargarInfo()
        verificarBloqueoYEscucharMensajes()
    }

    private fun configurarListeners() {
        binding.adjuntarFAB.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                imagenGaleria()
            } else {
                solicitarPermisoAlmacenamiento.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        binding.IbRegresar.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.enviarFAB.setOnClickListener {
            validarMensaje()
        }

        binding.IbBloquear.setOnClickListener {
            mostrarMenuBloqueo()
        }

        binding.audioFAB.setOnClickListener {
            if (estaGrabando) detenerGrabacion() else verificarPermisosAudio()
        }
    }

    private fun verificarPermisosAudio() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            iniciarGrabacion()
        } else {
            solicitarPermisoAudio.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    private val solicitarPermisoAudio = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) iniciarGrabacion() else Toast.makeText(this, "Permiso de audio denegado", Toast.LENGTH_SHORT).show()
    }

    private fun iniciarGrabacion() {
        val cacheDir = externalCacheDir ?: return
        // Nombre único para evitar AccessDeniedException por archivos bloqueados
        audioPath = "${cacheDir.absolutePath}/audio_${System.currentTimeMillis()}.m4a"

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()
        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioPath)
            try {
                prepare()
                start()
                estaGrabando = true
                actualizarUIStatusGrabando(true)
            } catch (e: IOException) {
                Log.e("AudioGrab", "Error: ${e.message}")
            }
        }
    }

    private fun detenerGrabacion() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            estaGrabando = false
            actualizarUIStatusGrabando(false)
            subirAudioStorage()
        } catch (e: Exception) {
            Log.e("AudioGrab", "Error al detener: ${e.message}")
            estaGrabando = false
            actualizarUIStatusGrabando(false)
        }
    }

    private fun actualizarUIStatusGrabando(grabando: Boolean) {
        if (grabando) {
            binding.audioFAB.backgroundTintList = ColorStateList.valueOf(Color.RED)
            binding.audioFAB.setImageResource(R.drawable.ic_enviar_chat) // Usar icono de stop/enviar
            binding.EtMensajeChat.hint = "Grabando..."
            binding.EtMensajeChat.isEnabled = false
        } else {
            binding.audioFAB.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_background))
            binding.audioFAB.setImageResource(R.drawable.ic_voice_msg)
            binding.EtMensajeChat.hint = "Escribe un mensaje..."
            binding.EtMensajeChat.isEnabled = true
        }
    }

    private fun subirAudioStorage() {
        val file = File(audioPath)
        if (!file.exists()) return

        progressDialog.setMessage("Enviando audio...")
        progressDialog.show()

        val tiempo = System.currentTimeMillis()
        val storageRef = FirebaseStorage.getInstance().getReference("AudiosChat/$tiempo.m4a")

        storageRef.putFile(Uri.fromFile(file))
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                storageRef.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    enviarMensaje(Constantes.MENSAJE_TIPO_AUDIO, task.result.toString(), tiempo)
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error al subir audio", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun obtenerMiNombre() {
        FirebaseDatabase.getInstance().getReference("Usuarios").child(miUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    miNombre = snapshot.child("nombres").value.toString()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun verificarBloqueoYEscucharMensajes() {
        val refMiBloqueo = FirebaseDatabase.getInstance().getReference("Bloqueados").child(miUid).child(uid)
        refMiBloqueo.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val yoLoBloquee = snapshot.exists()
                val tiempoBloqueo = if (yoLoBloquee) snapshot.child("tiempo").getValue(Long::class.java) ?: 0L else Long.MAX_VALUE
                aplicarEstadoBloqueoUI(yoLoBloquee)
                cargarMensajes(tiempoBloqueo)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun cargarMensajes(tiempoBloqueo: Long) {
        val refMensajes = FirebaseDatabase.getInstance().getReference("Chats").child(chatRuta)
        refMensajes.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mensajesList = ArrayList<Chat>()
                for (ds in snapshot.children) {
                    val chat = ds.getValue(Chat::class.java) ?: continue
                    if (chat.emisorUid == uid && chat.tiempo >= tiempoBloqueo) continue
                    if (chat.receptorUid == miUid && !chat.leido) ds.ref.child("leido").setValue(true)
                    mensajesList.add(chat)
                }
                binding.chatsRV.adapter = AdaptadorChat(this@ChatActivity, mensajesList)
                if (mensajesList.isNotEmpty()) binding.chatsRV.scrollToPosition(mensajesList.size - 1)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun aplicarEstadoBloqueoUI(bloqueado: Boolean) {
        val alpha = if (bloqueado) 0.4f else 1f
        binding.tvBloqueado.visibility = if (bloqueado) View.VISIBLE else View.GONE
        binding.EtMensajeChat.isEnabled = !bloqueado
        binding.adjuntarFAB.isEnabled = !bloqueado
        binding.adjuntarFAB.alpha = alpha
        binding.audioFAB.isEnabled = !bloqueado
        binding.audioFAB.alpha = alpha
        binding.enviarFAB.isEnabled = !bloqueado
        binding.enviarFAB.alpha = alpha
    }

    private fun mostrarMenuBloqueo() {
        val popup = android.widget.PopupMenu(this, binding.IbBloquear)
        val ref = FirebaseDatabase.getInstance().getReference("Bloqueados").child(miUid).child(uid)
        ref.get().addOnSuccessListener { snapshot ->
            val titulo = if (snapshot.exists()) "Desbloquear usuario" else "Bloquear usuario"
            popup.menu.add(titulo)
            popup.setOnMenuItemClickListener {
                toggleBloqueo(snapshot.exists())
                true
            }
            popup.show()
        }
    }

    private fun toggleBloqueo(actualmenteBloqueado: Boolean) {
        val ref = FirebaseDatabase.getInstance().getReference("Bloqueados").child(miUid).child(uid)
        if (actualmenteBloqueado) {
            ref.removeValue().addOnSuccessListener { Toast.makeText(this, "Usuario desbloqueado", Toast.LENGTH_SHORT).show() }
        } else {
            ref.setValue(mapOf("tiempo" to System.currentTimeMillis())).addOnSuccessListener {
                Toast.makeText(this, "Usuario bloqueado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarInfo() {
        FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    tokenReceptor = snapshot.child("fcmToken").value.toString()
                    binding.TxtNombreUsuario.text = snapshot.child("nombres").value.toString()
                    Glide.with(this@ChatActivity)
                        .load(snapshot.child("urlImagenPerfil").value.toString())
                        .placeholder(R.drawable.ic_imagen_perfil)
                        .into(binding.ToolbarIV)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun validarMensaje() {
        val mensaje = binding.EtMensajeChat.text.toString().trim()
        if (mensaje.isNotEmpty()) enviarMensaje(Constantes.MENSAJE_TIPO_TEXTO, mensaje, System.currentTimeMillis())
    }

    private fun enviarMensaje(tipoMensaje: String, mensaje: String, tiempo: Long) {
        val refChat = FirebaseDatabase.getInstance().getReference("Chats").child(chatRuta)
        val keyId = refChat.push().key ?: return

        FirebaseDatabase.getInstance().getReference("Bloqueados").child(uid).child(miUid).get().addOnSuccessListener { snapshot ->
            val estamosBloqueados = snapshot.exists()
            val hashMap = hashMapOf(
                "idMensaje" to keyId, "tipoMensaje" to tipoMensaje, "mensaje" to mensaje,
                "emisorUid" to miUid, "receptorUid" to uid, "tiempo" to tiempo, "leido" to false
            )

            refChat.child(keyId).setValue(hashMap).addOnSuccessListener {
                if (progressDialog.isShowing) progressDialog.dismiss()
                binding.EtMensajeChat.setText("")
                if (!estamosBloqueados) {
                    val mNotif = when(tipoMensaje) {
                        Constantes.MENSAJE_TIPO_IMAGEN -> "Te envió una imagen"
                        Constantes.MENSAJE_TIPO_AUDIO -> "Te envió un audio"
                        else -> mensaje
                    }
                    prepararNotificacion(mNotif)
                }
            }
        }
    }

    private fun imagenGaleria() {
        resultadoGaleriaARL.launch(Intent(Intent.ACTION_PICK).apply { type = "image/*" })
    }

    private val resultadoGaleriaARL = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            imagenUri = res.data?.data
            subirImgStorage()
        }
    }

    private val solicitarPermisoAlmacenamiento = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) imagenGaleria() else Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    private fun subirImgStorage() {
        val uri = imagenUri ?: return
        progressDialog.setMessage("Subiendo imagen...")
        progressDialog.show()

        val tiempo = System.currentTimeMillis()
        val storageRef = FirebaseStorage.getInstance().getReference("ImagenesChat/$tiempo")
        storageRef.putFile(uri).continueWithTask { task ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) enviarMensaje(Constantes.MENSAJE_TIPO_IMAGEN, task.result.toString(), tiempo)
            else { progressDialog.dismiss(); Toast.makeText(this, "Error al subir", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun prepararNotificacion(mensaje: String) {
        if (tokenReceptor.isEmpty() || tokenReceptor == "null") return
        lifecycleScope.launch {
            try {
                val token = withContext(Dispatchers.IO) { obtenerAccessToken() }
                enviarNotificacionV1(mensaje, token)
            } catch (e: Exception) {
                Log.e("NotifError", "${e.message}")
            }
        }
    }

    private suspend fun obtenerAccessToken(): String = withContext(Dispatchers.IO) {
        val stream = assets.open("service_account.json")
        val credentials = GoogleCredentials.fromStream(stream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        credentials.refreshIfExpired()
        credentials.accessToken.tokenValue
    }

    private fun enviarNotificacionV1(mensaje: String, accessToken: String) {
        val url = "https://fcm.googleapis.com/v1/projects/atlas-2e732/messages:send"
        val jsonBody = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", tokenReceptor)
                put("notification", JSONObject().apply { put("title", miNombre); put("body", mensaje) })
            })
        }
        val request = object : JsonObjectRequest(Method.POST, url, jsonBody, {}, {}) {
            override fun getHeaders() = mutableMapOf("Authorization" to "Bearer $accessToken", "Content-Type" to "application/json")
        }
        Volley.newRequestQueue(this).add(request)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaRecorder = null
    }
}