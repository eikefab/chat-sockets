# Interface Gráfica do Cliente

O cliente humano usa uma interface JavaFX. A GUI traduz as ações do usuário para os envelopes tipados definidos em `docs/protocolo.md`.

## Login

Ao iniciar o cliente, informe:

- Host do servidor.
- Porta TCP.
- Nome de usuário.
- Nome público, opcional. Quando vazio, usa o mesmo valor do nome de usuário.

O servidor normaliza o nome de usuário para minúsculas e impede outro login simultâneo com o mesmo nome, mesmo que digitado com maiúsculas/minúsculas diferentes.

Após login aceito, a janela principal exibe contatos, grupos, histórico e campo de envio.

## Conversas

A barra lateral lista usuários online e os grupos dos quais você participa.

- Selecione um usuário para carregar o histórico direto.
- Selecione um grupo para carregar o histórico do grupo.
- Use `Atualizar` para recarregar contatos e grupos manualmente.

Mensagens recebidas por evento aparecem na conversa aberta quando ela corresponde ao remetente ou grupo.

## Envio de mensagens

Digite o texto no campo inferior e use `Enviar`.

- Para usuário, a GUI envia `SEND_DIRECT`.
- Para grupo, a GUI envia `SEND_GROUP`.
- Em caso de erro, a mensagem do servidor aparece na barra de status.

## Grupos

A interface oferece botões para:

- `Criar`: envia `CREATE_GROUP`.
- `Entrar`: envia `JOIN_GROUP`.
- `Renomear`: envia `RENAME_GROUP` para o grupo selecionado.
- `Sair`: envia `LEAVE_GROUP` para o grupo selecionado.
- `Excluir`: envia `DELETE_GROUP` para o grupo selecionado após confirmação.

As regras de permissão continuam no servidor. Por exemplo, somente o dono pode renomear ou excluir grupo.

## Eventos

A GUI reage aos eventos assíncronos do servidor:

| Evento | Efeito na interface |
| --- | --- |
| `DIRECT_MESSAGE` | Adiciona a mensagem na conversa direta aberta e atualiza contatos. |
| `GROUP_MESSAGE` | Adiciona a mensagem no grupo aberto. |
| `USER_ONLINE` / `USER_OFFLINE` | Atualiza a lista de conversas. |
| Eventos de grupo | Atualizam a lista de grupos e a barra de status. |

Se a conexão cair, a GUI mostra a falha na barra de status e bloqueia o envio.
