# CareTrack Learning Guide

A study companion for the Java side of CareTrack, written for someone who already knows React, Node/Express and SQL. Every file and method named here exists in this repository.

**Suggested reading order (about 1 hour):**

1. `entity/RequestStatus.java`: the business rules in about 40 lines.
2. `service/SupportRequestService.java`: where the rules are enforced.
3. `controller/AgentRequestController.java`: how HTTP reaches the service.
4. `entity/SupportRequest.java` and `RequestEvent.java`: how Java objects map to tables.
5. `exception/GlobalExceptionHandler.java`: how errors become JSON.
6. `src/test/.../RequestStatusTest.java`, then `SupportRequestServiceTest.java`, then `RequestApiIntegrationTest.java`.

---

## 1. One request, end to end: "Change status"

An agent picks **Resolved**, types an explanation, and clicks **Change status**.

**React (frontend/src)**

1. `agent/StatusChangeForm.tsx` → `handleSubmit` trims the explanation, checks it's present, disables the button (`pending`), and calls `onSubmit`.
2. `agent/AgentRequestDetail.tsx` passes that to `agentApi.changeStatus(...)` in `api.ts`, which does `fetch('POST /api/agent/requests/CT-100004/status-changes', { newStatus, explanation })`.
3. In development, Vite's proxy (`vite.config.ts`) forwards `/api/...` to `localhost:8080`.

**Controller (HTTP layer)**

4. Spring's `DispatcherServlet` matches the URL to `AgentRequestController.changeStatus` using `@RequestMapping("/api/agent/requests")` + `@PostMapping("/{reference}/status-changes")`.
5. Jackson turns the JSON body into a `ChangeStatusBody` record. Its **compact constructor** trims `explanation` (and turns `"   "` into `null`).
6. `@Valid` runs Bean Validation (`@NotNull`, `@NotBlank`, `@Size`). On failure Spring throws `MethodArgumentNotValidException` *before your method runs*. `GlobalExceptionHandler.handleValidation` turns it into a 400 with `fieldErrors`.

**Service (business rules)**

7. `SupportRequestService.changeStatus` is annotated `@Transactional`, so Spring opens a database transaction before the method body runs.
8. `loadAny(reference)` normalizes the reference and calls `repository.findByReferenceNumber(...)`. If nothing is found, `orElseThrow` throws `RequestNotFoundException` (→ 404).
9. `current.canTransitionTo(next)` checks the rule table in `RequestStatus`. If it's false, `InvalidStatusTransitionException` is thrown (→ 409), and the transaction rolls back with nothing written.
10. `request.applyStatus(next, now)` changes the entity in memory. `request.addEvent(RequestEvent.statusChange(...))` appends the history entry. The event type is `REOPENED` when `current.isReopeningTo(next)` is true.

**Repository and database**

11. `repository.saveAndFlush(request)` makes Hibernate write the changes now: an `UPDATE support_request …` (status, updated_at, row_version) and, through `cascade = CascadeType.ALL`, an `INSERT INTO request_event …`.
12. The method returns, Spring **commits** the transaction, and both rows become permanent together.

**Back to React**

13. The service maps the entity to a `RequestDetailResponse` record (`RequestDetailResponse.from`). The controller wraps it in `ResponseEntity.status(201)`, and Jackson writes JSON.
14. `api.ts` resolves the promise, `setData(...)` replaces the displayed request, and the timeline re-renders with the new event. If the response was a 409, `api.ts` throws an `ApiError` and the form shows its message.

> Try it: put a breakpoint (or a `log.info`) in `changeStatus`, then run `scripts/api-demo.sh` and watch the order of events.

---

## 2. Express → Spring Boot

| Express / Node | Spring Boot in CareTrack |
| --- | --- |
| `const app = express(); app.listen(8080)` | `CareTrackApplication.main` → `SpringApplication.run`; embedded Tomcat on `server.port` |
| `const router = express.Router()` | A `@RestController` class with `@RequestMapping("/api/agent/requests")` |
| `router.post('/:reference/updates', handler)` | `@PostMapping("/{reference}/updates")` on a method |
| `req.params.reference` | `@PathVariable String reference` |
| `req.query.status` | `@RequestParam(required = false) RequestStatus status` (converted to an enum for you) |
| `req.body` + `express.json()` | `@RequestBody AddMessageBody body` (Jackson) |
| Validation middleware (Joi/zod/express-validator) | Bean Validation annotations on the record + `@Valid` |
| `res.status(201).json(x)` | `return ResponseEntity.status(HttpStatus.CREATED).body(x)` |
| Error middleware `(err, req, res, next)` | `@RestControllerAdvice` class with `@ExceptionHandler` methods |
| `require('./service')` / passing deps by hand | Dependency injection: Spring creates `@Service`/`@Component` objects and passes them to constructors |
| A `db.js` / Knex / Prisma model | `@Entity` classes + a `JpaRepository` interface |
| `prisma.request.findMany({ where: { status }, orderBy })` | `findByStatusOrderByUpdatedAtDescIdDesc(status)`: Spring Data writes the query from the method name |
| `await db.$transaction(async tx => { ... })` | `@Transactional` on a service method |
| `.env` + `process.env.PORT` | `application.properties` + environment variables (`${CARETRACK_DB_PATH:./data/caretrack}`) |
| `cors()` middleware | `AppConfig.addCorsMappings` |
| `nodemon` / `npm run dev` | `./mvnw spring-boot:run` |
| `package.json` + `npm` | `pom.xml` + Maven (`./mvnw` is like a pinned `npx`) |
| Jest + Supertest | JUnit 5 + MockMvc (`RequestApiIntegrationTest`) |
| `jest.mock('./repo')` | Mockito `@Mock` (`SupportRequestServiceTest`) |

The biggest mindset shift: in Express *you* wire everything together. In Spring you **declare** things (annotations), and the framework builds and connects the objects at startup.

---

## 3. Java concepts actually used here

| Concept | Where to look | What to notice |
| --- | --- | --- |
| Packages and `import` | every file | Package = folder. `com.caretrack.service` lives in `service/`. |
| Access modifiers | `SupportRequest` | Fields are `private`; changes go through methods (`applyStatus`, `addEvent`). `getEvents()` returns an **unmodifiable** list. |
| Constructors and `final` fields | `SupportRequestService` | Dependencies are `private final` and set once in the constructor (constructor injection). |
| `protected` no-arg constructor | `SupportRequest`, `RequestEvent` | JPA needs it to build objects from database rows; application code uses the full constructor. |
| Static factory methods | `RequestEvent.created(...)`, `.statusChange(...)` | Named constructors that guarantee each event type gets the right fields. |
| Enums with behavior | `RequestStatus` | Enums are classes: they can have methods (`canTransitionTo`). |
| Switch expressions (Java 14+) | `RequestStatus.allowedNextStatuses`, `Timeline.tsx` (TS equivalent) | `case OPEN -> ...` returns a value. The compiler checks that every enum constant is handled. |
| `EnumSet` | `RequestStatus` | A fast, ordered `Set` for enum values. |
| Records (Java 16+) | everything in `dto/` | Immutable data carriers: constructor, accessors (`body.message()`), `equals`, `toString` for free. |
| Compact constructors | `CreateRequestBody` | Code that runs inside a record's constructor. Used here to trim input before validation. |
| Nested record | `ApiErrorResponse.FieldError` | A small type that only makes sense inside another. |
| Annotations | everywhere | Metadata that frameworks read at runtime (`@Entity`, `@Transactional`, `@NotBlank`). |
| Interfaces and generics | `SupportRequestRepository extends JpaRepository<SupportRequest, Long>` | `<SupportRequest, Long>` = entity type and ID type. You write only the interface; Spring generates the class. |
| `Optional` | `findByReferenceNumber(...).orElseThrow(...)` | An explicit "maybe empty" instead of `null`. |
| Lambdas and method references | `.map(RequestSummaryResponse::from)`, `() -> new RequestNotFoundException(...)` | Like JS arrow functions. `Class::method` is shorthand for a lambda. |
| Streams | `listForAgent`, `RequestDetailResponse.from` | `stream().map(...).sorted(...).toList()` ≈ JS `.map().sort()`, but lazy and typed. |
| `Comparator` chaining | `RequestDetailResponse.CHRONOLOGICAL` | `comparing(createdAt).thenComparing(id)`: predictable tie-breaking. |
| Unchecked exceptions | `exception/` | Both custom exceptions extend `RuntimeException`, so callers don't need `throws` clauses. The handler maps them to HTTP codes. |
| `java.time` | `Instant`, `Clock`, `Duration`, `ChronoUnit` | `Instant` = a UTC moment. An injected `Clock` makes "now" testable. |
| `static final` constants | `ValidationLimits`, `DemoActors` | Compile-time constants can be used inside annotations (`@Size(max = SUBJECT_MAX)`). |
| Static imports | `SupportRequestService`, tests | `import static ...DEMO_AGENT_NAME;` lets you write the name without the class. |
| Text blocks (Java 15+) | `RequestApiIntegrationTest` | `"""` multi-line strings for JSON bodies; `.formatted(...)` fills in `%s`. |
| `var` | `DemoDataSeederTest` | Local type inference, like `const x = ...` when the type is obvious. |

---

## 4. Five small changes to make yourself

Do them in order; each ends with a test. Commit each one separately, so your own work shows in Git history.

**1. Add a category (warm-up: enums across the stack)**
Add `DEVICE_SUPPORT` ("Device support").
- Java: `entity/RequestCategory.java`.
- TypeScript: `types.ts` (`RequestCategory` union) and `labels.ts` (`CATEGORY_LABELS`).
- Verify: create one via `api-examples.http`; filter by it in the agent view.
- Note that no database migration was needed: categories are stored as strings (`EnumType.STRING`). Why would `ORDINAL` have been dangerous here?

**2. Test the reference generator (Mockito practice)**
Create `src/test/java/com/caretrack/service/ReferenceNumberGeneratorTest.java`.
- Test 1: 100 generated references all match `CT-[A-HJKMNP-Z2-9]{6}` (no 0, 1, I, L, O).
- Test 2: mock the repository so `existsByReferenceNumber` returns `true` once, then `false` (`when(...).thenReturn(true, false)`). Assert that `nextUnique()` still returns a reference and that `existsByReferenceNumber` was called twice (`verify(repo, times(2))`).

**3. Status counts endpoint (repository + service + controller)**
`GET /api/agent/requests/counts` → `{"OPEN": 2, "IN_PROGRESS": 3, "WAITING_ON_CUSTOMER": 1, "RESOLVED": 1}`.
- Repository: `long countByStatus(RequestStatus status);`
- Service: loop over `RequestStatus.values()` into an `EnumMap<RequestStatus, Long>`, in a `@Transactional(readOnly = true)` method.
- Controller: `@GetMapping("/counts")`. Does it clash with `@GetMapping("/{reference}")`? (Spring prefers the literal path, but test it.)
- Integration test in `RequestApiIntegrationTest` style.

**4. A new business rule: no agent updates on resolved requests**
An agent must reopen a resolved request before posting an update.
- In `SupportRequestService.addAgentUpdate`, throw an exception when status is `RESOLVED`. Create `RequestClosedException` and map it to 409 in `GlobalExceptionHandler`.
- Unit test in `SupportRequestServiceTest`: update on RESOLVED throws and `saveAndFlush` is `never()` called. Update the integration lifecycle test if it breaks.
- Frontend: hide or disable the update form when `request.status === 'RESOLVED'`, with a short explanation. (The backend rule still holds without the UI change. That's the point.)

**5. Subject search filter (derived queries + optional params)**
`GET /api/agent/requests?q=bill`, case-insensitive match on the subject.
- Repository: `findBySubjectContainingIgnoreCaseOrderByUpdatedAtDescIdDesc(String q)`.
- Now you have three optional filters, and the `if/else` chain in `listForAgent` gets ugly. Either combine filters in Java (`stream().filter(...)`) for now, or research **Spring Data JPA Specifications**. Be ready to explain the trade-off.
- Add a search input to `AgentView.tsx` (with a `<label>`).

---

## 5. Interview questions about this code

**Q1. Where are the status rules enforced, and why not in React?**
`RequestStatus.canTransitionTo` defines them, and `SupportRequestService.changeStatus` enforces them before saving. The UI is just one client. Anyone can call the API with curl (`api-examples.http` has a forbidden OPEN → RESOLVED example that returns 409). The UI shows only valid options because the API returns `allowedNextStatuses`, so the rules live in exactly one place.

**Q2. How do you guarantee a status change and its history entry are saved together?**
`changeStatus` is `@Transactional`. The entity change and the new `RequestEvent` are saved in one `saveAndFlush`, with the event cascaded through `@OneToMany(cascade = ALL)`. If anything throws before commit, the whole transaction rolls back. `forbiddenTransitionReturns409AndWritesNothing` checks that a rejected change leaves status and history untouched. *Follow-up worth knowing:* `@Transactional` works through a Spring proxy, so calling a `@Transactional` method from another method in the same class bypasses it. `DemoDataSeeder` avoids relying on that.

**Q3. Why return DTOs instead of the entities?**
Four reasons: (1) control the API shape, e.g. the internal `id` and `customerAccountId` are never exposed; (2) avoid lazy-loading surprises during JSON rendering, since `open-in-view` is off and mapping happens inside the transaction; (3) change the database without breaking clients; (4) different views need different shapes. The list uses `RequestSummaryResponse` without description or history.

**Q4. Why 409 for an invalid transition and 400 for validation errors?**
400 means the request itself is malformed (missing explanation, unknown enum). 409 means the request is well-formed but conflicts with the resource's *current state*: RESOLVED is a valid status, just not reachable from OPEN.

**Q5. How does validation handle `"   "`?**
The record's compact constructor calls `ValidationLimits.clean`, which trims and turns blank strings into `null`. Then `@NotBlank` reports "required" once. Without that, whitespace could pass `@Size(min = 5)` or produce two errors. `RequestValidationTest.whitespaceOnlyCountsAsMissingAndIsReportedOncePerField` covers it.

**Q6. What does `@Version` do here?**
Hibernate adds `WHERE row_version = ?` to each update and increments the value. If two agents edit the same request at once, the second save matches zero rows and Spring throws `ObjectOptimisticLockingFailureException`, which the handler returns as a 409 instead of silently overwriting.

**Q7. Why inject a `Clock` instead of calling `Instant.now()`?**
Testability and consistency. `SupportRequestServiceTest` uses `Clock.fixed(...)`, so it can assert exact timestamps. Timestamps are truncated to microseconds because that's what the database stores, so a value reads back exactly as written.

**Q8. How is seed data idempotent? What breaks it?**
`DemoDataSeeder.seed` inserts each request only if `existsByReferenceNumber` is false. Restarts skip existing ones (`seedingTwiceDoesNotCreateDuplicates`). Weakness: two app instances starting at the same moment could both see "missing". The unique constraint on `reference_number` would make one insert fail rather than duplicate. A real system would seed through a migration tool.

**Q9. How are reference numbers generated, and could they collide?**
`ReferenceNumberGenerator`: `CT-` + 6 characters from a 31-character alphabet with no look-alikes (~887 million combinations), using `SecureRandom`. It checks the database and retries up to 10 times, and the unique constraint is the final guard. Seed references use 0 and 1, which the generator never produces. *Trade-off:* the check-then-insert has a tiny race window; a database sequence would avoid it but gives guessable references.

**Q10. What's the difference between your unit and integration tests?**
Unit tests (`RequestStatusTest`, `SupportRequestServiceTest`, `RequestValidationTest`) run without Spring or a database, in milliseconds; Mockito stands in for the repository. Integration tests (`@SpringBootTest` + MockMvc) run the real controllers, validation, JSON, JPA and an in-memory H2 to prove the layers work together, e.g. that the history order survives a round trip to the database.

**Q11. Is there an N+1 query problem?**
The list endpoints map to `RequestSummaryResponse`, which never touches the lazy `events` collection, so a list is one query. The detail endpoint loads one request plus its events (two queries). If a list ever needed event data, I'd use a fetch join or `@EntityGraph`.

**Q12. How would you add real security?**
Add Spring Security. Authenticate users (session or JWT/OAuth2). Restrict `/api/agent/**` to an `AGENT` role. Replace `DEMO_CUSTOMER_ACCOUNT_ID` with the logged-in user's account in the customer queries, and record the real agent's identity in `actorName`. Today, switching views in the UI provides no protection; the README says so explicitly.

**Q13. What would you improve first?**
A good honest answer: authentication (Q12), Flyway migrations instead of `ddl-auto=update`, pagination, and replacing the `if/else` filter chain with Specifications once a third filter appears.
