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
- **`zeno.loop`** — a supervised step loop where one crashing step never kills
  the process.
- **`zeno.sandbox`** — the agent body in an msb microVM (Kubernetes pods later):
  an immutable image, a persistent per-agent volume, forwarded env, a lifetime cap.
- **`zeno.oci`** — an agent image from a Clojure spec, built via nix store paths
  without nix expressions or dockerTools.

Read on: [Architecture](architecture.md) walks through the model and the
boundaries; [Decisions](decisions.md) is the ADR log of why it is shaped this
way.
