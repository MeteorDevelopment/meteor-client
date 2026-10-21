#!/usr/bin/env bash
# Builds Meteor Client once per supported Minecraft version.
#
# Every version needs a descriptor in gradle/mc-versions/<version>.properties and its own
# sources in src/mc/<version>/. See COMPAT.md.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ $# -gt 0 ]; then
  versions=("$@")
else
  mapfile -t versions < <(
    find gradle/mc-versions -name '*.properties' -printf '%f\n' | sed 's/\.properties$//' | sort -V
  )
fi

if [ ${#versions[@]} -eq 0 ]; then
  echo "No version descriptors found in gradle/mc-versions/." >&2
  exit 1
fi

status=0
for version in "${versions[@]}"; do
  echo "=== Building for Minecraft $version ==="
  if ./gradlew build -PminecraftVersion="$version"; then
    # A mixin that points at something that no longer exists compiles fine and then throws
    # while the game starts, so the mixins are checked against the jar of the version.
    minecraft=$(grep '^minecraft=' "gradle/mc-versions/$version.properties" | cut -d= -f2)
    if python3 scripts/audit-mixin-targets.py "$minecraft" "src/mc/$version" src/main/java; then
      echo "--- OK: $version"
    else
      echo "--- FAILED: $version (the mixins do not fit)" >&2
      status=1
    fi
  else
    echo "--- FAILED: $version" >&2
    status=1
  fi
done

exit "$status"
