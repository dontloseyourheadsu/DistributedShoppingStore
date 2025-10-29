# DistributedShoppingStore

An end-to-end modular monolith for a real-time trading marketplace, designed for distributed deployment and orchestrated with .NET Aspire. It includes Blazor (web) and Avalonia (desktop) clients, a REST API gateway, a gRPC domain service, JWT authentication, and layered caching to keep item queries fast at scale.

Design diagrams:

- `system-design.mermaid` — Component/flow diagram
- `system-design.sequence-items.mermaid` — Cached item list with ETag sequence
- `system-design.sequence-join-room.mermaid` — Join room with capacity/access sequence
- `system-design.domain.mermaid` — Domain model (classes/relations)

## High-level goals

- Modular monolith: clear module boundaries inside one deployable back end, with seams to split into services later if needed.
- Orchestrated with .NET Aspire: run the REST API, gRPC service, Redis, database, and observability stack locally or in the cloud.
- Clients: Blazor Web and Avalonia Desktop use REST endpoints (no gRPC-web required).
- Internal gRPC: the REST API bridges requests to an internal gRPC Trading Service that hosts all domain logic.
- JWT auth: role-based access control (buyers, sellers, premium/verified). Premium verified sellers can host rooms with up to 20 buyers; others are limited to 5.
- Caching: item lists and popular room links cached with TTL + ETag-based conditional GET to minimize load.
- Pull model for items: removed push notifications for new offers to maximize cacheability; clients click to refresh.
- Search: by room name, seller, and category.
- Access control: public/private rooms, optional password, and shareable links.

## Architecture summary

- Blazor Web and Avalonia Desktop

  - Authenticate to obtain a JWT.
  - Call REST endpoints for all operations (list/search rooms, join, list items, place offers, etc.).

- REST API (ASP.NET Core Minimal API)

  - Validates JWT and authorization policies/roles.
  - Reads from a distributed cache (e.g., Redis) for item lists and popular links.
  - Bridges to the gRPC Trading Service for domain operations that miss in cache.
  - Implements ETag/If-None-Match for item list endpoints to enable 304 Not Modified responses.

- gRPC Trading Service (ASP.NET Core)

  - Modular domain: Users & AuthN/Z policies, Trading Rooms, Catalog/Items, Bidding, Links/Access, Search.
  - Owns data access (relational DB) and cache invalidation rules.
  - Concurrency control for offers (optimistic versioning) and room capacity enforcement (5 or 20 based on seller status).
  - No server push for new offers; item queries are pull-only to support caching.

- Auth Service (JWT issuer)

  - Issues JWTs with claims for roles (Buyer, Seller, Premium, Verified) and subject IDs.
  - Integrates with the same database for user profiles and status.

- Data and infra
  - Relational DB (e.g., PostgreSQL) for users, rooms, items, bids, links.
  - Redis for distributed caching: room item lists (by roomId) and popular room links (by slug).
  - Observability via OpenTelemetry to a local collector (e.g., OTLP to Jaeger/Seq/Elastic).
  - .NET Aspire orchestrates components and wiring for local dev and cloud-ready hosting.

## Key domain rules

- Roles

  - Buyer: may browse/search rooms, join, and place offers (per room rules).
  - Seller: may create/manage rooms and list items.
  - Premium/Verified sellers: capacity up to 20 buyers per room; regular sellers limited to 5.

- Trading rooms

  - Have name, category, seller, capacity limit, public/private flag, optional password, and shareable link (slug).
  - Private rooms require a valid password, enforced server-side; shareable links can include short-lived tokens.
  - Search supported by name (full/partial), seller username, and category.

- Items and offers
  - Items belong to a room; offers update the current price if higher.
  - Optimistic concurrency on bids prevents lost updates; API returns 409 on conflicts.
  - No push notifications for new offers; clients click refresh to pull an updated, cache-friendly list.

## Caching strategy

- Room item list

  - Cache key: `room:{roomId}:items` with TTL (e.g., 15–60 seconds).
  - Version/ETag: based on last item change version; used with `If-None-Match` to return 304.
  - Invalidate or bump version on new item creation or accepted higher offer.

- Popular links
  - Cache key: `roomlink:{slug}` with TTL (e.g., minutes). Promote to popular based on hit rate.
  - Short-lived link access tokens may be cached to improve lookups for frequently accessed private rooms.

## Security and auth

- JWT bearer tokens for both clients.
- Claims include: `sub` (user id), `role` (Buyer/Seller/Premium/Verified), optional policies like `canHost20`.
- REST API enforces authorization policies; gRPC enforces domain invariants.
- Rate limiting on REST endpoints (per IP and per user), input validation, and password hashing for private rooms.

## Example endpoints (REST)

- Auth: `POST /auth/login` -> JWT; `POST /auth/refresh`
- Rooms: `GET /rooms?name=&seller=&category=`, `POST /rooms`, `GET /rooms/{roomId}`, `POST /rooms/{roomId}/join`
- Items: `GET /rooms/{roomId}/items` (supports `If-None-Match`), `POST /rooms/{roomId}/items`
- Offers: `POST /rooms/{roomId}/items/{itemId}/offers`
- Links: `GET /rooms/link/{slug}` -> resolves room, handles public/private

Each endpoint maps to an internal gRPC method on the Trading Service. The REST->gRPC hop is internal-only.

## Observability and operations

- OpenTelemetry traces across REST and gRPC hops with baggage for correlation (userId, roomId).
- Structured logging with request IDs and room/item identifiers.
- Health checks and readiness for REST and gRPC services (Aspire surfaces these in the dashboard).

## How to explore the design

- Open `system-design.mermaid` to view the diagrams (a Mermaid extension helps preview).
- Diagrams include: component flow, request sequences (cached item list and join room), and domain model.

## Future evolution

- Split modules into independently deployable services if needed (Rooms, Items/Bids, Links) without changing public REST.
- Add search indexing for names/sellers/categories (e.g., PostgreSQL FTS or an external service) if query volume grows.
- Introduce gRPC streaming for non-item notifications (e.g., room membership changes) if UX needs real-time hints.
