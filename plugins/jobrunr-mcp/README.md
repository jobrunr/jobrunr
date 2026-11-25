# jobrunr-mcp

Connects a running JobRunr instance to the JobRunr MCP server at https://mcp.jobrunr.io so AI agents can inspect and
operate your jobs. The first beta, `org.jobrunr:jobrunr-mcp:1.0.0-beta.0`, is on Maven Central. The security changes
described here target `1.0.0-beta.1`, which has not been published yet.

The connector long-polls the MCP server over HTTPS. The connection is outbound only, so no inbound port has to be
opened. A plain HTTP hub URL is accepted only for a loopback address, such as `http://localhost:18080`, for local
testing. Every permitted request is answered in process by the dashboard's own `JobRunrApiHandler`, so the JSON is
identical to the dashboard's and the dashboard does not have to run.

The connector accepts only the dashboard routes used by the current MCP tools and cluster monitor. Requeueing,
deleting and triggering jobs and dismissing problems require an operate connector token (`jrc_operate_`). The hub
enforces this access level for agent requests, and the connector checks it again before executing a request.

The OSS connector returns stored job parameters without redaction. Those values may contain personal data. Parameter
redaction is planned as a Pro-only integration and is not available in this OSS connector. The current JobRunr Pro
runtime does not expose the OSS `JobRunrApiHandler`, so this connector cannot be used directly with it; a separate Pro
adapter is needed. The hub's Pro account entitlement supports unlimited connected nodes and clusters once a compatible
connector is available. OSS accounts can connect one cluster with up to two nodes.

## Requirements

- Java 17 or later
- JobRunr 8.5.0 or later (the published pom declares 8.5.0 as the minimum, so your own JobRunr version is kept)

## Usage

Plain Java:

```java
new JobRunrMcpConnector(token, storageProvider).start();
```

Spring Boot 3 and 4: add the dependency and set `jobrunr.dashboard.mcp.token`. Optionally override
`jobrunr.dashboard.mcp.url` for a local hub or a custom HTTPS endpoint. The auto configuration only starts when a
token is configured. Keep the connector token in a secret store or environment variable, not in source control.

## Releasing

`jobrunr-mcp` is **not** released together with JobRunr core. It has its own version line and is published by hand, so
the tag-triggered pipeline in `.woodpecker/release.yaml` skips it: the publish tasks only run when
`-PmcpVersion=<version>` is passed. `1.0.0-beta.0` is already published and cannot be replaced. The next planned
release is `1.0.0-beta.1`; local builds without `-PmcpVersion` use `1.0.0-beta.1-SNAPSHOT`.

Releases are uploaded manually through the [Central Portal](https://central.sonatype.com/publishing), so the only
secrets you need locally are the signing ones:

| Variable | Purpose |
| --- | --- |
| `SIGNING_KEY` | base64 of the armored PGP private key |
| `SIGNING_PASSWORD` | passphrase for that key |

One-time: the matching **public** key has to be on a keyserver Central checks, otherwise validation fails on an
unverifiable signature.

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID   # or keys.openpgp.org, pgp.mit.edu
```

### Building the upload bundle

```bash
./gradlew :plugins:jobrunr-mcp:centralBundle -PmcpVersion=YOUR_NEW_VERSION
```

This writes `plugins/jobrunr-mcp/build/central-bundle/jobrunr-mcp-<next-version>-bundle.zip`, laid out the way Central
expects:

```
org/jobrunr/jobrunr-mcp/<next-version>/jobrunr-mcp-<next-version>.jar
                                        jobrunr-mcp-<next-version>-sources.jar
                                        jobrunr-mcp-<next-version>-javadoc.jar
                                        jobrunr-mcp-<next-version>.pom
                                        ... plus .asc, .md5 and .sha1 for each
```

Do **not** zip `~/.m2/repository` by hand: `publishToMavenLocal` writes no `.md5`/`.sha1` files and Central requires
them. The `centralBundle` task publishes to a throwaway file repository under `build/` precisely to get them, and it
leaves out `maven-metadata.xml`, which does not belong in a bundle.

Worth checking before uploading: the pom declares `org.jobrunr:jobrunr:8.5.0`, and there is no `.module` file. Gradle
module metadata is disabled on purpose, because it would record the core version this jar was built against instead of
the minimum supported one.

### Uploading

1. Go to https://central.sonatype.com/publishing and upload the zip.
2. Wait for the deployment to reach `VALIDATED`.
3. Press **Publish**. Until you do, nothing is released; a failed validation can be dropped and re-uploaded.

Releases are permanent, so a published version can never be replaced — only superseded by a new one.

### Bumping the minimum JobRunr version

`minimumJobRunrVersion` in `build.gradle` is what consumers get in the pom, but the module compiles against
`project(':core')`. Before raising it, confirm the sources still compile against the version you are about to declare —
that is the whole point of the property.
