#!/usr/bin/env bash
set -euo pipefail
if [[ -z "${TELEGRAM_BOT_TOKEN:-}" || -z "${TELEGRAM_CHAT_ID:-}" ]]; then
    echo '::warning::Telegram notification skipped: bot token or chat ID is missing.'
    exit 0
fi

message="${GITHUB_WORKFLOW:?}: ${WORKFLOW_RESULT:?}
Repository: ${GITHUB_REPOSITORY:?}
Commit: ${GITHUB_SHA:?}
${GITHUB_SERVER_URL:?}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID:?}"

if ! curl --fail --silent --max-time 20 --retry 2 --retry-max-time 45 \
    --data-urlencode "chat_id=$TELEGRAM_CHAT_ID" --data-urlencode "text=$message" \
    "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage" >/dev/null; then
    echo 'Telegram Bot API rejected the notification or did not respond.' >&2
    exit 1
fi
