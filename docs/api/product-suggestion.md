# Product Suggestion API

Exposes the ordered list of catalog products that the LLM previously recommended for a specific chat message in the Melissa LLM service. Each assistant message can have an associated, ranked set of product suggestions (persisted at generation time); this resource reads them back by message id. The single endpoint is mounted under the class-level prefix `/api/v1/product-suggestion` and is marked `@Hidden` — it is an internal/downstream API, not part of the public OpenAPI surface.

> Response envelope, auth header model, and the global error format are documented once in [README.md](./README.md). Each endpoint below documents only the payload type `T` inside `CommonResponse<T>`.

---

## GET /api/v1/product-suggestion/{id}/products

**Internal API** — service-to-service only, not for external integration. Marked `@io.swagger.v3.oas.annotations.Hidden`, so it is excluded from the OpenAPI/Swagger surface but is still reachable at this path.

### Purpose
Returns the ranked product suggestions that were stored for a single chat message, identified by the message id (`{id}`). Each entry carries the catalog `productId` and its 1-based `position` (rank) in the recommendation list. A client (typically another internal service or the gateway/UI) calls this to render the product cards that accompany an assistant message after the message itself has already been delivered.

### Authentication
Header-based downstream trust (verified by `PrincipalAuthFilter`): requires the `X-User-Id`, `X-User-Roles`, and `X-Auth-Signature` headers injected by the api-gateway. The path is not in the permit-all list, so an unauthenticated request is rejected with `401` (missing headers => anonymous => `.anyRequest().authenticated()` denies; an HMAC signature mismatch => `401` from the filter). No per-role/permission annotation is present on the handler — any authenticated principal passes. Note: despite being authenticated, the handler performs **no ownership check** — it does not verify that the message (or its chat) belongs to the calling `X-User-Id`.

### Request

#### Path parameters
| Name | Type | Required | Description |
|---|---|:---:|---|
| id | Long | yes | The message id whose stored product suggestions are returned. Maps to `MessageProductSuggestion.messageId`. |

### Validation rules
- `id`: required path segment; must be a valid `Long`. A non-numeric value fails type conversion and returns `400` (`MethodArgumentTypeMismatchException` -> `COMMON_INVALID_INPUT`).

### Example request
```
GET /api/v1/product-suggestion/4821/products
X-User-Id: 17
X-User-Roles: ROLE_CLIENT
X-Auth-Signature: <base64-hmac>
```

### Responses
| Status | Payload | When |
|---|---|---|
| 200 | `List<`[ProductSuggestionDTO](./models.md#productsuggestiondto)`>` | Suggestions found for the message; returned ordered. Also returns `200` with an **empty array** (`[]`) when the message has no stored suggestions (or the message id does not exist) — see Business rules. |
| 401 | standard error | Missing/invalid identity headers (no auth, or HMAC mismatch). |

### Example response
```json
{
  "data": [
    { "productId": 1042, "position": 1 },
    { "productId": 318, "position": 2 },
    { "productId": 7765, "position": 3 }
  ]
}
```

Empty result (no suggestions stored for the message, or unknown message id):
```json
{
  "data": []
}
```

### Error cases
| Status | Error code/message | Cause |
|---|---|---|
| 400 | `COMMON_INVALID_INPUT` (e.g. "Kiritilgan ma'lumot noto'g'ri") | `{id}` is not a parseable `Long` (`MethodArgumentTypeMismatchException`). |
| 401 | `AUTH_SESSION_INVALID` / signature failure | Identity headers missing or HMAC signature mismatch (`PrincipalAuthFilter` aborts, or `.anyRequest().authenticated()` denies). |
| 500 | `COMMON_SOMETHING_WENT_WRONG` | Unexpected error (e.g. data-access failure). The global handler returns `204 No Content` with no body instead if the response is already committed or is an SSE stream — not applicable to this plain JSON endpoint, which returns `500`. |

> No `404` is produced for an unknown message id: the service does not look up the message entity, only its suggestion rows, and an absent message simply yields zero rows -> empty array with `200`.

### Business rules
- Backed by `MessageProductSuggestionService.getProductsByMessage(id)` -> `MessageProductSuggestionRepository.findAllByMessageId(id)` over the `message_product_suggestion` table.
- If no rows match the message id, the service returns `CommonResponse.success([])` (HTTP `200` with an empty `data` array) — there is no "not found" path. An unknown/invalid message id is therefore indistinguishable from a message that genuinely had no suggestions.
- Each result row maps to a `ProductSuggestionDTO` with `productId` and `position` copied straight from the persisted `MessageProductSuggestion`. `position` is the 1-based rank assigned at save time (`saveProductSuggestion` stores `index + 1`, so the first suggested product has `position = 1`).
- Result ordering reflects the repository's `findAllByMessageId` return order (no explicit `ORDER BY position` is applied by the read path). Clients that need strict rank order should sort by `position` rather than rely on array order — see Notes.

### Notes for frontend/mobile
- The payload `T` is a bare JSON array (`List<ProductSuggestionDTO>`) under `data`, not a paginated `Page<T>` — there is no paging, no `totalElements`, and the full set for the message is returned in one call.
- `productId` references a product in the catalog-service; this endpoint returns only the id and rank, not product details (name/price/image). Resolve product data via the catalog service.
- Do not depend on array order for ranking; sort by `position` ascending to get the intended recommendation order.
- `position` is a primitive `int` (always present, never null). `productId` is a `Long` and is always present for returned rows.
- An empty `data: []` is a normal, non-error response — treat it as "this message has no product suggestions" rather than an error.
