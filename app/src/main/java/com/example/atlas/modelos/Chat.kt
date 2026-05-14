package com.example.atlas.modelos

class Chat {

    // Atributos
    var idMensaje : String = ""
    var tipoMensaje : String = ""
    var mensaje : String = ""
    var emisorUid : String = ""
    var receptorUid : String = ""
    var tiempo : Long = 0

    var leido: Boolean = false

    // Constructor vacio
    constructor()

    // Constructor con todos los atributos
    constructor(idMensaje: String, tipoMensaje: String, mensaje: String, emisorUid: String, receptorUid: String, tiempo: Long) {
        this.idMensaje = idMensaje
        this.tipoMensaje = tipoMensaje
        this.mensaje = mensaje
        this.emisorUid = emisorUid
        this.receptorUid = receptorUid
        this.tiempo = tiempo
    }
}