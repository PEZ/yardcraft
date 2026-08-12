# Yardcraft

> [!NOTE]
> **WIP**! Come back tomorrow and this may be ready to take for a spin.

A recipe for lettting your AI Agent of choice help you design your yard, patio, parking, etcetera in [Blender](https://www.blender.org/). 

Your ideas can be rendered and iterated on solely in memory (in the REPL as we Clojurians say) without updating any files. When you're happy, the design is persisted as data and code (but I am repeating myself) in the project. You can also create custom UI in Blender in the REPL and iterate on it before persisting to files in the project.

[![YardCraft: Redesigning my terrace w/ Calva + Blender + Basilisp + Grok](https://img.youtube.com/vi/_JDSeMP8RhE/maxresdefault.jpg)](https://www.youtube.com/watch?v=_JDSeMP8RhE)

[YardCraft: Redesigning my terrace w/ Calva + Blender + Basilisp + Grok](https://www.youtube.com/watch?v=_JDSeMP8RhE)

## How it works

Blender is driven live by you and the AI agent using [Basilisp](https://github.com/basilisp-lang/basilisp) (Clojure on Python) over nREPL using [basilisp-blender](https://github.com/ikappaki/basilisp-blender). 

This recipe is provided to you as this project, and you then use yor AI Agent as your guide and doer to set things up. Once you have things set up, you enter genie mode and can throw your ideas at the agent which carries them out and maintains the project for you. True vibe designing.

## Prerequisites

* Git

The agent will help you get everything else in place. (If you don't have Git, the agent can help you with that too, of course.)

## Getting started

1. On [github.com/PEZ/yardcraft](https://github.com/PEZ/yardcraft), click **Use this template**
2. Clone your copy of the repo
3. Open the clone in your editor (the example below assumes **Cursor**).
4. Say **Hello** to your AI agent in a new chat. It takes things from there.

### Example chat

**Human:** Hello

**Agent:** Hello! Yardcraft is designing your yard (patio, parking, lawn, trees — whatever) in Blender with me as your AI pair. I build in Blender, you check the viewport in Blender, we save into this project when you're happy. Don't worry if you don't know Blender — I can guide you. How would you rate your Blender knowledge? **1** never used it → **5** expert.

**Human:** 2

**Agent:** Perfect. I can **do** the setup or only **give you instructions**. Which do you prefer?

**Human:** Do.

**Agent:** Editor tooling is in place. Next, install [latest Blender](https://www.blender.org/download/) and tell me when it's done.

**Human:** Blender's installed.

**Agent:** I've downloaded the Basilisp Blender extension to ~/Downloads/basilisp-blender.zip. In Blender: **Edit → Preferences → Get Extensions → Install From Disk…**, pick the zip, enable it if asked. Let me know when you're done, or if you need help.

**Human:** Extension installed

**Agent:** Now we need to start the nREPL server so that I can drive Blender. Open the Output settings tab and fidn the nREPL panel, and set the Basilisp Project Directory to this project folder. Then click **START SERVER**. Tell me when it's running.

![Blender Output Settings nREPL](recipe/readme/images/basilisp-blender-nrepl-panel.png)

**Human:** Server running.

**Agent:** Connected. You should see an empty site in Blender — just the stage, no house or lot yet. From here we can pull in maps, sketch over photos, try design ideas, fly-throughs, and a dimensioned plan for a contractor. Want to walk through a full pretend cook on **Example lot**? ([`EXAMPLE-COOKING.md`](EXAMPLE-COOKING.md))

> **Image (placeholder):** Blender viewport — empty site / stage, nothing built yet.

## Recipe content

- Example cook (maps → quote plan): [`EXAMPLE-COOKING.md`](EXAMPLE-COOKING.md)
- Site reading notes (human): [`site.md`](site.md)
- Agent orientation / Setup: [`AGENTS.md`](AGENTS.md)
- Recipe package (skills, example images, helper scripts): [`recipe/`](recipe/)

## Licence

[MIT](LICENSE)

(Free to use and open source. 🍻🗽)
