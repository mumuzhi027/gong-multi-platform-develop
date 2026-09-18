# Leave capability

Use for applying for leave, listing the current user's leave processes, reading status, or withdrawing an in-progress process.

Execution order for `leave.apply`:

1. Resolve the current user exclusively from backend authentication.
2. Load the active `qingjia` process definition and dynamic form schema.
3. Collect only missing required fields.
4. Derive affected class hours from the current user's schedule when possible.
5. Warn when no class-teacher communication attachment is present.
6. Build a deterministic preview and immutable parameter hash.
7. Execute only after explicit confirmation.
8. Verify by reading the created process and matching owner, process key, time, and reason.

Never expose provider credentials or attachment URLs to the model. Never retry an uncertain create request before verification.

