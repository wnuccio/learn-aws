# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**learn-aws** is a learning kata/exercise project focused on AWS integration and Java development. This is a Maven-based Java 17 project designed to explore and practice AWS SDK usage patterns.

## Build System

This project uses **Maven** for dependency management and building. The project targets Java 17.

### Common Maven Commands

**Clean and build:**
```bash
mvn clean compile
```

**Run all tests:**
```bash
mvn test
```

**Run a single test class:**
```bash
mvn test -Dtest=TestClassName
```

**Run a specific test method:**
```bash
mvn test -Dtest=TestClassName#methodName
```

**Package the project:**
```bash
mvn clean package
```

**Check for dependency updates:**
```bash
mvn versions:display-dependency-updates
```

**Format code (if using a code formatter plugin):**
```bash
mvn formatter:format
```

## Project Structure

```
learn-aws/
├── src/
│   ├── main/
│   │   ├── java/              # Main application code
│   │   └── resources/         # Configuration files, properties
│   └── test/
│       └── java/              # JUnit tests
├── pom.xml                    # Maven configuration and dependencies
└── target/                    # Build artifacts (generated)
```

## Key Configuration

- **Java Version:** 17
- **Source Encoding:** UTF-8
- **Group ID:** org.example
- **Artifact ID:** learn-aws
- **Current Version:** 1.0-SNAPSHOT

## Dependencies

The pom.xml is currently minimal. As the project grows, AWS SDK dependencies (such as `software.amazon.awssdk:*`) will be added. When adding AWS dependencies, consider:
- Using the AWS SDK v2 (software.amazon.awssdk)
- Including only the necessary service modules (s3, dynamodb, lambda, etc.)
- Adding test dependencies (JUnit 5, Mockito, etc.) to the `<scope>test</scope>` section

## Recommended Development Workflow

1. **Write tests first:** Create test classes in `src/test/java/`
2. **Implement functionality:** Add implementation in `src/main/java/`
3. **Verify locally:** Run `mvn test` before committing
4. **Keep dependencies minimal:** Only add what's needed for the current kata

## Testing

This project uses JUnit for testing. When dependencies are added:
- Add JUnit 5 (Jupiter) for test framework
- Consider Mockito for mocking AWS service calls
- Run `mvn test` to execute all tests
- Use meaningful test names that describe the behavior being tested

### Conventions
- Prefer a fake implementation of each repository interface (e.g. `FakeS3Repository implements S3Repository`) over a mocking framework; add extra getters on the fake as needed for assertions (e.g. `getObject`, `getItem`), beyond what the interface itself requires
- When multiple tests share setup, extract it into a `TestContext` class in the test package, instead of duplicating it per test
  - dependencies/fakes are fields on `TestContext` (tests need to assert against them afterward)
  - the system under test is exposed as a **method** on `TestContext` (e.g. `helloWorld()`), not a field, so each test gets a fresh instance
  - instantiate a new `TestContext` directly inside each `@Test` method; do not use `@BeforeEach`
- use full variable name `context`, not `ctx`;

## Notes for Future Development

- As AWS features are added, ensure proper credential handling (never commit real AWS credentials)
- Consider using AWS SDK mocking/local testing approaches where applicable
- Keep code examples clean and well-documented for learning purposes