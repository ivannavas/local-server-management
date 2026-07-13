# Local Server Management

A personal project I'm building to monitor and manage my own **home / local server**.

It's a [Spring Boot](https://spring.io/projects/spring-boot) application that exposes different aspects of the server in two ways:

- A **REST API** for dashboards, scripts, or quick checks.
- An **MCP (Model Context Protocol) server**, so an AI assistant can query and manage the server through natural language.

The MCP layer is powered by [**Sprout**](https://github.com/ivannavas/sprout-ai-framework), my own annotation-based framework for turning ordinary Java methods into AI-callable tools with almost no boilerplate.

> 🚧 **Work in progress.** The scope will keep growing over time, adapting to my needs.

## Getting started

### Prerequisites

- **JDK 25** (or newer).
- Native access is enabled via `--enable-native-access=ALL-UNNAMED` in `application.properties`.

### Run

Use the Maven wrapper included in the repo:

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

## Configuration

Configuration lives in `src/main/resources/application.properties`:

- The **REST API** runs on the default Spring Boot port (`8080`).
- The **MCP server** runs on port `3001`.

## License

Personal project.
