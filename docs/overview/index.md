---
title: Introduction
---

# zeno

zeno is a small Clojure substrate for running one **long-lived orchestrator**
that spawns **short-lived agents**. The orchestrator holds the secrets and the
process logic; every process is described in Lisp. An agent is spawned for a
single task, reaches back only through a **per-agent MCP** whose sole tool is
code-mode `eval` against a capability grant the orchestrator authored, and dies
when the task ends.

It exists because long-lived agents that run the whole loop themselves each poll
for their own events, carry the whole container's access into every call (a
prompt injection reaches everything), and fuse communication with work so a
config change restarts them and cuts off parallel sessions. zeno pulls the
orchestrator into its own layer and boxes each unit of work in a scoped,
ephemeral agent.

## The core primitives

- **`zeno.gateway`** — the always-on process and MCP gateway; holds the secrets,
  serves each role at `/mcp/<role>`.
- **`zeno.grant`** — the capability boundary; a deny-by-default SCI context
  exposing exactly the vocabulary the orchestrator injects.
- **`zeno.spawn`** — one ephemeral agent per task, pointed at a single role's
  MCP, torn down on exit.
- **`zeno.loop`** — the supervised loop and scheduler; a step loop where one
  crashing step never kills the process, plus `every`/`start!` that run a
  background daemon-thread scheduler for recurring processes.
- **`zeno.sandbox`** — the agent body in an msb microVM (Kubernetes pods later):
  an immutable image, a persistent per-agent volume, forwarded env, a lifetime cap.
- **`zeno.oci`** — an agent image from a Clojure spec, built via nix store paths
  without nix expressions or dockerTools.
- **`zeno.main`** — the launcher; composes the classpath and loads the config's
  `init.clj` at `$ZENO_HOME` (default `~/.zeno`), always coming up in a REPL even
  when the config is missing or throws.

## Running a config

zeno is both a library and a runnable launcher. `nix run
github:reflection-dev/zeno` loads a local config at `~/.zeno` (override with
`ZENO_HOME`).

zeno is a **runtime** and your config is the **program** it loads and runs — the
way a shell sources your rc or a runtime loads your program. zeno is the
runtime/core, `$ZENO_HOME` (default `~/.zeno`) is the config (a `deps.edn`
project with an `init.clj`, published like dotfiles as `<name>.zeno`), and
machines are workflow packages resolved on first run. The flake app composes the
classpath at launch — zeno core plus the config project, both as `:local/root` —
then `zeno.main` sets the `zeno.home` system property and `load-file`s
`init.clj`, which wires the instance. Config is read from local disk, so config
edits take effect on the next run without a push.

zeno always starts: a missing or throwing config is reported and you still land
in a working REPL. The default mode is that interactive REPL (with an nREPL
alongside); `--daemon` runs headless with only the nREPL server up to connect to.
The engine, not the instance, owns the loop and the daemon — `init.clj` calls
`zeno.loop/every` to register recurring processes and `zeno.main` runs the
scheduler that ticks them, in both modes.


Read on: [Architecture](architecture.md) walks through the model and the
boundaries; [Decisions](decisions.md) is the ADR log of why it is shaped this
way.
