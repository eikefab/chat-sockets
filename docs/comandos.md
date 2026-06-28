# Comandos da Interface de Terminal

A interface do cliente opera via terminal. Os comandos são prefixados com `/`. Mensagens sem o prefixo `/` são tratadas como comando desconhecido.

---

## Login

Ao iniciar o cliente, dois prompts são exibidos:

```
Nome de usuário: <digite>
Nome público (ENTER para usar '<username>'): <digite>
```

- **Nome de usuário**: identificador único, obrigatório, usado para endereçar mensagens diretas e identificar o usuário no sistema.
- **Nome público**: nome exibido aos demais usuários. Se deixado em branco, assume o mesmo valor do nome de usuário.

---

## Comandos

### `/list` ou `/listar`

Lista os contatos disponíveis: usuários cadastrados (com status online/offline) e grupos existentes. Também atualiza o cache local de grupos, necessário para que `/msg` e `/chat` reconheçam grupos automaticamente.

```
/list
```

Exemplo de saída:

```
=== Contatos Online ===
  alice (Alice Silva) [online]
  bob (você) [online]
  carlos [offline]

=== Grupos ===
  #devs - Desenvolvedores (dono: alice, 3 membros) [membro]
  #geral - Geral (dono: admin, 5 membros)
```

---

### `/msg <username|groupCode> <mensagem>`

Envia uma mensagem para um contato (mensagem direta) ou para um grupo (mensagem de grupo).

- Se o destino estiver no cache de grupos (atualizado via `/list`), a mensagem é enviada como `SEND_GROUP`.
- Caso contrário, tenta `SEND_DIRECT`. Se falhar porque o usuário não foi encontrado ou está offline, tenta `SEND_GROUP` como fallback.

```
/msg alice Olá, tudo bem?
/msg devs Reunião amanhã às 10h
```

Respostas:

- `Enviado para alice.` — mensagem direta enviada.
- `Enviado para #devs.` — mensagem de grupo enviada.
- `Erro: ...` — falha no envio (ex.: grupo não encontrado, permissão negada).

---

### `/chat <username|groupCode>`

Exibe o histórico de conversa com um contato (escopo `DIRECT`) ou de um grupo (escopo `GROUP`). Utiliza o cache de grupos para decidir o escopo automaticamente.

```
/chat alice
/chat devs
```

Exemplo de saída:

```
=== Histórico: alice ===
[14:30] alice: Olá!
[14:31] bob: Oi, tudo bem?
[14:32] alice: Tudo ótimo!
```

Se não houver mensagens:

```
=== Histórico: devs ===
(sem mensagens)
```

---

### `/reply <mensagem>` ou `/responder <mensagem>`

Responde à última mensagem recebida, seja ela direta ou de grupo. O contexto de resposta é definido automaticamente pelo evento de mensagem mais recente.

- Se a última mensagem foi uma `DIRECT_MESSAGE`, envia `SEND_DIRECT` para o remetente.
- Se foi uma `GROUP_MESSAGE`, envia `SEND_GROUP` para o grupo.
- Se nenhuma mensagem foi recebida ainda, exibe: `Nenhuma mensagem recebida para responder.`

```
/reply Claro, podemos sim!
/responder Combinado!
```

---

### `/sair`

Desconecta do servidor e encerra o cliente.

```
/sair
```

O cliente envia `LOGOUT` ao servidor, fecha a conexão e termina a execução.

---

## Notificações de mensagens recebidas

Ao receber uma mensagem (direta ou de grupo), o cliente exibe uma notificação no terminal:

- **Mensagem direta**: `[MENSAGEM RECEBIDA] <remetente>: <texto>`
- **Mensagem de grupo**: `[MENSAGEM RECEBIDA] <grupo>/<autor>: <texto>`

Exemplos:

```
[MENSAGEM RECEBIDA] alice: Olá, bob!
[MENSAGEM RECEBIDA] devs/carlos: Alguém viu o PR?
```

Essas mensagens atualizam automaticamente o contexto de `/reply`.

---

## Eventos informativos

O cliente também exibe notificações para eventos do sistema:

| Evento | Mensagem exibida |
|--------|-----------------|
| Usuário entra | `Alice Silva (alice) entrou no chat.` |
| Usuário sai | `carlos saiu do chat.` |
| Grupo criado | `Grupo criado: #devs - Desenvolvedores` |
| Grupo renomeado | `Grupo renomeado: #devs → Devs BR` |
| Grupo excluído | `Grupo excluído: #devs` |
| Membro entrou no grupo | `carlos entrou no grupo #devs.` |
| Membro saiu do grupo | `maria saiu do grupo #devs.` |

---

## Tratamento de erros

- **Conexão perdida**: `Conexão com o servidor perdida.` — o cliente é encerrado.
- **Login inválido**: exibe a mensagem de erro retornada pelo servidor e encerra.
- **Comando desconhecido**: `Comando desconhecido. Use /list para ver opções.`
- **Argumentos insuficientes**: exibe a sintaxe correta do comando.
