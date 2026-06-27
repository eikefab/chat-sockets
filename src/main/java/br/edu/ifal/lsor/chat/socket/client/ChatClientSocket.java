package br.edu.ifal.lsor.chat.socket.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

public class ChatClientSocket implements AutoCloseable {

  private final String host;
  private final int port;
  
  private Socket socket;
  private PrintWriter writer;
  private BufferedReader reader;

  // Agora quem criar o ChatClientSocket vai ter que falar o ip e a porta
  public ChatClientSocket(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public void openSocket() {
    try {
      // Usamos as variáveis dinâmicas em vez de valores fixos
      this.socket = new Socket(host, port);

      // O 'true' no PrintWriter ativa o auto-flush, enviando a mensagem na hora
      this.writer = new PrintWriter(socket.getOutputStream(), true); 
      this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      
      System.out.println("Conectado com sucesso ao servidor " + host + ":" + port);
      
    } catch (Exception exception) {
      // Tratamento básico de erro mais claro para o usuário
      System.err.println("Erro ao conectar no servidor " + host + ":" + port);
      System.err.println("Detalhe do erro: " + exception.getMessage());
      System.err.println("Verifique se o servidor está rodando e se o IP/Porta estão corretos.");
    }
  }

  public Socket getSocket() {
    return socket;
  }

  public void write(Consumer<BufferedReader> response, Object... lines) {
    Objects.requireNonNull(socket);
    Objects.requireNonNull(writer);
    Objects.requireNonNull(reader);

    if (this.socket.isClosed() || !this.socket.isConnected()) {
      throw new IllegalStateException("Socket está fechado!");
    }

    for (Object line : lines) {
      final String data = line.toString();
      writer.println(data);
    }
    
    // Como ativamos o auto-flush na linha 31, o writer.flush() manual aqui é opcional, 
    // mas mantê-lo não faz mal para garantir
    writer.flush();

    response.accept(reader);
  }

  @Override
  public void close() throws Exception {
    if (this.socket != null && !this.socket.isClosed()) {
      this.socket.close();
      System.out.println("Conexão encerrada.");
    }
  }
}