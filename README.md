<div align="center">

# zeno

_An always-on orchestrator for ephemeral agents. Company processes in Lisp; every agent boxed behind its own MCP._

<a href="https://github.com/reflection-dev/zeno/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/reflection-dev/zeno/actions/workflows/ci.yml/badge.svg"></a>
<a href="https://clojure.org"><img alt="Clojure" src="https://img.shields.io/badge/Clojure-tools.deps-5881D8?logo=clojure&logoColor=white"></a>
<a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-7aa2f7"></a>

</div>

zeno is a small Clojure substrate for running one **long-lived orchestrator**
that spawns **short-lived agents**. The orchestrator holds the secrets and the
process logic; every process is described in Lisp. An agent is spawned for a
single task, reaches back only through a **per-agent MCP** whose sole tool is
code-mode `eval` against a capability grant the orchestrator authored, and dies
when the task ends.

> Design notes:
> [Zeno, an always-on orchestrator for ephemeral agents](https://andysmith.ai/2026/Sep/9/zeno-an-always-on-orchestrator-for-ephemeral-agents/)
> and
> [Long-lived agents vs ephemeral agents](https://andysmith.ai/2026/Sep/9/long-lived-agents-vs-ephemeral-agents/).

## Contents

- [Why](#why)
- [The core primitives](#the-core-primitives)
- [Run an instance](#run-an-instance)
- [Quick start](#quick-start)
- [The boundaries](#the-boundaries)
- [An instance, one loop, many roles](#an-instance-one-loop-many-roles)
- [Non-goals](#non-goals)
- [Status](#status)
- [Contributing](#contributing)
- [License](#license)

## Why

Long-lived agents that run the whole loop themselves have three problems: every
agent polls for its own events (polling eats resources at scale); every call has
the whole container's access, so a prompt injection reaches everything; and
communication and work share one image, so changing config for one session
restarts the agent and cuts off parallel sessions.

zeno pulls the orchestrator out into its own layer. One long-lived image handles
communication and process logic; the actual work runs in ephemeral agents, each
with a grant scoped to just that task. The process itself is deterministic;
where no deterministic practice exists yet, it calls an LLM or an agent.

## The core primitives

| ns | what it is |
| --- | --- |
| **`zeno.gateway`** | the always-on process + MCP gateway. Holds the secrets and privilege; serves each role at `/mcp/<role>`, rebuilt per request from current code/config so live edits take effect without a restart. Spawned agents see only the gateway URL. |
| **`zeno.grant`** | the capability boundary. A deny-by-default SCI context exposing *exactly* the vocabulary the orchestrator injects (`:vocab`) plus `(context)`/`(tools)`; an agent's `(eval ...)` can reach nothing else — no fs, no shell, no network, except through granted fns. |
| **`zeno.spawn`** | one ephemeral agent = one task. Launches an external coding-agent CLI pointed at exactly one role's MCP, waits, and tears it down. The session holds no secrets — only the gateway URL. |
| **`zeno.loop`** | a supervised step loop. Steps are plain `[label fn]` pairs, redefinable live; a crashing step is isolated (logged, skipped) so one bad step never kills the process. |
| **`zeno.sandbox`** | the agent body in a sandbox. Boots an ephemeral msb microVM with an immutable image, a persistent per-agent volume (session + repo checkouts + scratch), the env the instance forwards, and a lifetime cap; `run` is generic (any argv, optional stdin), `omp` wraps the coding agent (task piped in, answer parsed out). msb backend now, Kubernetes pods later. |
| **`zeno.oci`** | an agent image from a Clojure spec. Realises package store paths via nix (`nixpkgs#git`, `github:…#omp`) and their closure, then assembles the OCI archive directly — no nix expressions, no dockerTools; the environment is data, nix is just the package source. |
| **`zeno.main`** | the launcher. `nix run` composes the classpath (zeno core + your config as `:local/root`s), then `zeno.main` resolves the config dir (`$ZENO_HOME`, else `~/.zeno`), publishes it as the `zeno.home` system property, and `load-file`s `<home>/init.clj` — the way emacs loads `~/.emacs.d/init.el`. |

## Run an instance

zeno is both a library and a runnable launcher. Think of it the way Emacs is
laid out: **zeno** is the binary/core (the `emacs` executable), **`~/.zeno`** is
your config (`~/.emacs.d`), and **machines** are reusable workflow packages
(ELPA packages) resolved on first run.

```
nix run github:reflection-dev/zeno          # loads ~/.zeno
ZENO_HOME=~/work/acme.zeno \
  nix run github:reflection-dev/zeno        # load a config elsewhere
```

Your config is an ordinary `deps.edn` project with an `init.clj` entrypoint,
published as `<name>.zeno` dotfiles (e.g. `acme.zeno`). Its `:paths` hold the
instance's own namespaces and its `:deps` pull in the machines it uses; on first
run those resolve ELPA-style into `~/.m2`/`~/.gitlibs`. `init.clj` wires the
instance — provisions identities, delivers accesses, builds the machines, and
runs the loop.

The flake's `zeno`/`default` app composes the classpath at launch: zeno core as
a `:local/root` self **plus** the config project at `~/.zeno` as a
`:local/root`. That is why `init.clj` can `require` both zeno's namespaces and
the config's own — everything is on one classpath before `zeno.main` runs.
`zeno.main` then resolves the config dir (`$ZENO_HOME`, else `~/.zeno`), sets the
`zeno.home` system property so init code can find its files regardless of the
working directory, and `load-file`s `<home>/init.clj`.

Config lives on local disk and is read from there, so editing `init.clj` or a
machine and re-running takes effect immediately — no push needed. Only changes to
zeno core itself need a push (or a pinned git dep).

## Quick start

zeno is also a library you can embed directly. Add it to a project's `deps.edn`:

```clojure
{:deps {io.github.reflection-dev/zeno {:local/root "../zeno"}}}
;; or a git dependency:
;; {:deps {io.github.reflection-dev/zeno {:git/url "https://github.com/reflection-dev/zeno"
;;                                        :git/sha "..."}}}
```

A minimal orchestrator: author a role's grant, start the gateway, spawn an agent
against it, and drive a supervised loop.

```clojure
(require '[zeno.gateway :as gateway]
         '[zeno.grant :as grant]
         '[zeno.spawn :as spawn]
         '[zeno.loop  :as zloop])

;; A role is a grant-spec: the vocabulary an agent of that role may call.
(defn worker-grant []
  {:vocab    {"read-file" (fn [p] (slurp p))
              "note"      (fn [m] (println "note:" m))}
   :docs     {"read-file" "(read-file path) - read a file"
              "note"      "(note msg) - record a note"}
   :ctx-info {:role "worker"}})

;; Start the always-on gateway; each role is served at /mcp/<role>.
(def img (gateway/start! {:port 7777 :roles {:worker worker-grant}}))

;; Spawn an ephemeral agent whose ONLY tool is (eval ...) against that grant.
(spawn/spawn {:gateway-url (:gateway-url img)
              :role        :worker
              :model       "your/model-id"
              :system      "You are a worker."
              :prompt      "Read README.md and note its first heading."})

;; Or drive a never-dying, supervised process loop.
(zloop/tick [["heartbeat" (fn [ctx] (update ctx :beats (fnil inc 0)))]] {})
```

## The boundaries

Two places run code, with opposite trust:

- **The orchestrator** is trusted full Clojure on the JVM — the operator wrote
  it. It `require`s libraries directly, uses real threads, and redefines its own
  functions live. It is not sandboxed, and it is not an LLM, so it cannot be
  talked into leaking.
- **An agent's `eval`** runs in SCI, a fresh context per spawn whose visible
  vocabulary *is* the capability grant. Untrusted agent-authored code cannot
  reach anything that was not injected.

Grants are **per-spawn and authored by the orchestrator** — not necessarily
minimal: a cheap classifier might get read-only error access; a coding agent
gets real read/write. The invariant is that the grant is chosen per task, and
each agent reaches it through its own MCP path.

See [docs/architecture.md](docs/overview/architecture.md) and
[docs/decisions.md](docs/overview/decisions.md) for the full model and the decision log.

## An instance, one loop, many roles

An instance is one process, one live image. It may serve **many roles** on one
gateway and orchestrate a whole unit — a team, a department, a company — with
the process described in Lisp. Scaling out means running **more instances** at
the boundaries where a unit is genuinely autonomous or lives in a different
trust domain, not running more loops inside one process.

## Non-goals

- **Not a VM/pod fabric.** The core *does* launch each agent in a sandbox
  (`zeno.sandbox` — an msb microVM today, Kubernetes pods later) with declared
  constraints, but it does not run the cluster or the hypervisor: the instance
  supplies the profile (image, egress, secrets, limits) and the runtime hosts it.
- **Not an application.** Reply routing, knowledge bases, feeds, publishing —
  the specific work — live in the instance (for example
  [meno](https://github.com/sm-th/meno), an auto-researcher), never in the core.
  The core names no role.
- **Not a multi-loop scheduler.** One instance is one process; coordination
  between instances lives a layer above the core.
- **No credential vault (yet).** Secrets come from the instance's config by
  reference; a vault can land when a second consumer needs one.

## Status

Pre-1.0, extracted from and battle-tested by [meno](https://github.com/sm-th/meno).
Working code is the source of truth; pin a revision.

## Contributing

Issues and PRs welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for the dev setup
and conventions. Be kind ([Code of Conduct](CODE_OF_CONDUCT.md)); report
vulnerabilities privately per [SECURITY.md](SECURITY.md).

## License

[MIT](LICENSE) (c) 2026 Andy Smith.
