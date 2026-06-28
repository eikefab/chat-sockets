# AGENTS.md

## Project Summary

Java 17 Gradle TCP chat application using Java object streams (`ObjectInputStream` / `ObjectOutputStream`) and in-memory state. Built for the Networks discipline at IFAL.

## Official Commands

```text
./gradlew test          # runs all tests
./gradlew spotlessCheck # checks formatting
./gradlew spotlessApply # fixes formatting
./gradlew jar           # builds executable jar
```

## Architecture Map

| Package | Role |
| --- | --- |
| `br.edu.ifal.lsor.chat.protocol` | Serializable envelopes (`ClientRequest`, `ServerResponse`, `ServerEvent`) and constants (`Actions`, `Codes`, `Events`) |
| `br.edu.ifal.lsor.chat.server` | In-memory chat state, rules, and action handling |
| `br.edu.ifal.lsor.chat.socket.server` | Server TCP transport: accepts sockets, manages registered client connections, dispatches events |
| `br.edu.ifal.lsor.chat.socket.client` | Client TCP transport: connects to server, correlates responses by `requestId`, forwards events |
| `br.edu.ifal.lsor.chat.socket` | Shared socket utilities (`ChatObjectInputFilters`) |
| `docs/protocolo.md` | Protocol contract (wire format, actions, codes, events, payload fields) |

## Rules

- Preserve protocol field names unless docs and tests change deliberately.
- Do not put socket types (`ClientConnection`, `Socket`, `ObjectOutputStream`) into the domain service package (`br.edu.ifal.lsor.chat.server`).
- Keep state in memory unless a plan explicitly introduces persistence.
- Add or keep tests for socket-boundary behavior before any refactor touching transport.
- Run `./gradlew spotlessCheck` before committing.
- The entrypoint is `ChatApplicationMain` with `--server` or `--client` flags.
