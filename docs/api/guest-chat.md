# Guest Chat API

Public, account-free chat endpoints for anonymous (guest) users. A guest is identified solely by an `X-Device-Id` header instead of an authenticated user — there is no JWT and no signed downstream identity headers on these routes. One persistent chat is maintained per device, letting the same device send messages and read back its history.

All handlers live under the class-level prefix `/api/guest` (`GuestChatController`) and wrap their payloads in the standard [`CommonResponse<T>`](../../README.md#standard-response-envelope) envelope.

---

## POST /api/guest/free-chat

### Purpose
Sends a guest user's text message to Melissa and returns the model's reply. On the first call for a given device it creates a new chat; subsequent calls reuse that device's existing (non-deleted) chat. Both the user message and the model reply are persisted before the reply is returned.

### Authentication
Public — this path is in the security permit-all list, so no authenticated principal is required. Instead, the caller MUST supply an `X-Device-Id` request header (enforced at the controller and re-validated in the service). No `X-User-Id`/`X-User-Roles`/`X-Auth-Signature` downstream headers are needed.

### Request

#### Headers
| Name | Required | Description |
|---|:---:|---|
| `X-Device-Id` | Yes | Opaque, client-generated device identifier. Scopes the guest chat. A missing header yields 400 (Spring `@RequestHeader`); a blank/whitespace-only value is rejected by the service with `MESSAGE_DEVICE_ID_REQUIRED`. |
| `Accept-Language` | No | Locale for the localized `errorMessage` on failures (`uz`, `ru`, `en`; default `uz`). |

#### Body — [GuestChatRequest](./models.md#guestchatrequest)
| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `message` | String | Yes | `@NotBlank` | The guest's chat message text. Trimmed server-side; an effectively empty value is rejected. |

### Validation rules
- `message`: required, must not be blank (`@NotBlank`). A blank body field fails bean validation → 400 `COMMON_INVALID_INPUT`. If the value is present but trims to empty (e.g. only whitespace) the service throws `MESSAGE_EMPTY` → 400.
- `X-Device-Id`: required header; blank/whitespace-only rejected by the service → 400 `MESSAGE_DEVICE_ID_REQUIRED`.

### Example request
```
POST /api/guest/free-chat
X-Device-Id: 1f2e3d4c-5b6a-7890-abcd-ef0123456789
Content-Type: application/json
```
```json
{
  "message": "Salom, menga noutbuk tavsiya qiling"
}
```

### Responses
| Status | Payload | When |
|---|---|---|
| 200 | [GuestMessageResponseDTO](./models.md#guestmessageresponsedto) | Message handled; returns the model's reply text. |

The `data` payload carries only the model's reply text:

### Example response
```json
{
  "data": {
    "message": "Albatta! Byudjetingiz va asosiy maqsadingizni aytib bering..."
  }
}
```

### Error cases
| Status | Error code/message | Cause |
|---|---|---|
| 400 | `MESSAGE_DEVICE_ID_REQUIRED` (e.g. "Qurilma ma'lumoti topilmadi. Qayta urinib ko'ring") | `X-Device-Id` header present but blank/whitespace-only (`BadRequestException`). |
| 400 | `MESSAGE_EMPTY` (e.g. "Xabar matni bo'sh bo'lishi mumkin emas") | `message` present but trims to empty (`BadRequestException`). |
| 400 | `COMMON_INVALID_INPUT` ("Kiritilgan ma'lumot noto'g'ri") | `message` blank/missing — bean validation (`MethodArgumentNotValidException`). |
| 400 | (Spring default) | `X-Device-Id` header entirely absent (required `@RequestHeader`). |
| 500 | `COMMON_SOMETHING_WENT_WRONG` | Unhandled error (e.g. downstream model/LLM call failure). Catch-all in `ExceptionHelper`. |

### Business rules
- Device id and message are trimmed; empty device id → `MESSAGE_DEVICE_ID_REQUIRED`, empty message → `MESSAGE_EMPTY`.
- Chat resolution is get-or-create per device: `saveOrGetGuestChat` returns the device's existing non-deleted chat, or creates and flushes a new `Chat` (seeded with the first message text) when none exists.
- The guest's message is persisted as a `Message` with `messageType = TEXT`, `messageAuthorityType = USER`, and an assigned per-chat `messageSeq` (via `MessageSequenceService`), then flushed.
- The message is handed to `GlobalMessageHandler.handleChatMessage` (no authenticated user → `userId` null) which returns a `ProductBasedMessage` keyed by a boolean "has product suggestions" flag.
- The model reply is persisted as a `Message` with `messageAuthorityType = MODEL`, `messageModelType = CLAUDE`, `hasProductSuggestions` set from that flag, and its own `messageSeq`, then flushed. Only the reply `text` is returned.

### Notes for frontend/mobile
- Generate and persist a stable `X-Device-Id` on the client; it is the only identity for guest chat continuity. Reusing it across calls continues the same conversation; a new value starts a fresh chat.
- The response intentionally returns only the reply text. The `hasProductSuggestions` flag is stored server-side but is NOT exposed in this response — it later surfaces as `contentType = PRODUCT` when reading history via `GET /api/guest/messages`.
- This call is synchronous (plain JSON, not SSE); it blocks until the model reply is generated and saved.

---

## GET /api/guest/messages

### Purpose
Returns the paginated message history for the guest chat belonging to the supplied device. Used to render the conversation transcript (both the guest's messages and Melissa's replies) on app load or scroll-back.

### Authentication
Public — permit-all path; no authenticated principal. Requires the `X-Device-Id` request header to scope the history to a device.

### Request

#### Query parameters
| Name | Type | Required | Default | Description |
|---|---|:---:|---|---|
| `page` | int | No | `0` | Zero-based page index. |
| `size` | int | No | `20` (Spring default) | Page size. |
| `sort` | string | No | `updatedAt,DESC` | Sort spec. Defaults to `updatedAt` descending via `@PageableDefault`. |

> Note: the default sort property is `updatedAt`, but `ChatMessagesDTO` exposes `createdAt` (not `updatedAt`) in its payload. Sorting is applied at the entity level.

#### Headers
| Name | Required | Description |
|---|:---:|---|
| `X-Device-Id` | Yes | Device identifier whose chat history is returned. Missing → 400 (`@RequestHeader`); blank/whitespace-only → `MESSAGE_DEVICE_ID_REQUIRED`. |
| `Accept-Language` | No | Locale for the localized `errorMessage` on failures (`uz`, `ru`, `en`; default `uz`). |

### Responses
| Status | Payload | When |
|---|---|---|
| 200 | [Page](./models.md#paget)\<[ChatMessagesDTO](./models.md#chatmessagesdto)\> | Returns the chat's messages page. If the device has no chat, returns an empty page. |

### Example response
```json
{
  "data": {
    "content": [
      {
        "messageId": 482,
        "messageText": "Albatta! Byudjetingizni ayting...",
        "messageType": "TEXT",
        "messageAuthorityType": "MODEL",
        "contentType": "PRODUCT",
        "createdAt": "2026-06-19 14:22:08.512"
      },
      {
        "messageId": 481,
        "messageText": "Salom, menga noutbuk tavsiya qiling",
        "messageType": "TEXT",
        "messageAuthorityType": "USER",
        "contentType": "GENERAL",
        "createdAt": "2026-06-19 14:22:05.110"
      }
    ],
    "totalElements": 2,
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
| 400 | `MESSAGE_DEVICE_ID_REQUIRED` (e.g. "Qurilma ma'lumoti topilmadi. Qayta urinib ko'ring") | `X-Device-Id` header present but blank/whitespace-only (`BadRequestException`). |
| 400 | (Spring default) | `X-Device-Id` header absent (required `@RequestHeader`). |
| 400 | `COMMON_INVALID_INPUT` | Malformed pagination/sort parameter type (`MethodArgumentTypeMismatchException`). |
| 500 | `COMMON_SOMETHING_WENT_WRONG` | Unhandled error. Catch-all in `ExceptionHelper`. |

### Business rules
- Device id is trimmed; empty → `MESSAGE_DEVICE_ID_REQUIRED`.
- Resolves the device's single most-recent non-deleted chat (`findTop1ByDeviceIdAndIsDeletedFalse`). If none exists, returns `Page.empty()` (HTTP 200, empty content) rather than an error — a brand-new device simply has no history.
- Only non-deleted messages of that chat are returned (`isDeleted = false`).
- `contentType` is derived per message: `PRODUCT` when the stored message has `hasProductSuggestions = true`, otherwise `GENERAL`. (Guest user messages are always `GENERAL`; model replies may be `PRODUCT`.)
- Read-only transaction; no state is mutated by this call.

### Notes for frontend/mobile
- `messageAuthorityType` distinguishes the two sides of the conversation: `USER` (the guest) vs `MODEL` (Melissa). Use it to align/style bubbles.
- `messageType` is currently always `TEXT` (the only `MessageType` constant).
- `contentType` (`PRODUCT` | `GENERAL`) flags whether the model reply included product suggestions — useful to trigger a product-card UI affordance.
- `createdAt` is a SQL `Timestamp` serialized in the service timezone (Asia/Tashkent), format `yyyy-MM-dd HH:mm:ss.SSS`.
- Default ordering is newest-first (`updatedAt` DESC). An empty `content` array with `totalElements: 0` means the device has no chat yet — treat as a fresh conversation, not an error.
