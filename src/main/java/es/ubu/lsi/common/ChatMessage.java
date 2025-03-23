package es.ubu.lsi.common;

import java.io.Serializable;

public class ChatMessage implements Serializable {

    private String remitente;
    private String contenido;
    private MessageType tipo;

    // constructor con los tres campos
    public ChatMessage(String remitente, String contenido, MessageType tipo) {
        this.remitente = remitente;
        this.contenido = contenido;
        this.tipo = tipo;
    }

    // getter para el remitente
    public String getRemitente() {
        return remitente;
    }

    // getter para el contenido
    public String getContenido() {
        return contenido;
    }

    // getter para el tipo de mensaje
    public MessageType getTipo() {
        return tipo;
    }
}
