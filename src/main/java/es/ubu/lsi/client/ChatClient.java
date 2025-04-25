package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

/**
 * interfaz que define las operaciones de un cliente de chat
 *
 * @author mario flores
 */
public interface ChatClient {
    /**
     * me conecto al servidor de chat
     */
    void conectar();

    /**
     * cierro la conexion con el servidor
     */
    void desconectar();

    /**
     * envio un mensaje al servidor
     *
     * @param mensaje el mensaje a enviar
     */
    void enviarMensaje(ChatMessage mensaje);

    /**
     * asigno un listener para recibir mensajes
     *
     * @param listener objeto que procesara los mensajes recibidos
     */
    void setListener(ChatClientListener listener);

    /**
     * bloqueo a un usuario para no recibir sus mensajes
     *
     * @param usuario nombre del usuario a bloquear
     */
    void bloquearUsuario(String usuario);

    /**
     * desbloqueo a un usuario para volver a recibir sus mensajes
     *
     * @param usuario nombre del usuario a desbloquear
     */
    void desbloquearUsuario(String usuario);
}