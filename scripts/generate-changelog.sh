#!/bin/bash
# generate-changelog.sh <from> <to>
# Outputs categorized markdown changelog to stdout

FROM="$1"
TO="$2"

WHATS_NEW=""
CHANGED=""
FIXED=""
REMOVED=""

while IFS=' ' read -r HASH MSG; do
  case "$MSG" in
    feat*|feature*)
      WHATS_NEW+="- ${MSG} (#${HASH})"$'\n'
      ;;
    fix*)
      FIXED+="- ${MSG} (#${HASH})"$'\n'
      ;;
    remove*)
      REMOVED+="- ${MSG} (#${HASH})"$'\n'
      ;;
    *)
      CHANGED+="- ${MSG} (#${HASH})"$'\n'
      ;;
  esac
done < <(git log --oneline --reverse "$FROM..$TO" 2>/dev/null || true)

if [ -n "$WHATS_NEW" ]; then
  echo "## Whats new ?"
  echo
  echo -n "$WHATS_NEW"
  echo
fi

if [ -n "$CHANGED" ]; then
  echo "## Changed"
  echo
  echo -n "$CHANGED"
  echo
fi

if [ -n "$FIXED" ]; then
  echo "## Fixed"
  echo
  echo -n "$FIXED"
  echo
fi

if [ -n "$REMOVED" ]; then
  echo "## Removed"
  echo
  echo -n "$REMOVED"
  echo
fi
