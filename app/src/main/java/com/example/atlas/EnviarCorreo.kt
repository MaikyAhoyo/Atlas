package com.example.atlas

import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EnviarCorreo {

    private const val MI_CORREO = "vee.rodzn@gmail.com"
    private const val MI_PASSWORD_APP = "fkgs qeym ypsg dlpd"

    fun enviar(receptor: String, passUsuario: String) {

        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.socketFactory.port", "465")
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            put("mail.smtp.auth", "true")
            put("mail.smtp.port", "465")
        }

        val session = Session.getDefaultInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(MI_CORREO, MI_PASSWORD_APP)
            }
        })

        Thread {
            try {
                val mm = MimeMessage(session)
                mm.setFrom(InternetAddress(MI_CORREO))
                mm.addRecipient(Message.RecipientType.TO, InternetAddress(receptor))
                mm.subject = "Bienvenido a Atlas"
                mm.setText("Hola,\n\nTu cuenta ha sido creada con éxito.\n\n" +
                        "Usuario: $receptor\n" +
                        "Contraseña: $passUsuario\n\n" +
                        "¡Bienvenido a la comunidad!")

                Transport.send(mm)

            } catch (e: MessagingException) {
                e.printStackTrace()
            }
        }.start()
    }
}