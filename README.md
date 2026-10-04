# MyShop

MyShop is a native JavaFX desktop application built with Maven. It combines
local preferences and behavior history with natural-language shopping intent,
real SerpApi product retrieval, and deterministic Java ranking.

## Requirements

- JDK 25 or newer
- Maven 3.x
- Git

The current development setup uses Oracle JDK 26, Maven 3.10.0, and JavaFX 27.

## Completed

- Project bootstrap
- Block 2 UI foundation
- Block 2.2 development-time UI interactions
- Warm, product-focused JavaFX design system with reusable CSS components
- Single-stage shell with Discover, Saved, and Compare navigation
- Mock-data Discover screen with categories, product cards, and recently viewed items
- Mock-backed search/category results and product-details routes
- Clickable products with persisted hearts after authentication
- Local account creation, login, logout, BCrypt password hashing, and SQLite persistence
- Three-step onboarding with persistent shopping preferences
- API-ready search request/result architecture backed by a deterministic mock provider
- Provider-neutral shopping-intent understanding with Groq, optional Gemini, and deterministic fallback parsing
- Deterministic recommendation ranking over provider-returned products with explainable match reasons
- Product Details with real images, metadata, safe provider links, and offers when supplied
- Persisted per-user Saved Products and Recently Viewed history
- Session-scoped Compare for up to three products
- Profile/settings editing for persisted preferences and history controls

## Still pending

- Deeper multi-store enrichment when a provider supports it
- Checkout, payments, and account recovery

The live product-search path is now isolated behind the SerpApi provider seam;
the application falls back to the internal mock catalog when the key or live
data is unavailable. Optional provider credentials are read only from the
runtime environment. Product Details and external links use only provider-returned
data; missing fields remain omitted.

## UI dependencies

The UI uses JavaFX Controls and FXML from the existing JavaFX dependencies. The
application also uses SQLite JDBC `3.49.1.0`, jBCrypt `0.4`, and JUnit Jupiter
`5.13.4` for isolated tests. No third-party icon library is required; the
project uses a small native JavaFX vector icon component so the icon language
stays consistent.

## Local authentication

The application starts on the authentication screen. Account creation validates
name, email, password length, and confirmation before hashing the password with
BCrypt and storing the user in SQLite. Login uses normalized email lookup and a
generic credential error for failed authentication. A successful login places
the `User` in the in-memory `SessionManager` and shows the existing MyShop
application shell; logout clears that session and returns to authentication.

On macOS the runtime database is stored at:

```text
~/Library/Application Support/MyShop/myshop.db
```

The database is created automatically and is ignored by Git. Tests use a
temporary isolated database instead.

## Onboarding and preferences

After account creation or login, MyShop checks the user's onboarding record.
Users without a completed preference row see a compact three-step flow that
captures shopping interests, up to four priorities, shopping style, and
optional favourite brands. Finishing the flow saves the choices and opens
Discover. Returning users with completed onboarding go directly to Discover;
logout still clears only the in-memory session.

Preferences are stored in the `user_preferences` SQLite table, keyed to the
user with a foreign key. The `UserPreference` model and `PreferenceService`
are intentionally independent of the catalog so search and recommendation
services can consume them without changing the onboarding UI.

## Search foundation

Search currently follows this internal pipeline:

```text
DiscoverView → SearchRequest → SearchService → ProductSearchProvider
             → RecommendationService → SearchResult → SearchResultsView
```

The runtime provider is selected by `SERPAPI_API_KEY`. When configured,
`SerpApiProductSearchAdapter` uses SerpApi's Google Shopping engine. Without
the key, or after a live request error, empty response, invalid key, or
malformed result, MyShop uses the deterministic `MockProductSearchProvider`
without exposing technical details in the UI. The earlier DataForSEO adapter
remains dormant for history and fixture tests; it is no longer selected by
the runtime factory.

Search supports query/category matching, category/brand/price/rating filters,
and Recommended, price, and rating sorting. `RecommendationService` ranks only
the products returned by the selected provider using deterministic Java logic.
Current query intent and explicit constraints outweigh persisted preferences;
saved categories, priorities, style, and favourite brands are secondary signals.
Each ranked product may carry up to three supported, human-readable reasons.

## Shopping intent understanding

Search still looks like a normal search to the user, but `SearchService` passes
each `SearchRequest` through the provider-neutral `ShoppingIntentProvider`
seam before invoking the selected product provider. The normal runtime chain is
`GroqShoppingIntentProvider`, then `GeminiShoppingIntentProvider` when a
Gemini key is configured, then `FallbackShoppingIntentProvider`. Product
search uses SerpApi when configured and otherwise uses the mock provider.

Groq uses Java's built-in `HttpClient` and Chat Completions structured JSON
schema output. The default model is `openai/gpt-oss-20b`; override it only when
needed with `GROQ_MODEL`. The Gemini provider remains available as an optional
secondary provider and uses the official Google Gen AI Java SDK.

Configure credentials only in the runtime environment:

```bash
export GROQ_API_KEY="your-key"
# Optional Groq model override:
export GROQ_MODEL="openai/gpt-oss-20b"

# Optional secondary provider:
export GEMINI_API_KEY="your-key"
# Optional Gemini model override:
export GEMINI_MODEL="gemini-3.8-flash"
```

If `GROQ_API_KEY` is absent or Groq fails, the configured Gemini provider is
tried next. If no usable provider is available, or structured output cannot be
mapped, MyShop automatically uses `FallbackShoppingIntentProvider`. The
fallback recognizes common categories, product types, colors, budgets, and
shopping priorities so the app remains usable without network access. No key
is stored in this repository; `.env*`, secret files, and runtime data are
ignored by Git.

Saved onboarding preferences are supplied as context when Gemini is available,
while explicit intent in the current query takes priority. Parsed category and
budget constraints are carried in the normalized `SearchRequest` and can
influence the live or mock product provider.

## Recommendation ranking

The recommendation layer sits after intent understanding and product retrieval:

```text
SearchRequest
  → ShoppingIntentProvider
  → ProductSearchProvider
  → RecommendationService
  → SearchResult / SearchResultsView
```

`RecommendationService` never calls an LLM, invents products, or creates
product facts. It scores only returned `Product` objects using a transparent
priority order: current product/query match, explicit category and budget,
requested color/brand/use-case/priority, persisted onboarding preferences,
and rating/value tie-breakers. Products over an explicit maximum budget are
heavily penalized; explicit price or rating sorting overrides recommended order.

The results screen keeps the intelligence quiet: cards remain normal shopping
cards and may show a small “Why this matches” note with only reasons supported
by the query, saved preferences, or returned product metadata.

The internal score is normalized to 0–100 but is not presented as an AI score.
The current weights are: product type 28, query-term match 14, category 16,
budget 18, color 10, preferred brand 8, use case 6, priority 6, saved category
4, favourite brand 4, saved priority 2, saved style 2, and rating up to 3.
An item above an explicit maximum budget receives a -36 penalty; explicit price
and rating sort modes bypass recommendation order entirely.

## Live product search

SerpApi is the preferred live product provider. It calls the
`google_shopping` engine, maps only returned shopping fields into `Product`
and `ProductOffer`, and requests India/English defaults with at most 20 local
results. Current intent is used to build the concise query, and an INR maximum
budget is passed as `max_price` and checked again during mapping.

Configure credentials only in the runtime environment:

```bash
export SERPAPI_API_KEY="your-key"
```

Optional runtime settings are `SERPAPI_COUNTRY`, `SERPAPI_LANGUAGE`,
`SERPAPI_GOOGLE_DOMAIN`, `SERPAPI_BASE_URL`, `SERPAPI_TIMEOUT_SECONDS`, and
`SERPAPI_MAX_RESULTS`. The default country is `in`, language is `en`, and
domain is `google.co.in`. The key is never hardcoded, logged, stored in
SQLite, or committed. Product image and merchant/product URLs are retained
only when SerpApi returns usable values; missing fields remain missing and
the existing artwork placeholder handles image-load failures.

## Verify the project

Compile the project:

```bash
mvn clean compile
```

Run the JavaFX application:

```bash
mvn javafx:run
```

The application opens on the MyShop authentication screen. After creating an
account or logging in, use the top navigation to switch between Discover,
Saved, and Compare. Search and category actions run through background intent
understanding, provider retrieval, and local recommendation ranking. Product
Details, Saved, Compare, Recently Viewed, and Profile & settings are available
after login.

Run the automated tests:

```bash
mvn clean test
```

## Demo flow

1. Register or log in and complete onboarding.
2. Search for `white sneakers under ₹5000 for college, comfortable and minimal`.
3. Review ranked live products and their restrained “Why this matches” reasons.
4. Open Product Details, save a product, compare up to three products, and open
   a provider-returned product link when available.
5. Revisit Saved, Recently Viewed, and Profile & settings.

If live credentials are unavailable, the deterministic intent parser and mock
catalog keep the application usable. No fake offers or external links are
created.

## Technology and limitations

Java, JavaFX 27, Maven, SQLite, BCrypt, Groq, optional Gemini, SerpApi, and
JUnit are used. Store offers depend on fields returned by the active provider;
prices and availability can change, and MyShop does not perform checkout or
payment. Authentication and preference data are local prototype features.
