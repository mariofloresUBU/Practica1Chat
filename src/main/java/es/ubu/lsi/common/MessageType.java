package es.ubu.lsi.common;

import java.io.Serializable;

/**
 * tipos de mensajes que se pueden enviar en el chat
 *
 * @author mario flores
 */
public enum MessageType implements Serializable {

    /**
     * cuando un cliente inicia sesion
     */
    LOGIN,

    /**
     * cuando un cliente cierra sesion
     */
    LOGOUT,

    /**
     * mensaje de texto normal entre usuarios
     */
    MENSAJE,

    /**
     * mensaje del sistema
     */
    SISTEMA,

    /**
     * mensaje privado dirigido a un usuario concreto
     */
    PRIVADO,

    /**
     * mensaje para bloquear a un usuario
     */
    BAN,

    /**
     * mensaje para desbloquear a un usuario
     */
    UNBAN;
}