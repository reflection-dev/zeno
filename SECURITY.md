# Security Policy

## Supported versions

zeno is released from `master`; fixes land there and there is no separate
maintenance branch. Consumers pin the dependency to a specific revision, so
security fixes are applied by bumping that revision.

## Reporting a vulnerability

Please report security issues **privately**:

- Preferred: GitHub's **"Report a vulnerability"** button under the repository's
  *Security* tab, or
- Email **andy@reflection.dev**.

Do not open a public issue for an undisclosed vulnerability. Include steps to
reproduce and the impact; you will get an acknowledgement within a few days.

## The security model

zeno's whole job is to run untrusted or semi-trusted agent code without giving
it the orchestrator's power. Two boundaries carry that weight:

- **The capability grant (`zeno.grant`).** An agent's `(eval ...)` runs in a
  deny-by-default SCI context whose only visible vocabulary is what the
  orchestrator injected. There is no fs, shell, or network reach except through
  a granted fn. Widening what an agent can reach without the orchestrator
  authoring it is a security bug.
- **The gateway (`zeno.gateway`).** The gateway holds the secrets and privilege;
  spawned agents receive only a gateway URL scoped to one role's path
  (`/mcp/<role>`). A spawned session must never see the secrets themselves, and
  one role's endpoint must not reach another role's grant.
- **The sandbox network boundary (`zeno.sandbox`).** When a body *does* hold
  secrets, they are delivered **network-bound**: the guest env carries only an
  `$MSB_<ENV>` placeholder, and msb releases the real value only toward the
  secret's allowed host — even inside encoded auth like git's base64 Basic — so a
  prompt-injected body with broad web egress has nothing to exfiltrate. Egress is
  deny-by-default with an allowlist. A real secret value reaching the guest env,
  disk, or argv — or a bound secret reaching a host it was not scoped to — is a
  security bug.

## Rules any contribution must uphold

- **An agent reaches only its injected grant.** No ambient capability, no
  reflection escape, no shared mutable state that lets one spawn read another's.
- **Secrets live in the orchestrator, never in a spawn.** The spawn carries the
  gateway URL and nothing else.
- **A spawned coding CLI has built-in tools that bypass the MCP.** A grant is
  only as tight as its tool controls; disabling the built-ins you did not intend
  to give is part of authoring the grant, not an afterthought.

If you find behaviour that violates any of these, please report it via the
channels above.
