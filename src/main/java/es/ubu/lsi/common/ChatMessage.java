package es.ubu.lsi.common;

import java.io.Serializable;

/**
 * CLASE QUE REPRESENTA UN MENSAJE EN EL CHAT.
 * ENCAPSULA TODA LA INFORMACION NECESARIA PARA TRANSMITIR
 * UN MENSAJE ENTRE CLIENTE Y SERVIDOR.
 *
 * @author MARIO FLORES
 * @version 1.0
 * @since MARZO 2025
 */
public class ChatMessage implements Serializable {

    /** IDENTIFICADOR DE VERSION PARA SERIALIZACION */
    private static final long serialVersionUID = 1L;

    /** NOMBRE DEL USUARIO QUE ENVIA EL MENSAJE */
    private String remitente;

    /** CONTENIDO DEL MENSAJE */
    private String contenido;

    /** TIPO DE MENSAJE (LOGIN, LOGOUT, MENSAJE, ETC.) */
    private MessageType tipo;

    /** DESTINATARIO DEL MENSAJE (PARA MENSAJES PRIVADOS) */
    private String destinatario;

    /**
     * CONSTRUCTOR PARA MENSAJES NORMALES.
     * CREA UN MENSAJE SIN DESTINATARIO ESPECIFICO.
     *
     * @param remitente QUIEN ENVIA EL MENSAJE
     * @param contenido TEXTO DEL MENSAJE
     * @param tipo TIPO DEL MENSAJE (VER MESSAGETYPE)
     */
    public ChatMessage(String remitente, String contenido, MessageType tipo) {
        this.remitente = remitente;
        this.contenido = contenido;
        this.tipo = tipo;
        this.destinatario = null; // NO HAY DESTINATARIO ESPECIFICO
    }

    /**
     * CONSTRUCTOR PARA MENSAJES PRIVADOS O COMANDOS ESPECIFICOS.
     * CREA UN MENSAJE CON UN DESTINATARIO ESPECIFICO.
     *
     * @param remitente QUIEN ENVIA EL MENSAJE
     * @param contenido TEXTO DEL MENSAJE
     * @param tipo TIPO DEL MENSAJE (VER MESSAGETYPE)
     * @param destinatario USUARIO AL QUE VA DIRIGIDO EL MENSAJE
     */
    public ChatMessage(String remitente, String contenido, MessageType tipo, String destinatario) {
        this.remitente = remitente;
        this.contenido = contenido;
        this.tipo = tipo;
        this.destinatario = destinatario;
    }

    /**
     * OBTIENE EL REMITENTE DEL MENSAJE.
     *
     * @return NOMBRE DEL REMITENTE
     */
    public String getRemitente() {
        return remitente;
    }

    /**
     * OBTIENE EL CONTENIDO DEL MENSAJE.
     *
     * @return TEXTO DEL MENSAJE
     */
    public String getContenido() {
        return contenido;
    }

    /**
     * OBTIENE EL TIPO DEL MENSAJE.
     *
     * @return TIPO DE MENSAJE (LOGIN, LOGOUT, ETC.)
     */
    public MessageType getTipo() {
        return tipo;
    }

    /**
     * OBTIENE EL DESTINATARIO DEL MENSAJE.
     *
     * @return DESTINATARIO (NULL SI ES MENSAJE GLOBAL)
     */
    public String getDestinatario() {
        return destinatario;
    }
}