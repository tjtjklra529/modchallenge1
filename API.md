# RecallTotemMod API

This document describes the mod's public endpoints and commands.

## HTTP endpoints (token-protected)
- GET /recalltotem/regions?token=<token>
  - Returns JSON array of protected regions.

- GET /recalltotem/audit?token=<token>
  - Returns recent audit lines as plain text.

- POST /recalltotem/webhook-resend?id=<index>&token=<token>
  - Replays a webhook delivery entry by index.

- (Dashboard static assets)
  - /recalltotem/dashboard/index.html
  - /recalltotem/dashboard/regions.js
  - /recalltotem/dashboard/deliveries.js

## Commands (in-game)
- /recall region add <x> <z> <radius> [dimension]
- /recall region remove <index>
- /recall region list
- /recall region edit <index> <field> <value>
- /recall region import <json>
- /recall region import-file <path>
- /recall region export <path>
- /recall region preview <index>
- /recall region preview-toggle
- /recall region schedule add/remove/list/validate
- /recall region claim/transfer/set-owner
- /recall region audit [lines]
- /recall region audit-filter <type> <query>
- /recall region whitelist add/remove/list
- /recall region whitelist bulk-apply <path> (dry-run) / bulk-apply-apply / export <path>

(See in-code Javadoc for parameter details and permission requirements.)
