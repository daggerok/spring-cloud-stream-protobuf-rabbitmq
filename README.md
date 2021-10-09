# Spring cloud stream + RabbitMQ + Protobuf [![CI](https://github.com/daggerok/spring-cloud-stream-protobuf-rabbitmq/actions/workflows/ci.yaml/badge.svg?branch=integration-services)](https://github.com/daggerok/spring-cloud-stream-protobuf-rabbitmq/actions/workflows/ci.yaml)

This repository demonstrates microservices pipelines and communications using http and amqp protocols with JSON and
Protobuf

```
 O__
/|   Hey, I wanna create user! 
/ \
     -> UserUI HTTP POST (JSON) request -> UserBackend
        -> UserBackend send converted (Protobuf) message -> RabbitMQ exchange
           -> RabbitMQ exchange send (Protobuf) message -> RabbitMQ UserService queue
              -> UserService send converted received User Entity insert query -> UserStore

 O__
/|   Hey, I wanna fetch user(s) data! 
/ \
     -> UserUI HTTP GET (JSON) request -> UserBackend
        -> UserBackend HTTP GET (Protobuf) request -> UserService
           -> UserService send User fetch query -> UserStore
        <- UserService <- User Entity from UserStore
     <- UserBackend <- Converted HTTP (Protobuf) response from UserService
  <- UserUI <- HTTP (JSON) resp from UserBackend
```

## Quickstart

```bash
git clone --depth=0 https://github.com/daggerok/spring-cloud-stream-protobuf-rabbitmq.git my-app && cd $_

./mvnw -f rabbitmq docker:start
./mvnw -f user-service spring-boot:start
./mvnw -f user-backend spring-boot:start

open http://127.0.0.1:8001
```

## Unit testing

```bash
./mvnw clean test
```

## E2E testing

```bash
rm -rf ~/.m2/repository/microservices
./mvnw clean install -DskipTests
./mvnw -f rabbitmq docker:start

./mvnw -f user-service spring-boot:start
./mvnw -f user-backend -Pe2e test

./mvnw -f user-service spring-boot:stop
./mvnw -f rabbitmq docker:stop docker:remove
```

## Manual integration testing

```bash
rm -rf ~/.m2/repository/microservices
./mvnw clean install -DskipTests
./mvnw -f rabbitmq docker:start

./mvnw -f user-service spring-boot:start # create durable queue
./mvnw -f user-service spring-boot:stop  # simulate downtime
./mvnw -f user-backend spring-boot:start # send messsage
curl -sSv 0:8001/api/v1/users -H'Content-Type:application/json' -d'{"username":"maksimko"}'
./mvnw -f user-service spring-boot:start # check message received

./mvnw -f user-backend spring-boot:stop
./mvnw -f user-service spring-boot:stop
./mvnw -f rabbitmq docker:stop docker:remove
```

## RTFM

- [Vue Petite](https://github.com/vuejs/petite-vue/blob/main/examples/todomvc.html)
- [RU / UA Roboto fonts](https://fonts.google.com/specimen/Roboto?preview.text=Almost%20before%20the%20ground.%20%D0%9E%D1%85%20%D0%B8%20%D0%B4%D0%B0!&preview.text_type=custom)
- [Maven Unit / Integration testing](https://www.baeldung.com/maven-integration-test)
- [Maven surefire plugin](https://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html)
