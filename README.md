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

## Requisitos
Escolha uma das formas de execução:

* **Servidor em Docker:** Docker e Docker Compose.
* **Cliente JavaFX local:** Java 17+ instalado. Não é necessário instalar Gradle, pois o projeto inclui o Gradle Wrapper (`./gradlew`).

## Como Executar com Docker Compose
O `docker-compose.yml` sobe o servidor por padrão na porta `8080`.

```bash
docker compose up --build chat-server
```

Em outro terminal, inicie o cliente JavaFX local apontando para o servidor:

```bash
./gradlew runClient \
  -Pchat.host=127.0.0.1 \
  -Pchat.port=8080
```

Para encerrar os containers:

```bash
docker compose down
```

## Como Executar com Docker
Também é possível usar apenas `docker build` e `docker run`.

Crie a imagem:

```bash
docker build -t chat-sockets .
```

Inicie o servidor:

```bash
docker run --rm -p 8080:8080 \
  -e APP_MODE=server \
  -e APP_HOST=0.0.0.0 \
  -e APP_PORT=8080 \
  -e APP_MAX_CLIENTS=50 \
  -e APP_LOG_LEVEL=INFO \
  chat-sockets
```

Em outro terminal, inicie o cliente JavaFX local apontando para o servidor no Docker:

```bash
./gradlew runClient \
  -Pchat.host=127.0.0.1 \
  -Pchat.port=8080
```

O cliente gráfico não é executado pelo Compose nesta versão, pois JavaFX precisa de uma sessão gráfica local.

### Variáveis de Ambiente da Imagem
| Variável | Padrão | Uso |
| --- | --- | --- |
| `APP_MODE` | `server` | Define o modo da aplicação: `server` ou `client`. |
| `APP_HOST` | `0.0.0.0` | Host de bind do servidor ou host de destino do cliente. |
| `APP_PORT` | `8080` | Porta usada pelo servidor ou pelo cliente ao conectar. |
| `APP_MAX_CLIENTS` | `50` | Limite de clientes simultâneos. Usado apenas no modo servidor. |
| `APP_LOG_LEVEL` | `INFO` | Nível de log do servidor: `ERROR`, `WARN`, `INFO`, `DEBUG` ou `TRACE`. |

## Como Executar pelo Terminal
Compile o JAR executável:

```bash
./gradlew jar
```

Inicie o servidor:

```bash
java -jar build/libs/chat-sockets-1.0-SNAPSHOT.jar \
  --server \
  --host 0.0.0.0 \
  --port 8080 \
  --max-clients 50 \
  --log-level INFO
```

Em outro terminal, inicie o cliente:

```bash
./gradlew runClient \
  -Pchat.host=127.0.0.1 \
  -Pchat.port=8080
```

O modo cliente também pode ser iniciado pelo entrypoint principal quando o runtime JavaFX estiver disponível:

```bash
java -jar build/libs/chat-sockets-1.0-SNAPSHOT.jar \
  --client \
  --host 127.0.0.1 \
  --port 8080
```

Para listar todas as opções da CLI:

```bash
java -jar build/libs/chat-sockets-1.0-SNAPSHOT.jar --help
```

## Opções Principais da CLI
| Opção | Modo | Padrão | Descrição |
| --- | --- | --- | --- |
| `--server` | Servidor | - | Executa a aplicação em modo servidor. |
| `--client` | Cliente | - | Executa a aplicação em modo cliente gráfico JavaFX. |
| `--host <host>` | Ambos | Servidor: `0.0.0.0`; cliente: `127.0.0.1` | Host de bind ou destino da conexão. |
| `--port <porta>` | Ambos | `8080` | Porta TCP. |
| `--max-clients <número>` | Servidor | `50` | Limite de clientes simultâneos. |
| `--log-level <nível>` | Servidor | `INFO` | Nível de log: `ERROR`, `WARN`, `INFO`, `DEBUG` ou `TRACE`. |
| `--help` | Ambos | - | Mostra a ajuda da CLI. |
| `--version` | Ambos | - | Mostra a versão da aplicação. |

## Verificação
Execute os testes:

```bash
./gradlew test
```

Confira a formatação antes de entregar alterações:

```bash
./gradlew spotlessCheck
```
