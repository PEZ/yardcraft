# Yardcraft

Design your yard, patio, and parking in **Blender**, driven live by **Basilisp** (Clojure on Python) over nREPL — explore options with an AI pair, then promote what you like into site facts.

Demo: [YardCraft: Redesigning my terrace w/ Calva + Blender + Basilisp + Grok](https://www.youtube.com/watch?v=_JDSeMP8RhE)

- Site reading notes (human): [`site.md`](site.md)
- Agent orientation / Setup: [`AGENTS.md`](AGENTS.md)
- Recipe package (skills, example images, helper scripts): [`recipe/`](recipe/)

## Session bootstrap (short)

1. Complete **Setup** in [`AGENTS.md`](AGENTS.md) once (install skills into your harness, Blender ≥ 5.2.0 LTS, basilisp-blender + Basilisp ≥ 0.5, Babashka, Epupp). Consumer clones then delete that Setup section.
2. Blender: Basilisp Project Directory = this repo; start nREPL.
3. Calva: connect sequence **`basilisp-blender`** (loads `user.lpy` / `user/init!`).
4. Rebuild empty defaults: `(require '[yardcraft.site :as site])` … `(site/ensure-site! site)` — expect `site-root` + sun/world defaults until you fill facts.

## Sponsor my open source work ♥️

This and many other projects are provided to you open source and free to use as you wish, by Peter Strömberg a.k.a. PEZ.

* https://github.com/sponsors/PEZ

## Licence

[MIT](LICENSE)

(Free to use and open source. 🍻🗽)

## Happy exploring ❤️

With or without AI 😀
