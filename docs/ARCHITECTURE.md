# MyShop Architecture

## Current foundation

MyShop is a modular JavaFX desktop application managed by Maven.

- `com.myshop.MyShopApplication` owns the single Stage and Scene, initializes
  the runtime database, wires authentication, and attaches the CSS design
  system.
- `navigation.ApplicationRouter` keeps one Stage/Scene architecture and swaps
  between the authentication root and the authenticated `AppShell` root.
- `view.AuthView` provides the login/create-account UI. `controller.AuthController`
  is a thin UI-facing delegate to `service.AuthService`.
- `service.AuthService` owns validation, normalized email lookup, registration,
  login, duplicate handling, and generic credential failures.
- `security.BCryptPasswordHasher` hashes passwords with BCrypt; plaintext
  passwords are never stored.
- `session.SessionManager` stores the current authenticated `User` in memory
  only. Logout clears it before returning to the authentication root.
- `repository.UserRepository` defines persistence operations and
  `repository.SqliteUserRepository` implements them through `DatabaseManager`.
  The database schema is initialized on application startup.
- `model.UserPreference` contains selected categories, shopping priorities,
  shopping style, optional favourite brands, onboarding completion, and an
  update timestamp.
- `repository.UserPreferenceRepository` defines preference persistence and
  `repository.SqliteUserPreferenceRepository` stores one preference row per
  user in SQLite. Existing users without a row remain incomplete.
- `service.PreferenceService` owns the allowed options, required selections,
  four-priority limit, onboarding completion, loading, and updates.
- `view.OnboardingView` is a three-step JavaFX flow that calls the service and
  never contains SQL.
- `component.AppShell` owns the persistent top navigation and central content
  region for an authenticated user and exposes logout through the user menu.
- `navigation.NavigationManager` swaps registered views in the central region
  and tracks the active destination without rebuilding the Stage. It creates
  `SearchRequest` instances for typed queries and category selections and
  passes `Product` objects to the detail route.
- `navigation.NavigationDestination` contains Discover, Saved, Compare,
  Search Results, Product Details, and Profile & settings; only the first three
  are primary top navigation items.
- `view.DiscoverView` is the scrollable shopping home screen with persisted
  recently viewed content when available.
- `view.SavedView` renders per-user SQLite snapshots and `view.CompareView`
  renders the session-scoped maximum-three selection.
- `view.SearchResultsView` is the results screen driven by a `SearchRequest`;
  it owns presentation, filters, sorting, and loading/error/empty states for
  either SerpApi or mock results.
- `view.ProductDetailsView` is driven by a `Product` and optional
  `Recommendation`; it omits unknown fields, renders known offers, and opens
  only validated provider-returned HTTP(S) links.
- `view.ProfileView` edits persisted onboarding preferences and provides clear
  controls for searches and recently viewed products.
- `model.SearchRequest` carries the raw query, optional category, current user,
  persisted preferences, filters, sort mode, and normalized shopping intent.
- `model.ShoppingIntent` is a provider-neutral structured representation of
  category, product type, budgets, brands, colors, use cases, priorities,
  keywords, and optional shopping style.
- `model.SearchResult` carries the original request, products, result count,
  applied filters, success/error state, and optional ranked recommendations.
- `service.SearchService` validates the request, enriches it with persisted
  preferences when needed, asks a `ShoppingIntentProvider` to understand the
  query, applies safe category/budget normalization, delegates to a product
  provider, and passes only returned products to `RecommendationService`.
- `model.Recommendation` pairs a provider-returned `Product` with a normalized
  deterministic score and up to three `RecommendationReason` entries.
- `service.RecommendationService` is provider-neutral. It ranks product type,
  query, category, explicit constraints, colors, brands, use cases, and
  priorities before applying lower-weight persisted preference and rating
  signals. It never calls an LLM or invents product facts.
- `service.SavedProductService` and `repository.SqliteSavedProductRepository`
  persist a minimal per-user product snapshot for offline Saved rendering.
- `service.HistoryService` and `repository.SqliteHistoryRepository` persist
  deduplicated recent searches and viewed product snapshots, and expose a small
  behavior profile used only as a low-weight ranking signal.
- `service.CompareService` owns the session-only three-product selection.
- `service.ProductOfferService` normalizes and caches offers already returned
  by a provider; no per-result seller requests are made.
- `service.ShoppingIntentProvider` is the provider-neutral intent seam.
- `service.ShoppingIntentProviderChain` tries Groq first, then a configured
  Gemini provider, then the deterministic fallback.
- `service.GroqShoppingIntentProvider` uses Java's built-in `HttpClient` with
  Groq Chat Completions structured JSON Schema output.
  `service.GeminiShoppingIntentProvider` remains an optional secondary
  provider using the official Google Gen AI Java SDK, while
  `service.FallbackShoppingIntentProvider` keeps search working without a key,
  network, or valid remote response.
- `service.ProductSearchProvider` is the product-search seam.
  `service.ProductSearchProviderFactory` selects SerpApi when its key is
  configured and otherwise selects
  `service.MockProductSearchProvider`.
- `service.LiveProductSearchProvider` owns live-request resilience and falls
  back to the mock provider after an API failure or empty live response.
- `service.DataForSeoProductSearchAdapter` uses Java's built-in `HttpClient`
  with Basic Authentication, submits a Google Shopping Products task, polls
  the advanced task result, and maps upstream fields into `Product` and
  `ProductOffer`. Its HTTP boundary is `DataForSeoTransport`; DataForSEO is
  dormant and is not selected by the runtime factory.
- `service.SerpApiProductSearchAdapter` uses Java's built-in `HttpClient` with
  the `google_shopping` engine, India/English defaults, local budget filtering,
  and fixture-injected HTTP responses. SerpApi-specific JSON stays inside the
  adapter layer.
- `view.SearchResultsView` owns only result presentation and filter controls.
  It invokes search asynchronously, renders loading/error/empty states, respects
  explicit sort modes, and reuses `ProductCard` for clickable results.
- `component.RecommendationNote` keeps ranking explanations visually restrained
  and uses only the reasons attached by `RecommendationService`.
- `controller` contains thin UI delegates and remains ready for future FXML
  controllers.
- `model` contains domain objects and API-ready search/result contracts.
- `service.MockCatalogService` provides clearly synthetic UI presentation data.
- `repository` contains persistence abstractions and SQLite implementations.
- `component` contains reusable JavaFX UI patterns including category cards,
  product cards, compact product items, section headings, prices, ratings,
  empty states, buttons, search fields, product artwork, and native vector
  icons. Product and compact cards emit their `Product` to the router when
  clicked; heart state is connected to `SavedProductService` when the shell is
  authenticated.
- `config` contains application configuration such as the platform-specific
  runtime database location.
- `util` will contain narrowly scoped shared utilities.
- `src/main/resources/com/myshop/view` is reserved for FXML views.
- `src/main/resources/com/myshop/css` contains `base.css`, `components.css`,
  and `screens.css`.
- `src/main/resources/com/myshop/images` and `icons` are reserved for assets.

## UI structure

The shell keeps the top bar mounted while `NavigationManager` swaps the
scrollable center view. Discover uses a vertically scrolling layout with a
  responsive `FlowPane` for category and product sections. Product artwork is
  geometric mock content, not live catalog imagery. The `Product` model includes
  a stable identifier, category, optional description and `imageUrl`, and
  `ProductArtwork` can render either a future image URL or the current local
  geometric placeholder. Saved and Compare
currently provide return-to-Discover empty states.

FXML support is retained in the module descriptor and controller package for
future screens; Block 2 uses code-based reusable components so the design
system and mock presentation remain easy to compose.

The product provider, offers, saved snapshots, compare state, history, and
profile preference edits are intentionally honest about their scope:
provider-returned data is used as-is, compare state is session-only, and the
local authentication prototype does not include account recovery or checkout.

## Authentication flow

```text
AuthView
   ↓
AuthController
   ↓
AuthService ───→ BCryptPasswordHasher
   ↓
UserRepository
   ↓
SqliteUserRepository → DatabaseManager → SQLite
   ↓
SessionManager
   ↓
ApplicationRouter → AppShell
```

Registration writes a hashed password and user metadata to SQLite. Login reads
the user through the repository and verifies the password through the hasher.
The router then mounts the existing shell on the same Stage/Scene. Logging out
clears `SessionManager` and mounts a fresh `AuthView`.

## Onboarding flow

```text
OnboardingView
     ↓
PreferenceService
     ↓
UserPreferenceRepository
     ↓
SQLite
```

After login or registration, `ApplicationRouter` checks
`PreferenceService.hasCompletedOnboarding(user.id())`. A missing row or an
incomplete row mounts `OnboardingView`; a completed row mounts `AppShell`.
Finishing onboarding writes or updates the user's preference row and then
opens Discover. Later search and recommendation services can consume
`UserPreference` alongside a user search query without changing this flow.

## Local persistence flow

```text
Authenticated user
   ├── UserPreferenceRepository → user_preferences
   ├── SavedProductRepository  → saved_products
   ├── HistoryRepository       → search_history / viewed_products
   └── CompareService          → session memory only
```

`DatabaseManager.initialize()` adds tables with `CREATE TABLE IF NOT EXISTS`,
so the saved/history additions are additive for existing local databases.
Every saved and history query is scoped by `user_id`; logout clears the session
and compare selection, while saved/history rows remain available to the next
authenticated user only through that user's ID.

## Search flow

```text
DiscoverView
      ↓
SearchRequest
      ↓
SearchService
      ↓
ShoppingIntentProviderChain
      ├── GroqShoppingIntentProvider (primary)
      ├── GeminiShoppingIntentProvider (optional secondary)
      └── FallbackShoppingIntentProvider
      ↓
normalized SearchRequest
      ↓
ProductSearchProvider
      ├── SerpApiProductSearchAdapter (when configured)
      └── MockProductSearchProvider (default/fallback)
      ↓
RecommendationService
      ├── current ShoppingIntent signals (primary)
      ├── returned Product metadata
      └── UserPreference signals (secondary)
      ↓
SearchResult
      ↓
SearchResultsView
```

Category cards use the same request path as typed searches. Additional
marketplace adapters can be added behind the same provider seam without
changing the search results screen or product-card contract.

### Recommendation ranking

`RecommendationService` scores only `Product` objects returned by the selected
provider. Its current normalized scoring weights are product type 28, query
terms 14, category 16, budget 18, color 10, preferred brand 8, use case 6,
priority 6, persisted category 4, favourite brand 4, persisted priority 2,
saved style 2, and rating up to 3. Products above an explicit maximum budget
receive a -36 penalty. The resulting internal score is normalized to 0–100,
but the UI uses restrained labels such as “Strong match” rather than exposing
an AI score. Explicit price and rating sort modes bypass recommendation order.

Reasons are emitted only when supported by the current query, structured
ShoppingIntent, persisted UserPreference, or returned product metadata. Groq
understands language; it never chooses products or supplies product facts.

### Fallback flow

```text
Groq failure or missing key → Gemini → deterministic intent fallback
SerpApi failure or missing key → MockProductSearchProvider
Provider products → deterministic ranking → JavaFX result states
```

All HTTP work is performed by background search tasks. Remote image failures,
invalid links, missing offers, and provider errors degrade to local empty or
fallback states rather than exposing raw diagnostics in the UI.

### Intent configuration and safety

`config.GroqConfig` reads `GROQ_API_KEY` and optional `GROQ_MODEL`, defaulting
the model centrally to `openai/gpt-oss-20b`. `config.GeminiConfig` reads
`GEMINI_API_KEY` and optional `GEMINI_MODEL`. Keys are never hardcoded or
written to the repository. Groq sends the raw query and saved onboarding
preferences through a strict JSON Schema request; Gemini uses its structured
JSON configuration. `ShoppingIntentJsonMapper` maps provider JSON into
`ShoppingIntent`; prose parsing of provider output is not used. The JavaFX
results task performs `SearchService.search` in the background, so provider
network requests do not block the Application Thread.

SerpApi live search is isolated behind the product provider seam and keeps the
mock provider as a resilience fallback. DataForSEO remains dormant rather than
being selected by the runtime factory. Recommendation ranking is deterministic
Java logic over returned products; it does not make per-product Groq requests
and does not expose an AI score in the UI.
