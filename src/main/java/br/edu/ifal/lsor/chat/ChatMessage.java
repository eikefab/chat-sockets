package br.edu.ifal.lsor.chat;

import java.time.Instant;
import java.util.UUID;

import com.google.gson.Gson;

public class ChatMessage {

  private final UUID id;
  private final String message;
  private final String createdAt; // Simplificado para String para facilitar o tráfego JSON

  // Instância do Gson pronta para uso
  private static final Gson gson = new Gson();

  public ChatMessage(UUID id, String message, Instant createdAt) {
    this.id = id;
    this.message = message;
    this.createdAt = createdAt.toString(); // Salva no formato padrão ISO-8601
  }

  public ChatMessage(String message, Instant createdAt) {
    this(UUID.randomUUID(), message, createdAt);
  }

  public ChatMessage(String message) {
    this(message, Instant.now());
  }

  public UUID getId() {
    return id;
  }

  public String getMessage() {
    return message;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  // Task 18: Método para transformar o objeto em texto JSON para enviar pela rede
  public String toJson() {
    return gson.toJson(this);
  }

  // Task 18: Método para transformar o texto JSON recebido da rede de volta em objeto
  public static ChatMessage fromJson(String json) {
    return gson.fromJson(json, ChatMessage.class);
  }
}