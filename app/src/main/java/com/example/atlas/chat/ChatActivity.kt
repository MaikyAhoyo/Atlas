package com.example.atlas.chat

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.atlas.adaptadores.AdaptadorChat
import com.example.atlas.Constantes
import com.example.atlas.R
import com.example.atlas.modelos.Chat
import com.example.atlas.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

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

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        uid = intent.getStringExtra("uid")!!
        miUid = firebaseAuth.uid!!

        chatRuta = Constantes.rutaChat(uid, miUid)

        FirebaseDatabase.getInstance().getReference("Usuarios")
            .child(miUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    miNombre = "${snapshot.child("nombres").value}"
                }
                override fun onCancelled(error: DatabaseError) {}
            })

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

        cargarInfo()
        cargarMensajes()
    }

    private fun cargarMensajes() {
        val mensajesArrayList = ArrayList<Chat>()
        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.child(chatRuta)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    mensajesArrayList.clear()
                    for (ds: DataSnapshot in snapshot.children) {
                        try {
                            val chat = ds.getValue(Chat::class.java)!!

                            if (chat.receptorUid == miUid && !chat.leido) {
                                ds.ref.child("leido").setValue(true)
                            }

                            mensajesArrayList.add(chat)
                        } catch (e: Exception) {
                            Log.e("FirebaseError", "Error al cargar los mensajes: ${e.message}")
                        }
                    }

                    val adaptadorChat = AdaptadorChat(this@ChatActivity, mensajesArrayList)
                    binding.chatsRV.adapter = adaptadorChat
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Error al cargar los mensajes: ${error.message}")
                }
            })
    }

    private fun cargarInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = "${snapshot.child("nombres").value}"
                    val imagen = "${snapshot.child("urlImagenPerfil").value}"

                    tokenReceptor = "${snapshot.child("fcmToken").value}"

                    binding.TxtNombreUsuario.text = nombres

                    try {
                        Glide.with(applicationContext)
                            .load(imagen)
                            .placeholder(R.drawable.ic_imagen_perfil)
                            .into(binding.ToolbarIV)
                    } catch (e: Exception) {
                        Log.e("ChatActivity", "${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Error: ${error.message}")
                }
            })
    }

    private fun imagenGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultadoGaleriaARL.launch(intent)
    }

    private val resultadoGaleriaARL =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            if (resultado.resultCode == Activity.RESULT_OK) {
                val data = resultado.data
                imagenUri = data!!.data
                subirImgStorage()
            } else {
                Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show()
            }
        }

    private val solicitarPermisoAlmacenamiento =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { esConcecido ->
            if (esConcecido) {
                imagenGaleria()
            } else {
                Toast.makeText(this, "El permiso de almacenamiento no ha sido concedido", Toast.LENGTH_SHORT).show()
            }
        }

    private fun subirImgStorage() {
        progressDialog.setMessage("Subiendo imagen")
        progressDialog.show()

        val tiempo = Constantes.obtenerTiempoDis()
        val nombreRutaImg = "ImagenesChat/$tiempo"
        val storageRef = FirebaseStorage.getInstance().getReference(nombreRutaImg)
        storageRef.putFile(imagenUri!!)
            .addOnSuccessListener { taskSnapshot ->
                val uriTask = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val urlImagen = uriTask.result.toString()
                if (uriTask.isSuccessful) {
                    enviarMensaje(Constantes.MENSAJE_TIPO_IMAGEN, urlImagen, tiempo)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "No se pudo enviar la imagen debido a ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validarMensaje() {
        val mensaje = binding.EtMensajeChat.text.toString().trim()
        val tiempo = Constantes.obtenerTiempoDis()

        if (mensaje.isEmpty()) {
            Toast.makeText(this, "Ingrese un mensaje", Toast.LENGTH_SHORT).show()
        } else {
            enviarMensaje(Constantes.MENSAJE_TIPO_TEXTO, mensaje, tiempo)
        }
    }

    private fun enviarMensaje(tipoMensaje: String, mensaje: String, tiempo: Long) {
        progressDialog.setMessage("Enviando mensaje")
        progressDialog.show()

        val refChat = FirebaseDatabase.getInstance().getReference("Chats")
        val keyId = "${refChat.push().key}"
        val hashMap = HashMap<String, Any>()

        hashMap["idMensaje"] = "$keyId"
        hashMap["tipoMensaje"] = "$tipoMensaje"
        hashMap["mensaje"] = "$mensaje"
        hashMap["emisorUid"] = "$miUid"
        hashMap["receptorUid"] = "$uid"
        hashMap["tiempo"] = tiempo
        hashMap["leido"] = false

        refChat.child(chatRuta)
            .child(keyId)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()

                val mensajeNotif = if (tipoMensaje == Constantes.MENSAJE_TIPO_IMAGEN) {
                    "Te envió una imagen"
                } else {
                    mensaje
                }

                prepararNotificacion(mensajeNotif, miNombre)

                binding.EtMensajeChat.setText("")
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "No se pudo enviar el mensaje debido a ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun prepararNotificacion(mensaje: String, nombreEmisor: String) {
        Log.d("NotifDebug", "tokenReceptor: $tokenReceptor")
        if (tokenReceptor.isEmpty() || tokenReceptor == "null") return

        Thread {
            try {
                val accessToken = obtenerAccessToken()
                enviarNotificacionV1(mensaje, accessToken, nombreEmisor)
            } catch (e: Exception) {
                Log.e("NotifError", "Error obteniendo token: ${e.message}")
            }
        }.start()
    }

    private fun obtenerAccessToken(): String {
        val stream = assets.open("service_account.json")
        val credentials = com.google.auth.oauth2.GoogleCredentials
            .fromStream(stream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }

    private fun enviarNotificacionV1(mensaje: String, accessToken: String, nombreEmisor: String) {
        val projectId = "atlas-2e732"

        val queue = com.android.volley.toolbox.Volley.newRequestQueue(this)
        val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

        val jsonMessage = org.json.JSONObject().apply {
            put("token", tokenReceptor)
            put("notification", org.json.JSONObject().apply {
                put("title", nombreEmisor)
                put("body", mensaje)
            })
        }

        val jsonBody = org.json.JSONObject().apply {
            put("message", jsonMessage)
        }

        val request = object : com.android.volley.toolbox.JsonObjectRequest(
            Method.POST, url, jsonBody,
            { Log.d("NotifSuccess", "¡Notificación enviada!") },
            { error -> Log.e("NotifError", "Fallo: ${error.message}") }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Content-Type" to "application/json"
                )
            }
        }

        queue.add(request)
    }
}