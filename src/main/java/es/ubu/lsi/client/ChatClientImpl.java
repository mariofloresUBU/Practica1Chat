package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * implementacion del cliente de chat
 *
 * @author mario flores
 */
public class ChatClientImpl implements ChatClient {

    private static final String HOST = "localhost";
    private static final int PUERTO = 1500;

    private Socket socket;
    private ObjectInputStream entrada;
    private ObjectOutputStream salida;
    private ChatClientListener listener;
    private String nickname;
    private Set<String> usuariosBloqueados;
    private boolean conectado;

    /**
     * constructor por defecto
     */
    public ChatClientImpl() {
        this.usuariosBloqueados = new HashSet<>();
        this.conectado = false;
    }

    /**
     * constructor con nickname
     *
     * @param nickname nombre de usuario para el chat
     */
    public ChatClientImpl(String nickname) {
        this();
        this.nickname = nickname;
    }

    /**
     * establezco una conexion con el servidor
     */
    @Override
    public void conectar() {
        try {
            // me conecto al servidor por socket
            socket = new Socket(HOST, PUERTO);
            conectado = true;

            // inicializo los flujos de entrada y salida
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());

            // creo un hilo para escuchar mensajes del servidor
            Thread receptor = new Thread(() -> {
                try {
                    ChatMessage mensaje;
                    while (conectado && (mensaje = (ChatMessage) entrada.readObject()) != null) {
                        // verifico si el mensaje es de un usuario bloqueado
                        if (mensaje.getTipo() == MessageType.MENSAJE &&
                                usuariosBloqueados.contains(mensaje.getRemitente())) {
                            // si lo esta, ignoro el mensaje
                            continue;
                        }

                        // cuando recibo un mensaje, se lo paso al listener
                        if (listener != null) {
                            listener.onMensajeRecibido(mensaje);
                        }
                    }
                } catch (IOException e) {
                    if (conectado) {
                        System.out.println("conexion cerrada por el servidor");
                        desconectar();
                    }
                } catch (ClassNotFoundException e) {
                    System.out.println("error al procesar mensaje recibido");
                }
            });
            receptor.start();

            // envio mensaje de login
            if (nickname != null) {
                enviarMensaje(new ChatMessage(nickname, "conectandose", MessageType.LOGIN));
            }

            System.out.println("conectado al servidor");
        } catch (IOException e) {
            System.out.println("no pude conectar con el servidor");
            conectado = false;
        }
    }

    /**
     * cierro la conexion con el servidor
     */
    @Override
    public void desconectar() {
        try {
            conectado = false;

            // envio mensaje de logout si estoy conectado
            if (socket != null && !socket.isClosed() && salida != null) {
                enviarMensaje(new ChatMessage(nickname, "desconectandose", MessageType.LOGOUT));
            }

            // cierro los recursos
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("error al cerrar la conexion");
        }
    }

    /**
     * envio un mensaje al servidor
     *
     * @param mensaje el mensaje a enviar
     */
    @Override
    public void enviarMensaje(ChatMessage mensaje) {
        if (!conectado) {
            System.out.println("no estoy conectado al servidor");
            return;
        }

        try {
            salida.writeObject(mensaje);
            salida.flush();
        } catch (IOException e) {
            System.out.println("error al enviar mensaje");
            desconectar();
        }
    }

    /**
     * configuro un listener para recibir mensajes
     *
     * @param listener objeto que procesara los mensajes recibidos
     */
    @Override
    public void setListener(ChatClientListener listener) {
        this.listener = listener;
    }

    /**
     * bloqueo a un usuario para no recibir sus mensajes
     *
     * @param usuario nombre del usuario a bloquear
     */
    @Override
    public void bloquearUsuario(String usuario) {
        if (usuario == null || usuario.trim().isEmpty()) {
            return;
        }

        // añado al conjunto de bloqueados
        usuariosBloqueados.add(usuario);

        // informo al servidor del bloqueo
        if (conectado) {
            enviarMensaje(new ChatMessage(nickname, usuario, MessageType.BAN, usuario));
        }

        System.out.println("usuario " + usuario + " bloqueado");
    }

    /**
     * desbloqueo a un usuario para volver a recibir sus mensajes
     *
     * @param usuario nombre del usuario a desbloquear
     */
    @Override
    public void desbloquearUsuario(String usuario) {
        if (usuario == null || usuario.trim().isEmpty()) {
            return;
        }

        // elimino del conjunto de bloqueados
        usuariosBloqueados.remove(usuario);

        // informo al servidor del desbloqueo
        if (conectado) {
            enviarMensaje(new ChatMessage(nickname, usuario, MessageType.UNBAN, usuario));
        }

        System.out.println("usuario " + usuario + " desbloqueado");
    }

    /**
     * verifico si un usuario esta bloqueado
     *
     * @param usuario nombre a verificar
     * @return true si esta bloqueado, false en caso contrario
     */
    public boolean estaUsuarioBloqueado(String usuario) {
        return usuariosBloqueados.contains(usuario);
    }

    /**
     * establezco el nickname del cliente
     *
     * @param nickname nuevo nombre para el cliente
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * obtengo el nickname actual
     *
     * @return nombre de usuario
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * metodo main para probar el cliente en consola
     *
     * @param args argumentos de linea de comandos
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // solicito el nickname
        System.out.print("introduce tu nickname: ");
        String nickname = scanner.nextLine().trim();

        // creo e inicio el cliente
        ChatClientImpl cliente = new ChatClientImpl(nickname);

        // asigno el listener que imprime los mensajes recibidos
        cliente.setListener(mensaje -> {
            String tipo = mensaje.getTipo().toString();
            String remitente = mensaje.getRemitente();
            String contenido = mensaje.getContenido();

            // formato segun el tipo de mensaje
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
                    System.out.println("Usuario " + remitente + " se ha conectado");
                    break;
                case LOGOUT:
                    System.out.println("Usuario " + remitente + " se ha desconectado");
                    break;
                default:
                    System.out.println("[" + tipo + "] " + remitente + ": " + contenido);
            }
        });

        // me conecto al servidor
        cliente.conectar();

        // explico los comandos disponibles
        System.out.println("\nComandos disponibles:");
        System.out.println("  /msg <usuario> <mensaje> - Enviar mensaje privado");
        System.out.println("  /ban <usuario> - Bloquear mensajes de un usuario");
        System.out.println("  /unban <usuario> - Desbloquear mensajes de un usuario");
        System.out.println("  /logout - Salir del chat");
        System.out.println("\nEscribe tus mensajes:");

        // leo el texto del usuario en bucle
        String texto;
        boolean continuar = true;

        while (continuar && cliente.conectado) {
            texto = scanner.nextLine();

            // procesamiento de comandos
            if (texto.startsWith("/")) {
                String[] partes = texto.split("\\s+", 3); // divido en máximo 3 partes

                switch (partes[0].toLowerCase()) {
                    case "/logout":
                        continuar = false;
                        break;

                    case "/ban":
                        if (partes.length > 1) {
                            cliente.bloquearUsuario(partes[1]);
                        } else {
                            System.out.println("Uso: /ban <usuario>");
                        }
                        break;

                    case "/unban":
                        if (partes.length > 1) {
                            cliente.desbloquearUsuario(partes[1]);
                        } else {
                            System.out.println("Uso: /unban <usuario>");
                        }
                        break;

                    case "/msg":
                        if (partes.length > 2) {
                            ChatMessage msgPrivado = new ChatMessage(
                                    nickname, partes[2], MessageType.PRIVADO, partes[1]);
                            cliente.enviarMensaje(msgPrivado);
                        } else {
                            System.out.println("Uso: /msg <usuario> <mensaje>");
                        }
                        break;

                    default:
                        System.out.println("Comando desconocido: " + partes[0]);
                }
            } else if (!texto.trim().isEmpty()) {
                // mensaje normal
                ChatMessage mensaje = new ChatMessage(nickname, texto, MessageType.MENSAJE);
                cliente.enviarMensaje(mensaje);
            }
        }

        // cierro el scanner y desconecto
        scanner.close();
        cliente.desconectar();
        System.out.println("Sesión finalizada");
    }
}