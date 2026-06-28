/*
deliveries.js - webhook deliveries panel helper
Renders delivery entries and provides resend/do-not-retry controls.
Resend calls POST /recalltotem/webhook-resend?id=<idx>&token=...
*/
function renderDelivery(entry, idx) {
  const attempts = entry.attempts || 0;
  const nextRetry = entry.nextRetryAt || '';
  const status = entry.httpCode || entry.lastStatus || -1;
  const color = status >= 200 && status < 300 ? 'green' : (status >= 400 && status < 500 ? 'orange' : 'red');
  // UI elements: show attempts, nextRetry, doNotRetry toggle, resend button
  // Resend button calls POST /recalltotem/webhook-resend?id=idx&token=...
}
