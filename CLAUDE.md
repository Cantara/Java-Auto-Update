# Java-Auto-Update

## Purpose
A wrapper application that automatically upgrades Java applications by polling ConfigService for new versions, downloading updates, and managing the application lifecycle. Supports both Linux and Windows deployments with proxy support and TLS configuration.

## Tech Stack
- Language: Java 8+
- Framework: Java Service Wrapper
- Build: Maven
- Key dependencies: ConfigService-SDK, Hystrix, SLF4J

## Architecture
Standalone agent that runs alongside your Java application. Periodically polls ConfigService for configuration changes and new artifact versions. When an update is detected, it downloads the new version, applies configuration changes, and restarts the managed application. Uses Hystrix commands for resilient HTTP communication with ConfigService.

## Key Entry Points
- `jau.properties` - Main configuration (ConfigService URL, credentials, update interval)
- `config_override/jau.properties` - Override configuration
- Windows service: `java-auto-update install/start/remove`
- Linux: `java -jar java-auto-update-*.jar`

## Development
```bash
# Build
mvn clean install

# Run
java -Dconfigservice.url=http://localhost:7000/jau/serviceconfig/query?clientid=clientid1 -jar target/java-auto-update-*.jar
```

## Domain Context
Application deployment and lifecycle management. Client-side counterpart to ConfigService, enabling automated zero-downtime updates for Java applications in production environments. Supports the Cantara continuous deployment infrastructure.
