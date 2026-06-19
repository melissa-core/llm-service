# Chat API

Manages a user's chat conversations in the Melissa LLM service: create, rename, list, fetch a single chat, page through its messages, soft-delete it, and (internal) link an anonymous device chat to a now-authenticated user. All endpoints are mounted under the class-level prefix `/api/v1/chat` and operate strictly on chats owned by the caller (the user id is taken from the verified identity headers, never from the request body).

> Response envelope, auth header model, and the global error format are documented once in [README.md](./README.md). Each endpoint below documents only the payload type `T` inside `CommonResponse<T>`.

---

## POST /api/v1/chat

### Purpose
Creates a new, empty chat conversation owned by the authenticated user with the supplied title. Call this when the user starts a fresh conversation thread before sending the first message.

### Authentication
Header-based downstream trust (verified by `PrincipalAuthFilter`): requires the `X-User-Id`, `X-User-Roles`, and `X-Auth-Signature` headers injected by the api-gateway. The request is rejected with `401` if the signature is invalid or the headers are absent (the path is not in the permit-all list). No per-role/permission annotation — any authenticated principal may call it. The owning user id is derived from `X-User-Id` (via `SecurityUtil.getCurrentUserId()`), not from the body.

### Request

#### Body — [CreateChatRequestDTO](./models.md#createchatrequestdto)
| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| title | String | yes | `@NotBlank` (non-null, non-whitespace) | Display title of the new chat. |

### Validation rules
- `title`: required, must not be blank (null, empty, or whitespace-only is rejected with `400`).

### Example request
```json
{
  "title": "Summer dress recommendations"
}
```

### Responses
| Status | Payload | When |
|---|---|---|
| 200 | [ResponseMessageDTO](./models.md#responsemessagedto) | Chat created and persisted. |
| 400 | standard error | `title` is blank/missing. |
| 401 | standard error | Missing/invalid identity headers. |

### Example response
```json
{
  "data": {
    "message": "Chat muvaffaqiyatli saqlandi"
  }
}
```

### Error cases
| Status | Error code/message | Cause |
|---|---|---|
| 400 | `COMMON_INVALID_INPUT` (e.g. "Kiritilgan ma'lumot noto'g'ri") | `@NotBlank` validation on `title` fails (`MethodArgumentNotValidException`). |
| 401 | `AUTH_SESSION_INVALID` / signature failure | Identity headers missing or HMAC mismatch. |
| 500 | `COMMON_SOMETHING_WENT_WRONG` | Unexpected persistence error. |

### Business rules
- The chat is created with `userId = X-User-Id` and `isDeleted = false`. No `deviceId` is set for this path (that field is used only by guest/temporary chats).
- The returned `message` is a localized confirmation string resolved from `MessageCode.CHAT_SAVED` using the request `Accept-Language` (default locale `uz`).

### Notes for frontend/mobile
- The response does NOT return the created chat id or object — only a confirmation message. To obtain the new chat's id, re-fetch the list via `GET /api/v1/chat` (ordered by `updatedAt` DESC, so the new chat appears first).

---

## PUT /api/v1/chat/{id}

### Purpose
Renames an existing chat owned by the authenticated user. Call this when the user edits a conversation's title.

### Authentication
Header-based downstream trust (see [README.md](./README.md)). Requires valid `X-User-Id`/`X-User-Roles`/`X-Auth-Signature`. No role/permission annotation. The chat must belong to the caller — ownership is enforced in the service by querying with the current user id.

### Request

#### Path parameters
| Name | Type | Required | Description |
|---|---|:---:|---|
| id | Long | yes | Id of the chat to rename. |

#### Body — [CreateChatRequestDTO](./models.md#createchatrequestdto)
| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| title | String | yes | `@NotBlank` | New title for the chat. |

### Validation rules
- `id`: path variable, must be a valid `Long` (a non-numeric value yields `400` `MethodArgumentTypeMismatchException`).
- `title`: required, must not be blank.

### Example request
```json
{
  "title": "Winter coats under $200"
}
```

### Responses
| Status | Payload | When |
|---|---|---|
| 200 | [ResponseMessageDTO](./models.md#responsemessagedto) | Chat found, owned by caller, and renamed. |
| 400 | standard error | `title` blank or `id` not a number. |
| 401 | standard error | Missing/invalid identity headers. |
| 404 | standard error | No non-deleted chat with this id belongs to the caller. |

### Example response
```json
{
  "data": {
    "message": "Chat muvaffaqiyatli yangilandi"
  }
}
```

### Error cases
| Status | Error code/message | Cause |
|---|---|---|
| 400 | `COMMON_INVALID_INPUT` | `title` fails `@NotBlank`, or `id` path value is not a `Long`. |
| 401 | `AUTH_SESSION_INVALID` / signature failure | Identity headers missing or HMAC mismatch. |
| 404 | `CHAT_NOT_FOUND` ("Chat topilmadi yoki unga kirish imkoni yo'q") | `id` is null, or no chat with that id, `userId = caller`, `isDeleted = false` exists. Thrown as `ItemNotFoundException` (→ `NOT_FOUND`). |

### Business rules
- Ownership + soft-delete check: only a chat where `id = {id}`, `userId = current user`, and `isDeleted = false` can be updated; otherwise `404`.
- **Gotcha (state loss):** the update rebuilds the `Chat` via a builder that copies only `id`, `userId`, and the new `title`. Fields NOT carried over (e.g. `createdAt`, `deviceId`, and any audit timestamps not auto-managed) may be reset/overwritten on save. Treat a rename as potentially resetting `createdAt`-style metadata. Flagged as **Needs confirmation** with the entity/auditing config before relying on `createdAt` being preserved across renames.

### Notes for frontend/mobile
- Only the title is updatable through this endpoint; there is no separate partial-update path.

---

## GET /api/v1/chat

### Purpose
Returns a paginated, newest-first list of the authenticated user's chats (one entry per conversation, with a preview subtitle). Used to render the chat/conversation sidebar or history list.

### Authentication
Header-based downstream trust (see [README.md](./README.md)). Requires valid identity headers. No role/permission annotation. Results are scoped to the caller's `userId`.

### Request

#### Query parameters
| Name | Type | Required | Default | Description |
|---|---|:---:|---|---|
| page | Integer | no | `0` | Zero-based page index. |
| size | Integer | no | `20` (Spring default) | Page size. |
| sort | String | no | `updatedAt,DESC` | Sort spec `field,(asc\|desc)`. Defaults to most-recently-updated first via `@PageableDefault(sort = "updatedAt", direction = DESC)`. |

### Responses
| Status | Payload | When |
|---|---|---|
| 200 | [Page](./models.md#paget)&lt;[ChatPageDTO](./models.md#chatpagedto)&gt; | Always (empty `content` if the user has no chats). |
| 401 | standard error | Missing/invalid identity headers. |

### Example response
```json
{
  "data": {
    "content": [
      {
        "id": 42,
        "title": "Summer dress recommendations",
        "subtitle": "Sizda qanday byudjet bor?",
        "createdAt": "2026-06-18T14:32:05.000+05:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 20,
    "first": true,
    "last": true
  }
}
```

### Business rules
- Lists only the caller's chats (the query is filtered by `userId`). Soft-deleted chats are excluded.
- `subtitle` is a preview line derived from the chat's latest `USER`-authored message (`MessageAuthorityType.USER`); it can be `null`/empty for a brand-new chat with no user messages yet.
- Ordering is by `updatedAt` DESC by default, so the most recently active chat appears first.

### Notes for frontend/mobile
- Pagination is the standard Spring `Page` envelope — see [Page&lt;T&gt;](./models.md#paget) in models.md for all fields.
- `subtitle` is nullable; render a placeholder when absent.
- `createdAt` is a `Timestamp` serialized in the service timezone (`Asia/Tashkent`, `+05:00`), not UTC.

---

## GET /api/v1/chat/{id}

### Purpose
Fetches a single chat (id, title, creation time) owned by the authenticated user. Use it to load the header/metadata of a conversation the user opens.

### Authentication
Header-based downstream trust (see [README.md](./README.md)). Requires valid identity headers. No role/permission annotation. Ownership is enforced server-side.

### Request

#### Path parameters
| Name | Type | Required | Description |
|---|---|:---:|---|
| id | Long | yes | Id of the chat to fetch. |

### Responses
| Status | Payload | When |
|---|---|---|
| 200 | [ChatDTO](./models.md#chatdto) | Chat exists, is not deleted, and belongs to the caller. |
| 401 | standard error | Missing/invalid identity headers. |
| 404 | standard error | No matching non-deleted chat owned by the caller. |

### Example response
```json
{
  "data": {
    "id": 42,
    "title": "Summer dress recommendations",
    "createdAt": "2026-06-18T14:32:05.000+05:00"
  }
}
```

### Error cases
| Status | Error code/message | Cause |
|---|---|---|
| 401 | `AUTH_SESSION_INVALID` / signature failure | Identity headers missing or HMAC mismatch. |
| 404 | `CHAT_NOT_FOUND` ("Chat topilmadi yoki unga kirish imkoni yo'q") | No chat with `id = {id}`, `userId = caller`, `isDeleted = false`. Thrown as `ItemNotFoundException` (→ `NOT_FOUND`). |

### Business rules
- Returns `404` (not `403`) when the chat exists but belongs to another user — ownership and existence are indistinguishable to the caller by design.

### Notes for frontend/mobile
- This endpoint returns only `id`, `title`, `createdAt` — it does NOT include messages or `subtitle`. Load messages via `GET /api/v1/chat/{id}/messages`.

---

## GET /api/v1/chat/{id}/messages

### Purpose
Returns a paginated, newest-first list of messages within one chat owned by the authenticated user. Use it to render the conversation transcript (and to page back through history).

### Authentication
Header-based downstream trust (see [README.md](./README.md)). Requires valid identity headers. No role/permission annotation. Both the chat ownership check and the message query are scoped to the caller's `userId`.

### Request

#### Path parameters
| Name | Type | Required | Description |
|---|---|:---:|---|
| id | Long | yes | Id of the chat whose messages to fetch. |

#### Query parameters
| Name | Type | Required | Default | Description |
|---|---|:---:|---|---|
| page | Integer | no | `0` | Zero-based page index. |
| size | Integer | no | `20` (Spring default) | Page size. |
| sort | String | no | `createdAt,DESC` | Sort spec. Defaults to newest message first via `@PageableDefault(sort = "createdAt", direction = DESC)`. |

### Responses
| Status | Payload | When |
|---|---|---|
| 200 | [Page](./models.md#paget)&lt;[ChatMessagesDTO](./models.md#chatmessagesdto)&gt; | Chat exists, is not deleted, and belongs to the caller. |
| 401 | standard error | Missing/invalid identity headers. |
| 404 | standard error | No matching non-deleted chat owned by the caller. |

### Example response
```json
{
  "data": {
    "content": [
      {
        "messageId": 1007,
        "messageText": "Bu ko'ylakni ko'rib chiqing",
        "messageType": "TEXT",
        "messageAuthorityType": "AI",
        "contentType": "PRODUCT",
        "createdAt": "2026-06-18T14:33:10.000+05:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 20,
    "first": true,
    "last": true
  }
}
```

### Error cases
| Status | Error code/message | Cause |
|---|---|---|
| 401 | `AUTH_SESSION_INVALID` / signature failure | Identity headers missing or HMAC mismatch. |
| 404 | `CHAT_NOT_FOUND` ("Chat topilmadi yoki unga kirish imkoni yo'q") | The chat (`id`, `userId = caller`, not deleted) does not exist. Thrown as `ItemNotFoundException` (→ `NOT_FOUND`). |

### Business rules
- The chat ownership/existence check runs first; only then are messages queried (also filtered by `id` + caller `userId`).
- `messageType` and `messageAuthorityType` are returned as raw strings in this DTO; `contentType` is the [MessageContentType](./models.md#messagecontenttype) enum (`PRODUCT` or `GENERAL`).

### Notes for frontend/mobile
- Messages are returned newest-first by default. To render a top-to-bottom transcript, reverse each page client-side or request an ascending sort (`sort=createdAt,ASC`).
- `contentType = PRODUCT` indicates the message carries product/suggestion content (rendered differently from `GENERAL` chat text).
- `createdAt` is in `Asia/Tashkent` (`+05:00`).

---

## DELETE /api/v1/chat/{id}

### Purpose
Soft-deletes a chat owned by the authenticated user (sets its `isDeleted` flag). The chat is hidden from listings but not physically removed. Call this when the user deletes a conversation.

### Authentication
Header-based downstream trust (see [README.md](./README.md)). Requires valid identity headers. No role/permission annotation. Ownership is enforced server-side.

### Request

#### Path parameters
| Name | Type | Required | Description |
|---|---|:---:|---|
| id | Long | yes | Id of the chat to delete. |

### Responses
| Status | Payload | When |
|---|---|---|
| 200 | [ResponseMessageDTO](./models.md#responsemessagedto) | Chat found, owned by caller, and marked deleted. |
| 401 | standard error | Missing/invalid identity headers. |
| 404 | standard error | No matching non-deleted chat owned by the caller. |

### Example response
```json
{
  "data": {
    "message": "Chat o'chirildi"
  }
}
```

### Error cases
| Status | Error code/message | Cause |
|---|---|---|
| 401 | `AUTH_SESSION_INVALID` / signature failure | Identity headers missing or HMAC mismatch. |
| 404 | `CHAT_NOT_FOUND` ("Chat topilmadi yoki unga kirish imkoni yo'q") | `id` is null, or no chat with `id`, `userId = caller`, `isDeleted = false`. Thrown as `ItemNotFoundException` (→ `NOT_FOUND`). |

### Business rules
- **Soft delete only:** the operation sets `isDeleted = true` and saves; the row and its messages are retained in the database.
- Deleting an already-deleted chat returns `404` (the lookup filters `isDeleted = false`), so delete is effectively idempotent from the client's perspective — a second call fails with `CHAT_NOT_FOUND`.

---

## POST /api/v1/chat/activate-chat/{key}

### Purpose
Links an anonymous/temporary chat created against a device (during guest usage) to the now-authenticated user, transferring ownership of the chat and its messages. Invoked internally after a guest signs in, so their pre-login conversation continues under their account.

### Authentication
**Internal API** — service-to-service only, not for external integration. Marked `@io.swagger.v3.oas.annotations.Hidden`, so it is excluded from the OpenAPI/Swagger surface but still reachable. It is NOT in the permit-all list, so it still requires valid `X-User-Id`/`X-User-Roles`/`X-Auth-Signature` identity headers; the target user is the caller (`X-User-Id`).

### Request

#### Path parameters
| Name | Type | Required | Description |
|---|---|:---:|---|
| key | String | yes | The device id (`deviceId`) of the temporary/guest chat to claim. |

### Responses
| Status | Payload | When |
|---|---|---|
| 200 | [ResponseMessageDTO](./models.md#responsemessagedto) | Always on success, including when no temporary chat is found (see business rules). |
| 401 | standard error | Missing/invalid identity headers. |

### Example response (chat found and linked)
```json
{
  "data": {
    "message": "Oldingi chat hisobingizga muvaffaqiyatli ulanildi"
  }
}
```

### Example response (no temporary chat for the device)
```json
{
  "data": {
    "message": "Ushbu qurilmada avvalgi chat topilmadi"
  }
}
```

### Error cases
| Status | Error code/message | Cause |
|---|---|---|
| 401 | `AUTH_SESSION_INVALID` / signature failure | Identity headers missing or HMAC mismatch. |
| 500 | `COMMON_SOMETHING_WENT_WRONG` | Unexpected persistence error during ownership transfer. |

### Business rules
- Looks up the most recent non-deleted chat for `deviceId = {key}` (`findTop1ByDeviceIdAndIsDeletedFalse`).
- **No-op success:** if no such temporary chat exists, the endpoint logs a warning and still returns `200` with the localized `CHAT_TEMPORARY_NOT_FOUND` message (`"Ushbu qurilmada avvalgi chat topilmadi"`) — it does NOT return `404`.
- On success it reassigns ownership of both the chat (`activateChatByDeviceId`) and all its messages (`messageRepository.setUserId`) to the current user id, then returns the `CHAT_ACTIVATED` message.
- Runs in a single `@Transactional` unit, so the chat and message reassignment commit together.

### Notes for frontend/mobile
- Because the "not found" case is a `200` with a different message string, clients must inspect the `data.message` to distinguish "linked" from "nothing to link" — there is no distinct status code or boolean.
