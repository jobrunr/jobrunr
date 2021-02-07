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

## Redis

`docker run -d -p 6379:6379 redis`

### Jedis

```java
    @Bean
public StorageProvider storageProvider(JobMapper jobMapper){
final JedisRedisStorageProvider jedisRedisStorageProvider=new JedisRedisStorageProvider(getJedisPool(),rateLimit().withoutLimits());
        jedisRedisStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return jedisRedisStorageProvider;
        }

private JedisPool getJedisPool(){
        return new JedisPool("127.0.0.1",6379);
        }
```

### Lettuce

```java
    @Bean
public StorageProvider storageProvider(JobMapper jobMapper){
final LettuceRedisStorageProvider lettuceRedisStorageProvider=new LettuceRedisStorageProvider(getRedisClient(),rateLimit().withoutLimits());
        lettuceRedisStorageProvider.setJobMapper(jobMapper);
        return lettuceRedisStorageProvider;
        }

private RedisClient getRedisClient(){
        return RedisClient.create(RedisURI.create("127.0.0.1",6379));
        }
```

## Mongo

`docker run -d -p 27017:27017 mongo:4.4`

```java
    @Bean
public StorageProvider storageProvider(JobMapper jobMapper){
final MongoDBStorageProvider dbStorageProvider=new MongoDBStorageProvider(mongoClient(),rateLimit().withoutLimits());
        dbStorageProvider.setJobMapper(jobMapper);
        return dbStorageProvider;
        }

private MongoClient mongoClient(){
        CodecRegistry codecRegistry=CodecRegistries.fromRegistries(
        CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),
        MongoClientSettings.getDefaultCodecRegistry()
        );
        return MongoClients.create(
        MongoClientSettings.builder()
        .applyToClusterSettings(builder->builder.hosts(Arrays.asList(new ServerAddress("127.0.0.1",27017))))
        .codecRegistry(codecRegistry)
        .build());
        }
```

## ElasticSearch

`docker run -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:7.9.1`

```java
    @Bean
public StorageProvider storageProvider(JobMapper jobMapper){
final ElasticSearchStorageProvider elasticSearchStorageProvider=new ElasticSearchStorageProvider(getElasticSearchClient(),rateLimit().withoutLimits());
        elasticSearchStorageProvider.setJobMapper(jobMapper);
        return elasticSearchStorageProvider;
        }

private RestHighLevelClient getElasticSearchClient(){
        return new RestHighLevelClient(
        RestClient.builder(
        new HttpHost("127.0.0.1",9200,"http")));

        }
```