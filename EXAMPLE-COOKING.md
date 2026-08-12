# Example cooking

Pretend chat after [Getting started](README.md#getting-started). **Example lot** — a Swedish suburban house — using the sample overlays in [`recipe/example-source-images/`](recipe/example-source-images/).

You're already connected; empty site on screen.

---

## Example lot — maps and light table

**Human:** Let's cook Example lot.

**Agent:** I've put a first lot outline into Blender at real scale from the map. Orbit from above — does the shape match your sense of the property?

![Lot / road light-table overlay](recipe/example-source-images/lot-road.png)

**Human:** Outline's in the right ballpark. Use the lot/road sketch for the road edge.

**Agent:** Road edge is traced on the light table. Check the viewport — when it looks right we lock it and do the house next.

![House / terrace overlay](recipe/example-source-images/house-bod-terrace.png)

**Human:** House and terrace read right. Use the elevation sketch if you need a feel for the slope — don't invent spot heights.

![Elevation lines overlay](recipe/example-source-images/elevation-lines.png)

**Agent:** Rough slope is in. Uncertain heights stay as open questions. Take a look — house, lot, road, terrace should feel like one place.

> **Image (placeholder):** Blender viewport — Example lot base: house, lot, road edge, rough terrace.

**Human:** Looks like a place. Lock that as base.

**Agent:** Base is locked. You can throw design “what ifs” at me now without mixing them into the survey facts.

---

## Suggestions

**Human:** What if the terrace roof were larger and we added a short privacy screen toward the road?

**Agent:** Try this in the viewport — larger roof plus a screen. In the Yardcraft side panel you can flip between the suggestion and the base.

> **Image (placeholder):** Viewport with larger roof + screen; Yardcraft panel Show / Base.

**Human:** Roof yes, screen a bit shorter. Save that suggestion.

**Agent:** Shortened and saved. Flip Show / Base anytime you want to compare.

---

## Fly tour

**Human:** Give me a short fly-by that sells the terrace idea.

**Agent:** Playing a short tour aimed at the terrace. How's the pacing — too fast on the approach?

**Human:** Slow the approach; hold longer on the roof edge.

**Agent:** Adjusted — play it again when you're ready. We can keep tweaking until it sells the idea.

> **Image (placeholder):** A frame from the fly-by looking at the suggested roof.

---

## Quote plan

**Human:** Contractor sheet for the terrace suggestion — top-down dimensions.

**Agent:** Dimensioned plan is ready — open `out/quote-plan.svg` when you want. It's drawn from the design facts, not a screenshot.

> **Image (placeholder):** Dimensioned terrace plan (SVG).

**Human:** Good enough to send. Thanks.

**Agent:** Genie mode from here — throw ideas; we try them live, save when you're happy.
