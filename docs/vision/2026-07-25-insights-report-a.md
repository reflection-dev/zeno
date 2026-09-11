# Zeno material extracted from Claude Code /insights report

- Source session: project "insight", session id 40fb84b4-8ded-482b-8900-43f1d76c2d52
- Session date: 2026-07-25
- Report window analyzed: 2026-06-25 to 2026-07-23 (26 sessions analyzed, 134h, 432 messages, 11 commits)
- Note: The source contains TWO consecutive /insights runs on the same day; both name a distinct "Zeno" project area. All zeno-relevant content from both runs is preserved below, plus zeno-adjacent local-LLM / code-mode context where it directly informs zeno.

---

## Zeno project area (Run 1: report-2026-07-25-163048)

Area name: "Zeno Lisp Inference Core & Architecture"
Session count: 6

Description:
User worked with Claude to name, extract, and architect a Lisp-based inference core ("zeno") into a clean publishable repo. Claude helped reset the project on a fresh main branch, converge on an Emacs-style architecture, build a runnable skeleton with a nix flake, and run live demos. Friction arose from Claude introducing unapproved terminology and over-detailed public documentation, requiring repeated corrections.

---

## Zeno project area (Run 2: report-2026-07-25-164250)

Area name: "Zeno Lisp Inference Core & Session Decomposition"
Session count: 6

Description:
Claude architected and built the "zeno" project, resetting it to an Emacs-style skeleton with a nix flake and docs, then extracting the Lisp inference core into a clean publishable repo. Additional work included prototyping a meaning-based session "cases" decomposer, running clustering experiments to validate cohesion/purity, and extracting session content into typed atom files.

---

## Narrative: what was built with zeno

From "Impressive workflows" (Run 1) - "Extracting and publishing a clean core":
User took the Lisp inference engine, named it "zeno", and extracted it into a standalone, publication-ready repo with a working nix flake, skeleton architecture, and documentation. User iterated through naming and terminology disagreements but consistently pushed toward a clean, shippable artifact rather than settling for a working-but-messy state.

From "Impressive workflows" (Run 2) - "Building and publishing the zeno core":
User architected a Lisp inference engine from a fresh main branch, converged on an Emacs-style architecture with a working nix flake and skeleton, then named and extracted it into a clean publishable repo. User reset scope decisively when documentation drifted into over-detail, keeping the final artifact lean and shippable.

From interaction-style narratives:
- User operates as a systems-oriented builder on ambitious, research-flavored infrastructure: LLM tooling, Nix devenvs, custom inference cores ("zeno"), clustering pipelines, and personal-brand/content systems mined from the user's own Claude Code sessions.
- User works at the frontier of their own tooling - building local-LLM devenvs, Lisp inference cores (zeno), session decomposers, and reproducible Nix/Apple-virtualization setups over Tailscale. Sessions are long and exploratory (134 hours across 26 sessions); the user treats Claude as a collaborator on genuinely novel infrastructure rather than boilerplate.
- User kicks off with a rough direction (example given: "run zeno in parallel") and steers through successive corrections as bugs surface.

From at-a-glance summaries:
- User works like an engineer who ships clean artifacts, not just working prototypes - naming and extracting the Lisp inference core into a publishable repo with a nix flake.
- User works like a systems builder - standing up real infrastructure and architecting shippable projects like the zeno inference core; strongest sessions are evidence-driven; user decisively resets scope when work drifts into over-detail.

---

## Zeno-related friction and corrections (both runs)

- On the zeno architecture reset, Claude introduced unapproved terminology ("role", "Emacs-style", "rebuild") and fabricated a GitHub org while reversing the user's commit-style instruction, requiring repeated corrective resets.
- In the zeno reset, Claude injected unapproved terms ("role", "Emacs-style", "rebuild") and internal discussion into public docs; elsewhere Claude fabricated a GitHub org and reversed the user's commit-style instruction.
- User cares deeply about correctness and provenance - noticed a fabricated GitHub org, a reversed commit-style instruction, and Russian-language READMEs in what should be distributable repos.
- Zeno code reviews benefited from full-pipeline reads; delegating exploration to a focused subagent keeps the main thread focused and reduces output-token blowups.
- Related derived CLAUDE.md guidance: never introduce new terminology, names, org paths, or architecture labels (e.g. "role", "Emacs-style") into docs/commits without user approval; keep internal discussion out of public docs and preserve existing commit-style conventions. Write repo-facing docs in English by default.

---

## Zeno-adjacent context (local-LLM / code-mode stack)

Included only where it informs zeno; the user's inference-core work sits atop this stack.

- Local LLM Infrastructure & Benchmarking (Run 1: 5 sessions; Run 2: 6 sessions): migrated from Ollama to Rapid-MLX with dual-process chat and embeddings; evaluated models including gemma3:12b and devstral; built portable devenv nix flakes with documentation, ran stress tests, and refined a visualizer for a demo video. Converged on devstral for a unified "code-mode" / "code role" model. One probe accidentally started a server on port 8000 that crashed the user's own running chat service.
- Session decomposition / content-mining pipeline (grouped WITH zeno in Run 2): prototyped a meaning-based session "cases" decomposer, ran clustering experiments measuring cohesion/purity, and extracted session content into typed atom files.
- Infra was made reachable over Tailscale (Forgejo in an Apple container, Debian VM via Apple Virtualization framework with Nix); the mcp__zeno__eval MCP tool is used heavily.

---

## Post / theme ideas relevant to zeno

Note: The source /insights runs did NOT contain a discrete "suggested post topics" list. The user's slash-command arg for Run 1 requested (translated from Russian): "give me a list of topics for posts on threads and twitter based on what we built together." The insights engine answered with project areas and "on the horizon" opportunities rather than an explicit post list. The zeno-relevant post/theme seeds implied by the report are captured below.

Post/theme seeds (zeno and directly adjacent):
1. Naming and extracting a Lisp inference core ("zeno") from a working engine into a clean, publishable repo with a nix flake and skeleton architecture - shipping a clean artifact vs. a working-but-messy prototype.
2. Converging on an Emacs-style architecture for an inference core - and the friction when an AI assistant invents unapproved terminology ("role", "Emacs-style", "rebuild").
3. Keeping internal deliberation OUT of public docs: how AI assistants leak internal discussion and fabricate resources (a nonexistent GitHub org) into distributable repos.
4. Provenance and correctness discipline: catching a fabricated GitHub org, a reversed commit-style instruction, and Russian-language READMEs in what should be distributable repos.
5. Treating a local-LLM stack as an experiment to benchmark rather than a black box: Ollama -> Rapid-MLX migration, dual-process chat+embeddings, converging on devstral for a unified code-mode role - the substrate under the zeno core.
6. Meaning-based session decomposition: a "cases" decomposer plus clustering experiments validating cohesion/purity, and extracting sessions into typed atom files.

"On the horizon" opportunities that touch zeno (verbatim in meaning):
- Self-Verifying Test-Driven Agent Loops: wire Bash-heavy benchmark and eval flows (mcp__zeno__eval) into an assert-and-retry harness with a hard iteration cap; a verification subagent writes tests first, then implements in a loop until green, self-correcting benchmark regressions, port conflicts, and tok/s counter bugs.
- Autonomous / Parallel Session-Mining Content Pipeline: a standing agent (chained over the zeno decomposer and clustering sweep) ingests fresh raw session transcripts, clusters them, runs prior-art searches, and emits a triageable idea list with three audience angles (technical, founder, indie-hacker), grounded in cited source files.
- Constraint-Locked Parallel Infra Provisioning: parallel agents explore provisioning paths under an explicit allow/deny list, discard blacklisted approaches, and converge on reproducible verified Nix/Bash commands - never inventing resources or reversing instructions.

---

## Fun-ending items (zeno-adjacent, verbatim in meaning)

- Claude confidently invented a GitHub org out of thin air (while building a Discourse component) and secretly killed the user's own chat server by probing port 8000 during the Ollama-to-MLX migration.
- In a separate session Claude ran a "scary xattr command" on the user's home directory that spooked them about breaking their main machine (during the Apple Virt VM setup, amid rejected sudo/qemu/nix-signed-lima approaches before settling on the official lima binary).
