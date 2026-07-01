#!/usr/bin/env bash
# PreToolUse hook for Bash — block `git add -A` / `git add .` / `git add *`
# Rule: only explicit file paths allowed for staging (CLAUDE.md + memory rule)
# Input: JSON on stdin with { tool_input: { command: "..." } }
# Exit: 0 allow, 2 block (Claude sees stderr as reason)

set -euo pipefail

input="$(cat)"
cmd="$(printf '%s' "$input" | grep -oE '"command"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"command"[[:space:]]*:[[:space:]]*"//; s/"$//')"

# Match blocked patterns:
#   git add -A / git add --all / git add . / git add *
if printf '%s' "$cmd" | grep -qE '(^|[[:space:];&|])git[[:space:]]+add[[:space:]]+(-A|--all|\.|\*)([[:space:]]|$)'; then
  cat >&2 <<'EOF'
BLOCKED: `git add -A` / `git add .` / `git add *` are forbidden in this repo.

Reason: CLAUDE.md + memory rule — must stage only files you actually changed,
to avoid sweeping in files from parallel sessions or secrets.

Use explicit paths instead:
  git add path/to/file1 path/to/file2

Or reset and re-add explicitly:
  git restore --staged .
  git add <specific files>
EOF
  exit 2
fi

exit 0
