#!/usr/bin/env python3
"""Audit the mixins against a Minecraft version.

A mixin that targets a method which no longer exists compiles fine and then throws while the
game starts, so compiling is not enough. This script checks, for every mixin:

  1. that the target class of @Mixin exists,
  2. that every injected method exists on that target class,
  3. that every @At(INVOKE) / @At(FIELD) target still resolves.

Usage:
    scripts/audit-mixin-targets.py <version> [<root> ...]

The version is a Minecraft version id ("26.4-snapshot-4"), not the Meteor version key
("26.4"). The roots default to the shared sources plus the version specific tree.

The mapped (deobfuscated) Minecraft jar is read from the Fabric Loom cache:
    ~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/<version>/
"""
import os
import re
from pathlib import Path
import subprocess
import sys
import zipfile

CACHE = os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf")

MIXIN_RE = re.compile(r"@Mixin\s*\(\s*(?:value\s*=\s*)?([\w.]+)\.class")
MIXIN_TARGETS_RE = re.compile(r'@Mixin\s*\([^)]*targets\s*=\s*"([\w.$]+)"')
METHOD_RE = re.compile(r'method\s*=\s*"([\w$<>]+)(?:\([^"]*\))?"')
METHOD_LIST_RE = re.compile(r'method\s*=\s*\{([^}]*)\}')
DESCRIPTOR_RE = re.compile(r'target\s*=\s*"(L[^"]+;)"')
IMPORT_RE = re.compile(r"^import\s+(?:static\s+)?([\w.]+)\s*;", re.M)


def jar(version):
    directory = os.path.join(CACHE, version)
    if not os.path.isdir(directory):
        sys.exit(f"No Minecraft jar for {version} in {CACHE}")
    jars = [f for f in os.listdir(directory) if f.endswith(".jar")]
    if not jars:
        sys.exit(f"No Minecraft jar for {version} in {directory}")
    return os.path.join(directory, jars[0])


def members(javap_output):
    """Method and field names declared by a class."""
    names = set()
    for line in javap_output.splitlines():
        stripped = line.strip()
        method = re.search(r"([\w$<>]+)\(", stripped)
        if method:
            names.add(method.group(1))
        elif stripped.endswith(";") and "(" not in stripped:
            parts = stripped[:-1].split()
            if parts:
                names.add(parts[-1])
    return names


def superclass(javap_output):
    """The superclass of a class, if any."""
    for line in javap_output.splitlines():
        stripped = line.strip()
        match = re.match(r"(?:public |final |abstract |static )*class [\w$.]+ extends ([\w$.]+)", stripped)
        if match:
            return match.group(1)
    return None


def find_member(archive, out_dir, fqn, member):
    """Look a member up in the class and in its superclasses.

    A descriptor names the type the call site used, which is often a subclass of the class that
    actually declares the member, so a plain lookup would report false positives.
    """
    seen = set()
    current = fqn
    while current and current not in seen:
        seen.add(current)
        entry = current.replace(".", "/") + ".class"
        if entry not in archive.namelist():
            return False
        archive.extract(entry, out_dir)
        result = subprocess.run(["javap", "-p", "-classpath", out_dir, current],
                                capture_output=True, text=True)
        if member in members(result.stdout):
            return True
        current = superclass(result.stdout)
    return False


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    version = sys.argv[1]
    roots = sys.argv[2:] or ["src/main/java"]

    path = jar(version)
    out_dir = f"/tmp/meteor-mixin-audit-{version}"
    os.makedirs(out_dir, exist_ok=True)
    archive = zipfile.ZipFile(path)
    classes = set(n[:-6].replace("/", ".") for n in archive.namelist() if n.endswith(".class"))

    problems = []
    descriptor_targets = []
    checked_methods = 0
    checked_targets = 0

    # the version specific MixinTargets constants, so that mixins referring to them can be checked
    mixin_targets = {}
    targets_file = Path(f"src/mc/{version.split('-')[0]}/meteordevelopment/meteorclient/mc/MixinTargets.java")
    for root in roots:
        candidate = Path(root) / "meteordevelopment/meteorclient/mc/MixinTargets.java"
        if candidate.is_file():
            targets_file = candidate
            break
    if targets_file.is_file():
        for m in re.finditer(r'public static final String (\w+) = "([^"]*)"',
                             targets_file.read_text(encoding="utf-8")):
            mixin_targets[m.group(1)] = m.group(2)

    for root in roots:
        for dirpath, _, files in os.walk(root):
            for name in files:
                if not name.endswith(".java"):
                    continue
                file = os.path.join(dirpath, name)
                text = open(file, encoding="utf-8", errors="ignore").read()
                relative = file.split("/meteorclient/")[-1]

                imports = dict((i.rsplit(".", 1)[-1], i) for i in IMPORT_RE.findall(text))

                # 1. @Mixin targets
                for match in MIXIN_RE.finditer(text):
                    target = match.group(1)
                    fqn = target if "." in target else imports.get(target)
                    if fqn and fqn.startswith(("net.minecraft", "com.mojang")) and fqn not in classes:
                        problems.append(f"{relative}: mixin target class {fqn} does not exist")
                for match in MIXIN_TARGETS_RE.finditer(text):
                    target = match.group(1)
                    if target.startswith(("net.minecraft", "com.mojang")) and target not in classes:
                        problems.append(f"{relative}: mixin target class {target} does not exist")

                # 2. injected methods
                injected = {m.group(1) for m in METHOD_RE.finditer(text)}
                # MixinTargets.X constants are resolved from the version specific class
                for m in re.finditer(r"MixinTargets\.([A-Z_]+)", text):
                    value = mixin_targets.get(m.group(1))
                    if value is None:
                        problems.append(f"{relative}: MixinTargets.{m.group(1)} is not defined")
                    elif value.startswith("L") and ";" in value:
                        # an invoke target: Lowner/Class;method(args)ret
                        owner = value[1:value.index(";")]
                        member = value[value.index(";") + 1:].split("(")[0]
                        descriptor_targets.append((owner.replace("/", "."), member))
                    else:
                        injected.add(value)
                for match in METHOD_LIST_RE.finditer(text):
                    for part in match.group(1).split(","):
                        part = part.strip().strip('"')
                        if part:
                            injected.add(part.split("(")[0].strip('"'))
                injected = {m for m in injected if m and not m.startswith("lambda$")}

                for match in MIXIN_RE.finditer(text):
                    target = match.group(1)
                    fqn = target if "." in target else imports.get(target)
                    if not fqn or fqn not in classes:
                        continue
                    for method in sorted(injected):
                        checked_methods += 1
                        if method == "<init>":
                            continue  # constructors are always there, javap does not name them
                        # a value like "apply(Lfoo/Bar;)V" is a full descriptor
                        name = method.split("(")[0]
                        if not find_member(archive, out_dir, fqn, name):
                            problems.append(f"{relative}: {fqn}#{method} does not exist")

                # 3. @At(INVOKE) / @At(FIELD) targets
                for match in DESCRIPTOR_RE.finditer(text):
                    descriptor = match.group(1)
                    semicolon = descriptor.index(";")
                    owner = descriptor[1:semicolon]
                    rest = descriptor[semicolon + 1:]
                    member = rest[:rest.index("(")] if "(" in rest else rest.split(":")[0]
                    fqn = owner.replace("/", ".")
                    if not fqn.startswith(("net.minecraft", "com.mojang")):
                        continue  # belongs to another mod
                    if fqn not in classes:
                        problems.append(f"{relative}: target class {fqn} does not exist")
                        continue
                    checked_targets += 1
                    if not find_member(archive, out_dir, fqn, member):
                        problems.append(f"{relative}: target {fqn}#{member} does not exist")

    for owner, member in descriptor_targets:
        checked_targets += 1
        if owner not in classes:
            problems.append(f"target class {owner} does not exist")
        elif not find_member(archive, out_dir, owner, member):
            problems.append(f"target {owner}#{member} does not exist")

    print(f"Minecraft {version}: {checked_methods} injected methods, {checked_targets} invoke targets checked")
    if not problems:
        print("  OK: every mixin target resolves")
        return 0

    for problem in sorted(set(problems)):
        print(f"  {problem}")
    print(f"  {len(set(problems))} problem(s)")
    return 1


if __name__ == "__main__":
    sys.exit(main())
