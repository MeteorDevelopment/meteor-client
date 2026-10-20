#!/usr/bin/env python3
"""Report which classes referenced by the version-specific sources moved between two
Minecraft versions.

Minecraft periodically reorganises its packages (26.3 moved the whole GPU API out of
`com.mojang.blaze3d.*` into `com.mojang.renderpearl.*`). This script compares the classes a
version's sources use against two Loom-cached jars and prints an import-rewrite map, so
porting a new version starts from facts instead of guesswork.

Usage:
    scripts/detect-moved-classes.py <version-specific-src-dir> <from-version> <to-version>

Example:
    scripts/detect-moved-classes.py src/mc/26.2 26.2 26.3

Exit status is 0 even when classes moved; inspect the output to decide what to port.
"""
import re
import subprocess
import sys
from pathlib import Path

LOOM = Path.home() / ".gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf"


def entries(version):
    jar = LOOM / version / f"minecraft-merged-deobf-{version}.jar"
    if not jar.is_file():
        sys.exit(f"Loom has no jar for Minecraft {version} at {jar}.\nBuild that version once first.")
    out = subprocess.run(["unzip", "-Z1", str(jar)], capture_output=True, text=True).stdout
    return {l for l in out.splitlines() if l.endswith(".class")}


def main():
    if len(sys.argv) != 4:
        sys.exit(__doc__)

    src = Path(sys.argv[1])
    old_entries, new_entries = entries(sys.argv[2]), entries(sys.argv[3])

    by_simple_name = {}
    for e in new_entries:
        by_simple_name.setdefault(e.rsplit("/", 1)[-1][:-6], []).append(e[:-6].replace("/", "."))

    refs = set()
    for f in sorted(src.rglob("*.java")):
        for ref in re.findall(r"\bnet\.minecraft\.[A-Za-z0-9_.]+|\bcom\.mojang\.[A-Za-z0-9_.]+", f.read_text()):
            refs.add(ref.rstrip("."))

    moved, removed, unchanged = [], [], []
    for ref in sorted(refs):
        if ref.replace(".", "/") + ".class" in new_entries:
            unchanged.append(ref)
            continue
        hits = by_simple_name.get(ref.rsplit(".", 1)[-1], [])
        (moved if hits else removed).append((ref, hits))

    print(f"references: {len(refs)}   unchanged: {len(unchanged)}\n")
    print(f"MOVED ({len(moved)}) - import rewrite:")
    for ref, hits in moved:
        for hit in hits:
            print(f"  {ref} -> {hit}")
    print(f"\nREMOVED ({len(removed)}) - no 1:1 replacement, porting required by hand:")
    for ref, _ in removed:
        print(f"  {ref}")


if __name__ == "__main__":
    main()
