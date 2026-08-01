# Distribution packaging

InboxPilot ships as an executable Spring Boot JAR, runnable ZIP and TAR.GZ
archives, and an OCI image that can be built with Spring Boot buildpacks.

## Build archives

```bash
./gradlew distributionZip distributionTar
```

Artifacts are written to `build/distributions`. Each archive has one top-level
`inboxpilot-<version>` directory containing:

- `bin/inboxpilot` and `bin/inboxpilot.bat` launchers;
- `lib/inboxpilot.jar`, the executable application;
- the example configuration, user documentation, and README.

After extracting an archive, run:

```bash
./inboxpilot-*/bin/inboxpilot labels list
```

Java 21 or newer must be available on `PATH`. Put local configuration and token
state outside the extracted distribution so upgrades do not overwrite them.

## Build a container image

With a Docker-compatible daemon available:

```bash
./gradlew bootBuildImage
```

The resulting local image is named
`ghcr.io/kekukhvy/inboxpilot:<project-version>`. Spring Boot buildpacks create a
non-root, layered OCI image with the Java runtime included. The task is kept
separate from `build`, so normal builds do not require a container daemon.

Pass OAuth credentials and other overrides as environment variables at runtime.
Mount token, report, checkpoint, and rollback paths on persistent volumes; the
container filesystem itself should be treated as disposable. Never bake OAuth
credentials or cached tokens into an image.
