# Spring cloud stream + RabbitMQ + Protobuf
This repository contains RabbitMQ Protobuf starters with its usage
samples for `spring-rabbit` and
`spring-cloud-starter-stream-rabbit` modules

## Quickstart

```bash
git clone --depth=0 https://github.com/daggerok/spring-cloud-stream-protobuf-rabbitmq.git my-app && cd $_
```

```bash
./mvnw -f rabbitmq docker:start
./mvnw clean test
./mvnw -f rabbitmq docker:stop docker:remove
```

## Integration testing

```bash
rm -rf ~/.m2/repository/com/github/daggerok
./mvnw -f rabbitmq docker:start
./mvnw clean install -DskipTests
./mvnw -f consumer spring-boot:run # to create durable, then CTRL+C
./mvnw -f producer spring-boot:run # then in a separate terminal:
http :8080 message="Hello, World"  # and press CTRL+C for producer
./mvnw -f consumer spring-boot:run # check message and press CTRL+C
./mvnw -f rabbitmq docker:stop docker:remove
```

<!--

# Getting Started

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.5.5/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/2.5.5/maven-plugin/reference/html/#build-image)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.5.5/reference/htmlsingle/#boot-features-developing-web-applications)

### Guides

The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/bookmarks/)

-->
