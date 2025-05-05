package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * HILO PARA MANEJAR CADA CLIENTE CONECTADO AL SERVIDOR.
 * GESTIONA LA COMUNICACION CON UN CLIENTE ESPECIFICO,
 * PROCESANDO LOS MENSAJES RECIBIDOS Y ENVIANDO LOS MENSAJES.
 *
 * @author MARIO FLORES
 * @version 1.0
 * @since MARZO 2025
 */
public class ServerThreadForClient extends Thread {

    /** SOCKET DE CONEXION CON EL CLIENTE */
    private Socket socket;

    /** REFERENCIA AL SERVIDOR PRINCIPAL */
    private ChatServerImpl servidor;

    /** FLUJO DE ENTRADA PARA RECIBIR MENSAJES */
    private ObjectInputStream entrada;

    /** FLUJO DE SALIDA PARA ENVIAR MENSAJES */
    private ObjectOutputStream salida;

    /** NOMBRE DE USUARIO DEL CLIENTE */
    private String nickname;

    /** CONJUNTO DE USUARIOS BLOQUEADOS POR ESTE CLIENTE */
    private Set<String> usuariosBloqueados;

    /** INDICA SI EL CLIENTE ESTA CONECTADO */
    private boolean conectado;

    /**
     * CONSTRUCTOR DEL HILO PARA CADA CLIENTE.
     * INICIALIZA LOS RECURSOS NECESARIOS PARA LA COMUNICACION.
     *
     * @param socket SOCKET DE CONEXION CON EL CLIENTE
     * @param servidor REFERENCIA AL SERVIDOR PRINCIPAL
     */
    public ServerThreadForClient(Socket socket, ChatServerImpl servidor) {
        this.socket = socket;
        this.servidor = servidor;
        this.usuariosBloqueados = new HashSet<>();
        this.conectado = true;
    }

    /**
     * TAREA PRINCIPAL DEL HILO: LEER MENSAJES DEL CLIENTE.
     * PROCESA LOS MENSAJES RECIBIDOS SEGUN SU TIPO.
     */
    @Override
    public void run() {
        try {
            // CREO LOS FLUJOS DE ENTRADA Y SALIDA
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());

            // LEO LOS MENSAJES QUE LLEGAN DESDE EL CLIENTE
            ChatMessage mensaje;
            while (conectado && (mensaje = (ChatMessage) entrada.readObject()) != null) {
                // GUARDO EL NICKNAME DEL CLIENTE SI ES EL PRIMER MENSAJE
                if (nickname == null && mensaje.getRemitente() != null) {
                    nickname = mensaje.getRemitente();
                }

                // PROCESO SEGUN EL TIPO DE MENSAJE
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
                        // REENVIO EL MENSAJE A TODOS
                        servidor.broadcast(mensaje);
                        break;
                }
            }
        } catch (IOException e) {
            // EL CLIENTE SE DESCONECTO ABRUPTAMENTE O HUBO UN ERROR
            System.out.println("CLIENTE " + nickname + " DESCONECTADO O ERROR: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("ERROR AL PROCESAR MENSAJE: " + e.getMessage());
        } finally {
            cerrarConexion();
            servidor.eliminarCliente(this);
        }
    }

    /**
     * PROCESO UN MENSAJE DE LOGIN.
     * NOTIFICA A TODOS LOS USUARIOS DE LA NUEVA CONEXION.
     *
     * @param mensaje MENSAJE DE LOGIN RECIBIDO
     */
    private void procesarLogin(ChatMessage mensaje) {
        // NOTIFICO A TODOS LOS USUARIOS DE LA NUEVA CONEXION
        ChatMessage notificacion = new ChatMessage(
                "Server",
                "EL USUARIO " + mensaje.getRemitente() + " SE HA CONECTADO",
                MessageType.SISTEMA
        );
        servidor.broadcast(notificacion);
    }

    /**
     * PROCESO UN MENSAJE DE LOGOUT.
     * MARCA AL CLIENTE COMO DESCONECTADO.
     *
     * @param mensaje MENSAJE DE LOGOUT RECIBIDO
     */
    private void procesarLogout(ChatMessage mensaje) {
        // MARCO COMO DESCONECTADO PARA SALIR DEL BUCLE DE LECTURA
        conectado = false;
    }

    /**
     * PROCESO UN MENSAJE PRIVADO.
     * INTENTA ENTREGAR EL MENSAJE AL DESTINATARIO.
     *
     * @param mensaje MENSAJE PRIVADO RECIBIDO
     */
    private void procesarMensajePrivado(ChatMessage mensaje) {
        // INTENTO ENTREGAR EL MENSAJE
        boolean entregado = servidor.enviarMensajePrivado(mensaje);

        // SI NO SE PUDO ENTREGAR, NOTIFICO AL REMITENTE
        if (!entregado) {
            ChatMessage error = new ChatMessage(
                    "Server",
                    "NO SE PUDO ENTREGAR TU MENSAJE. EL USUARIO " +
                            mensaje.getDestinatario() + " NO EXISTE O TE HA BLOQUEADO.",
                    MessageType.SISTEMA,
                    mensaje.getRemitente()
            );
            enviarMensaje(error);
        }
    }

    /**
     * PROCESO UN MENSAJE DE BLOQUEO.
     * REGISTRA EL BLOQUEO Y NOTIFICA A TODOS LOS USUARIOS.
     *
     * @param mensaje MENSAJE DE BLOQUEO RECIBIDO
     */
    private void procesarBan(ChatMessage mensaje) {
        String bloqueado = mensaje.getDestinatario();
        bloquearUsuario(bloqueado);
        servidor.bloquearUsuario(nickname, bloqueado);

        // NOTIFICO A TODOS LOS USUARIOS
        ChatMessage notificacion = new ChatMessage(
                "Server",
                mensaje.getContenido(),
                MessageType.SISTEMA
        );
        servidor.broadcast(notificacion);
    }

    /**
     * PROCESO UN MENSAJE DE DESBLOQUEO.
     * ELIMINA EL BLOQUEO Y NOTIFICA A TODOS LOS USUARIOS.
     *
     * @param mensaje MENSAJE DE DESBLOQUEO RECIBIDO
     */
    private void procesarUnban(ChatMessage mensaje) {
        String desbloqueado = mensaje.getDestinatario();
        desbloquearUsuario(desbloqueado);
        servidor.desbloquearUsuario(nickname, desbloqueado);

        // NOTIFICO A TODOS LOS USUARIOS
        ChatMessage notificacion = new ChatMessage(
                "Server",
                mensaje.getContenido(),
                MessageType.SISTEMA
        );
        servidor.broadcast(notificacion);
    }

    /**
     * ENVIO UN MENSAJE AL CLIENTE.
     * EL MENSAJE SE SERIALIZA Y SE ENVIA AL CLIENTE.
     *
     * @param mensaje EL MENSAJE A ENVIAR
     */
    public void enviarMensaje(ChatMessage mensaje) {
        if (!conectado) {
            return;
        }

        try {
            salida.writeObject(mensaje);
            salida.flush();
        } catch (IOException e) {
            System.out.println("NO PUDE ENVIAR MENSAJE A " + nickname + ": " + e.getMessage());
            conectado = false;
        }
    }

    /**
     * CIERRO LA CONEXION CON EL CLIENTE.
     * LIBERA TODOS LOS RECURSOS ASOCIADOS A LA CONEXION.
     */
    public void cerrarConexion() {
        try {
            conectado = false;

            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("ERROR AL CERRAR LA CONEXION DEL CLIENTE " + nickname);
        }
    }

    /**
     * BLOQUEO A UN USUARIO PARA ESTE CLIENTE.
     * AÑADE AL USUARIO A LA LISTA DE BLOQUEADOS.
     *
     * @param usuario USUARIO A BLOQUEAR
     */
    public void bloquearUsuario(String usuario) {
        if (usuario != null && !usuario.trim().isEmpty()) {
            usuariosBloqueados.add(usuario);
        }
    }

    /**
     * DESBLOQUEO A UN USUARIO PARA ESTE CLIENTE.
     * ELIMINA AL USUARIO DE LA LISTA DE BLOQUEADOS.
     *
     * @param usuario USUARIO A DESBLOQUEAR
     */
    public void desbloquearUsuario(String usuario) {
        if (usuario != null) {
            usuariosBloqueados.remove(usuario);
        }
    }

    /**
     * VERIFICO SI UN USUARIO ESTA BLOQUEADO POR ESTE CLIENTE.
     *
     * @param usuario USUARIO A VERIFICAR
     * @return TRUE SI ESTA BLOQUEADO, FALSE EN CASO CONTRARIO
     */
    public boolean tieneUsuarioBloqueado(String usuario) {
        return usuariosBloqueados.contains(usuario);
    }

    /**
     * OBTENGO EL NICKNAME DEL CLIENTE.
     *
     * @return NICKNAME DEL CLIENTE
     */
    public String getNickname() {
        return nickname;
    }
}