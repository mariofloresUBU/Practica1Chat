package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

public interface ChatClient {
    void conectar(); // me conecto al servidor
    void desconectar(); // cierro la conexion
    void enviarMensaje(ChatMessage mensaje); // envio un mensaje al servidor
    void setListener(ChatClientListener listener); // asigno un listener para recibir mensajes
}
