# Marginal zeno mentions (archival)

Two transcripts where zeno appears only in passing. Both are incidental references, not design substance.

## local-llm (session 2547de89-4579-45de-894f-16519fecae3e, 2026-07-10)

Zeno connection: config reuse only. Zeno is used as the reference for git identity and commit-signing setup, nothing about zeno's design.

- The user asked to commit to a branch `ollama` using the same name, email, and signing key as in zeno.
- Zeno signs commits with an SSH key. The assistant created the `ollama` branch and committed as author Andy Smith <me@andysmith.ai> (same as zeno), signed with SSH key `id_alchery.pub`, verified as a Good signature (status G), commit a4b15be.
- Rest of the session is unrelated to zeno: reworking the local-llm stack from Ollama to Rapid-MLX (declarative install via devenv + uv from pyproject.toml, one `rapid-mlx serve` process for both chat and embeddings). The `ollama` branch was kept as a clean snapshot; rapid-mlx changes moved to `master`, left uncommitted.

No zeno substance beyond "use the same author/email/signing key as zeno."

## banjo (session 11414564-ecdf-405b-b37e-3fdbb045569b, 2026-08-20)

Zeno connection: naming only. "Banjo" is the internal code name for the first live instance of zeno; the discussion is about whether Banjo works as a public brand.

- The user asked whether plain "Banjo" is usable and to check if it's taken.
- The assistant found "just Banjo" has more collisions than the alternative "Clancy":
  1. Direct competitor: there is already a "Banjo AI" personal AI assistant in the App Store (live, updated in 2026; Apple Calendar, "close circles," weekly journal, recently moved to Gemini T1). Same niche.
  2. Reputational baggage: the notorious Utah "Banjo" AI-surveillance company (a $20M contract that fell through, founder with a KKK past, later renamed safeXai). The press remembers "Banjo AI" for that scandal.
  3. Still active: Banjo Health, AI for prior authorization. Different niche but holds the brand.
- Conclusion: keep `banjo` as an internal code name (described as "banjo = first live instance of zeno"), where the Banjo Paterson reference is clean and personal, but pick a separate public brand in advance because a live same-named personal-AI assistant plus the surveillance-scandal association make Banjo weak publicly. The assistant offered to check other names from the Paterson universe (Jarrah, Matilda, Swaggie, Snowy).

The only zeno-relevant fact: Banjo is the internal code name for the first live instance of zeno.
