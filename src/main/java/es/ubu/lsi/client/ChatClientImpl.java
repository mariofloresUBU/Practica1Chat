package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

import java.io.*;
import java.net.Socket;
import es.ubu.lsi.common.MessageType;


public class ChatClientImpl implements ChatClient {

    private static final String HOST = "localhost";
    private static final int PUERTO = 1500;

    private Socket socket;
    private ObjectInputStream entrada;
    private ObjectOutputStream salida;
    private ChatClientListener listener;

    // metodo para conectar con el servidor
    @Override
    public void conectar() {
        try {
            // me conecto al servidor por socket
            socket = new Socket(HOST, PUERTO);

            // inicializo los flujos de entrada y salida
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());

            // creo un hilo para escuchar mensajes del servidor
            Thread receptor = new Thread(() -> {
                try {
                    ChatMessage mensaje;
                    while ((mensaje = (ChatMessage) entrada.readObject()) != null) {
                        // cuando recibo un mensaje, se lo paso al listener
                        if (listener != null) {
                            listener.onMensajeRecibido(mensaje);
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("conexion cerrada o error al recibir mensaje");
                }
            });
            receptor.start();

            System.out.println("conectado al servidor");
        } catch (IOException e) {
            System.out.println("no pude conectar con el servidor");
            e.printStackTrace();
        }
    }

    // metodo para cerrar la conexion
    @Override
    public void desconectar() {
        try {
            if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("error al cerrar la conexion");
        }
    }

    // metodo para enviar un mensaje al servidor
    @Override
    public void enviarMensaje(ChatMessage mensaje) {
        try {
            salida.writeObject(mensaje);
            salida.flush();
        } catch (IOException e) {
            System.out.println("error al enviar mensaje");
        }
    }

    // metodo para asignar el listener
    @Override
    public void setListener(ChatClientListener listener) {
        this.listener = listener;
    }

    // metodo main para probar el cliente en consola
    public static void main(String[] args) {
        ChatClientImpl cliente = new ChatClientImpl();

        // asigno el listener que simplemente imprime los mensajes recibidos
        cliente.setListener(mensaje -> {
            System.out.println("mensaje recibido:");
            System.out.println("tipo: " + mensaje.getTipo());
            System.out.println("de: " + mensaje.getRemitente());
            System.out.println("contenido: " + mensaje.getContenido());
        });

        // me conecto al servidor
        cliente.conectar();

        // creo un mensaje de tipo MENSAJE con remitente "mario" y texto "hola servidor!"
        ChatMessage mensaje = new ChatMessage("mario", "hola servidor!", MessageType.MENSAJE);

        // envio el mensaje al servidor
        cliente.enviarMensaje(mensaje);
    }

}
