#!/usr/bin/env python3

from __future__ import annotations

import argparse
import re
from collections import Counter
from pathlib import Path


PRIMITIVE_DESCRIPTORS = {
    "void": "V",
    "boolean": "Z",
    "byte": "B",
    "char": "C",
    "short": "S",
    "int": "I",
    "long": "J",
    "float": "F",
    "double": "D",
}

CLASS_LINE_RE = re.compile(r"^(?P<mojmap>\S+) -> (?P<official>\S+):$")
FIELD_LINE_RE = re.compile(
    r"^\s{4}(?P<type>\S+) (?P<mojmap_name>[^\s(]+) -> (?P<official_name>\S+)$"
)
METHOD_LINE_RE = re.compile(
    r"^\s{4}(?:(?P<line_from>\d+):(?P<line_to>\d+):)?"
    r"(?P<return_type>\S+) (?P<mojmap_name>[^(]+)\((?P<args>[^)]*)\)"
    r"(?::(?P<src_from>\d+):(?P<src_to>\d+))? -> (?P<official_name>\S+)$"
)


def dotted_to_internal(name: str) -> str:
    return name.replace(".", "/")


def to_descriptor(type_name: str, mojmap_to_official: dict[str, str]) -> str:
    dims = 0
    while type_name.endswith("[]"):
        dims += 1
        type_name = type_name[:-2]

    if type_name.endswith("..."):
        dims += 1
        type_name = type_name[:-3]

    primitive = PRIMITIVE_DESCRIPTORS.get(type_name)
    if primitive is not None:
        return "[" * dims + primitive

    internal = dotted_to_internal(type_name)
    internal = mojmap_to_official.get(internal, internal)
    return "[" * dims + f"L{internal};"


def to_method_descriptor(
    return_type: str, args: str, mojmap_to_official: dict[str, str]
) -> str:
    arg_descriptors: list[str] = []
    if args:
        for arg in args.split(","):
            arg = arg.strip()
            if arg:
                arg_descriptors.append(to_descriptor(arg, mojmap_to_official))

    return (
        f"({''.join(arg_descriptors)})"
        f"{to_descriptor(return_type, mojmap_to_official)}"
    )


def parse_proguard_files(paths: list[Path]) -> tuple[dict[str, str], dict[tuple[str, str, str], str], dict[tuple[str, str, str], str]]:
    class_map: dict[str, str] = {}
    mojmap_to_official: dict[str, str] = {}
    field_map: dict[tuple[str, str, str], str] = {}
    method_map: dict[tuple[str, str, str], str] = {}

    for path in paths:
        for raw_line in path.read_text().splitlines():
            class_match = CLASS_LINE_RE.match(raw_line)
            if not class_match:
                continue

            mojmap_internal = dotted_to_internal(class_match.group("mojmap"))
            official_internal = class_match.group("official")

            previous = class_map.get(official_internal)
            if previous is not None and previous != mojmap_internal:
                raise ValueError(
                    f"Conflicting class mapping for {official_internal}: "
                    f"{previous} vs {mojmap_internal}"
                )

            class_map[official_internal] = mojmap_internal
            mojmap_to_official[mojmap_internal] = official_internal

    for path in paths:
        current_owner: str | None = None
        for raw_line in path.read_text().splitlines():
            if not raw_line or raw_line.startswith("#"):
                continue

            class_match = CLASS_LINE_RE.match(raw_line)
            if class_match:
                current_owner = class_match.group("official")
                continue

            if current_owner is None or raw_line.startswith("    #"):
                continue

            method_match = METHOD_LINE_RE.match(raw_line)
            if method_match:
                descriptor = to_method_descriptor(
                    method_match.group("return_type"),
                    method_match.group("args"),
                    mojmap_to_official,
                )
                key = (
                    current_owner,
                    descriptor,
                    method_match.group("official_name"),
                )
                method_map.setdefault(key, method_match.group("mojmap_name"))
                continue

            field_match = FIELD_LINE_RE.match(raw_line)
            if field_match:
                descriptor = to_descriptor(
                    field_match.group("type"), mojmap_to_official
                )
                key = (
                    current_owner,
                    descriptor,
                    field_match.group("official_name"),
                )
                field_map.setdefault(key, field_match.group("mojmap_name"))
                continue

    return class_map, field_map, method_map


def rewrite_tiny(
    yarn_tiny: Path,
    output_tiny: Path,
    class_map: dict[str, str],
    field_map: dict[tuple[str, str, str], str],
    method_map: dict[tuple[str, str, str], str],
) -> Counter:
    stats = Counter()
    current_owner: str | None = None
    output_lines: list[str] = []

    for raw_line in yarn_tiny.read_text().splitlines():
        line = raw_line.rstrip("\n")
        tokens = line.split("\t")

        if tokens[0] == "c":
            current_owner = tokens[1]
            stats["classes_total"] += 1
            mapped = class_map.get(current_owner)
            if mapped:
                tokens[3] = mapped
                stats["classes_mapped"] += 1
            output_lines.append("\t".join(tokens))
            continue

        if len(tokens) > 1 and tokens[1] == "f" and current_owner is not None:
            stats["fields_total"] += 1
            key = (current_owner, tokens[2], tokens[3])
            mapped = field_map.get(key)
            if mapped:
                tokens[5] = mapped
                stats["fields_mapped"] += 1
            output_lines.append("\t".join(tokens))
            continue

        if len(tokens) > 1 and tokens[1] == "m" and current_owner is not None:
            stats["methods_total"] += 1
            key = (current_owner, tokens[2], tokens[3])
            mapped = method_map.get(key)
            if mapped:
                tokens[5] = mapped
                stats["methods_mapped"] += 1
            output_lines.append("\t".join(tokens))
            continue

        output_lines.append(line)

    output_tiny.parent.mkdir(parents=True, exist_ok=True)
    output_tiny.write_text("\n".join(output_lines) + "\n")
    return stats


def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Generate a Tiny v2 mapping whose named namespace uses Mojang names, "
            "starting from a Yarn Tiny v2 mapping and Mojang ProGuard maps."
        )
    )
    parser.add_argument("--yarn-tiny", type=Path, required=True)
    parser.add_argument(
        "--mojmap",
        type=Path,
        nargs="+",
        required=True,
        help="One or more Mojang ProGuard mapping files, usually client.txt and server.txt.",
    )
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    class_map, field_map, method_map = parse_proguard_files(args.mojmap)
    stats = rewrite_tiny(
        args.yarn_tiny,
        args.output,
        class_map,
        field_map,
        method_map,
    )

    print(
        {
            "classes_total": stats["classes_total"],
            "classes_mapped": stats["classes_mapped"],
            "fields_total": stats["fields_total"],
            "fields_mapped": stats["fields_mapped"],
            "methods_total": stats["methods_total"],
            "methods_mapped": stats["methods_mapped"],
            "class_map_entries": len(class_map),
            "field_map_entries": len(field_map),
            "method_map_entries": len(method_map),
        }
    )


if __name__ == "__main__":
    main()
