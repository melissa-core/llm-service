# Message API

Endpoints for sending a chat message to the AI assistant (synchronous and streaming) and for soft-deleting a message. All routes are under the class prefix `/api/v1/message`.

> Response envelope, auth header model, and the global error format are documented once in [README.md](./README.md). Each endpoint below documents only the payload type `T` inside `CommonResponse<T>`.

---

## POST /api/v1/message/send

### Purpose
Sends a user message to the AI assistant and returns the assistant's full reply in a single synchronous response. If `chatId` is omitted a brand-new chat is created (titled from the message); otherwise the message is appended to the caller's existing chat. The response indicates whether the reply is a product recommendation or a general answer.

### Authentication
Header-based downstream trust (see [README.md](./README.md)). Requires valid `X-User-Id` / `X-User-Roles` / `X-Auth-Signature` identity headers injected by the api-gateway. No role/permission annotation — only the binary authenticated-vs-public split. The acting user is taken from `X-User-Id`; all chat/message ownership is enforced server-side against that id.

### Request

#### Body — [MessageSendRequestDTO](./models.md#messagesendrequestdto)
| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `message` | String | Yes | `@NotBlank`, `@Size(max = 1024)` | The user's prompt text. |
| `chatId` | Long | No | — | Existing chat to append to. When `null`, a new chat is created and titled from `message`. |

### Validation rules
- `message`: required, must be non-blank, at most 1024 characters.
- `chatId`: optional; when provided it must reference a chat owned by the caller and not deleted, otherwise the request fails with 404.

### Example request
```json
{
  "message": "Recommend me a laptop for programming under $1500",
  "chatId": 42
}
```

### Responses
| Status | Payload | When |
|---|---|---|
| 200 | [MessageResponseDTO](./models.md#messageresponsedto) | Message accepted; assistant reply generated and both user + model messages persisted. |

`MessageResponseDTO` fields:

| Field | Type | Description |
|---|---|---|
| `message` | String | The assistant's full reply text. |
| `chatId` | Long | The chat the exchange belongs to (newly created when `chatId` was omitted in the request). |
| `userMessageId` | Long | Persisted id of the saved user message. |
| `modelMessageId` | Long | Persisted id of the saved assistant (model) message. |
| `contentType` | [MessageContentType](./models.md#messagecontenttype) | `PRODUCT` when the reply carries product suggestions, otherwise `GENERAL`. |

### Example response
```json
{
  "data": {
    "message": "For programming under $1500 I'd recommend ...",
    "chatId": 42,
    "userMessageId": 1001,
    "modelMessageId": 1002,
    "contentType": "PRODUCT"
  }
}
```

### Error cases
| Status | Error code/message | Cause |
|---|---|---|
| 400 | `COMMON_INVALID_INPUT` | `message` blank or longer than 1024 chars (`MethodArgumentNotValidException`). |
| 404 | `CHAT_NOT_FOUND` | `chatId` was supplied but no matching non-deleted chat exists for the caller (`ItemNotFoundException`). |
| 401 | `AUTH_SESSION_INVALID` / signature mismatch | Missing or invalid identity headers — rejected by `PrincipalAuthFilter` / security config before reaching the controller. |
| 500 | `COMMON_SOMETHING_WENT_WRONG` | Unhandled failure during AI generation or persistence (catch-all in `ExceptionHelper`). |

### Business rules
- When `chatId` is `null`, a new `Chat` is created for the caller with a title derived from the message (`ChatUtil.getChatTitle`) and flushed before the user message is saved.
- When `chatId` is provided, the chat is loaded via `findByIdAndUserIdAndIsDeletedFalse` — it must exist, belong to the caller, and not be soft-deleted, otherwise `CHAT_NOT_FOUND` (404).
- The user message is persisted with an assigned `messageSeq` (monotonic per-chat sequence) before the model is invoked.
- The assistant reply is produced by `GlobalMessageHandler.handleChatMessage`; the boolean "has product suggestions" result drives `contentType` (`PRODUCT` vs `GENERAL`) and is stored on the saved model message.
- After saving the model message, `chatPostProcessService.processRecommendedMessage` links any recommended product ids to that message.
- This is the synchronous (non-streaming) counterpart to `POST /api/v1/message/send-stream`.

### Notes for frontend/mobile
- Omit `chatId` to start a new conversation; read the returned `chatId` from the response to continue it on subsequent sends.
- `contentType` lets the UI decide whether to render a product-recommendation layout (`PRODUCT`) or a plain text answer (`GENERAL`).
- This endpoint blocks until the full reply is generated. For incremental token-by-token rendering use the streaming endpoint instead (internal — see below).

---

## POST /api/v1/message/send-stream

### Purpose
Streams the assistant's reply incrementally as Server-Sent Events (`text/event-stream`) so the client can render tokens as they arrive. Like `/send`, it creates a new chat when `chatId` is omitted, persists the user message up front, and persists the full model message once streaming completes.

### Authentication
**Internal API** — service-to-service only, not for external integration. Marked `@io.swagger.v3.oas.annotations.Hidden`, so it is excluded from the OpenAPI/Swagger surface but is still reachable. It is NOT in the permit-all list, so it still requires valid `X-User-Id` / `X-User-Roles` / `X-Auth-Signature` identity headers; the acting user is taken from `X-User-Id`.

### Request

#### Body — [MessageSendRequestDTO](./models.md#messagesendrequestdto)
| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `message` | String | Yes | `@NotBlank`, `@Size(max = 1024)` | The user's prompt text. |
| `chatId` | Long | No | — | Existing chat to append to. When `null`, a new chat is created and titled from `message`. |

### Validation rules
- `message`: required, non-blank, at most 1024 characters.
- `chatId`: optional; when provided it must reference a non-deleted chat owned by the caller.

### Example request
```json
{
  "message": "Tell me about wireless earbuds",
  "chatId": 42
}
```

### Responses
| Status | Payload | When |
|---|---|---|
| 200 | `text/event-stream` of [ServerSentEvent](#sse-event-stream) | Streaming connection established; events are emitted until `done`. |
| 204 | (empty) | The catch-all handler suppresses the body when the response is already committed or is an SSE stream, returning No Content instead of corrupting the stream. |

<a id="sse-event-stream"></a>
The stream emits named SSE events:

| `event` | `data` payload | Meaning |
|---|---|---|
| `delta` | String | One incremental chunk of assistant text. Emitted zero or more times; empty chunks are filtered out. |
| `done` | [MessageResponseDTO](./models.md#messageresponsedto) | Terminal success event: the full reply with `chatId`, `userMessageId`, `modelMessageId`, and `contentType`. Sent once after all deltas. |
| `error` | [ResponseMessageDTO](./models.md#responsemessagedto) | Terminal failure event: a localized error message (`COMMON_SOMETHING_WENT_WRONG`). Errors during streaming surface here, never as the standard JSON envelope. |

### Example response
```text
event:delta
data:"For wireless"

event:delta
data:" earbuds I'd suggest ..."

event:done
data:{"message":"For wireless earbuds I'd suggest ...","chatId":42,"userMessageId":1001,"modelMessageId":1002,"contentType":"PRODUCT"}
```

Error termination instead of `done`:
```text
event:error
data:{"message":"Nimadir noto'g'ri ketdi"}
```

### Error cases
| Status / event | Error code/message | Cause |
|---|---|---|
| `error` event | `COMMON_SOMETHING_WENT_WRONG` | Any failure once the stream has started (AI call, persistence) — delivered in-stream, not via `ExceptionHelper`. |
| 400 | `COMMON_INVALID_INPUT` | `message` blank or over 1024 chars — bean validation fails before streaming begins. |
| 404 | `CHAT_NOT_FOUND` | `chatId` supplied but no matching non-deleted chat owned by the caller. |
| 401 | `AUTH_SESSION_INVALID` / signature mismatch | Missing or invalid identity headers. |
| 204 | (no body) | Catch-all error after the response is committed / on an SSE response — body suppressed to avoid corrupting the stream. |

### Business rules
- The current user id and request `Locale` are captured on the servlet thread up front, because `SecurityContextHolder` and the resolved locale are thread-locals that do NOT propagate to the reactor `boundedElastic` scheduler used for streaming and error handling.
- The user message (and a new chat, if `chatId` was null) is persisted synchronously before any tokens are streamed, identical to `/send`.
- Delta events carry raw extracted text content; empty extracted chunks are dropped.
- After the last delta, `completeStreamedMessage` persists the assembled (trimmed) model message, links recommended products (`processRecommendedMessage`), and runs `chatPostProcessService.processClaude` for post-processing/summarization bookkeeping, then emits the `done` event.
- Errors after streaming starts are caught by `onErrorResume`, logged, and converted into a single `error` SSE event using the locale captured on the request thread; they are never routed through the global `ExceptionHelper`.

### Notes for frontend/mobile
- Consume with an SSE client; treat `delta` events as append-to-buffer and stop on either `done` (success) or `error` (failure).
- The authoritative message ids and `contentType` arrive only in the `done` event, not in `delta` events.
- A stream that ends without a `done` event (e.g. an `error` event, a `204`, or a dropped connection) should be treated as a failed exchange even though the user message was already persisted.
- This endpoint is hidden from Swagger and intended for the gateway/front-channel that already injects identity headers; it is not part of the public OpenAPI contract.

---

## POST /api/v1/message/delete/{id}

### Purpose
Soft-deletes a single message owned by the caller. The message row is marked `isDeleted = true` rather than removed, so it is excluded from future chat-message listings.

### Authentication
Header-based downstream trust (see [README.md](./README.md)). Requires valid `X-User-Id` / `X-User-Roles` / `X-Auth-Signature` identity headers. No role/permission annotation. Ownership is enforced server-side: the lookup is scoped to the caller's `userId`.

### Request

#### Path parameters
| Name | Type | Required | Description |
|---|---|:---:|---|
| `id` | long | Yes | Id of the message to soft-delete. Must belong to the caller and not already be deleted. |

### Example request
```text
POST /api/v1/message/delete/1002
```
(no request body)

### Responses
| Status | Payload | When |
|---|---|---|
| 200 | [ResponseMessageDTO](./models.md#responsemessagedto) | Message found, owned by caller, and marked deleted. `message` is the localized "message deleted" text. |

### Example response
```json
{
  "data": {
    "message": "Message deleted"
  }
}
```

### Error cases
| Status | Error code/message | Cause |
|---|---|---|
| 404 | `MESSAGE_NOT_FOUND` | No non-deleted message with the given `id` exists for the caller (`ItemNotFoundException`). |
| 400 | `COMMON_INVALID_INPUT` | `id` path segment is not a valid `long` (`MethodArgumentTypeMismatchException`). |
| 401 | `AUTH_SESSION_INVALID` / signature mismatch | Missing or invalid identity headers. |
| 500 | `COMMON_SOMETHING_WENT_WRONG` | Unhandled failure during deletion. |

### Business rules
- Lookup uses `findByIdAndUserIdAndIsDeletedFalse(id, userId)` — a message owned by another user, a non-existent id, or an already-deleted message all yield `MESSAGE_NOT_FOUND` (404), with no distinction between "not found" and "not yours".
- Deletion is a soft delete: `update Message set isDeleted = true where id = :id`. The row is retained for audit/history and merely hidden from message listings; it is not physically removed.
- The whole operation runs in a single transaction (`@Transactional`).
- The success payload `message` is a localized string resolved from `MessageCode.MESSAGE_DELETED` via `LocalizationService` (respects the request `Accept-Language`).

### Notes for frontend/mobile
- Despite the destructive semantics this is a `POST` (not `DELETE`) with the id in the path and no body.
- A 404 here is the generic "not found or not yours" response — do not assume the message never existed; it may simply belong to another user or already be deleted.
