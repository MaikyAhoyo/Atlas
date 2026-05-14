package com.example.atlas.chat

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import org.json.JSONObject

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

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

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
    }

    private fun obtenerMiNombre() {
        FirebaseDatabase.getInstance().getReference("Usuarios").child(miUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    miNombre = "${snapshot.child("nombres").value}"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun verificarBloqueoYEscucharMensajes() {
        // Solo escuchamos si YO he bloqueado al otro
        val refMiBloqueo = FirebaseDatabase.getInstance().getReference("Bloqueados").child(miUid).child(uid)

        refMiBloqueo.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val yoLoBloquee = snapshot.exists()

                val tiempoBloqueo = if (yoLoBloquee) {
                    snapshot.child("tiempo").getValue(Long::class.java) ?: 0L
                } else {
                    Long.MAX_VALUE
                }

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

                    // Si el mensaje es del otro y se envió después de ser bloqueado, NO se añade a la lista
                    if (chat.emisorUid == uid && chat.tiempo >= tiempoBloqueo) {
                        continue
                    }

                    if (chat.receptorUid == miUid && !chat.leido) {
                        ds.ref.child("leido").setValue(true)
                    }
                    mensajesList.add(chat)
                }

                binding.chatsRV.adapter = AdaptadorChat(this@ChatActivity, mensajesList)
                if (mensajesList.isNotEmpty()) {
                    binding.chatsRV.scrollToPosition(mensajesList.size - 1)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun aplicarEstadoBloqueoUI(bloqueado: Boolean) {
        if (bloqueado) {
            binding.tvBloqueado.visibility = View.VISIBLE
            binding.EtMensajeChat.isEnabled = false
            binding.adjuntarFAB.isEnabled = false
            binding.adjuntarFAB.alpha = 0.4f
            binding.enviarFAB.isEnabled = false
            binding.enviarFAB.alpha = 0.4f
        } else {
            binding.tvBloqueado.visibility = View.GONE
            binding.EtMensajeChat.isEnabled = true
            binding.EtMensajeChat.hint = "Escribe un mensaje..."
            binding.adjuntarFAB.isEnabled = true
            binding.adjuntarFAB.alpha = 1f
            binding.enviarFAB.isEnabled = true
            binding.enviarFAB.alpha = 1f
        }
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
            ref.removeValue().addOnSuccessListener {
                Toast.makeText(this, "Usuario desbloqueado", Toast.LENGTH_SHORT).show()
            }
        } else {
            val datosBloqueo = mapOf("tiempo" to System.currentTimeMillis())
            ref.setValue(datosBloqueo).addOnSuccessListener {
                Toast.makeText(this, "Usuario bloqueado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarInfo() {
        FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = "${snapshot.child("nombres").value}"
                    val imagen = "${snapshot.child("urlImagenPerfil").value}"
                    tokenReceptor = "${snapshot.child("fcmToken").value}"

                    binding.TxtNombreUsuario.text = nombres
                    Glide.with(this@ChatActivity)
                        .load(imagen)
                        .placeholder(R.drawable.ic_imagen_perfil)
                        .into(binding.ToolbarIV)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun validarMensaje() {
        val mensaje = binding.EtMensajeChat.text.toString().trim()
        if (mensaje.isEmpty()) {
            Toast.makeText(this, "Ingrese un mensaje", Toast.LENGTH_SHORT).show()
        } else {
            enviarMensaje(Constantes.MENSAJE_TIPO_TEXTO, mensaje, System.currentTimeMillis())
        }
    }

    private fun enviarMensaje(tipoMensaje: String, mensaje: String, tiempo: Long) {
        val refChat = FirebaseDatabase.getInstance().getReference("Chats").child(chatRuta)
        val keyId = refChat.push().key ?: return

        // Ver si estamos bloqueados
        val refBloqueoReceptor = FirebaseDatabase.getInstance().getReference("Bloqueados").child(uid).child(miUid)

        refBloqueoReceptor.get().addOnSuccessListener { snapshot ->
            val estamosBloqueados = snapshot.exists()

            val hashMap = HashMap<String, Any>()
            hashMap["idMensaje"] = keyId
            hashMap["tipoMensaje"] = tipoMensaje
            hashMap["mensaje"] = mensaje
            hashMap["emisorUid"] = miUid
            hashMap["receptorUid"] = uid
            hashMap["tiempo"] = tiempo
            hashMap["leido"] = false

            // El mensaje se guarda
            refChat.child(keyId).setValue(hashMap)
                .addOnSuccessListener {
                    if (progressDialog.isShowing) progressDialog.dismiss()
                    binding.EtMensajeChat.setText("")

                    // SOLO enviamos notificación si NO estamos bloqueados
                    if (!estamosBloqueados) {
                        val mensajeNotif = if (tipoMensaje == Constantes.MENSAJE_TIPO_IMAGEN) "Te envió una imagen" else mensaje
                        prepararNotificacion(mensajeNotif)
                    }
                }
                .addOnFailureListener { e ->
                    if (progressDialog.isShowing) progressDialog.dismiss()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun imagenGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultadoGaleriaARL.launch(intent)
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
        if (imagenUri == null) return
        progressDialog.setMessage("Subiendo imagen...")
        progressDialog.show()

        val tiempo = System.currentTimeMillis()
        val storageRef = FirebaseStorage.getInstance().getReference("ImagenesChat/$tiempo")

        storageRef.putFile(imagenUri!!)
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                storageRef.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    enviarMensaje(Constantes.MENSAJE_TIPO_IMAGEN, task.result.toString(), tiempo)
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error al subir", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // NOTIFICACIONES

    private fun prepararNotificacion(mensaje: String) {
        if (tokenReceptor.isEmpty() || tokenReceptor == "null") return
        Thread {
            try {
                val accessToken = obtenerAccessToken()
                enviarNotificacionV1(mensaje, accessToken)
            } catch (e: Exception) {
                Log.e("NotifError", "${e.message}")
            }
        }.start()
    }

    private fun obtenerAccessToken(): String {
        val stream = assets.open("service_account.json")
        val credentials = GoogleCredentials.fromStream(stream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }

    private fun enviarNotificacionV1(mensaje: String, accessToken: String) {
        val projectId = "atlas-2e732"
        val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

        val jsonBody = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", tokenReceptor)
                put("notification", JSONObject().apply {
                    put("title", miNombre)
                    put("body", mensaje)
                })
            })
        }

        val request = object : JsonObjectRequest(Method.POST, url, jsonBody,
            { Log.d("FCM", "Notificación enviada con éxito") },
            { Log.e("FCM", "Error enviando notificación") }
        ) {
            override fun getHeaders(): MutableMap<String, String> =
                mutableMapOf("Authorization" to "Bearer $accessToken", "Content-Type" to "application/json")
        }
        Volley.newRequestQueue(this).add(request)
    }
}