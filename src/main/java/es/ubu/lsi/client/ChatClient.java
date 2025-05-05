package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

/**
 * INTERFAZ QUE DEFINE LAS OPERACIONES DE UN CLIENTE DE CHAT.
 * DEFINE EL CONTRATO QUE DEBEN CUMPLIR TODOS LOS CLIENTES QUE
 * QUIERAN CONECTARSE AL SISTEMA DE CHAT.
 *
 * @author MARIO FLORES
 * @version 1.0
 * @since MARZO 2025
 */
public interface ChatClient {
    /**
     * ESTABLECE UNA CONEXION CON EL SERVIDOR DE CHAT.
     * DEBE SER LLAMADO ANTES DE CUALQUIER OTRA OPERACION.
     */
    void conectar();

    /**
     * CIERRA LA CONEXION CON EL SERVIDOR DE CHAT.
     * LIBERA TODOS LOS RECURSOS ASOCIADOS A LA CONEXION.
     */
    void desconectar();

    /**
     * ENVIA UN MENSAJE AL SERVIDOR DE CHAT.
     * EL SERVIDOR DISTRIBUIRA EL MENSAJE SEGUN SU TIPO.
     *
     * @param mensaje EL MENSAJE A ENVIAR AL SERVIDOR
     */
    void enviarMensaje(ChatMessage mensaje);

    /**
     * ASIGNA UN LISTENER PARA RECIBIR MENSAJES.
     * ESTE LISTENER SERA NOTIFICADO CUANDO LLEGUEN NUEVOS MENSAJES.
     *
     * @param listener OBJETO QUE PROCESARA LOS MENSAJES RECIBIDOS
     */
    void setListener(ChatClientListener listener);

    /**
     * BLOQUEA A UN USUARIO PARA NO RECIBIR SUS MENSAJES.
     * NOTIFICA AL SERVIDOR SOBRE ESTE BLOQUEO Y ACTUALIZA
     * LA LISTA LOCAL DE USUARIOS BLOQUEADOS.
     *
     * @param usuario NOMBRE DEL USUARIO A BLOQUEAR
     */
    void bloquearUsuario(String usuario);

    /**
     * DESBLOQUEA A UN USUARIO PARA VOLVER A RECIBIR SUS MENSAJES.
     * NOTIFICA AL SERVIDOR SOBRE ESTE DESBLOQUEO Y ACTUALIZA
     * LA LISTA LOCAL DE USUARIOS BLOQUEADOS.
     *
     * @param usuario NOMBRE DEL USUARIO A DESBLOQUEAR
     */
    void desbloquearUsuario(String usuario);
}