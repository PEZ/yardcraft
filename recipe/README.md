# Recipe package

Bootstrap and teaching materials that ship with the Yardcraft template. They are **not** your site’s durable facts.

| Path | Role |
|---|---|
| [`skills/`](skills/) | Agent skills to **copy** into the harness skill location during Setup (Cursor → `.cursor/skills/`). Keep this tree as the canonical package. |
| [`example-source-images/`](example-source-images/) | Sample light-table overlays for learning the sketch workflow — not survey truth for your lot. |
| [`scripts/`](scripts/) | Optional host-side helpers (e.g. height-grid fetch). Extend using the country reference skills under `skills/references/`. |

### Skills (canonical under `skills/`)

| Skill | Role |
|---|---|
| [`yardcraft-setup`](skills/yardcraft-setup/) | Layer 1 — harness, Babashka, Blender, basilisp-blender, connect, `ensure-demo!` |
| [`yardcraft-base-design`](skills/yardcraft-base-design/) | Layer 2 — orchestrate maps/sketches/facts into a real base (loads composables) |
| `basilisp`, `basilisp-blender`, `yardcraft-*`, `references/sweden-lantmateriet/` | Composable dialect / scene / product skills — stay separate; phase skills do not swallow them |

Live project code stays at the repo root (`src/yardcraft/`, `assets/`, …). Your own source photos go under `source-images/` at the repo root when you create them.
