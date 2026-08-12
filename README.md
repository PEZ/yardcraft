# Yardcraft

> [!NOTE]
> **WIP**! Come back tomorrow and this may be ready to take for a spin.

A recipe for letting your AI Agent of choice help you design your yard, patio, parking, etcetera in [Blender](https://www.blender.org/). 

Your ideas can be rendered and iterated on solely in memory (in the REPL as we Clojurians say) without updating any files. When you're happy, the design is persisted as data and code (but I am repeating myself) in the project. You can also create custom UI in Blender in the REPL and iterate on it before persisting to files in the project.

[![YardCraft: Redesigning my terrace w/ Calva + Blender + Basilisp + Grok](https://img.youtube.com/vi/_JDSeMP8RhE/maxresdefault.jpg)](https://www.youtube.com/watch?v=_JDSeMP8RhE)

[YardCraft: Redesigning my terrace w/ Calva + Blender + Basilisp + Grok](https://www.youtube.com/watch?v=_JDSeMP8RhE)

## How it works

Blender is driven live by you and the AI agent using [Basilisp](https://github.com/basilisp-lang/basilisp) (Clojure on Python) over nREPL using [basilisp-blender](https://github.com/ikappaki/basilisp-blender). 

This recipe is provided to you as this project, and you then use your AI Agent as your guide and doer to set things up. Once you have things set up, you enter genie mode and can throw your ideas at the agent which carries them out and maintains the project for you. True vibe designing.

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

**Agent:** Now we need to start the nREPL server so that I can drive Blender. Open the **Output Properties** tab (printer icon) and find the **Basilisp nREPL server** panel. Set the project path to this project folder, then click **START SERVER**. Tell me when it's running.

![Blender Output Settings nREPL](recipe/readme/images/basilisp-blender-nrepl-panel.png)

**Human:** Server running.

**Agent:** Next step is to connect me to the Blender nREPL server. In Cursor issue the command: **Calva: Connect to a running REPL server in the project**. Then select **basilisp-blender** in the **Project Type** menu.

**Human:** Done, I see a green **basilisp-blender** indicator in the status bar.

**Agent:** ⚡️ That means we are connected. Let me test the REPL. You should see an empty site in Blender — just the stage, no house or lot yet. I'll put something more fun there. Watch in Blender or here...

... The agent hammers on the REPL ...

What do you see now?

**Human:** I see **YARDCRAFT** in perspective, with some furniture on. And... I think a sundial ... And it's like I am flying around it in a circle, with the design always centered in the view. I also see a **Yardcraft** UI panel with some widgets.

> **Image (placeholder):** GIF: Blender viewport — The scene described above.

**Agent:** Sweet. The UI should have a time-of-day slider you can use to see the change in how the shadows fall (and thus the sundial showing the time). You can then click the **Set time** button to align the lounger chairs to face the sun.

**Human:** Yeah, totally works!

**Agent**: You can exit the fly-cam mode by zooming or panning in Blender. To enable it again, click the **Fly Cam** button.

**Human:** Great. Now what?

**Agent:** Now I will need to ask you for information and material so that we can get the base structure of your yard up to replace the demo design. Material such as maps and sketch over photos, and we will try to find information from the web and any open APIs to make the manual labor less. You can look at the [`EXAMPLE-COOKING.md`](EXAMPLE-COOKING.md) document to get an idea for what the process may look like. Or just jump straight in at the deep end, let me know when you are ready for the next step.

**Human:** Let's get cranking!





## Recipe content

- Example cook (maps → quote plan): [`EXAMPLE-COOKING.md`](EXAMPLE-COOKING.md)
- Site reading notes (human): [`site.md`](site.md)
- Agent orientation / Setup: [`AGENTS.md`](AGENTS.md)
- Recipe package (skills, example images, helper scripts): [`recipe/`](recipe/)

## Licence

[MIT](LICENSE)

(Free to use and open source. 🍻🗽)
