package es.ubu.lsi.server;

/**
 * interfaz para el servidor de chat
 *
 * @author mario flores
 */
public interface ChatServer {
    /**
     * arranco el servidor para empezar a recibir conexiones
     */
    void startup();

    /**
     * detengo el servidor y cierro todas las conexiones
     */
    void shutdown();
}