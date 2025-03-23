package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

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

        // creo un scanner para leer desde teclado
        Scanner scanner = new Scanner(System.in);

        // guardo el nombre del remitente (puedes cambiar esto si haces login)
        String remitente = "mario";

        // muestro instrucciones
        System.out.println("escribe mensajes (escribe 'logout' para salir del chat):");

        // leo el texto del usuario en bucle
        while (true) {
            // leo una linea del usuario
            String texto = scanner.nextLine();

            // si escribe logout, salgo del bucle
            if (texto.equalsIgnoreCase("logout")) {
                // creo un mensaje de tipo LOGOUT
                ChatMessage mensajeLogout = new ChatMessage(remitente, "cerrando sesion", MessageType.LOGOUT);
                cliente.enviarMensaje(mensajeLogout);
                break;
            }

            // creo un mensaje normal de tipo MENSAJE con el texto escrito
            ChatMessage mensaje = new ChatMessage(remitente, texto, MessageType.MENSAJE);

            // envio el mensaje al servidor
            cliente.enviarMensaje(mensaje);
        }

        // cierro el scanner y desconecto
        scanner.close();
        cliente.desconectar();
    }
}
