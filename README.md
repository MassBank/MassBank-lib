# MassBank-lib
MassBank java library.

To use the library depend on:
```xml
<dependency>
    <groupId>io.github.massbank</groupId>
    <artifactId>massbank-lib</artifactId>
    <version>1.3</version>
</dependency>
```

## Developing in a Dev Container

On Linux, start the rootless Podman user socket with
`systemctl --user start podman.socket` before opening the repository in a Dev
Container. The editor must inherit `XDG_RUNTIME_DIR` and be configured to use
Podman. Docker Compose is not required.

The container provides JDK 25 and Maven. Run `mvn -B package` to build and test
the library. Testcontainers uses the mounted Podman socket to start PostgreSQL
17 for integration tests; no separate database service or sibling repository
is required. Only open trusted code in this container: the socket gives it
access to the host user's Podman API.
