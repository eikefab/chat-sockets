package br.edu.ifal.lsor.chat.socket.client;

import br.edu.ifal.lsor.chat.ChatMessage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Objects;

public class ChatClientSocket implements AutoCloseable {

  private final String host;
  private final int port;
  
  private Socket socket;
  private ObjectOutputStream output;
  private ObjectInputStream input;

  public ChatClientSocket(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public void openSocket() {
    try {
      this.socket = new Socket(host, port);
      this.output = new ObjectOutputStream(socket.getOutputStream());
      this.output.flush();
      this.input = new ObjectInputStream(socket.getInputStream());
      
      System.out.println("Conectado com sucesso ao servidor " + host + ":" + port);
      
    } catch (Exception exception) {
      System.err.println("Erro ao conectar no servidor " + host + ":" + port);
      System.err.println("Detalhe do erro: " + exception.getMessage());
      System.err.println("Verifique se o servidor está rodando e se o IP/Porta estão corretos.");
    }
  }

  public Socket getSocket() {
    return socket;
  }

  public String send(ChatMessage message) {
    Objects.requireNonNull(socket);
    Objects.requireNonNull(output);
    Objects.requireNonNull(input);

    if (this.socket.isClosed() || !this.socket.isConnected()) {
      throw new IllegalStateException("Socket está fechado!");
    }

    try {
      output.writeObject(message);
      output.flush();
      return (String) input.readObject();
    } catch (Exception exception) {
      throw new IllegalStateException("Falha ao enviar mensagem.", exception);
    }
  }

  @Override
  public void close() throws Exception {
    if (this.socket != null && !this.socket.isClosed()) {
      this.socket.close();
      System.out.println("Conexão encerrada.");
    }
  }
}
