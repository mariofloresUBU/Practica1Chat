package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * hilo para manejar cada cliente conectado al servidor
 *
 * @author mario flores
 */
public class ServerThreadForClient extends Thread {

    private Socket socket;
    private ChatServerImpl servidor;
    private ObjectInputStream entrada;
    private ObjectOutputStream salida;
    private String nickname;
    private Set<String> usuariosBloqueados;
    private boolean conectado;

    /**
     * constructor del hilo para cada cliente
     *
     * @param socket socket de conexion con el cliente
     * @param servidor referencia al servidor principal
     */
    public ServerThreadForClient(Socket socket, ChatServerImpl servidor) {
        this.socket = socket;
        this.servidor = servidor;
        this.usuariosBloqueados = new HashSet<>();
        this.conectado = true;
    }

    /**
     * tarea principal del hilo: leer mensajes del cliente
     */
    @Override
    public void run() {
        try {
            // creo los flujos de entrada y salida
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());

            // leo los mensajes que llegan desde el cliente
            ChatMessage mensaje;
            while (conectado && (mensaje = (ChatMessage) entrada.readObject()) != null) {
                // guardo el nickname del cliente si es el primer mensaje
                if (nickname == null && mensaje.getRemitente() != null) {
                    nickname = mensaje.getRemitente();
                }

                // proceso segun el tipo de mensaje
                switch (mensaje.getTipo()) {
                    case LOGIN:
                        procesarLogin(mensaje);
                        break;

                    case LOGOUT:
                        procesarLogout(mensaje);
                        break;

                    case PRIVADO:
                        procesarMensajePrivado(mensaje);
                        break;

                    case BAN:
                        procesarBan(mensaje);
                        break;

                    case UNBAN:
                        procesarUnban(mensaje);
                        break;

                    case MENSAJE:
                    default:
                        // reenvio el mensaje a todos
                        servidor.broadcast(mensaje);
                        break;
                }
            }
        } catch (IOException e) {
            // el cliente se desconecto abruptamente o hubo un error
            System.out.println("cliente " + nickname + " desconectado o error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("error al procesar mensaje: " + e.getMessage());
        } finally {
            cerrarConexion();
            servidor.eliminarCliente(this);
        }
    }

    /**
     * proceso un mensaje de login
     *
     * @param mensaje mensaje de login recibido
     */
    private void procesarLogin(ChatMessage mensaje) {
        // notifico a todos los usuarios de la nueva conexion
        ChatMessage notificacion = new ChatMessage(
                "Server",
                "El usuario " + mensaje.getRemitente() + " se ha conectado",
                MessageType.SISTEMA
        );
        servidor.broadcast(notificacion);
    }

    /**
     * proceso un mensaje de logout
     *
     * @param mensaje mensaje de logout recibido
     */
    private void procesarLogout(ChatMessage mensaje) {
        // marco como desconectado para salir del bucle de lectura
        conectado = false;
    }

    /**
     * proceso un mensaje privado
     *
     * @param mensaje mensaje privado recibido
     */
    private void procesarMensajePrivado(ChatMessage mensaje) {
        // intento entregar el mensaje
        boolean entregado = servidor.enviarMensajePrivado(mensaje);

        // si no se pudo entregar, notifico al remitente
        if (!entregado) {
            ChatMessage error = new ChatMessage(
                    "Server",
                    "No se pudo entregar tu mensaje. El usuario " +
                            mensaje.getDestinatario() + " no existe o te ha bloqueado.",
                    MessageType.SISTEMA,
                    mensaje.getRemitente()
            );
            enviarMensaje(error);
        }
    }

    /**
     * proceso un mensaje de bloqueo
     *
     * @param mensaje mensaje de bloqueo recibido
     */
    private void procesarBan(ChatMessage mensaje) {
        String bloqueado = mensaje.getContenido();
        bloquearUsuario(bloqueado);
        servidor.bloquearUsuario(nickname, bloqueado);
    }

    /**
     * proceso un mensaje de desbloqueo
     *
     * @param mensaje mensaje de desbloqueo recibido
     */
    private void procesarUnban(ChatMessage mensaje) {
        String desbloqueado = mensaje.getContenido();
        desbloquearUsuario(desbloqueado);
        servidor.desbloquearUsuario(nickname, desbloqueado);
    }

    /**
     * envio un mensaje al cliente
     *
     * @param mensaje el mensaje a enviar
     */
    public void enviarMensaje(ChatMessage mensaje) {
        if (!conectado) {
            return;
        }

        try {
            salida.writeObject(mensaje);
            salida.flush();
        } catch (IOException e) {
            System.out.println("no pude enviar mensaje a " + nickname + ": " + e.getMessage());
            conectado = false;
        }
    }

    /**
     * cierro la conexion con el cliente
     */
    public void cerrarConexion() {
        try {
            conectado = false;

            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("error al cerrar la conexion del cliente " + nickname);
        }
    }

    /**
     * bloqueo a un usuario para este cliente
     *
     * @param usuario usuario a bloquear
     */
    public void bloquearUsuario(String usuario) {
        if (usuario != null && !usuario.trim().isEmpty()) {
            usuariosBloqueados.add(usuario);
        }
    }

    /**
     * desbloqueo a un usuario para este cliente
     *
     * @param usuario usuario a desbloquear
     */
    public void desbloquearUsuario(String usuario) {
        if (usuario != null) {
            usuariosBloqueados.remove(usuario);
        }
    }

    /**
     * verifico si un usuario esta bloqueado por este cliente
     *
     * @param usuario usuario a verificar
     * @return true si esta bloqueado, false en caso contrario
     */
    public boolean tieneUsuarioBloqueado(String usuario) {
        return usuariosBloqueados.contains(usuario);
    }

    /**
     * obtengo el nickname del cliente
     *
     * @return nickname del cliente
     */
    public String getNickname() {
        return nickname;
    }
}