# Contributing to the Blaaiz Java SDK

Thank you for considering a contribution to the Blaaiz Java SDK. This document gives the
guidelines for work on the project.

## Getting Started

1. Fork the repository.
2. Clone your fork: `git clone https://github.com/your-username/blaaiz-java-sdk.git`
3. Make sure you have JDK 11 or later and Maven 3.8 or later.
4. Create a branch: `git checkout -b feature/your-feature-name`

## Development Workflow

### Run the tests

```bash
# All tests
mvn test

# One test class
mvn test -Dtest=CustomerServiceTest

# One test method
mvn test -Dtest=CustomerServiceTest#createSendsCustomerPayload

# Tests with a coverage report (written to target/site/jacoco/index.html)
mvn test
```

### Build

```bash
# Compile and package the jar
mvn package

# Install into your local repository
mvn install

# Build the artifacts that Maven Central requires, without signing them
mvn -Prelease -DskipTests -Dgpg.skip=true package
```

### Generate the Javadoc

```bash
mvn javadoc:javadoc
```

## Code Style

- Keep to the style of the code around you.
- Use 4 spaces for indentation. Do not use tabs.
- Keep lines at 120 characters or less.
- Write Javadoc for public types and public methods.
- Add tests for new functionality.
- Update the documentation when behavior changes.

## API Guidelines

- Keep to the existing naming conventions.
- Use `snake_case` for API parameter keys, because the Blaaiz API uses `snake_case`.
- Use `camelCase` for Java variables and methods.
- Validate required fields before you send a request.
- Throw `BlaaizException` for API and transport failures. Throw `IllegalArgumentException`
  for local input validation failures.

### Parity with the other SDKs

The Blaaiz SDKs for Java, Node.js, Laravel/PHP, and Python expose the same API surface. If you
add or change a method here, record whether the other SDKs need the same change. When behavior
looks incorrect but the other SDKs do the same thing, keep the parity and record the problem in
a Javadoc comment. Do not make one SDK different on its own.

## Testing

- Write unit tests for all new methods.
- Mock the `OkHttpClient` transport. Do not make network calls in unit tests.
- Test the success path and the failure path.
- Keep tests short and easy to read.

## Pull Request Process

1. Make sure `mvn test` passes.
2. Update `README.md` and the pages in `docs/` if you added or changed a feature.
3. Make sure the build gives no new warnings.
4. Make sure all CI checks pass.
5. Ask a maintainer for a review.

## Reporting Issues

- Use GitHub Issues for bug reports and feature requests.
- Give the steps to reproduce the problem.
- Include a code sample if you can.
- Give your environment: JDK version, SDK version, and operating system.

## Security

- Never commit API keys, client secrets, or webhook secrets.
- Report security vulnerabilities privately. Refer to [SECURITY.md](SECURITY.md).
- Use environment variables for sensitive values.

## Questions?

If you have a question about a contribution:

1. Look at the open issues and discussions.
2. Open a new issue with the "question" label.
3. Send an email to onboarding@blaaiz.com.

Thank you for your contributions.
