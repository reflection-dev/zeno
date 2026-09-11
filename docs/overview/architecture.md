---
title: Architecture
---

# Architecture

zeno separates a **long-lived orchestrator** from **short-lived agents**. One
instance is a single process that runs the orchestrator: it holds the secrets
and the process logic, talks to the outside world, and spawns an ephemeral agent
whenever a piece of the process needs reasoning that no deterministic practice
covers yet. The process itself is written in Lisp; the agent is boxed behind its
own MCP and dies when its task is done.

The core is a generic substrate — it knows *how* to hold a gateway, grant a
capability, spawn an agent, and supervise a loop. Everything specific (what the
process does, which roles exist, what each may touch) lives in the **instance**
and is supplied as data and code.

> The design notes in the repo's [`docs/vision/`](https://github.com/reflection-dev/zeno/tree/master/docs/vision) are the earlier, broader sketches
> this core was distilled from (including a wider set of speculative host
> primitives). The shipped core is deliberately leaner than those notes; where
> they disagree, this document and the running code win.

## The four primitives

The whole core is four small namespaces.

### `zeno.image` — the always-on process + MCP gateway

One long-lived HTTP server. The instance supplies `:roles`, a map of
role-keyword to a zero-arg fn returning that role's grant-spec, rebuilt per
request from current config/code so live edits take effect without a restart.
Each role is served at `/mcp/<role>`. The image holds the secrets and privilege;
a spawned agent sees only the gateway URL.

### `zeno.grant` — the capability boundary

Builds a deny-by-default SCI context exposing exactly the vocabulary the
instance injects: `:vocab` (name → fn), `:docs` (name → one-liner shown by
`(tools)`), and `:ctx-info` (returned by `(context)`). An agent's code can call
the granted verbs plus `(context)`/`(tools)` and nothing else — no fs, no shell,
no network, except through a granted fn. Adding a capability is a code edit to
the vocab, not prompt engineering.

### `zeno.spawn` — one ephemeral agent = one task

Launches an external coding-agent CLI in print mode, pointed at exactly one
role's MCP endpoint, waits for it, and returns `{:exit :out :err}`. The session
holds no secrets — only the gateway URL — and is gone when the task ends. An
optional advisor overlay attaches a reviewer as a quality gate without mutating
the operator's global agent config.

### `zeno.loop` — the supervised step loop

A loop is an ordered seq of `[label step-fn]`; each step takes the context map
and returns it. Steps are plain fns, redefinable live in the image. A crashing
step is isolated (logged, skipped) and the context passes through, so one bad
step never kills the process.

## Two evaluators, opposite trust

- **The orchestrator** runs as full Clojure on the JVM. It is trusted — the
  operator wrote it — so it `require`s libraries directly, uses real threads,
  and redefines its own functions live. It is not sandboxed, and it is not an
  LLM, so it cannot be talked into misusing what it holds.
- **An agent's `eval`** runs in SCI, a fresh context per spawn whose visible
  vocabulary is exactly its grant. Untrusted agent-authored code cannot reach
  anything that was not injected.

SCI is therefore not the substrate of the system — it is scoped to one job:
bounding an agent's code-mode eval.

## The agent boundary is a per-role MCP

When the orchestrator spawns an agent, it hands that agent a single MCP endpoint
over HTTP (`/mcp/<role>`) as its only channel back in. The direction is inverted
from a normal editor session: zeno is the orchestrator that *serves* a curated
tool to the agents it commands. The central (and, today, only) tool is
code-mode `eval`, run against that role's SCI grant. HTTP, not stdio, because the
tool must execute inside the orchestrator's live process against its state.

## Per-spawn grants, not always minimal

The instance authors each role's grant. A grant is whatever that agent needs: a
cheap classifier might get narrow read-only access; a coding agent gets real
read/write. The invariant is that the grant is per-role and chosen by the
orchestrator, not that it is minimal. A spawned coding CLI also has built-in
tools that bypass the MCP, so a grant is only as tight as its tool controls.

## One instance, many roles, one process

An instance is one process and one live image. It may serve many roles on one
gateway and orchestrate a whole unit — a team, a department, a company — with the
process described in Lisp. Scaling out means running more instances at the
boundaries where a unit is genuinely autonomous or lives in a different trust
domain, not running more loops inside one process. Coordination between
instances lives a layer above the core.

## Communication is a separate seam

Work and communication are meant to be separated: an agent calls something like
`(ask-user "question")`, is interrupted, and resumes when the answer arrives —
regardless of whether the channel is chat, GitHub issues, or email. The
mechanism (interrupt/resume + a channel interface) is a core concern; the
concrete channel is instance config. This seam is designated but not yet
implemented — it is written when a second consumer needs it, not retrofitted
speculatively.

## State and sandboxing

Long-lived work keeps its state of record **outside** the volatile image; the
image is a working context, not the source of truth, and the loop is idempotent
so a restart resumes rather than repeats.

Isolation of a spawned agent — its own microVM, a Kubernetes pod, or simply
running next to the orchestrator — is the **instance's** concern, layered on
depending on scale. The core does not mandate a sandbox.
