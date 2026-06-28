# API Examples

## Fetch regions (curl)
curl "http://<server>:8123/recalltotem/regions?token=<token>"

## Fetch audit
curl "http://<server>:8123/recalltotem/audit?token=<token>"

## Resend delivery
curl -X POST "http://<server>:8124/recalltotem/webhook-resend?id=3&token=<token>"
