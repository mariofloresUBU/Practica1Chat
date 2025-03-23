package es.ubu.lsi.common;

import java.io.Serializable;

// defino los tipos posibles de mensajes que se pueden enviar por el chat
public enum MessageType implements Serializable {

    // cuando un cliente inicia sesion
    LOGIN,

    // cuando un cliente cierra sesion
    LOGOUT,

    // mensaje de texto normal entre usuarios
    MENSAJE,

    // mensaje del sistema (como "usuario conectado" o "usuario expulsado")
    SISTEMA,

    // mensaje privado dirigido a un usuario concreto
    PRIVADO;

    // podria anadir metodos auxiliares si hiciera falta
}
