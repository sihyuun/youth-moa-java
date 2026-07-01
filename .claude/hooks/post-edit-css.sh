#!/usr/bin/env bash
# PostToolUse hook — detect self-referencing CSS custom properties in main.css
# Trigger: after Edit/Write on any file. Only runs check if file is a .css file.
# Rule: `--color-x: var(--color-x)` is a bug (produces invalid value → color unset).
# Historical incident: --color-text-tri self-reference broke text-tri color (STATE.md).

set -euo pipefail

input="$(cat)"
# Extract file_path from tool_input
file="$(printf '%s' "$input" | grep -oE '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"file_path"[[:space:]]*:[[:space:]]*"//; s/"$//')"

# Only check CSS files
case "$file" in
  *.css) ;;
  *) exit 0 ;;
esac

# File may not exist yet (e.g. Write on non-existent path — should exist post-Write though)
if [ ! -f "$file" ]; then
  exit 0
fi

# Detect `--x: var(--x)` — CSS custom property that references itself.
# Regex: capture --NAME on LHS, then look for var(--NAME) on RHS on same line.
selfref="$(grep -nE '^\s*(--[a-zA-Z0-9_-]+)\s*:[^;]*\bvar\(\1\)' "$file" || true)"

if [ -n "$selfref" ]; then
  cat >&2 <<EOF
WARN: self-referencing CSS custom property detected in $file

$selfref

This produces an invalid CSS value and the property will be unset (transparent / initial).
Historical incident: --color-text-tri self-reference broke text-tri color (see STATE.md).

Fix by referencing a base value (e.g. oklch() literal or another token):
  --color-text-tri: oklch(0.7 0.02 280);  /* not var(--color-text-tri) */
EOF
  # Soft warning — allow save. Change to `exit 2` to hard block.
fi

exit 0
