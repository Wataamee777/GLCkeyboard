#!/usr/bin/env sh
set -eu

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

echo "gradle command not found. Please install Gradle or add a Gradle wrapper jar." >&2
exit 127
