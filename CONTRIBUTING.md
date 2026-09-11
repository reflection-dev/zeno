# Contributing

Thanks for looking. zeno is a small, opinionated core; the bar for what lands in
it is deliberately high (see [Non-goals](README.md#non-goals)).

## Dev setup

Requires [Nix](https://nixos.org/download.html) with flakes, or a local
[Clojure CLI](https://clojure.org/guides/install_clojure) + JDK 21+.

```
nix develop          # clojure + jdk + rlwrap on PATH
clojure -M:test      # run the deterministic test suite
clojure -M:nrepl     # a bare nREPL for live development
```

The tests are deterministic and offline: no network, no spawned agents. Keep
them that way — anything that shells out to an agent CLI or hits a socket does
not belong in the suite.

## What belongs in the core

zeno is a generic substrate. A change belongs here only if it stays true to the
model in [docs/decisions.md](docs/overview/decisions.md):

- **The core names no role.** No workflow, path, or application name in the
  source — including comments and examples. After a change, grep for
  application names and expect it clean.
- **Keep the trust split.** The orchestrator is trusted full Clojure; SCI is
  scoped to an agent's `eval` only. Do not sandbox the orchestrator, and do not
  widen an agent's reach beyond its injected grant.
- **Prefer data and plain redefinable fns** over configuration knobs and hooks.

Anything application-specific belongs in the instance, not here.

## Conventions

- Clojure, two-space indent; keep namespaces small and single-purpose.
- Every public fn carries a docstring describing its capability, not its
  implementation.
- New behaviour that changes the model gets an ADR in
  [docs/decisions.md](docs/overview/decisions.md), newest at the bottom.

## PRs

Keep them focused. Describe what changed and why, note any model/ADR impact, and
confirm `clojure -M:test` passes.
