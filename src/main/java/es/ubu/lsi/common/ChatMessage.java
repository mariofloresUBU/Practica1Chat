package es.ubu.lsi.common;

import java.io.Serializable;

/**
 * clase que representa un mensaje en el chat
 *
 * @author mario flores
 */
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    private String remitente;
    private String contenido;
    private MessageType tipo;
    private String destinatario; // para mensajes privados

    /**
     * constructor para mensajes normales
     *
     * @param remitente quien envia el mensaje
     * @param contenido texto del mensaje
     * @param tipo tipo del mensaje (ver MessageType)
     */
    public ChatMessage(String remitente, String contenido, MessageType tipo) {
        this.remitente = remitente;
        this.contenido = contenido;
        this.tipo = tipo;
        this.destinatario = null; // no hay destinatario específico
    }

    /**
     * constructor para mensajes privados o comandos especificos
     *
     * @param remitente quien envia el mensaje
     * @param contenido texto del mensaje
     * @param tipo tipo del mensaje (ver MessageType)
     * @param destinatario usuario al que va dirigido el mensaje
     */
    public ChatMessage(String remitente, String contenido, MessageType tipo, String destinatario) {
        this.remitente = remitente;
        this.contenido = contenido;
        this.tipo = tipo;
        this.destinatario = destinatario;
    }

    /**
     * obtengo el remitente del mensaje
     *
     * @return nombre del remitente
     */
    public String getRemitente() {
        return remitente;
    }

    /**
     * obtengo el contenido del mensaje
     *
     * @return texto del mensaje
     */
    public String getContenido() {
        return contenido;
    }

    /**
     * obtengo el tipo del mensaje
     *
     * @return tipo de mensaje (LOGIN, LOGOUT, etc)
     */
    public MessageType getTipo() {
        return tipo;
    }

    /**
     * obtengo el destinatario del mensaje
     *
     * @return destinatario (null si es mensaje global)
     */
    public String getDestinatario() {
        return destinatario;
    }
}