#!/bin/bash
# generate-changelog.sh <from> <to>
# Outputs categorized markdown changelog with descriptions to stdout

FROM="$1"
TO="$2"

WHATS_NEW=""
CHANGED=""
FIXED=""
REMOVED=""

while IFS='§' read -r HASH SUBJECT BODY; do
  SHORT_HASH="${HASH:0:7}"
  LINE="- **${SUBJECT}** ([${SHORT_HASH}](https://github.com/Lampalon142/LifeMod/commit/${HASH}))"

  if [ -n "$BODY" ]; then
    LINE+=$'\n'
    while IFS= read -r BODY_LINE; do
      if [ -n "$BODY_LINE" ]; then
        LINE+="  ${BODY_LINE}"$'\n'
      fi
    done <<< "$BODY"
  fi

  case "$SUBJECT" in
    feat*|feature*)
      WHATS_NEW+="$LINE"$'\n'
      ;;
    fix*)
      FIXED+="$LINE"$'\n'
      ;;
    remove*)
      REMOVED+="$LINE"$'\n'
      ;;
    *)
      CHANGED+="$LINE"$'\n'
      ;;
  esac
done < <(git log --format="%H§%s§%b" --reverse "$FROM..$TO" 2>/dev/null || true)

if [ -n "$WHATS_NEW" ]; then
  echo "## 🆕 What's New"
  echo
  echo -n "$WHATS_NEW"
  echo
fi

if [ -n "$CHANGED" ]; then
  echo "## 🔧 Changed"
  echo
  echo -n "$CHANGED"
  echo
fi

if [ -n "$FIXED" ]; then
  echo "## 🐛 Fixed"
  echo
  echo -n "$FIXED"
  echo
fi

if [ -n "$REMOVED" ]; then
  echo "## ❌ Removed"
  echo
  echo -n "$REMOVED"
  echo
fi
