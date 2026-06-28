package br.edu.ifal.lsor.chat.server;

import java.util.UUID;

record UserRecord(UUID memberId, String username, String displayName) {}
