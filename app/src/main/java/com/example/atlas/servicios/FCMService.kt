package com.example.atlas.servicios

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.atlas.MainActivity // Asegúrate que este sea el nombre de tu actividad principal
import com.example.atlas.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            mostrarNotificacion(it.title, it.body)
        }
    }

    private fun mostrarNotificacion(titulo: String?, mensaje: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val canalId = "Canal_Atlas_ID"
        val notificationBuilder = NotificationCompat.Builder(this, canalId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "Notificaciones de Atlas", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(canal)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}