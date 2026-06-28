# RecallTotemMod

A Fabric mod that provides "Recall Anchor" protection and region management for Minecraft servers.

## Features
- Define circular protected regions with deny messages and admin notes.
- Schedule windows (ISO-8601) to enable/disable region enforcement.
- LuckPerms integration (reflection-based) to allow role-based bypass.
- Per-region whitelist (UUIDs) to allow specific players.
- In-game commands to manage regions, schedules, ownership, and whitelists.
- Web export and token-protected dashboard to visualize regions.
- Audit logging and webhook notifications for administrative actions.
- Webhook delivery store and automatic retry scheduler.

## Quick start
1. Place the mod jar in your Fabric `mods/` folder.
2. Edit `config/recalltotem/recalltotem.json` to configure web export token, webhook URL, etc.
3. Restart server and use `/recall region` commands as OP.

## Schedule examples
- Add schedule:
  `/recall region schedule add 0 2026-07-01T08:00:00Z 2026-07-01T20:00:00Z`

## Security
- Web endpoints are token-protected. Keep tokens secret and restrict dashboard access.
- Webhooks support HMAC signature header for verification.

(Full documentation in API.md and in-code Javadoc.)
