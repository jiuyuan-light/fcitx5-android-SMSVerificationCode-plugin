#!/usr/bin/env bash
set -euo pipefail

UPSTREAM_REPO="fcitx5-android/fcitx5-android"
SELF_REPO="${GITHUB_REPOSITORY:-}"

LATEST_TAG="$(
  curl -s "https://api.github.com/repos/${UPSTREAM_REPO}/releases/latest" \
    | sed -n 's/.*"tag_name"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' \
    | head -n 1
)"

if [[ -z "${LATEST_TAG}" || "${LATEST_TAG}" == "null" ]]; then
  echo "failed to fetch upstream latest tag" >&2
  exit 1
fi

echo "latest_tag=${LATEST_TAG}"

if [[ -z "${SELF_REPO}" ]]; then
  echo "update_needed=true"
  exit 0
fi

HTTP_CODE="$(
  curl -s -o /dev/null -w "%{http_code}" \
    "https://api.github.com/repos/${SELF_REPO}/releases/tags/${LATEST_TAG}"
)"

if [[ "${HTTP_CODE}" == "200" ]]; then
  echo "update_needed=false"
else
  echo "update_needed=true"
fi
