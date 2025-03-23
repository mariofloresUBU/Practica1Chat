package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServerImpl implements ChatServer {

    private static final int PUERTO = 1500;
    private ServerSocket servidor;
    private List<ServerThreadForClient> clientes = new CopyOnWriteArrayList<>();

    @Override
    public void startup() {
        try {
            // inicio el socket del servidor para escuchar conexiones
            servidor = new ServerSocket(PUERTO);
            System.out.println("servidor iniciado en el puerto " + PUERTO);

            // entro en un bucle infinito para aceptar clientes
            while (true) {
                // acepto una conexion entrante
                Socket socket = servidor.accept();
                System.out.println("cliente conectado desde " + socket.getInetAddress());

                // creo un hilo para gestionar al cliente
                ServerThreadForClient hilo = new ServerThreadForClient(socket, this);
                clientes.add(hilo);
                hilo.start(); // inicio el hilo
            }
        } catch (IOException e) {
            // muestro el error si no puedo iniciar el servidor
            System.out.println("error al iniciar el servidor");
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        try {
            // cierro las conexiones con todos los clientes
            for (ServerThreadForClient cliente : clientes) {
                cliente.cerrarConexion();
            }

            // cierro el socket del servidor si esta abierto
            if (servidor != null && !servidor.isClosed()) {
                servidor.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // envio un mensaje a todos los clientes conectados
    public void broadcast(ChatMessage mensaje) {
        for (ServerThreadForClient cliente : clientes) {
            cliente.enviarMensaje(mensaje);
        }
    }

    // elimino un cliente de la lista cuando se desconecta
    public void eliminarCliente(ServerThreadForClient cliente) {
        clientes.remove(cliente);
    }

    // metodo main para lanzar el servidor desde consola
    public static void main(String[] args) {
        ChatServerImpl servidor = new ChatServerImpl();
        servidor.startup();
    }
}
