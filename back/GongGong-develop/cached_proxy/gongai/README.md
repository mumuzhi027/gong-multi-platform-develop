# Gong AI Beta backend

The AI layer runs behind the existing proxy authentication. A model may choose a generic capability, but only backend tools can access campus providers. The current user is always derived from the bearer token and is never accepted in chat JSON.

Environment variables:

- `DEEPSEEK_API_KEY`: required for model-based routing; no secret is committed.
- `DEEPSEEK_BASE_URL`: optional, defaults to `https://api.deepseek.com`.
- `DEEPSEEK_MODEL`: optional, defaults to `deepseek-v4.1-flash`; confirm the exact provider model identifier during deployment.
- `SESSION_VAULT_MASTER_KEY`: reserved for encrypted extracurricular provider sessions. The live SSO token exchange is not present in the current repository and must be configured before enabling leave writes.

When no model key is configured, a deterministic Chinese intent router keeps the supported read-only queries available. Tool results are rendered directly and are never appended to the model prompt.

