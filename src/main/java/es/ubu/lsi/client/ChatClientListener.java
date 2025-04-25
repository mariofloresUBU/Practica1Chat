package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

/**
 * interfaz para recibir notificaciones de mensajes del chat
 *
 * @author mario flores
 */
public interface ChatClientListener {
    /**
     * metodo que se llama cuando se recibe un mensaje del servidor
     *
     * @param mensaje el mensaje recibido
     */
    void onMensajeRecibido(ChatMessage mensaje);
}