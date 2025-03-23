package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

public interface ChatClientListener {
    void onMensajeRecibido(ChatMessage mensaje); // metodo que se llama cuando recibo un mensaje
}
