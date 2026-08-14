# VS Code family adapter (progressive disclosure)

Load this when Observe says the harness is **VS Code family** (Cursor, VS Code + Copilot, other forks). The main **`yardcraft-setup`** skill stays harness-agnostic; this file holds the verified Calva + Calva Backseat Driver path.

**Naming (agent-private):** introduce **Calva Backseat Driver**, then **Backseat Driver**. Visitor-facing status uses outside-in wording from the main skill — not that short name.

No alternate Clojure clients on VS Code family. Do not invent Backseat Driver “peers.”

## Install Calva + Backseat Driver

```bash
# Cursor
cursor --list-extensions
cursor --install-extension betterthantomorrow.calva
cursor --install-extension betterthantomorrow.calva-backseat-driver

# VS Code (same extensions)
code --list-extensions
code --install-extension betterthantomorrow.calva
code --install-extension betterthantomorrow.calva-backseat-driver
```

Confirm with `--list-extensions`. Repo ships `.vscode/settings.json` with the **`basilisp-blender`** connect sequence.

## Agent bridge gate (MCP)

Three states (same as main skill) — Phase **nREPL client the AI can use** is **(3)**:

1. Extensions installed  
2. Calva jacked in / connected (green **bb** / **basilisp-blender**)  
3. This chat can call Backseat Driver eval tools  

```
λ backseat_driver_mcp_gate.
  Observe(MCP_tools)
  → must_see(clojure_evaluate_code ∨ clojure_list_sessions)
  | extension_installed ≠ agent_can_eval
  | Calva_status_bar_green ≠ agent_can_eval
  | ¬tools ∨ 0_tools ∨ server_missing → STOP ∧ guide(reload_window)
  | ¬workaround(Joyride→Calva_API ∨ shell_nREPL) as_substitute
  | gate_pass → then jack-in / Connect_to_Blender_REPL / ensure-demo!
```

**Observe:** require `clojure_evaluate_code` and/or `clojure_list_sessions`. Server listed with **0 tools = fail**.

**STOP:** do not treat agent-driven Babashka / Blender connect / `ensure-demo!` as done until the gate passes. Leave the Phase row unchecked until then.

**Invariant:** Joyride (or similar) may diagnose; must not substitute for Backseat Driver.

### When the gate fails — human guidance (default)

Outside-in. Prefer the smallest ask:

> The editor tools are installed, but my link into them isn’t live yet. Please run **Developer: Reload Window** from the Command Palette, then tell me when that’s done.

Re-Observe MCP tools after they confirm. Do **not** lead with a multi-step MCP troubleshooting essay.

### If reload alone doesn’t restore tools

Only then escalate (still outside-in; introduce **Calva Backseat Driver** once if naming helps them find settings):

1. Toggle the Backseat Driver MCP server off → on in the editor’s MCP settings  
2. Command Palette: **Start the MCP socket server** / **Register MCP Server with Cursor** (Cursor) — or the equivalent Backseat Driver MCP register/start on other VS Code-family builds  
3. Fully quit and reopen the editor  

Still no alternate agent bridges.

## Clojure CLI → LSP unblock

Workspace ships `"calva.enableClojureLspOnStart": "never"` in `.vscode/settings.json`.

```
λ clojure_lsp_observe.
  clojure_on_PATH? → remove("calva.enableClojureLspOnStart" = "never")
  | ¬install(Java ∨ Clojure) as_Yardcraft_setup
```

## Babashka jack-in

**Calva: Start a Project REPL and Connect (Jack-in)** → Project Type **Babashka**. Confirm ember + green **`bb`**. Agent eval still needs the MCP gate above.

## Connect to Blender REPL (Calva)

1. **Calva: Connect to a running REPL server in the project**  
2. Sequence: **`basilisp-blender`** (not generic `basilisp` alone)  
3. Expect green **`basilisp-blender`**  
4. `user/init!` via connect sequence — re-run only after Blender restart / blown `sys.path`  

MCP gate must already be green before treating connect/demo as agent-driven success.
