#!/usr/bin/env bash
# Walks one request through its whole lifecycle using curl.
# Usage: start the backend, then run  ./scripts/api-demo.sh  [base-url]
set -euo pipefail
BASE="${1:-http://localhost:8080}"
JSON='Content-Type: application/json'

step() { printf '\n\033[1m== %s\033[0m\n' "$1"; }

step "Create a request"
created=$(curl -sS -X POST "$BASE/api/customer/requests" -H "$JSON" -d '{
  "customerName": "Jordan Rivera",
  "category": "BILLING",
  "subject": "Charged twice for one payment",
  "description": "My card was charged twice on the 3rd for a single bill payment."
}')
echo "$created"
REF=$(printf '%s' "$created" | grep -o '"referenceNumber":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "Reference: $REF"

change() {
  step "Status -> $1"
  curl -sS -o /dev/null -w "HTTP %{http_code}\n" -X POST "$BASE/api/agent/requests/$REF/status-changes" \
    -H "$JSON" -d "{\"newStatus\":\"$1\",\"explanation\":\"$2\"}"
}

step "Forbidden: OPEN -> RESOLVED (expect 409)"
curl -sS -w "\nHTTP %{http_code}\n" -X POST "$BASE/api/agent/requests/$REF/status-changes" \
  -H "$JSON" -d '{"newStatus":"RESOLVED","explanation":"Skipping ahead"}'

change IN_PROGRESS "Reviewing the duplicate charge"
change WAITING_ON_CUSTOMER "Please confirm the last four digits of the card"

step "Customer message (status stays WAITING_ON_CUSTOMER)"
curl -sS -o /dev/null -w "HTTP %{http_code}\n" -X POST "$BASE/api/customer/requests/$REF/messages" \
  -H "$JSON" -d '{"message":"It ends in 4242."}'

change IN_PROGRESS "Customer confirmed card details"
change RESOLVED "Duplicate charge refunded"
change IN_PROGRESS "Customer reports the refund has not arrived"

step "Final state and full history"
curl -sS "$BASE/api/customer/requests/$REF"
echo
