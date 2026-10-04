# Live product-provider research

Research snapshot: 5 October 2026

## Decision summary

MyShop should keep `ProductSearchProvider` as the application seam and add
vendor-specific implementations behind `LiveProductSearchAdapter`. The
Block 7C selects SerpApi as the preferred live provider. The application uses
`MockProductSearchProvider` when `SERPAPI_API_KEY` is missing or a SerpApi
request fails. The DataForSEO attempt remains documented as dormant history.

For an India-focused student project, the practical shortlist is:

1. **Flipkart Affiliate API** if the project can obtain and keep an approved
   affiliate account. It exposes India-oriented product data, prices, images,
   stock, and affiliate product URLs, but the documented keyword-search
   endpoint is marked for deprecation, so current access must be confirmed
   before implementation.
2. **DataForSEO Merchant API / Google Shopping** if multi-store comparison is
   the primary requirement and a small paid API budget is acceptable. Its
   product and seller endpoints are a closer match for `Product` plus
   `ProductOffer` than a single-store affiliate API.
3. **Amazon Creators API** for an Amazon-only experience if the project has a
   fully accepted Associates account and meets the API eligibility rules. It
   should be treated as a single marketplace, not a comparison source.

SerpApi is now the selected quick prototype for Google Shopping-shaped
results. Its recurring pricing and dependence on a third-party results service
remain limitations. eBay Browse API is a useful
single-marketplace alternative but its documented Buy API marketplace list
does not include India.

No option below requires MyShop to scrape shopping sites directly. Direct
scraping is intentionally out of scope.

## Current repository fit

The existing contract is:

```text
SearchRequest -> ProductSearchProvider.search(...) -> List<Product>
```

`ProductSearchProvider` was left unchanged. The existing `Product` already
contains the fields needed for a basic live result (`id`, `brand`, `name`,
`category`, `rating`, `store`, and `imageUrl`) as well as legacy presentation
price fields. The smallest safe extension is a backwards-compatible
`List<ProductOffer> offers` component. Existing constructors default it to an
empty list, so mock catalog data and current UI code continue to work.

`ProductOffer` is an immutable normalized store-level record:

```text
ProductOffer
  productId
  storeName
  price
  originalPrice
  currency
  productUrl
  availability
```

An offer requires a non-negative price and a real HTTP(S) product URL. The
model does not create placeholder prices, guessed URLs, or synthetic offers.
Availability is normalized to a small enum and can remain `UNKNOWN` when the
upstream source does not provide a usable status.

`LiveProductSearchProvider` delegates to `LiveProductSearchAdapter` and owns
fallback behavior. `SerpApiProductSearchAdapter` calls the documented
`google_shopping` search endpoint using Java's built-in `HttpClient`; tests
inject a fixture transport instead of calling the paid service. The earlier
DataForSEO adapter remains available but is not runtime-selected.

### Current offer behavior

The current SerpApi integration maps a `ProductOffer` only when the upstream
result supplies a real store/source, non-negative price, currency, and a
valid HTTP(S) product URL. Product Details sorts and displays those known
offers without fabricating sellers, prices, ratings, or links. There are no
per-result seller-enrichment calls yet, so a product may legitimately have
zero or one known offer even when the provider returns a broader result set.
This keeps the comparison UI honest while leaving room for a future
multi-store provider or enrichment pass.

## Candidate comparison

| Candidate | What it provides | Authentication / access | Direct product URL | Prices | Multi-store comparison | Limitations, cost, and scraping status |
|---|---|---|---|---|---|---|
| [Flipkart Affiliate API](https://affiliate.flipkart.com/api-docs/af_overview.html) | India-focused product search/feed data, brand, images, MRP, selling price, stock, offers | Registered Flipkart Affiliate account; requests use affiliate ID and API token | Yes; documented `productUrl` includes affiliate tracking | Yes; INR fields include selling price and MRP | No; primarily Flipkart inventory, though it is suitable as one offer source | The documented keyword endpoint is marked “deprecated soon” in the [API reference](https://affiliate.flipkart.com/api-docs/af_prod_ref.html); result count is capped at 10 for keyword search. No direct scraping by MyShop. |
| [DataForSEO Merchant API](https://docs.dataforseo.com/v3/merchant-api-overview/) | Google Shopping product listings, product details, prices, sellers, images, ratings, availability | DataForSEO account and API authentication; asynchronous task POST/GET flow | Yes; product and seller records expose product/seller URLs | Yes; current/regular price and currency are documented | **Yes**; product and sellers endpoints are explicitly intended for seller/price comparison | Pay-as-you-go. Its current pricing page advertises $1 free credits and a standard queue price from $0.001 per item; budget and location coverage must be validated. This is a vendor API, not a MyShop scraper. |
| [SerpApi Google Shopping](https://serpapi.com/google-shopping-api) | Google Shopping result title, price, source/merchant, rating, reviews, thumbnail, and link | SerpApi API key | Usually yes when a result contains `link`; validate whether it is a merchant URL or an intermediate result URL before displaying | Yes when Google Shopping exposes a price | Partially; multiple merchants can be returned, but product identity and seller grouping are less explicit than DataForSEO’s seller endpoint | Current pricing lists a free 250-search plan, then $25/month for 1,000 searches and higher tiers. Terms, geography, result stability, and merchant-link behavior need review. MyShop would call the API; it would not scrape shopping pages itself. |
| [Amazon Creators API](https://affiliate-program.amazon.com/creatorsapi/docs/en-us/introduction) | Amazon catalog search, title/brand/item data, images, offers, price, availability, and Amazon detail links | Accepted Amazon Associates account, Creators API registration, credentials, and marketplace partner tag; current docs describe eligibility requirements | Yes; item responses include `detailPageURL` | Yes through `OffersV2`, subject to marketplace and customer context | No; Amazon-only, with multiple offer listings for an Amazon item rather than multiple stores | Amazon’s current API is eligibility-gated; the docs say access requires an accepted Associates account and qualifying sales. The older PA-API docs direct users to migrate to Creators API. No free usage tier or stable student quota is assumed here. |
| [eBay Browse API](https://developer.ebay.com/develop/api/buy/browse_api) | Keyword item search, item details, images, seller/item data, prices, and availability | eBay developer application and Application access token | Yes; item and affiliate web URL fields are available depending on request headers | Yes | No; primarily eBay listings, not a cross-retailer comparison API | The documented Buy API marketplace list does not include India. A sandbox is available, but production coverage and currency/location behavior must be checked. No scraping by MyShop. |

### Candidate details

#### Flipkart

Flipkart is the best geographic match for an India-first demo. Its affiliate
documentation describes product-feed and keyword-search APIs and documents
`productId`, image URLs, brand, `productUrl`, MRP, selling price, currency, and
`inStock`. That maps cleanly to one `Product` with one `ProductOffer`.

The risk is API lifecycle: the keyword-search page says that endpoint will be
deprecated soon and points to the newer Product Feed API. This must be tested
with a real affiliate account before any adapter is written. The API should
not be treated as a generic multi-store comparison source.

#### DataForSEO

DataForSEO is the strongest match for the `ProductOffer` direction. Its
Merchant API documents separate Google Shopping product, product-info, and
seller flows. Product details include image URLs, ratings, current/regular
prices, currency, availability, and URLs; seller results provide seller names,
seller URLs, and seller-specific prices. This supports grouping several offers
under one normalized product, subject to reliable product matching.

The trade-off is asynchronous request orchestration and usage cost. A future
adapter should isolate task submission, polling, timeouts, location settings,
and response mapping from `SearchService`.

#### SerpApi

SerpApi has the simplest apparent request shape for a student prototype:
submit a Google Shopping query and map `title`, `price`, `source`, `rating`,
`thumbnail`, and `link`. Its current pricing page lists a small free plan and
paid monthly plans.

It is less explicit about canonical product identity and seller grouping than
DataForSEO. A future adapter should never assume that every returned `link` is
a direct merchant product page; it must validate the URL and preserve only
the upstream value.

#### Amazon Creators API

Amazon’s current Creators API is a realistic source for Amazon India when the
project has the required Associates access. `SearchItems` supports keyword
search and can request images, item information, and `OffersV2` price and
availability resources. It returns Amazon detail URLs and supports the India
marketplace through the documented locale configuration.

It is not a multi-store solution. Access requirements are material for a
student project: Amazon documents accepted Associates status, API
registration, credentials, and qualifying-sales eligibility. The integration
should target Creators API rather than starting a new PA-API implementation.

#### eBay Browse API

eBay’s Browse API has a clean OAuth-style application-token flow and useful
item-summary/item-detail fields. It is technically straightforward and has a
sandbox, but India is absent from the documented Buy API marketplace list.
It is therefore a fallback for a globally scoped demo, not the default India
provider.

## Configuration

SerpApi uses one provider-specific secret and contains no committed secrets:

```dotenv
SERPAPI_API_KEY=
SERPAPI_COUNTRY=in
SERPAPI_LANGUAGE=en
SERPAPI_GOOGLE_DOMAIN=google.co.in
SERPAPI_TIMEOUT_SECONDS=15
SERPAPI_MAX_RESULTS=20
```

`SerpApiConfig` reads these values and redacts the key from its `toString()`.
The optional base URL override is intended for controlled testing; production
defaults to `https://serpapi.com/search.json`. DataForSEO environment settings
are no longer part of runtime provider selection.

## Future integration plan

1. Verify the SerpApi account, terms, India coverage, rate limits, and URL
   behavior in the target deployment.
2. Map only upstream fields: do not infer a price, rating, image, store, or
   link when the response omits it.
3. Normalize every merchant listing into `ProductOffer`; use the upstream
   product identifier where available and define a documented fallback only
   for grouping, not for inventing a URL.
4. Populate the legacy `Product` display fields from the selected primary
   result while retaining all verified offers on the product.
5. Add HTTP timeout, rate-limit, error, cache, and stale-data handling inside
   the adapter boundary. `SearchService` and the JavaFX views should continue
   to depend only on `ProductSearchProvider` and `SearchResult`.
6. Add later price-comparison enrichment only after product identity and
   seller grouping requirements are settled.

## Open decisions after Block 7C

- Is the first release India-only, or must it support several countries?
- Is one-store search acceptable for the first live release, or is
  multi-store comparison a hard requirement?
- Should product grouping use provider IDs only, or add a later normalized
  GTIN/brand/model matching strategy?
- Which affiliate disclosure and click-out behavior are required for the UI?
