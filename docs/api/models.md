# llm-service — Shared Data Models

> Generated from source code. Regenerate instead of editing by hand.
> Notes inside `<!-- manual:start --> ... <!-- manual:end -->` blocks survive regeneration.

This page defines the request/response DTOs and enums referenced by the resource pages
([chat.md](./chat.md), [guest-chat.md](./guest-chat.md), [message.md](./message.md),
[product-suggestion.md](./product-suggestion.md)). The universal response envelope
`CommonResponse<T>` and the standard error format are documented in
[README.md](./README.md#standard-response-envelope); only the payload type `T` is detailed here.

All `createdAt` / `Timestamp` fields are serialized in the service timezone `Asia/Tashkent`
(`+05:00`), e.g. `2026-06-18T14:32:05.000+05:00` (see `spring.jackson.time-zone`).

---

## ChatDTO

Single-chat metadata returned by `GET /api/v1/chat/{id}`. Carries only header/identity fields —
no messages, no subtitle. (`uz.melisa.dto.chat.ChatDTO`)

| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `id` | Long | — | — | Chat identifier. |
| `title` | String | — | — | Chat display title. |
| `createdAt` | Timestamp | — | — | Creation instant, rendered in `Asia/Tashkent`. |

```json
{
  "id": 42,
  "title": "Summer dress recommendations",
  "createdAt": "2026-06-18T14:32:05.000+05:00"
}
```

---

## ChatMessagesDTO

One message inside a chat transcript. Returned (paged) by `GET /api/v1/chat/{id}/messages`
(authenticated) and `GET /api/guest/messages` (guest). (`uz.melisa.dto.chat.ChatMessagesDTO`)

| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `messageId` | Long | — | — | Persisted message id. |
| `messageText` | String | — | — | The message body text. |
| `messageType` | String | — | — | Raw message type string (e.g. `TEXT`). Not typed as an enum in this DTO. |
| `messageAuthorityType` | String | — | — | Raw author string identifying who produced the message (e.g. `USER`, `AI` / `MODEL`). Not typed as an enum in this DTO. |
| `contentType` | [MessageContentType](#messagecontenttype) | — | — | Whether the message carries product suggestions (`PRODUCT`) or plain chat (`GENERAL`). |
| `createdAt` | Timestamp | — | — | Creation instant, rendered in `Asia/Tashkent`. |

```json
{
  "messageId": 1007,
  "messageText": "Bu ko'ylakni ko'rib chiqing",
  "messageType": "TEXT",
  "messageAuthorityType": "AI",
  "contentType": "PRODUCT",
  "createdAt": "2026-06-18T14:33:10.000+05:00"
}
```

> Note: `messageType` and `messageAuthorityType` are plain strings here even though the underlying
> entity uses enums; treat their exact set of values as **Needs confirmation** against the entity
> rather than assuming. `contentType` is the typed [MessageContentType](#messagecontenttype) enum.

---

## ChatPageDTO

One row in the authenticated chat list (`GET /api/v1/chat`). Includes a `subtitle` preview
not present in [ChatDTO](#chatdto). (`uz.melisa.dto.chat.ChatPageDTO`)

| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `id` | Long | — | — | Chat identifier. |
| `title` | String | — | — | Chat display title. |
| `subtitle` | String | — | — | Preview line derived from the chat's latest user-authored message. Nullable/empty for a brand-new chat with no user messages yet. |
| `createdAt` | Timestamp | — | — | Creation instant, rendered in `Asia/Tashkent`. |

```json
{
  "id": 42,
  "title": "Summer dress recommendations",
  "subtitle": "Sizda qanday byudjet bor?",
  "createdAt": "2026-06-18T14:32:05.000+05:00"
}
```

---

## CommonResponse&lt;T&gt;

The universal response envelope wrapping every endpoint payload.
(`uz.melisa.dto.common.CommonResponse`)

| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `data` | T | — | `@JsonInclude(NON_NULL)` | Success payload. Omitted from JSON when `null` (i.e. on failures). |
| `errorMessage` | String | — | `@JsonInclude(NON_NULL)` | Localized error text on failure. Omitted from JSON when `null` (i.e. on success). |
| `status` | [ApiResponseStatus](#apiresponsestatus) | — | `@JsonIgnore` | NOT serialized. Only drives the HTTP status code via `ResponseUtil` (`ResponseEntity.status(status.getHttpStatus())`). |

Because `data` and `errorMessage` are `NON_NULL` and `status` is ignored, a success body contains
only `data` and an error body contains only `errorMessage`.

Success example:
```json
{
  "data": { "message": "Chat muvaffaqiyatli saqlandi" }
}
```

Error example:
```json
{
  "errorMessage": "Ma'lumot topilmadi"
}
```

See the full envelope and error-format documentation in
[README.md](./README.md#standard-response-envelope).

---

## CreateChatRequestDTO

Request body for creating (`POST /api/v1/chat`) and renaming (`PUT /api/v1/chat/{id}`) a chat.
(`uz.melisa.dto.chat.CreateChatRequestDTO`)

| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `title` | String | Yes | `@NotBlank` | Chat display title. Must be non-null and not whitespace-only. |

Validation:
- `title`: required, must not be blank (null, empty, or whitespace-only → 400 `COMMON_INVALID_INPUT`).

```json
{
  "title": "Summer dress recommendations"
}
```

---

## GuestChatRequest

Request body for a guest free-chat message (`POST /api/guest/free-chat`).
(`uz.melisa.dto.claude.GuestChatRequest`)

| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `message` | String | Yes | `@NotBlank` | The guest's chat message text. Trimmed server-side; an effectively empty value is rejected (`MESSAGE_EMPTY`). |

Validation:
- `message`: required, must not be blank (`@NotBlank`) → 400 `COMMON_INVALID_INPUT`; if present but trims to empty → 400 `MESSAGE_EMPTY`.

```json
{
  "message": "Salom, menga noutbuk tavsiya qiling"
}
```

---

## GuestMessageResponseDTO

Reply payload for a guest free-chat message (`POST /api/guest/free-chat`). Carries only the model's
reply text. (`uz.melisa.dto.guest.GuestMessageResponseDTO`)

| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `message` | String | — | — | The model's reply text. |

```json
{
  "message": "Albatta! Byudjetingiz va asosiy maqsadingizni aytib bering..."
}
```

> The server-side `hasProductSuggestions` flag is NOT exposed here; it later surfaces as
> `contentType = PRODUCT` when reading history via `GET /api/guest/messages`.

---

## MessageContentType

Marks whether a message carries product suggestions or is plain chat content.
(`uz.melisa.enums.MessageContentType`)

| Constant | Meaning |
|---|---|
| `PRODUCT` | The message carries product/suggestion content (rendered differently from general chat). |
| `GENERAL` | The message is general chat text with no product suggestions. |

---

## MessageResponseDTO

Reply payload for the authenticated synchronous send (`POST /api/v1/message/send`).
(`uz.melisa.dto.message.MessageResponseDTO`)

| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `message` | String | — | — | The assistant's full reply text. |
| `chatId` | Long | — | — | Chat the exchange belongs to (newly created when `chatId` was omitted in the request). |
| `userMessageId` | Long | — | — | Persisted id of the saved user message. |
| `modelMessageId` | Long | — | — | Persisted id of the saved assistant (model) message. |
| `contentType` | [MessageContentType](#messagecontenttype) | — | — | `PRODUCT` when the reply carries product suggestions, otherwise `GENERAL`. |

```json
{
  "message": "For programming under $1500 I'd recommend ...",
  "chatId": 42,
  "userMessageId": 1001,
  "modelMessageId": 1002,
  "contentType": "PRODUCT"
}
```

---

## MessageSendRequestDTO

Request body for sending a message to the assistant — both the blocking send
(`POST /api/v1/message/send`) and the hidden streaming variant (`POST /api/v1/message/send-stream`).
(`uz.melisa.dto.message.MessageSendRequestDTO`)

| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `message` | String | Yes | `@NotBlank`, `@Size(max = 1024)` | The user's prompt text. |
| `chatId` | Long | No | — | Existing chat to append to. When `null`, a new chat is created and titled from `message`. Must reference a chat owned by the caller (not deleted) when provided, else 404. |

Validation:
- `message`: required, non-blank, at most 1024 characters.
- `chatId`: optional; when provided must reference a non-deleted chat owned by the caller.

```json
{
  "message": "Recommend me a laptop for programming under $1500",
  "chatId": 42
}
```

---

## ProductSuggestionDTO

One product suggestion attached to an assistant message. Returned as a list by the hidden
`GET /api/v1/product-suggestion/products` endpoint. (`uz.melisa.dto.product.ProductSuggestionDTO`)

| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `productId` | Long | — | — | Identifier of the suggested product (resolve against the catalog service). |
| `position` | int | — | — | Zero/ordinal position of this suggestion within the message's suggestion list (primitive `int`, always present). |

```json
{
  "productId": 9001,
  "position": 0
}
```

---

## ResponseMessageDTO

Generic single-field message wrapper used for create/update/delete confirmations across
`ChatController` and `MessageController`. (`uz.melisa.dto.ResponseMessageDTO`)

| Field | Type | Required | Constraints | Description |
|---|---|:---:|---|---|
| `message` | String | — | — | Localized human-readable confirmation message (resolved from a `MessageCode` via `LocalizationService`). |

```json
{
  "message": "Chat muvaffaqiyatli saqlandi"
}
```

---

## ApiResponseStatus

Internal status enum carried on `CommonResponse.status` (`@JsonIgnore`, never serialized). Its sole
purpose is to map to an HTTP status code via `ResponseUtil.buildResponseDTO`
(`ResponseEntity.status(status.getHttpStatus())`). Clients never see this value directly — they
observe the resulting HTTP status code. (`uz.melisa.enums.ApiResponseStatus`)

| Constant | HTTP status | Meaning |
|---|---|---|
| `INVALID_PARAMETER` | 400 Bad Request | Invalid request parameter / bean-validation failure. |
| `OK` | 200 OK | Success (set by `CommonResponse.success(...)`). |
| `BAD_REQUEST` | 400 Bad Request | Generic bad request (e.g. type mismatch). |
| `INTERNAL_SERVER_ERROR` | 500 Internal Server Error | Unhandled/unexpected error. |
| `UNAUTHORIZED` | 401 Unauthorized | Missing/invalid authentication or session. |
| `UNSUPPORTED_FILE_TYPE` | 400 Bad Request | Unsupported file/media type. |
| `NOT_FOUND` | 404 Not Found | Requested resource does not exist (or is not visible to the caller). |
| `METHOD_NOT_ALLOWED` | 405 Method Not Allowed | HTTP method not allowed on the resource. |
| `NO_STATIC_RESOURCES` | 404 Not Found | No matching static resource / page. |
| `CONFLICT` | 409 Conflict | State conflict. |
| `FORBIDDEN` | 403 Forbidden | Authenticated but not permitted. |

---

<a id="paget"></a>

## Page&lt;T&gt;

Spring Data `Page` envelope used by all paginated endpoints (authenticated chat list, authenticated
chat-message list, guest message history). It is wrapped inside `CommonResponse.data`. Pagination is
driven by the standard Spring query parameters.

Request query parameters:

| Name | Type | Required | Default | Description |
|---|---|:---:|---|---|
| `page` | Integer | No | `0` | Zero-based page index. |
| `size` | Integer | No | `20` (Spring default) | Items per page. |
| `sort` | String | No | per-endpoint (e.g. `updatedAt,DESC` or `createdAt,DESC`) | Sort spec `field,(asc\|desc)`; multiple `sort` params allowed. Each endpoint sets its own `@PageableDefault`. |

Response fields (the commonly used subset):

| Field | Type | Description |
|---|---|---|
| `content` | T[] | The page's items. Empty array when there are no results. |
| `totalElements` | Long | Total number of items across all pages. |
| `totalPages` | Integer | Total number of pages. |
| `number` | Integer | Current zero-based page index. |
| `size` | Integer | Page size. |
| `first` | Boolean | `true` if this is the first page. |
| `last` | Boolean | `true` if this is the last page. |

```json
{
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
```

> Spring serializes additional fields (e.g. `numberOfElements`, `empty`, `pageable`, `sort`) that
> are not listed here; rely only on the fields above.

<!-- manual:start -->
<!-- manual:end -->
