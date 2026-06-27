# Chat em Rede Local (Sockets TCP)

Projeto desenvolvido para a disciplina de Redes no Instituto Federal de Alagoas (IFAL). Trata-se de uma aplicação Cliente-Servidor construída em Java puro, focada na comunicação bidirecional utilizando Sockets TCP.

## Membros da Equipe
* Eike Fabrício
* Hugo Alexandre dos Santos
* João Henrique
* Matheus Alexandre

## Arquitetura Básica do Projeto
O projeto utiliza uma arquitetura **Cliente-Servidor** centralizada.
* **Servidor (`ChatServerMain`):** Atua como o nó central. Ele aguarda conexões em uma porta específica (8080) e delega o atendimento de cada cliente para uma nova Thread (gerenciada por um `ExecutorService`). 
* **Cliente (`ChatClientSocket`):** Conecta-se ao IP e Porta do servidor para enviar e receber dados.

## Protocolo de Comunicação e Fluxo de Conexão
A troca de mensagens entre o cliente e o servidor ocorre no formato **JSON**, utilizando a biblioteca Gson para serialização. O fluxo de conexão obedece a seguinte ordem:

1. **Handshake Inicial:** O cliente aponta para o IP/Porta do servidor e abre o Socket.
2. **Conexão Estabelecida:** O servidor aceita o Socket, extrai o IP de origem e inicia um loop de escuta contínua (`BufferedReader`).
3. **Tráfego de Dados:** O cliente envia uma string JSON contendo os dados da classe `ChatMessage` (id, message, createdAt).
4. **Confirmação (Bidirecional):** Ao ler e desserializar o JSON, o servidor processa a mensagem e envia imediatamente uma string de texto de volta para o cliente como confirmação.
5. **Desconexão Segura:** O cliente pode enviar o comando `/sair`. O servidor reconhece o comando, quebra o loop de escuta, fecha os buffers e encerra o socket de forma limpa, liberando a porta.

## Como Executar
1. Certifique-se de ter o Java 17+ instalado.
2. Inicie o servidor executando a classe `ChatServerMain.java`.
3. Inicie um ou mais clientes instanciando a classe `ChatClientSocket.java` com o IP do servidor (ou `127.0.0.1` para testes locais) e a porta `8080`.