# Chat “server errors” — diagnosis (pointer)

**Canonical audit:** [`AUDIT_chat_auth_jwt_vc14.md`](./AUDIT_chat_auth_jwt_vc14.md)

**Live status (2026-09-16):** Agent-chat “server error” symptoms are **resolved via backend interventions**. User-facing chat should be healthy on current store + local builds without waiting on the app JWT package.

Sep 2026 diagnosis still holds for the **client** side: Play release JWT/R8 auth thrash was real (not “EC2 down”). Hardening remains in-tree for **vc14** as defense-in-depth; it is no longer the gate for stopping the live chat-error wave.