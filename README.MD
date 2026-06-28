# Chat em Rede Local (Sockets TCP)

Projeto desenvolvido para a disciplina de Redes no Instituto Federal de Alagoas (IFAL). Trata-se de uma aplicação Cliente-Servidor construída em Java puro, focada na comunicação bidirecional utilizando Sockets TCP.

## Membros da Equipe
* Eike Fabrício
* Hugo Alexandre dos Santos
* João Henrique
* Matheus Alexandre

## Arquitetura Básica do Projeto
O projeto utiliza uma arquitetura **Cliente-Servidor** centralizada.
* **Servidor:** Atua como o nó central. Ele aguarda conexões em uma porta específica (padrão 8080) e delega o atendimento de cada cliente para uma nova Thread (gerenciada por um `ExecutorService`).
* **Cliente:** Conecta-se ao IP e Porta do servidor para enviar e receber dados.

## Protocolo de Comunicação e Fluxo de Conexão
A troca de mensagens entre o cliente e o servidor ocorre por stream de objetos Java usando `ObjectInputStream` e `ObjectOutputStream`. O contrato completo está em [`docs/protocolo.md`](docs/protocolo.md). O fluxo de conexão obedece a seguinte ordem:

1. **Handshake Inicial:** O cliente aponta para o IP/Porta do servidor e abre o Socket.
2. **Conexão Estabelecida:** O servidor aceita o Socket e inicia um loop de escuta para `ClientRequest`.
3. **Login Obrigatório:** O primeiro envelope deve ser `LOGIN`; antes disso, outras ações retornam `AUTH_REQUIRED`.
4. **Tráfego Duplex:** O cliente envia `ClientRequest`, o servidor responde com `ServerResponse` usando o mesmo `requestId` e também pode emitir `ServerEvent`.
5. **Desconexão Segura:** O cliente envia `LOGOUT`. Comandos textuais como `/sair` pertencem à interface do cliente e devem ser traduzidos para envelopes antes de chegar ao servidor.

## Como Executar
1. Certifique-se de ter o Java 17+ instalado.
2. Compile o jar: `./gradlew jar`
3. Inicie o servidor: `java -jar build/libs/chat-sockets-1.0-SNAPSHOT.jar --server --host 0.0.0.0 --port 8080 --max-clients 50`
4. Inicie o cliente: `java -jar build/libs/chat-sockets-1.0-SNAPSHOT.jar --client --host 127.0.0.1 --port 8080`
5. Use `--help` para ver todas as opções.



## Verificação
```text
./gradlew test
./gradlew spotlessCheck
```
