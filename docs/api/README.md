# llm-service — API Reference
> Generated from source code. Regenerate instead of editing by hand.
> Notes inside <!-- manual:start --> ... <!-- manual:end --> blocks survive regeneration.

Melissa chat / LLM microservice. Exposes authenticated chat-conversation management, message
exchange with the AI assistant (blocking and streaming), public device-scoped guest chat, and
product-suggestion lookup. `spring.application.name = llm-service`.

## Base URL

No `server.servlet.context-path` is set in any profile, so endpoints are served at the bare root
(paths begin directly at `/api/...`, no context prefix).

- `server.port` is `${PORT}` in `prod`/`dev` (env-injected, no default) and `${PORT:8090}` in
  `local`.
- Per the deploy runbook the service listens on **8090**, and the api-gateway fronts it on **8081**.
- Effective local base URL: `http://localhost:8090`.

This is an **internal/downstream service**. Externally it is reached through the api-gateway, which
authenticates the end user and injects the signed identity headers described below. Do not call it
directly from clients in production.

## Authentication

Header-based downstream trust — **not** a client-facing JWT. There is no bearer token validated here.
The api-gateway authenticates the end user and forwards three signed headers, verified by
`PrincipalAuthFilter` (a `OncePerRequestFilter` registered before `UsernamePasswordAuthenticationFilter`):

| Header | Description |
|---|---|
| `X-User-Id` | The authenticated user id. Becomes the `Authentication` principal. |
| `X-User-Roles` | Comma-separated roles; each token becomes a `SimpleGrantedAuthority` (e.g. `ROLE_CLIENT`). |
| `X-Auth-Signature` | Base64 HMAC over the payload `userId + "\|" + roles` using the shared `application.downstream.hmacSecret` (`DownstreamHmacSigner`). Verified with constant-time `MessageDigest.isEqual`; a mismatch → 401 and the request is aborted. |

If any of the three headers is missing, the filter does nothing (no authentication is set) and the
request proceeds anonymously — `SecurityConfig`'s `.anyRequest().authenticated()` then rejects it
with **401** unless the path is permit-all. Session policy is `STATELESS`; CSRF is disabled.

**Permit-all (no auth) paths:** `/actuator/**`, `/v3/api-docs`, `/v3/api-docs/**`, `/swagger-ui/**`,
`/swagger-ui.html`, `/api/guest/free-chat`, `/api/guest/messages`. The guest endpoints are public but
require an `X-Device-Id` header instead of the identity headers. Internal servlet dispatches
(`ASYNC`/`FORWARD`/`ERROR`) are also permit-all so the async re-dispatch that finalizes SSE `Flux`
streams isn't denied on an empty `SecurityContext`.

**Permissions** are role/authority-based via the authorities derived from `X-User-Roles`. There are
no custom `@PreAuthorize`/permission annotations on the controllers in this service — gating is the
binary authenticated-vs-public split plus whatever the gateway enforces upstream. Several endpoints
are marked `@io.swagger.v3.oas.annotations.Hidden` (excluded from the OpenAPI/Swagger surface but
still reachable): chat activate, message send-stream, and product-suggestion products.

## Standard response envelope

Every endpoint wraps its payload in `CommonResponse<T>` (`uz.melisa.dto.common.CommonResponse`):

| Field | Type | Serialized | Description |
|---|---|---|---|
| `data` | T | when non-null | Success payload. Omitted from JSON when `null` (`@JsonInclude(NON_NULL)`). |
| `errorMessage` | String | when non-null | Localized error text on failure. Omitted when `null`. |
| `status` | `ApiResponseStatus` | never (`@JsonIgnore`) | Drives the HTTP status code only (`ResponseEntity.status(status.getHttpStatus())`); never appears in the body. |

Built via static factories `CommonResponse.success(data)` (status `OK`) and
`CommonResponse.failure(message, status)`. Because `data`/`errorMessage` are `NON_NULL` and `status`
is ignored, a success body contains only `data` and an error body only `errorMessage`.

Success example (HTTP 200):
```json
{
  "data": { "message": "Chat created successfully" }
}
```

Error example (HTTP status comes from the `ApiResponseStatus`; body carries only the message):
```json
{
  "errorMessage": "Ma'lumot topilmadi"
}
```

### Error format

A single global handler, `uz.melisa.exp.ExceptionHelper` (`@RestControllerAdvice`,
`@Order(Integer.MIN_VALUE)` — highest precedence), returns the standard envelope with only
`errorMessage` populated. The text is localized from a `MessageCode` via `LocalizationService` using
the request `Accept-Language` / `Locale` (supported: `uz`, `ru`, `en`; default `uz`). Raw business
strings are never echoed to the client. The HTTP status comes from the `ApiResponseStatus` passed to
`CommonResponse.failure`.

| Exception | HTTP status | Message code |
|---|---|---|
| `BusinessException` | from `e.getStatus()` | `e.getMessageCode()`, or `COMMON_SOMETHING_WENT_WRONG` if the code is null |
| `BadCredentialsException` | 401 Unauthorized | `AUTH_INVALID_CREDENTIALS` |
| `MethodArgumentNotValidException` | 400 (`INVALID_PARAMETER`) | `COMMON_INVALID_INPUT` |
| `EntityNotFoundException` | 404 Not Found | `COMMON_DATA_NOT_FOUND` |
| `UsernameNotFoundException` | 404 Not Found | `AUTH_USER_NOT_FOUND` |
| `MethodArgumentTypeMismatchException` | 400 Bad Request | `COMMON_INVALID_INPUT` |
| `NoResourceFoundException` | 404 (`NO_STATIC_RESOURCES`) | `COMMON_PAGE_NOT_FOUND` |
| `uz.melisa.exp.AuthenticationException` | 401 Unauthorized | `AUTH_SESSION_INVALID` |
| `Exception` (catch-all) | 500 Internal Server Error | `COMMON_SOMETHING_WENT_WRONG` |

Example error body: `{ "errorMessage": "Kiritilgan ma'lumot noto'g'ri" }`.

> **SSE caveat:** for the catch-all `Exception` handler, if the response is already committed or is a
> `text/event-stream` (SSE) response, the error body is suppressed and **204 No Content** is returned
> instead, to avoid corrupting an in-flight stream.

## Resources

| Resource | Endpoints | Description |
|---|---|---|
| [Chat](./chat.md) | 7 | Authenticated CRUD over a user's chat conversations: create, update, list (paged), get by id, list messages (paged), delete, plus a hidden activate-chat-by-key endpoint. |
| [Guest Chat](./guest-chat.md) | 2 | Public (no-auth) device-scoped guest chat keyed by an `X-Device-Id` header: send a free-chat message and fetch paged guest message history. |
| [Message](./message.md) | 3 | Send a chat message to the LLM (blocking JSON), a hidden SSE token-streaming variant (send-stream), and delete a message. |
| [Product Suggestion](./product-suggestion.md) | 1 | Hidden endpoint returning the list of product suggestions attached to a given assistant message. |

## Data models

See [models.md](./models.md). Shared types include the `CommonResponse<T>` envelope,
`ResponseMessageDTO`, `ChatMessagesDTO`, the `ApiResponseStatus` (HTTP-status) and
`MessageContentType` enums, and the `Page<T>` pagination envelope, alongside the per-resource request
and response DTOs.

<!-- manual:start -->
<!-- manual:end -->
