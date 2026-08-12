# Yardcraft

A recipe for lettting your AI Agent of choice help you design your yard, patio, parking, etcetera in [Blender](https://www.blender.org/). 

Your ideas can be rendered and iterated on solely in memory (in the REPL as we Clojurians say) without updating any files. When you're happy, the design is persisted as data and code (but I am repeating myself) in the project. You can also create custom UI in Blender in the REPL and iterate on it before persisting to files in the project.

[![YardCraft: Redesigning my terrace w/ Calva + Blender + Basilisp + Grok](https://img.youtube.com/vi/_JDSeMP8RhE/maxresdefault.jpg)](https://www.youtube.com/watch?v=_JDSeMP8RhE)

[YardCraft: Redesigning my terrace w/ Calva + Blender + Basilisp + Grok](https://www.youtube.com/watch?v=_JDSeMP8RhE)

## How it works

Blender is driven live by you and the AI agent using [Basilisp](https://github.com/basilisp-lang/basilisp) (Clojure on Python) over nREPL using [basilisp-blender](https://github.com/ikappaki/basilisp-blender). 

This recipe is provided to you as this project, and you then use yor AI Agent as your guide to set things up.

## Session bootstrap (short)

1. Complete **Setup** in [`AGENTS.md`](AGENTS.md) once (install skills into your harness, Blender ≥ 5.2.0 LTS, basilisp-blender + Basilisp ≥ 0.5, Babashka, Epupp). Consumer clones then delete that Setup section.
2. Blender: Basilisp Project Directory = this repo; start nREPL.
3. Calva: connect sequence **`basilisp-blender`** (loads `user.lpy` / `user/init!`).
4. Rebuild empty defaults: `(require '[yardcraft.site :as site])` … `(site/ensure-site! site)` — expect `site-root` + sun/world defaults until you fill facts.

## Recipe content

- Site reading notes (human): [`site.md`](site.md)
- Agent orientation / Setup: [`AGENTS.md`](AGENTS.md)
- Recipe package (skills, example images, helper scripts): [`recipe/`](recipe/)

## Licence

[MIT](LICENSE)

(Free to use and open source. 🍻🗽)
