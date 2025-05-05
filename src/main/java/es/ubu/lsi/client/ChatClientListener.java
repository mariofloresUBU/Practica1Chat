package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

/**
 * INTERFAZ PARA RECIBIR NOTIFICACIONES DE MENSAJES DEL CHAT.
 * DEFINE EL MECANISMO DE CALLBACK QUE SE UTILIZARA CUANDO
 * SE RECIBAN NUEVOS MENSAJES DEL SERVIDOR.
 *
 * @author MARIO FLORES
 * @version 1.0
 * @since MARZO 2025
 */
public interface ChatClientListener {
    /**
     * METODO QUE SE LLAMA CUANDO SE RECIBE UN MENSAJE DEL SERVIDOR.
     * ESTE METODO ES INVOCADO POR EL CLIENTE CADA VEZ QUE
     * RECIBE UN NUEVO MENSAJE DEL SERVIDOR.
     *
     * @param mensaje EL MENSAJE RECIBIDO DEL SERVIDOR
     */
    void onMensajeRecibido(ChatMessage mensaje);
}