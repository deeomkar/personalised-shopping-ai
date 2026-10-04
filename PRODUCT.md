# Product

<!-- impeccable:product-schema 1 -->

## Platform

adaptive

## Stack

Existing native Java desktop application using Java, JavaFX, Maven, and CSS;
FXML support is retained for future view/controller work.

## Users

Inferred from the supplied brief: people browsing products on a laptop or
desktop who want shopping discovery to feel personal, calm, and useful.

## Product Purpose

MyShop is a personalized shopping application. Its first experience should
help a user discover products, revisit saved items, and prepare to compare
options without making the product feel like a chatbot.

## Positioning

Inferred from the supplied brief: intelligence should work invisibly behind a
credible, product-focused shopping experience rather than being presented as
an AI assistant.

## Operating Context

The current product is a native JavaFX desktop application used in a
comfortable laptop-sized window. Block 2 establishes the Discover, Saved, and
Compare destinations with mock data only.

## Capabilities and Constraints

- The application uses one Stage and one Scene with a reusable application shell.
- Navigation swaps the central content view without recreating the Stage.
- Discover, Saved, and Compare are functional presentation destinations.
- Product content, save state, search, comparison, and profile behavior are
  intentionally mocked or deferred.
- Local authentication is implemented with SQLite persistence and an
  in-memory session; external APIs, recommendations, and live product data
  remain deferred.
- Three-step onboarding stores shopping interests, priorities, shopping style,
  and optional favourite brands in SQLite for the authenticated user.

## Brand Commitments

- Product name: MyShop.
- The visual language is clean, warm, editorial, calm, product-focused, and
  slightly premium.
- Avoid chatbot/AI-wrapper signals, robot mascots, chat bubbles, excessive
  gradients, excessive glassmorphism, and overly rounded controls.
- Use a warm off-white background, white surfaces, charcoal text, muted grey
  secondary text, a restrained accent, and subtle green success states.

## Evidence on Hand

No live product data, brand assets, or product photography were supplied.
Mock products and geometric placeholder artwork must remain clearly synthetic
until real sources are approved.

## Product Principles

- Make discovery feel human and product-led.
- Keep intelligence quiet and useful rather than performative.
- Prefer calm hierarchy and readable spacing over visual noise.
- Build reusable foundations before adding product-specific behavior.

## Accessibility & Inclusion

Use readable contrast, visible focus states, descriptive control labels,
keyboard-friendly controls, and layouts that remain usable when the window is
resized.
