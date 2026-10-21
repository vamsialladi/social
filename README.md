# Spring Boot Project Structure

A clean, standard layered architecture skeleton for Spring Boot.

## Directory Layout

```text
social/
├── pom.xml                                      # Maven project configuration
├── .gitignore                                   # Standard Java/Maven gitignore
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/social/
    │   │       ├── SocialApplication.java      # Application main entry point
    │   │       ├── config/                     # Configuration classes (Security, WebMvc, Swagger)
    │   │       ├── controller/                 # REST API Controllers (@RestController)
    │   │       ├── dto/                        # Data Transfer Objects
    │   │       │   ├── request/                # Request payloads / Records
    │   │       │   └── response/               # Response payloads / Records
    │   │       ├── entity/                     # JPA Entities / Database Models (@Entity)
    │   │       ├── exception/                  # Custom Exceptions & GlobalExceptionHandler
    │   │       ├── repository/                 # Spring Data JPA Repositories (@Repository)
    │   │       ├── service/                    # Business Service Interfaces
    │   │       │   └── impl/                   # Service Implementations (@Service)
    │   │       └── util/                       # Constants, Helper methods, and Utilities
    │   └── resources/
    │       ├── application.yml                 # Main application configuration
    │       ├── application-dev.yml             # Development profile configuration
    │       └── application-prod.yml            # Production profile configuration
    └── test/
        ├── java/
        │   └── com/social/
        │       └── SocialApplicationTests.java # Context loading test
        └── resources/
            └── application-test.yml            # Test profile configuration
```

## Build & Run

- **Compile**: `mvn clean compile`
- **Run Tests**: `mvn test`
- **Run Application**: `mvn spring-boot:run`
