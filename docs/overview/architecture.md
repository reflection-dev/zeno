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

## The core primitives

The core is a handful of small namespaces, split into an **orchestrator seam**
(the always-on side) and an **agent-body seam** (how a spawned agent is built
and run).

### `zeno.gateway` — the always-on process + MCP gateway

One long-lived HTTP server. The instance supplies `:roles`, a map of
role-keyword to a zero-arg fn returning that role's grant-spec, rebuilt per
request from current config/code so live edits take effect without a restart.
Each role is served at `/mcp/<role>`. The gateway holds the secrets and privilege;
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

### `zeno.loop` — the supervised loop and scheduler

A step loop is an ordered seq of `[label step-fn]`; each step takes the context
map and returns it. Steps are plain fns, redefinable live in the image. `tick`
runs one pass; a crashing step is isolated (logged, skipped) and the context
passes through, so one bad step never kills the process.

On top of that sits a background scheduler. `every` registers (or replaces) a
recurring process — a name, an interval, and a zero-arg fn — and `start!` runs a
daemon-thread scheduler that ticks the due processes on their intervals,
supervised, so a throwing process is logged and the others keep running. The
engine owns this loop and daemon: `zeno.main` calls `start!`, so an instance's
registered processes run in both interactive and `--daemon` modes. An `init.clj`
only calls `every` to declare what to run — it never spawns threads or writes its
own loop.

### `zeno.sandbox` — the agent body in a microsandbox

Launches an ephemeral body in an msb microVM: an immutable image, a persistent
named volume mounted at the working dir (the agent's durable home — session,
repo checkouts, scratch), the env the instance forwards, and a lifetime cap. The
body boots sub-second, does one run, and is destroyed; state survives in the
volume, not the body. `run` is the generic primitive (any argv, optional stdin);
`omp` wraps the coding agent (task piped in, headless, answer parsed from its
JSON stream). msb is the backend today; a Kubernetes-pod backend lands later.

### `zeno.oci` — an agent image from a Clojure spec

Turns a Clojure `{:packages :env :cmd :workdir}` spec into an OCI image, using
nix only to realise package store paths and their closure, then assembling the
docker-save archive directly and loading it into the sandbox. No nix expressions
and no dockerTools — the environment is data, nix is just the package source.

### `zeno.main` — the launcher

zeno is both a library and a runnable launcher. It is a **runtime** that loads
your **config** — the way a shell sources your rc or a runtime loads your
program. `nix run github:reflection-dev/zeno` loads a local config: zeno is the
runtime/core, the config at `~/.zeno` is a `deps.edn` project with an `init.clj`
(published like dotfiles as `<name>.zeno`), and machines are workflow packages
resolved on first run. The flake's `zeno`/`default` app composes the classpath at
launch — zeno core as a `:local/root` self **plus** the config project as a
`:local/root`, bringing its `:paths` and machine `:deps` onto one classpath —
then runs `zeno.main`. `zeno.main` resolves the config dir (`$ZENO_HOME`, else
`~/.zeno`), publishes it as the `zeno.home` system property so init code can find
its files regardless of the working directory, and `load-file`s `<home>/init.clj`,
which requires both zeno's namespaces and the config's own to wire the instance.
Config is read from local disk, so config edits take effect on the next run
without a push; only zeno-core changes need one.

zeno **always starts**. A missing config or an `init.clj` that throws is reported
and you still land in a working REPL, rather than the launcher refusing to boot.
The default mode is that interactive REPL, with an nREPL server started alongside
(its port written to `<home>/.nrepl-port`) so a client can connect while you
type. `--daemon` runs headless: no interactive REPL, just the nREPL server
staying up for an editor/client to connect to later. In both modes `zeno.main`
calls `zeno.loop/start!`, so the instance's registered processes run either way.

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

The core also owns *how* a spawned agent is isolated: `zeno.sandbox` runs each
body in an msb microVM (a Kubernetes-pod backend later) with a declared image,
egress allowlist, forwarded secrets and a lifetime cap. The body is disposable;
its durable state lives in a persistent per-agent volume. What the instance
supplies is the *profile* — the concrete constraints for a role — not the
mechanism (see ADR-0012, ADR-0014).
