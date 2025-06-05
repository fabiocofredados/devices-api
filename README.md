# REST API to persist and manage device resources.

## Development Stack

* Java 21
* Spring boot
* Spring JPA
* Spring Actuator
* Junit
* Mockito
* Mysql

## Development Details
* The development is in layers.
* Domain, Repository, Mapper, Service and Controller
* The business logic is in Service and Mapper layers.

## Build and Deploy

* Build the project:
    * Open a terminal
    * Go to project root folder
    * Type: ``` ./gradlew bootJar ```
    * Type: ```docker compose up -d ```

* API Usage
    * Access the url: http://localhost:8280/swagger-ui/index.html#/
