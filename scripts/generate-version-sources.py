#!/usr/bin/env python3
"""Generate src/mc/<to>/ from src/mc/<from>/ by rewriting classes that moved between versions.

Minecraft periodically reorganises its packages (26.3 moved the GPU API out of
`com.mojang.blaze3d.*` into `com.mojang.renderpearl.*`). Classes that still exist under a new
name are rewritten automatically; classes that disappeared cannot be rewritten and are listed so
they can be ported by hand.

Usage:
    scripts/generate-version-sources.py <from-version> <to-version> [--compat-impl NewImplName]

Example:
    scripts/generate-version-sources.py 26.2 26.3 --compat-impl Mc263Compat
"""
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MC = ROOT / "src/mc"
LOOM = Path.home() / ".gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf"


def entries(version):
    jar = LOOM / version / f"minecraft-merged-deobf-{version}.jar"
    if not jar.is_file():
        sys.exit(f"No Loom jar for Minecraft {version} at {jar}.\nBuild that version once first.")
    out = subprocess.run(["unzip", "-Z1", str(jar)], capture_output=True, text=True).stdout
    return {l for l in out.splitlines() if l.endswith(".class")}


def build_rename_map(src, old_entries, new_entries):
    """FQN -> FQN for every referenced class that exists under a different name."""
    by_simple = {}
    for e in new_entries:
        by_simple.setdefault(e.rsplit("/", 1)[-1][:-6], []).append(e[:-6].replace("/", "."))

    refs = set()
    for f in src.rglob("*.java"):
        text = f.read_text()
        refs.update(r.rstrip(".") for r in re.findall(r"\b(?:net\.minecraft|com\.mojang)\.[A-Za-z0-9_.]+", text))

    renames = {}
    for ref in refs:
        if ref.replace(".", "/") + ".class" in new_entries:
            continue  # unchanged
        hits = by_simple.get(ref.rsplit(".", 1)[-1], [])
        if len(hits) == 1:
            renames[ref] = hits[0]

    # longest first so GpuDeviceBackend is not clobbered by GpuDevice
    return sorted(renames.items(), key=lambda kv: -len(kv[0])), sorted(set(refs) - set(renames) - set(renames.values()))


def main():
    args = sys.argv[1:]
    if len(args) < 2:
        sys.exit(__doc__)
    frm, to = args[0], args[1]
    impl_name = None
    if "--compat-impl" in args:
        impl_name = args[args.index("--compat-impl") + 1]

    src, dst = MC / frm, MC / to
    if not src.is_dir():
        sys.exit(f"No version sources at {src}")
    if dst.exists():
        shutil.rmtree(dst)

    renames, unmatched = build_rename_map(src, entries(frm), entries(to))

    # the compatibility implementation class is version specific
    impl_files = sorted(src.rglob("*Compat.java"))
    impl_old = impl_files[0].name[:-5] if impl_files else None
    impl_new = impl_name or (impl_old.replace(frm.replace(".", ""), to.replace(".", "")) if impl_old else None)

    written = 0
    for f in sorted(src.rglob("*.java")):
        text = f.read_text()
        rel = str(f.relative_to(src))

        if impl_new and impl_old:
            rel = rel.replace(impl_old, impl_new)
            text = text.replace(impl_old, impl_new)

        for old, new in renames:
            text = re.sub(rf"\b{re.escape(old)}\b", new, text)

        out = dst / rel
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(text)
        written += 1

    print(f"generated {written} files into src/mc/{to}/")
    print(f"  rewritten class references : {len(renames)}")
    print(f"  compatibility impl        : {impl_old} -> {impl_new}")
    if unmatched:
        print(f"  NOT rewritten ({len(unmatched)}) - port by hand:")
        for u in unmatched:
            print(f"    {u}")


if __name__ == "__main__":
    main()
