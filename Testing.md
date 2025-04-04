# Testing

## Postgres

`docker run -p 5432:5432 -e POSTGRES_PASSWORD=postgres -d postgres`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres
```

## MySQL

`docker run -p 3306:3306 -e MYSQL_ROOT_PASSWORD=mysql -e MYSQL_DATABASE=mysql -d mysql:5.7`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mysql
spring.datasource.username=root
spring.datasource.password=mysql
```

## MariaDB

`docker run -p 3306:3306 -e MYSQL_ROOT_PASSWORD=mysql -e MYSQL_DATABASE=mysql -d mariadb`

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/mysql
spring.datasource.username=root
spring.datasource.password=mysql
```

## SQL Server

`docker run -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=sqlServer(!)' -p 1433:1433 -d mcr.microsoft.com/mssql/server:2017-latest`

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=tempdb
spring.datasource.username=sa
spring.datasource.password=sqlServer(!)
```

## Oracle

`docker run -d --env DB_PASSWD=oracle -p 1527:1521 -p 5507:5500 -it --shm-size="8g" container-registry.oracle.com/database/standard:12.1.0.2`

```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1527:ORCL
spring.datasource.username=system
spring.datasource.password=oracle
```

## DB2

`docker run -itd --privileged=true -p 50000:50000 -e LICENSE=accept -e DB2INST1_PASSWORD=db2password -e DBNAME=testdb ibmcom/db2`

```properties
spring.datasource.url=jdbc:db2://127.0.0.1:50000/testdb
spring.datasource.username=db2inst1
spring.datasource.password=db2password
```

## Mongo

`docker run -d -p 27017:27017 mongo:4.4`

```java

@Bean
public StorageProvider storageProvider(JobMapper jobMapper) {
    final MongoDBStorageProvider dbStorageProvider = new MongoDBStorageProvider(mongoClient(), rateLimit().withoutLimits());
    dbStorageProvider.setJobMapper(jobMapper);
    return dbStorageProvider;
}

private MongoClient mongoClient() {
    CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),
            MongoClientSettings.getDefaultCodecRegistry()
    );
    return MongoClients.create(
            MongoClientSettings.builder()
                    .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress("127.0.0.1", 27017))))
                    .codecRegistry(codecRegistry)
                    .build());
}
```

## DocumentDB

- Start new DocumentDB instance in cloud with username jobrunr and password jobrunr123.
  note: TLS must be enabled, see https://stackoverflow.com/questions/68322959/why-am-i-getting-connection-timed-out-when-connecting-to-aws-document-db-from-my
- Create a new Ubuntu EC2 instance in the same VPC (Virtual Private Cloud) and GENERATE a new key pair (will be downloaded automatically). Call it
  default-documentdb.pem
- chmod 400 default-documentdb.pem
- Change the Inbound Rules of the EC2 instance and ADD A NEW rule to allow access from everywhere
- Create a new DocumentDB cluster parameter group and disable TLS
- Assign the new Cluster Parameter Group to the DocumentDB cluster
- Restart the cluster
- Use the link provided by Amazon and create a ConnectionString with it passing it to the client

```java

@Bean
private MongoClient mongoClient() {
    CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),
            MongoClientSettings.getDefaultCodecRegistry()
    );
    if (mongoClient == null) {
        mongoClient = MongoClients.create(
                MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString("mongodb://jobrunr:jobrunr123@docdb-2023-04-24-09-47-54.cluster-cjpre4alt9oy.us-east-1.docdb.amazonaws.com:27017/?replicaSet=rs0&readPreference=secondaryPreferred&retryWrites=false"))
                        .codecRegistry(codecRegistry)
                        .build());

    }
    return mongoClient;
}
```