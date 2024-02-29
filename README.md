# jobsprintr

> [!NOTE]
> This is a fork of https://github.com/d1hq/jobsprintr

## How to build?
* `cd jobsprintr`
* `cd core/src/main/resources/org/jobrunr/dashboard/frontend`
* `npm i`
* `npm run build`
* `cd -`
* `./gradlew publishToMavenLocal`

## Setup local development environmnt
Build eclipse files - import to Eclipse
* `./gradlew eclipse`
Build without testing
* `./gradlew assemble`
Publish to local repository
* `./gradlew publishToMavenLocal`
