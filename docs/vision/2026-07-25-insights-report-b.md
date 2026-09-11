# Zeno Extract from Claude Code /insights Report (B)

- Source session: project "insight", session id 0cfb0aa8-d024-41ef-a47d-9f697eeb0c08
- Session date: 2026-07-25
- Note: Zeno-related material extracted verbatim-in-meaning from a /insights usage report; Russian fragments translated to English. Raw archival.

---

## Project Area: Zeno Lisp Inference Core & Architecture

- Session count: 5
- Description: User reset and rebuilt the zeno project around an Emacs-style architecture, extracting a clean Lisp inference core into a publishable repo with a working Nix flake and docs. Claude built runnable skeletons, ran live demos via eval, and extracted session content into typed atom files, but repeatedly needed correction over premature terminology and over-detailed public documentation.

---

## Zeno-adjacent context: Local LLM Infrastructure & Benchmarking

(Included because it directly informs the zeno code-mode model choice.)

- Session count: 6
- Description: User migrated between local inference stacks (Ollama, Rapid-MLX) and built portable Nix devenv flakes for running models like gemma3 and devstral. Claude ran benchmarks, measured tok/s and thinking-mode differences, and helped converge on a unified code-mode model, though a probe once crashed the user's own chat service and README language/nixpkgs-pin choices caused friction.

---

## Narrative mentions of zeno

### From the interaction-style narrative

- User works at the frontier of exploratory systems-building, tackling ambitious infrastructure and tooling projects: local LLM devenvs, Apple virtualization VMs, Discourse components, session-decomposition pipelines like "zeno" and "banjo".
- The user leans on Claude as a genuine collaborator across long sessions (134 hours over 26 sessions). Heavy Bash/Edit/Write usage and use of custom MCP tools (zeno eval) and Agents show the user is building and running real systems, not just asking questions.
- When Claude's proposals were unclear the user asked for simpler re-explanations ("I didn't understand what that does") rather than abandoning the thread.

### Zeno-specific friction

- On the zeno privacy concern, Claude misunderstood the user's "leak" worry twice (thinking it was tool names, then topic choice) before finally checking logs.
- Multiple sessions required corrective resets after Claude injected unapproved vocabulary (e.g. "role", "Emacs-style", "rebuild") and over-abstracted the user's requests; user pushed back to keep internal discussion details out of public docs and to keep public documentation from being over-detailed.

---

## "What works" - Impressive workflow: Architecting the zeno inference core

Description: The user drove a multi-session effort to reset a project on a clean branch, converge on an Emacs-style architecture, and extract a Lisp inference core into a publishable ~/zeno repo. This was paired with runnable skeletons, working nix flakes, and typed atom extraction, keeping Claude focused on shipping real, committed artifacts rather than abstract plans.

Intro line: Over 26 sessions spanning a month, the user has used Claude Code as a serious infrastructure and R&D partner, building the zeno inference core, local-LLM tooling, and content pipelines mined from their own session data.

### Adjacent impressive workflow: Local-LLM benchmarking and migration (informs zeno code-mode)

The user systematically evaluated local models on Ollama and MLX, stress-testing gemma3:12b and gemma4, benchmarking tok/s and thinking-mode differences, and converging on devstral for a unified code-mode role. The user turned Claude into a rigorous evaluator, using measured cohesion/purity and live demos to make grounded infrastructure decisions.

---

## "At a Glance" mentions of zeno

- What's working: The user works in an ambitious, systems-oriented way, driving multi-session efforts like architecting the zeno inference core, benchmarking local LLMs, and building pipelines that mine their own session data for content. Their real strength is keeping Claude anchored to shipping concrete, committed artifacts (runnable skeletons, working flakes, measured benchmarks) rather than abstract plans, and consistently redirecting it back to real evidence when it drifts.

---

## "On the Horizon" - zeno-relevant opportunities

### Parallel Local-Model Benchmark Swarm (uses zeno eval; feeds code-mode model choice)

- What's possible: The user's sessions repeatedly evaluate local LLMs one at a time (gemma3:12b, devstral, MLX vs Ollama). Instead, dispatch parallel agents that each stress-test a different model/config and report cohesion, tok/s, and thinking-mode metrics into one comparison table. Critically, sandbox each probe to its own port and process group so an agent never kills the running chat service again, turning a frustrating serial grind into a hands-off benchmark report.
- How to try: Launch multiple Agent instances, each isolated to a dedicated port and given explicit "do not touch ports/processes you didn't start" constraints, then aggregate results with mcp__zeno__eval.
- Copyable prompt: "Benchmark these local models [list] for my code-mode use case. Spawn one subagent per model running in parallel. Rules: each agent must use its own unique port, never bind port 8000, and never kill processes it didn't start - check for existing listeners first and refuse to conflict. Each agent measures tok/s, thinking-mode behavior, and answer quality on the same 5 prompts. Aggregate everything into a single ranked comparison table and recommend the best model with reasons."

---

## Suggested post/theme ideas about zeno

(From the assistant's follow-up in this session: the assistant read 30 real facet files and produced themes grounded only in what was actually built. For each theme it gave a Twitter angle - technical, sharp - and a Threads angle - story/broader. Russian translated to English below.)

### zeno - Lisp inference core

**Theme 4. Emacs-style architecture for an inference core** (reset zeno on a new main)
- Twitter: "A core you can redefine like in Emacs: overridable loop + per-spawn MCP."
- Threads: Why I reset the project from scratch and rethought the architecture instead of patching it.

**Theme 5. A live Lisp image in which the agent performs a real task via eval** (live demo)
- Twitter: Short video/log: I give a task - the image solves it live.
- Threads: "Inference not as a pipeline, but as a live image" - what that changes in the work.

### Zeno-adjacent theme group: Local LLM on your own hardware (informs code-mode model choice)

**Theme 1. Ollama -> MLX: the migration and what actually changed in tok/s** (rapid-mlx/gemma sessions)
- Twitter: Thread with numbers: gemma on Ollama vs MLX vs rapid-mlx on Apple Silicon, tok/s + the difference in thinking-mode.
- Threads: "Ran local models for half a year - here's what I understood about when local really replaces cloud."

**Theme 2. A portable devenv flake for local LLMs with a single `nix develop`** (devenv flake stack)
- Twitter: Mini-guide: a repo flake that brings up chat + embeddings as two processes, reproducibly.
- Threads: "I want to hand a friend my LLM environment with one command - why Nix, not Docker."

**Theme 3. How I picked one model for a code-mode agent** (converged on devstral)
- Twitter: "Ran gemma3:12b, devstral and others on the same prompts - devstral won, here's why."
- Threads: The story of how I stopped collecting models and picked one that works.

### Zeno-adjacent theme group: banjo / analyzing my own sessions (session-decomposition, feeds zeno atoms)

**Theme 6. Decomposing long sessions into meaning-based "cases"** (cases decomposer + atoms)
- Twitter: "I cut my Claude Code sessions into meaning threads, not by time - into an Obsidian vault."
- Threads: "My history of working with AI as a knowledge base": how I extract durable atoms from logs.

**Theme 7. Content straight from my own work logs** (blog/patent mining from sessions)
- Twitter: "I mine post ideas from my own assistant sessions, not from my head."
- Threads: About how the work process itself becomes the source of topics.

(Note: themes 8-12 in the same list - Forgejo in an Apple container over Tailscale, Linux VM on Mac via Apple Virtualization vz + Nix, a no-backend imageboard for howm notes, a Discourse auto-timestamp component, and an essay pre-publication checking pipeline - are self-hosted-infra / meta-process items not specific to zeno, and are omitted here.)

### Assistant follow-up options offered

- Expand any numbered theme into a ready-to-post piece (with numbers from benchmarks - would pull the real logs).
- Rank by "will land / easy to write."
- Assemble into a content calendar.

---

## Suggestions / conventions relevant to zeno (docs discipline)

From the report's suggested CLAUDE.md additions, the item most directly tied to zeno docs friction:

- "Do NOT introduce new terminology, framing, or architectural labels (e.g. 'role', 'Emacs-style', 'rebuild') into docs, commits, or public writing unless the user explicitly used or approved them. Keep internal discussion details out of public docs."
  - Why: Multiple sessions required corrective resets after Claude injected unapproved vocabulary and over-abstracted the user's requests.

---

## Source references

- Report URL: file:///Users/user/.claude/usage-data/report-2026-07-25-164509.html
- HTML file: /Users/user/.claude/usage-data/report-2026-07-25-164509.html
- Facets directory: /Users/user/.claude/usage-data/facets
- Overall report window: 207 sessions total, 26 analyzed, 432 messages, 134h, 11 commits, 2026-06-25 to 2026-07-23
