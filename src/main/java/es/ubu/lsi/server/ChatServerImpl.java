package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * implementacion del servidor de chat
 *
 * @author mario flores
 */
public class ChatServerImpl implements ChatServer {

    private static final int PUERTO = 1500;
    private static final Logger logger = Logger.getLogger("ChatServer");

    private ServerSocket servidor;
    private List<ServerThreadForClient> clientes = new CopyOnWriteArrayList<>();
    private Map<String, List<String>> usuariosBloqueados = new HashMap<>();
    private boolean ejecutando = false;

    /**
     * constructor del servidor
     */
    public ChatServerImpl() {
        // configuro el log para guardar mensajes
        try {
            FileHandler fh = new FileHandler("chat_server.log", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            System.out.println("no pude crear el archivo de log");
        }
    }

    /**
     * inicio el servidor y acepto conexiones
     */
    @Override
    public void startup() {
        try {
            // inicio el socket del servidor para escuchar conexiones
            servidor = new ServerSocket(PUERTO);
            ejecutando = true;

            log("servidor iniciado en el puerto " + PUERTO);

            // entro en un bucle para aceptar clientes
            while (ejecutando) {
                try {
                    // acepto una conexion entrante
                    Socket socket = servidor.accept();
                    log("cliente conectado desde " + socket.getInetAddress());

                    // creo un hilo para gestionar al cliente
                    ServerThreadForClient hilo = new ServerThreadForClient(socket, this);
                    clientes.add(hilo);
                    hilo.start(); // inicio el hilo
                } catch (IOException e) {
                    if (ejecutando) {
                        log("error al aceptar conexion: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            // muestro el error si no puedo iniciar el servidor
            log("error al iniciar el servidor: " + e.getMessage());
        }
    }

    /**
     * detengo el servidor y cierro todas las conexiones
     */
    @Override
    public void shutdown() {
        try {
            ejecutando = false;

            // notifico a todos los clientes
            broadcast(new ChatMessage("Server", "El servidor se está cerrando", MessageType.SISTEMA));

            // cierro las conexiones con todos los clientes
            for (ServerThreadForClient cliente : clientes) {
                cliente.cerrarConexion();
            }

            // limpio la lista de clientes
            clientes.clear();

            // cierro el socket del servidor si esta abierto
            if (servidor != null && !servidor.isClosed()) {
                servidor.close();
            }

            log("servidor detenido");
        } catch (IOException e) {
            log("error al cerrar el servidor: " + e.getMessage());
        }
    }

    /**
     * envio un mensaje a todos los clientes conectados
     *
     * @param mensaje el mensaje a difundir
     */
    public void broadcast(ChatMessage mensaje) {
        log("broadcast: " + mensaje.getRemitente() + " -> " + mensaje.getContenido());

        for (ServerThreadForClient cliente : clientes) {
            // si es un mensaje normal, verifico bloqueos
            if (mensaje.getTipo() == MessageType.MENSAJE) {
                // si el cliente tiene bloqueado al remitente, no le envio el mensaje
                if (cliente.tieneUsuarioBloqueado(mensaje.getRemitente())) {
                    continue;
                }
            }

            // envio el mensaje al cliente
            cliente.enviarMensaje(mensaje);
        }
    }

    /**
     * envio un mensaje privado a un cliente especifico
     *
     * @param mensaje el mensaje privado a enviar
     * @return true si se entrego, false si no se encontro el destinatario
     */
    public boolean enviarMensajePrivado(ChatMessage mensaje) {
        if (mensaje.getDestinatario() == null) {
            return false;
        }

        log("mensaje privado: " + mensaje.getRemitente() + " -> " +
                mensaje.getDestinatario() + ": " + mensaje.getContenido());

        boolean entregado = false;

        // busco al destinatario
        for (ServerThreadForClient cliente : clientes) {
            if (cliente.getNickname().equals(mensaje.getDestinatario())) {
                // verifico si el destinatario ha bloqueado al remitente
                if (!cliente.tieneUsuarioBloqueado(mensaje.getRemitente())) {
                    cliente.enviarMensaje(mensaje);
                    entregado = true;
                }
                break;
            }
        }

        // también envío el mensaje al remitente para que vea su propio mensaje privado
        for (ServerThreadForClient cliente : clientes) {
            if (cliente.getNickname().equals(mensaje.getRemitente())) {
                cliente.enviarMensaje(mensaje);
                break;
            }
        }

        return entregado;
    }

    /**
     * elimino un cliente de la lista cuando se desconecta
     *
     * @param cliente el hilo del cliente a eliminar
     */
    public void eliminarCliente(ServerThreadForClient cliente) {
        if (cliente == null) {
            return;
        }

        clientes.remove(cliente);
        log("cliente " + cliente.getNickname() + " eliminado, quedan " + clientes.size());

        // notifico a los demás que un usuario se ha desconectado
        if (cliente.getNickname() != null) {
            broadcast(new ChatMessage("Server",
                    "El usuario " + cliente.getNickname() + " se ha desconectado",
                    MessageType.SISTEMA));
        }
    }

    /**
     * añado un bloqueo entre usuarios
     *
     * @param bloqueador usuario que bloquea
     * @param bloqueado usuario que es bloqueado
     */
    public void bloquearUsuario(String bloqueador, String bloqueado) {
        // busco al cliente que bloquea
        for (ServerThreadForClient cliente : clientes) {
            if (cliente.getNickname().equals(bloqueador)) {
                cliente.bloquearUsuario(bloqueado);
                log("usuario " + bloqueador + " ha bloqueado a " + bloqueado);
                break;
            }
        }
    }

    /**
     * elimino un bloqueo entre usuarios
     *
     * @param desbloqueador usuario que desbloquea
     * @param desbloqueado usuario que es desbloqueado
     */
    public void desbloquearUsuario(String desbloqueador, String desbloqueado) {
        // busco al cliente que desbloquea
        for (ServerThreadForClient cliente : clientes) {
            if (cliente.getNickname().equals(desbloqueador)) {
                cliente.desbloquearUsuario(desbloqueado);
                log("usuario " + desbloqueador + " ha desbloqueado a " + desbloqueado);
                break;
            }
        }
    }

    /**
     * registro eventos en el log
     *
     * @param mensaje texto a registrar
     */
    private void log(String mensaje) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());
        System.out.println("[" + timestamp + "] " + mensaje);
        logger.info(mensaje);
    }

    /**
     * metodo main para lanzar el servidor desde consola
     *
     * @param args argumentos de linea de comandos
     */
    public static void main(String[] args) {
        ChatServerImpl servidor = new ChatServerImpl();
        servidor.startup();
    }