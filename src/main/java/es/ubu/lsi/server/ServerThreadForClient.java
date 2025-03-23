package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;

import java.io.*;
import java.net.Socket;

public class ServerThreadForClient extends Thread {

    private Socket socket;
    private ChatServerImpl servidor;
    private ObjectInputStream entrada;
    private ObjectOutputStream salida;

    public ServerThreadForClient(Socket socket, ChatServerImpl servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try {
            // creo los flujos de entrada y salida
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());

            // leo los mensajes que llegan desde el cliente
            ChatMessage mensaje;
            while ((mensaje = (ChatMessage) entrada.readObject()) != null) {
                // imprimo el mensaje recibido en el servidor
                System.out.println("mensaje recibido en el servidor:");
                System.out.println("tipo: " + mensaje.getTipo());
                System.out.println("de: " + mensaje.getRemitente());
                System.out.println("contenido: " + mensaje.getContenido());

                // reenvio el mensaje a todos los clientes conectados
                servidor.broadcast(mensaje);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("cliente desconectado o error en la comunicacion");
        } finally {
            cerrarConexion();
            servidor.eliminarCliente(this);
        }
    }

    public void enviarMensaje(ChatMessage mensaje) {
        try {
            salida.writeObject(mensaje);
            salida.flush();
        } catch (IOException e) {
            System.out.println("no pude enviar el mensaje a un cliente");
        }
    }

    public void cerrarConexion() {
        try {
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("error al cerrar la conexion del cliente");
        }
    }
}
