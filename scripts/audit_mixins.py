#!/usr/bin/env python3

import json
import pathlib
import re
import subprocess
import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
MIXINS_JSON = ROOT / "src/main/resources/meteor-client.mixins.json"
MIXIN_DIR = ROOT / "src/main/java/meteordevelopment/meteorclient/mixin"
MINECRAFT_JAR = ROOT / ".gradle-home/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.1/minecraft-merged-deobf-26.1.jar"


CLASS_RE = re.compile(r"@Mixin\((.*?)\)", re.S)
IMPORT_RE = re.compile(r"^import\s+([\w\.]+);", re.M)
ACCESSOR_RE = re.compile(r'@Accessor\("([^"]+)"\)\s*(?:public\s+)?(?:abstract\s+)?[\w\.<>,\[\] ?]+\s+(\w+)\s*\(', re.S)
INVOKER_RE = re.compile(r'@Invoker\("([^"]+)"\)\s*(?:public\s+)?(?:abstract\s+)?[\w\.<>,\[\] ?]+\s+(\w+)\s*\(', re.S)
METHOD_ASSIGN_RE = re.compile(r'method\s*=\s*(?:\{([^}]*)\}|"([^"]+)")', re.S)
STRING_RE = re.compile(r'"([^"]+)"')
SHADOW_FIELD_RE = re.compile(r"@Shadow(?:\s*@Final)?\s*(?:private|protected|public)\s+[\w\.<>,\[\] ?]+\s+(\w+)\s*;", re.S)
SHADOW_METHOD_RE = re.compile(r"@Shadow(?:\s*@Final)?\s*(?:private|protected|public)\s+(?:abstract\s+)?[\w\.<>,\[\] ?]+\s+(\w+)\s*\(", re.S)


def target_class(source: str) -> str | None:
    match = CLASS_RE.search(source)
    if not match:
        return None

    body = match.group(1)
    target_match = re.search(r'targets\s*=\s*"([^"]+)"', body)
    if target_match:
        return target_match.group(1)

    class_match = re.search(r"([A-Za-z_][A-Za-z0-9_]*)\.class", body)
    if not class_match:
        return None

    imports = {name.rsplit(".", 1)[-1]: name for name in IMPORT_RE.findall(source)}
    return imports.get(class_match.group(1))


def class_info(target: str, cache: dict[str, tuple[set[str], set[str]] | None]):
    if target in cache:
        return cache[target]

    proc = subprocess.run(
        ["javap", "-classpath", str(MINECRAFT_JAR), "-p", target],
        capture_output=True,
        text=True,
    )
    if proc.returncode != 0:
        cache[target] = None
        return None

    methods = set()
    fields = set()

    for raw_line in proc.stdout.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("Compiled from") or line in {"{", "}"}:
            continue

        if "(" in line and ")" in line:
            name = line.split("(", 1)[0].strip().split()[-1]
            methods.add(name)
        elif line.endswith(";"):
            name = line[:-1].strip().split()[-1]
            fields.add(name)

    cache[target] = (methods, fields)
    return cache[target]


def iter_method_selectors(source: str):
    for block, single in METHOD_ASSIGN_RE.findall(source):
        values = [single] if single else STRING_RE.findall(block)
        for value in values:
            if value.startswith("lambda$"):
                continue
            name = value.split("(", 1)[0]
            if name not in {"<init>", "<clinit>"}:
                yield name


def main() -> int:
    mixin_names = json.loads(MIXINS_JSON.read_text())["client"]
    cache: dict[str, tuple[set[str], set[str]] | None] = {}
    problems: list[tuple[str, str, str]] = []

    for mixin_name in mixin_names:
        path = MIXIN_DIR / f"{mixin_name}.java"
        if not path.exists():
            problems.append((mixin_name, "missing-file", str(path)))
            continue

        source = path.read_text()
        target = target_class(source)
        if not target:
            continue

        info = class_info(target, cache)
        if info is None:
            problems.append((mixin_name, "missing-target-class", target))
            continue

        methods, fields = info

        for field_name, _method_name in ACCESSOR_RE.findall(source):
            if field_name not in fields:
                problems.append((mixin_name, "missing-field", f"{target}::{field_name}"))

        for target_name, _method_name in INVOKER_RE.findall(source):
            if target_name != "<init>" and target_name not in methods:
                problems.append((mixin_name, "missing-invoker-target", f"{target}::{target_name}"))

        for field_name in SHADOW_FIELD_RE.findall(source):
            if field_name not in fields:
                problems.append((mixin_name, "missing-shadow-field", f"{target}::{field_name}"))

        for method_name in SHADOW_METHOD_RE.findall(source):
            if method_name not in methods:
                problems.append((mixin_name, "missing-shadow-method", f"{target}::{method_name}"))

        for method_name in iter_method_selectors(source):
            if method_name not in methods:
                problems.append((mixin_name, "missing-injection-method", f"{target}::{method_name}"))

    seen = set()
    for problem in problems:
        if problem in seen:
            continue
        seen.add(problem)
        print("\t".join(problem))

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
