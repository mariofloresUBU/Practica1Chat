package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * IMPLEMENTACION DEL CLIENTE DE CHAT.
 * PROPORCIONA LA FUNCIONALIDAD COMPLETA PARA CONECTARSE
 * AL SERVIDOR, ENVIAR Y RECIBIR MENSAJES, Y GESTIONAR
 * EL BLOQUEO DE USUARIOS.
 *
 * @author MARIO FLORES
 * @version 1.0
 * @since MARZO 2025
 */
public class ChatClientImpl implements ChatClient {

    /** DIRECCION POR DEFECTO DEL SERVIDOR */
    private static final String HOST = "localhost";

    /** PUERTO POR DEFECTO DEL SERVIDOR */
    private static final int PUERTO = 1500;

    /** SOCKET DE CONEXION CON EL SERVIDOR */
    private Socket socket;

    /** FLUJO DE ENTRADA PARA RECIBIR MENSAJES */
    private ObjectInputStream entrada;

    /** FLUJO DE SALIDA PARA ENVIAR MENSAJES */
    private ObjectOutputStream salida;

    /** LISTENER PARA NOTIFICAR MENSAJES RECIBIDOS */
    private ChatClientListener listener;

    /** NOMBRE DE USUARIO EN EL CHAT */
    private String nickname;

    /** CONJUNTO DE USUARIOS BLOQUEADOS */
    private Set<String> usuariosBloqueados;

    /** INDICA SI EL CLIENTE ESTA CONECTADO AL SERVIDOR */
    private boolean conectado;

    /**
     * CONSTRUCTOR POR DEFECTO.
     * INICIALIZA LAS ESTRUCTURAS DE DATOS NECESARIAS.
     */
    public ChatClientImpl() {
        this.usuariosBloqueados = new HashSet<>();
        this.conectado = false;
    }

    /**
     * CONSTRUCTOR CON NICKNAME.
     * INICIALIZA LAS ESTRUCTURAS Y ESTABLECE EL NICKNAME.
     *
     * @param nickname NOMBRE DE USUARIO PARA EL CHAT
     */
    public ChatClientImpl(String nickname) {
        this();
        this.nickname = nickname;
    }

    /**
     * ESTABLECE UNA CONEXION CON EL SERVIDOR.
     * CREA LOS FLUJOS DE ENTRADA/SALIDA Y UN HILO PARA
     * ESCUCHAR MENSAJES ENTRANTES.
     */
    @Override
    public void conectar() {
        try {
            // ME CONECTO AL SERVIDOR POR SOCKET
            socket = new Socket(HOST, PUERTO);
            conectado = true;

            // INICIALIZO LOS FLUJOS DE ENTRADA Y SALIDA
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());

            // CREO UN HILO PARA ESCUCHAR MENSAJES DEL SERVIDOR
            Thread receptor = new Thread(() -> {
                try {
                    ChatMessage mensaje;
                    while (conectado && (mensaje = (ChatMessage) entrada.readObject()) != null) {
                        // VERIFICO SI EL MENSAJE ES DE UN USUARIO BLOQUEADO
                        if (mensaje.getTipo() == MessageType.MENSAJE &&
                                usuariosBloqueados.contains(mensaje.getRemitente())) {
                            // SI LO ESTA, IGNORO EL MENSAJE
                            continue;
                        }

                        // CUANDO RECIBO UN MENSAJE, SE LO PASO AL LISTENER
                        if (listener != null) {
                            listener.onMensajeRecibido(mensaje);
                        }
                    }
                } catch (IOException e) {
                    if (conectado) {
                        System.out.println("CONEXION CERRADA POR EL SERVIDOR");
                        desconectar();
                    }
                } catch (ClassNotFoundException e) {
                    System.out.println("ERROR AL PROCESAR MENSAJE RECIBIDO");
                }
            });
            receptor.start();

            // ENVIO MENSAJE DE LOGIN
            if (nickname != null) {
                enviarMensaje(new ChatMessage(nickname, "conectandose", MessageType.LOGIN));
            }

            System.out.println("CONECTADO AL SERVIDOR");
        } catch (IOException e) {
            System.out.println("NO PUDE CONECTAR CON EL SERVIDOR");
            conectado = false;
        }
    }

    /**
     * CIERRA LA CONEXION CON EL SERVIDOR.
     * ENVIA UN MENSAJE DE LOGOUT Y LIBERA TODOS LOS RECURSOS.
     */
    @Override
    public void desconectar() {
        try {
            conectado = false;

            // ENVIO MENSAJE DE LOGOUT SI ESTOY CONECTADO
            if (socket != null && !socket.isClosed() && salida != null) {
                enviarMensaje(new ChatMessage(nickname, "desconectandose", MessageType.LOGOUT));
            }

            // CIERRO LOS RECURSOS
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("ERROR AL CERRAR LA CONEXION");
        }
    }

    /**
     * ENVIA UN MENSAJE AL SERVIDOR.
     * EL MENSAJE SE SERIALIZA Y SE ENVIA AL SERVIDOR.
     *
     * @param mensaje EL MENSAJE A ENVIAR
     */
    @Override
    public void enviarMensaje(ChatMessage mensaje) {
        if (!conectado) {
            System.out.println("NO ESTOY CONECTADO AL SERVIDOR");
            return;
        }

        try {
            salida.writeObject(mensaje);
            salida.flush();
        } catch (IOException e) {
            System.out.println("ERROR AL ENVIAR MENSAJE");
            desconectar();
        }
    }

    /**
     * CONFIGURA UN LISTENER PARA RECIBIR MENSAJES.
     * ESTABLECE EL OBJETO QUE SERA NOTIFICADO DE NUEVOS MENSAJES.
     *
     * @param listener OBJETO QUE PROCESARA LOS MENSAJES RECIBIDOS
     */
    @Override
    public void setListener(ChatClientListener listener) {
        this.listener = listener;
    }

    /**
     * BLOQUEA A UN USUARIO PARA NO RECIBIR SUS MENSAJES.
     * AÑADE AL USUARIO A LA LISTA DE BLOQUEADOS Y NOTIFICA AL SERVIDOR.
     *
     * @param usuario NOMBRE DEL USUARIO A BLOQUEAR
     */
    @Override
    public void bloquearUsuario(String usuario) {
        if (usuario == null || usuario.trim().isEmpty()) {
            return;
        }

        // AÑADO AL CONJUNTO DE BLOQUEADOS
        usuariosBloqueados.add(usuario);

        // INFORMO AL SERVIDOR DEL BLOQUEO
        if (conectado) {
            ChatMessage mensaje = new ChatMessage(
                    nickname,
                    nickname + " ha baneado a " + usuario,
                    MessageType.BAN,
                    usuario
            );
            enviarMensaje(mensaje);
        }

        System.out.println("USUARIO " + usuario + " BLOQUEADO");
    }

    /**
     * DESBLOQUEA A UN USUARIO PARA VOLVER A RECIBIR SUS MENSAJES.
     * ELIMINA AL USUARIO DE LA LISTA DE BLOQUEADOS Y NOTIFICA AL SERVIDOR.
     *
     * @param usuario NOMBRE DEL USUARIO A DESBLOQUEAR
     */
    @Override
    public void desbloquearUsuario(String usuario) {
        if (usuario == null || usuario.trim().isEmpty()) {
            return;
        }

        // ELIMINO DEL CONJUNTO DE BLOQUEADOS
        usuariosBloqueados.remove(usuario);

        // INFORMO AL SERVIDOR DEL DESBLOQUEO
        if (conectado) {
            ChatMessage mensaje = new ChatMessage(
                    nickname,
                    nickname + " ha desbaneado a " + usuario,
                    MessageType.UNBAN,
                    usuario
            );
            enviarMensaje(mensaje);
        }

        System.out.println("USUARIO " + usuario + " DESBLOQUEADO");
    }

    /**
     * VERIFICA SI UN USUARIO ESTA BLOQUEADO.
     *
     * @param usuario NOMBRE A VERIFICAR
     * @return TRUE SI ESTA BLOQUEADO, FALSE EN CASO CONTRARIO
     */
    public boolean estaUsuarioBloqueado(String usuario) {
        return usuariosBloqueados.contains(usuario);
    }

    /**
     * ESTABLECE EL NICKNAME DEL CLIENTE.
     *
     * @param nickname NUEVO NOMBRE PARA EL CLIENTE
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * OBTIENE EL NICKNAME ACTUAL.
     *
     * @return NOMBRE DE USUARIO
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * METODO PRINCIPAL PARA EJECUTAR EL CLIENTE EN MODO CONSOLA.
     * SOLICITA UN NICKNAME, CONFIGURA EL CLIENTE Y PROCESA COMANDOS.
     *
     * @param args ARGUMENTOS DE LINEA DE COMANDOS (HOST Y NICKNAME)
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String host = HOST;
        String nickname;

        // PROCESO LOS ARGUMENTOS SI EXISTEN
        if (args.length >= 2) {
            host = args[0];
            nickname = args[1];
        } else {
            // SOLICITO EL NICKNAME
            System.out.print("INTRODUCE TU NICKNAME: ");
            nickname = scanner.nextLine().trim();
        }

        // CREO E INICIO EL CLIENTE
        ChatClientImpl cliente = new ChatClientImpl(nickname);

        // ASIGNO EL LISTENER QUE IMPRIME LOS MENSAJES RECIBIDOS
        cliente.setListener(mensaje -> {
            String tipo = mensaje.getTipo().toString();
            String remitente = mensaje.getRemitente();
            String contenido = mensaje.getContenido();

            // MARIO FLORES PATROCINA EL MENSAJE:
            System.out.println("MARIO FLORES PATROCINA EL MENSAJE:");

            // FORMATO SEGUN EL TIPO DE MENSAJE
            switch (mensaje.getTipo()) {
                case MENSAJE:
                    System.out.println("[" + remitente + "]: " + contenido);
                    break;
                case SISTEMA:
                    System.out.println("[SISTEMA]: " + contenido);
                    break;
                case PRIVADO:
                    System.out.println("[PRIVADO de " + remitente + "]: " + contenido);
                    break;
                case LOGIN:
                    System.out.println("USUARIO " + remitente + " SE HA CONECTADO");
                    break;
                case LOGOUT:
                    System.out.println("USUARIO " + remitente + " SE HA DESCONECTADO");
                    break;
                default:
                    System.out.println("[" + tipo + "] " + remitente + ": " + contenido);
            }
        });

        // ME CONECTO AL SERVIDOR
        cliente.conectar();

        // EXPLICO LOS COMANDOS DISPONIBLES
        System.out.println("\nCOMANDOS DISPONIBLES:");
        System.out.println("  /msg <usuario> <mensaje> - ENVIAR MENSAJE PRIVADO");
        System.out.println("  /ban <usuario> - BLOQUEAR MENSAJES DE UN USUARIO");
        System.out.println("  /unban <usuario> - DESBLOQUEAR MENSAJES DE UN USUARIO");
        System.out.println("  /logout - SALIR DEL CHAT");
        System.out.println("\nESCRIBE TUS MENSAJES:");

        // LEO EL TEXTO DEL USUARIO EN BUCLE
        String texto;
        boolean continuar = true;

        while (continuar && cliente.conectado) {
            texto = scanner.nextLine();

            // PROCESAMIENTO DE COMANDOS
            if (texto.startsWith("/")) {
                String[] partes = texto.split("\\s+", 3); // DIVIDO EN MÁXIMO 3 PARTES

                switch (partes[0].toLowerCase()) {
                    case "/logout":
                        continuar = false;
                        break;

                    case "/ban":
                        if (partes.length > 1) {
                            cliente.bloquearUsuario(partes[1]);
                            System.out.println("USUARIO " + partes[1] + " HA SIDO BLOQUEADO");
                        } else {
                            System.out.println("USO: /ban <usuario>");
                        }
                        break;

                    case "/unban":
                        if (partes.length > 1) {
                            cliente.desbloquearUsuario(partes[1]);
                            System.out.println("USUARIO " + partes[1] + " HA SIDO DESBLOQUEADO");
                        } else {
                            System.out.println("USO: /unban <usuario>");
                        }
                        break;

                    case "/msg":
                        if (partes.length > 2) {
                            ChatMessage msgPrivado = new ChatMessage(
                                    nickname, partes[2], MessageType.PRIVADO, partes[1]);
                            cliente.enviarMensaje(msgPrivado);
                        } else {
                            System.out.println("USO: /msg <usuario> <mensaje>");
                        }
                        break;

                    default:
                        System.out.println("COMANDO DESCONOCIDO: " + partes[0]);
                }
            } else if (!texto.trim().isEmpty()) {
                // MENSAJE NORMAL
                ChatMessage mensaje = new ChatMessage(nickname, texto, MessageType.MENSAJE);
                cliente.enviarMensaje(mensaje);
            }
        }

        // CIERRO EL SCANNER Y DESCONECTO
        scanner.close();
        cliente.desconectar();
        System.out.println("SESION FINALIZADA");
    }
}