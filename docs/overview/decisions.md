---
title: Decisions
---

# Decision log

Chronological, ADR-style. Newest at the bottom. Each entry states the context,
the decision, why, and consequences. Several are inherited from the earlier
design notes in the repo's [`docs/vision/`](https://github.com/reflection-dev/zeno/tree/master/docs/vision) and revised to match the shipped core.

---

## ADR-0001 — One instance is one process; many roles, not many loops

**Context.** A process could run one loop, or many cooperating loops behind an
in-process scheduler; and an instance could be pinned to a single role or
orchestrate a whole unit.

**Decision.** A zeno instance is one process and one live image. It may serve
**many roles** on one gateway and orchestrate a whole unit — a team, a
department, a company — with the process described in Lisp. Scaling out means
running **more instances** at the boundaries where a unit is genuinely
autonomous or in a different trust domain; coordination between instances lives
above the core.

**Why.** The core stays small — a supervisor around a loop plus a gateway. Many
roles on one image is already how the reference instance runs (a `:roles` map),
and it keeps a unit's process readable as data (one role, one namespace) rather
than sprawled across processes. The privacy/authority boundary moves to the
per-spawn grant (ADR-0007), which is enforced in one auditable place rather than
distributed across process lines.

**Consequences.** No in-core scheduler or routing bus. A unit is split into its
own instance when operational coupling (shared image fate, one nREPL, one
supervisor) outgrows the readability win — the same signal as "is this unit
autonomous?". This supersedes the stricter "one instance = one loop = one role"
of the vision notes.

---

## ADR-0002 — The core names no role

**Decision.** The core source contains zero references to any role, workflow, or
application path — including comments and examples. It learns everything
specific from the instance (the `:roles` it is handed, the steps it is given).

**Why.** The value is a reusable substrate. If the core knows a specific process
by name, it is no longer generic.

**Consequences.** Instances reach the core only through generic seams: the
gateway, the grant, the spawn, the loop. After any core edit, grep the source
for application names and expect it clean.

---

## ADR-0003 — Full Clojure for the orchestrator; SCI only for an agent's eval

**Context.** Running all authored code in a sandboxed interpreter assumes it is
untrusted. That is costly (single-threaded SCI, no direct `require`) and the
orchestrator is operator-written and trusted.

**Decision.** Split by trust. The orchestrator runs as full Clojure on the JVM.
SCI is retained only to bound an agent's code-mode `eval` (ADR-0006).

**Why.** A sandbox protects against untrusted code; the orchestrator is not
untrusted. Sandboxing it buys nothing and costs parallelism and direct library
access.

**Consequences.** The orchestrator requires libraries directly and uses real
threads. SCI survives, scoped narrowly to the agent boundary.

---

## ADR-0004 — Durable state is external; the image is disposable

**Decision.** The state of record lives outside the volatile image; the image is
a working context. The loop keys its state by stable ids and is idempotent.

**Why.** Decouples liveness from durability: the instance can restart and resume
rather than lose or repeat work.

**Consequences.** Loop and role code must be idempotent. The core does not ship a
store; where state lives (git, a database) is the instance's choice.

---

## ADR-0005 — The agent boundary is a per-role MCP over HTTP

**Decision.** Each role is served a dedicated MCP endpoint over HTTP at
`/mcp/<role>`; that endpoint is a spawned agent's only channel back into zeno.
The agent CLI is launched pointed at exactly that endpoint.

**Why.** MCP is the protocol these agent CLIs already speak, so "what the agent
may do" is expressed directly as its MCP surface. HTTP (not stdio) because the
tool must run inside the orchestrator's live process against its state; a stdio
server would be a child of the agent, unable to reach the host.

**Consequences.** The core runs an HTTP server with per-role routing, rebuilt per
request from current code/config.

---

## ADR-0006 — The agent's central tool is code-mode eval in a per-spawn SCI grant

**Decision.** The tool behind the MCP is code-mode `eval`: the agent writes
Clojure, evaluated in a fresh SCI context seeded with that role's grant. Handlers
run in the orchestrator with full power, but only injected capabilities are
reachable.

**Why.** Code-mode lets the agent compose capabilities rather than being limited
to prewired verbs, with injection as the sandbox.

**Consequences.** Each spawn gets its own SCI context; since a context is never
shared across threads, SCI not being thread-safe is a non-issue even with
parallel spawns.

---

## ADR-0007 — Per-spawn capability grants, authored by the orchestrator, not always minimal

**Decision.** The instance authors each role's grant — narrow for an exploratory
task, wide for a trusted coding task. The invariant is that the grant is
per-role and chosen by the orchestrator, not that it is minimal.

**Consequences / caveat.** A spawned coding CLI has built-in tools (`Bash`,
`Read`, `Write`) that bypass the MCP. A grant is only as tight as its tool
controls: disable the built-ins you did not intend to give; enable them
deliberately to let an agent write code.

---

## ADR-0008 — Communication is a core seam, separated from work (designated)

**Context.** An agent sometimes needs to ask a human, and the channel (chat,
GitHub issues, email) varies by instance.

**Decision.** Communication is a core concern expressed as a seam — an agent
calls something like `(ask-user ...)`, is interrupted, and resumes when the
answer arrives — while the concrete channel is instance config. This cleanly
separates communication from the work.

**Status.** Designated, **not yet implemented**. It is written when a second
consumer needs it, not retrofitted from a single instance's guesses.

---

## ADR-0009 — Sandboxing is the instance's concern; not mandated by the core (open)

**Context.** A spawned agent may run in its own environment or right next to the
orchestrator, depending on scale. The reference instance itself already runs
inside a microVM, which makes nesting another microVM per spawn unclear.

**Decision.** The core does not mandate or manage a sandbox. Isolation
(microVM, Kubernetes pod, or none) is layered on by the instance.

**Status.** Open. The likely path is running the orchestrator on Kubernetes so
it can spawn agents as pods with scoped permissions; deferred to avoid
over-complicating the current setup.

---

## ADR-0010 — The agent-CLI backend is currently `omp`, hardcoded (to generalise later)

**Context.** `zeno.spawn` launches a concrete agent CLI (`omp`) with concrete
flags and an advisor overlay.

**Decision.** Keep the single hardcoded backend for now rather than inventing a
pluggable backend abstraction for one consumer.

**Why.** It works and is battle-tested; an abstraction designed against a single
caller is a guess.

**Status.** Known concreteness. Generalise the backend when a second consumer
needs a different CLI.

---

## ADR-0011 — Shipped code is the source of truth over design notes

**Decision.** Where the earlier design notes (`vision/`) and the running,
battle-tested code disagree, the code wins and the docs are updated to match.

**Why.** The core was distilled by extracting what actually earned its place in a
working instance, not by implementing a speculative design. Notes that never
shipped (a wider host-primitive set, a fixed plan/act/observe template loop) are
history, not spec.

---

## ADR-0012 — Sandbox orchestration is a core capability (supersedes ADR-0009's "deferred")

**Context.** ADR-0009 left agent isolation to the instance and deferred it. In
practice every instance needs to run an agent in a sandbox with declared
constraints (image, egress, secrets, lifetime), so leaving it out pushed the
same plumbing into each instance.

**Decision.** The core owns *how* to launch an agent body in a sandbox.
`zeno.sandbox` runs an ephemeral body in a microsandbox (msb) microVM today; a
Kubernetes-pod backend lands later. Core is the launch mechanism and the
constraint vocabulary (image, volume, env, egress, network-bound secrets,
resource limits, TTL); the instance supplies the *profile* — the concrete
values for a role.

**Why.** "Run an agent in a described box" is universal, and the constraint spec
is a portable contract while the backend (msb → k8s) evolves under it.

**Consequences.** The core carries an msb dependency at that seam; the spec is
one auditable place. The core still names no role (ADR-0002): a profile is data
the instance hands in. This supersedes ADR-0009's "deferred, not mandated".

---

## ADR-0013 — An agent's environment is described in Clojure and built without dockerTools

**Decision.** An agent image is a Clojure spec — `{:packages :env :cmd :workdir}`
— not a hand-written nix file. `zeno.oci` uses nix only to realise package store
paths (`nixpkgs#git`, `github:…#omp`) and their closure, then assembles the OCI
(docker-save) archive directly and loads it into the sandbox runtime. No nix is
generated from strings and none is authored per image.

**Why.** No mature Clojure→Nix DSL exists, and string-generating nix is fragile.
Describing the environment as data keeps it alongside the rest of the
orchestration; nix stays an implementation detail (a package source), and image
correctness (store closure + symlink farm + archive format) is small and owned.

**Consequences.** Building a linux image on macOS needs a linux builder for
packages the caches don't serve; cached packages substitute with none. A package
is a nixpkgs attr name or a full flake installable.

---

## ADR-0014 — The agent body is disposable; durable state lives in a per-agent volume

**Decision.** The sandbox body is ephemeral — it boots (sub-second), does one
run, and is destroyed. Anything that must survive lives in a named, persistent
volume mounted at the working dir: the agent's session, its repo checkouts and
branch work, its scratch. The image is immutable infra; the volume is the
mutable per-agent home. The instance decides the volume key (per agent, per
topic — its choice).

**Why.** Disposable bodies keep isolation cheap and idempotent (ADR-0004); a
persistent home lets sessions resume and repos persist across bodies without
keeping a VM warm. The volume *is* the diff over the immutable image.

**Consequences.** One live body per volume at a time (a conversation is
sequential); parallelism is a second agent/volume. The volume is a host dir now,
a PVC (or object-store-backed) later.

---

## ADR-0015 — Secrets are resolved by the instance and forwarded as named env

**Decision.** The core launcher forwards to a sandbox only the env it is handed.
An agent declares the secrets it needs *by name*; the instance resolves the
values from its own store (e.g. secretspec) and passes them as env. An agent
gets only its declared secrets — an orchestrator-held token it did not name never
enters its box.

**Why.** Selective forwarding is the capability boundary for secrets: a
prompt-injected body cannot exfiltrate what was never put in it. Resolution is
instance policy (which store); forwarding is the core mechanism.

**Consequences.** Values pass as plain env today; network-bound secrets (msb
`--secret-conf`, delivered only toward allowed hosts, never on VM disk) are the
hardening step, using the same `{:env :hosts}` declaration.
