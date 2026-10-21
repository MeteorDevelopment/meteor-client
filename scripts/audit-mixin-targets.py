#!/usr/bin/env python3
"""Audit the mixins against a Minecraft version.

A mixin that targets something which no longer exists compiles fine and then throws while the
game starts, so compiling is not enough. This script checks, for every mixin:

  1. that the target class of @Mixin exists,
  2. that the mixin has the kind Mixin requires of its target,
  3. that every @Shadow field and method exists on the target,
  4. that every injected / overwritten method exists on the target,
  5. that every @Accessor / @Invoker member exists on the target,
  6. that every @At(INVOKE) / @At(FIELD) target still resolves,
  7. that the handler of an injector takes the arguments of the method it injects into,
  8. that a static method is injected into with a static handler.

Usage:
    scripts/audit-mixin-targets.py <version> [<root> ...]

The version is a Minecraft version id ("26.4-snapshot-4"), not the Meteor version key
("26.4"). The roots default to the shared sources plus the version specific tree.

The mapped (deobfuscated) Minecraft jar is read from the Fabric Loom cache:
    ~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/<version>/
"""
import os
import re
import subprocess
import sys
import zipfile
from pathlib import Path

CACHE = os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf")

MIXIN_RE = re.compile(r"@Mixin\s*\(\s*(?:value\s*=\s*)?([\w.]+)\.class")
MIXIN_TARGETS_RE = re.compile(r'@Mixin\s*\([^)]*targets\s*=\s*"([\w.$]+)"')
METHOD_RE = re.compile(r'method\s*=\s*"([^"]*)"')
METHOD_LIST_RE = re.compile(r'method\s*=\s*\{([^}]*)\}')
DESCRIPTOR_RE = re.compile(r'target\s*=\s*"(L[^"]+;)"')
IMPORT_RE = re.compile(r"^import\s+(?:static\s+)?([\w.]+)\s*;", re.M)
ACCESSOR_RE = re.compile(r"@(Accessor|Invoker)\s*\(\s*(?:value\s*=\s*)?\"([\w$<>]+)\"")
ACCESSOR_RE_BARE = re.compile(r"@(Accessor|Invoker)\s*$")
TYPE_RE = re.compile(r"^(?:public\s+)?(?:abstract\s+|final\s+)?(class|interface|enum)\s+(\w+)", re.M)

MODIFIERS = ("public", "private", "protected", "static", "final", "abstract", "synchronized",
             "native", "default", "transient", "volatile", "strictfp")

# the words that start a statement and are not a declaration
KEYWORDS = ("return", "throw", "new ", "case ", "if ", "if(", "else ", "else{", "for ", "for(",
            "while ", "while(", "switch ", "switch(", "catch ", "catch(", "do ", "do{", "try ",
            "try{", "assert ", "assert(", "yield ", "break", "continue", "instanceof", "} else",
            "} catch", "} finally", "default:")

# the injectors of sponge mixin, the handler takes the arguments of the target method
ARG_INJECTORS = ("Inject", "Redirect")
# the injectors of sponge mixin, the handler takes the value and a callback
VALUE_INJECTORS = ("ModifyArg", "ModifyArgs", "ModifyConstant", "ModifyVariable")
# the injectors of mixin extras, their handlers have their own rules
EXTRAS_INJECTORS = ("ModifyExpressionValue", "ModifyReturnValue", "ModifyReceiver", "WrapWithCondition")
ALL_INJECTORS = ARG_INJECTORS + VALUE_INJECTORS + EXTRAS_INJECTORS + ("Overwrite", "WrapOperation")


LOCAL_RE = re.compile(r'@(Local|Share)\s*\(\s*(?:name\s*=\s*)?"([^"]+)"')
DECOMPILE_CACHE = os.path.expanduser(
    "~/.gradle/caches/fabric-loom/decompile/v1.zip")


def decompiled_sources():
    """The decompiled sources of Minecraft, by class name.

    Loom keeps them in a cache of files that hold the name of the class and its source. The
    sources have the names of the parameters and the local variables, which the jar itself does
    not have, so they are the only way to check the @Local captures.
    """
    if not os.path.isfile(DECOMPILE_CACHE):
        return {}

    import struct
    import zipfile

    sources = {}
    with zipfile.ZipFile(DECOMPILE_CACHE) as archive:
        for entry in archive.namelist():
            data = archive.read(entry)
            if not data.startswith(b"LOOM"):
                continue

            name = source = None
            for key in (b"NAME", b"SRC"):
                index = data.find(key)
                if index < 0:
                    continue
                length = struct.unpack(">I", data[index + 4:index + 8])[0]
                value = data[index + 8:index + 8 + length]
                if key == b"NAME":
                    name = value.decode("latin1")
                else:
                    source = value.decode("utf-8", "ignore")

            if name and source:
                sources.setdefault(name, []).append(source)

    return sources


def source_methods(source, method):
    """The declaration and the body of every overload of a method of a decompiled source."""
    found = []

    for match in re.finditer(r"^\s+(?:public|private|protected)[^\n]*\b" + re.escape(method) + r"\s*\(",
                             source, re.M):
        declaration = source[match.start():source.find(")", match.end()) + 1]
        start = source.find("{", match.end())
        if start < 0:
            continue

        depth = 0
        for index in range(start, len(source)):
            if source[index] == "{":
                depth += 1
            elif source[index] == "}":
                depth -= 1
                if depth == 0:
                    found.append((declaration, source[start:index + 1]))
                    break

    return found


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


def is_interface(javap_output):
    return any(re.match(r"(?:public |abstract |static |final )*interface ", line.strip())
               for line in javap_output.splitlines())


def descriptor_of(line, method):
    """The internal descriptor of a javap method line."""
    match = re.search(r"\b" + re.escape(method) + r"\(", line)
    if match is None:
        return None

    head = line[:match.start()].strip().split()
    return_type = head[-1] if head else "void"

    start = line.find("(", match.start())
    end = line.find(")", start)
    if start < 0 or end < 0:
        return None

    args = split_parameters(line[start + 1:end])
    return "(" + to_descriptor(args) + ")" + to_descriptor([return_type])


def method_lines(javap_output, method):
    """The javap lines of a method, for every overload of it."""
    lines = []
    for line in javap_output.splitlines():
        stripped = line.strip()
        if re.search(r"\b" + re.escape(method) + r"\(", stripped):
            lines.append(stripped)
    return lines


def arity_of(line):
    """The amount of arguments of a javap method line."""
    start = line.find("(")
    end = line.find(")", start)
    if start < 0 or end < 0:
        return None
    args = line[start + 1:end].strip()
    if not args:
        return 0
    return len([a for a in args.split(",") if a.strip()])


def is_static_line(line):
    return "static" in line[:line.find("(")]


def argument_types_of(line):
    """The types of the arguments of a javap method line."""
    start = line.find("(")
    end = line.find(")", start)
    if start < 0 or end < 0:
        return None
    args = line[start + 1:end].strip()
    if not args:
        return []
    return [simple_name(a) for a in args.split(",") if a.strip()]


PRIMITIVES = {"I": "int", "J": "long", "F": "float", "D": "double", "Z": "boolean",
              "B": "byte", "C": "char", "S": "short", "V": "void"}


def descriptor_types(descriptor):
    """The types of the arguments of a method descriptor."""
    start = descriptor.find("(")
    end = descriptor.find(")", start)
    if start < 0 or end < 0:
        return []

    types = []
    args = descriptor[start + 1:end]
    i = 0
    while i < len(args):
        if args[i] == "L":
            end_of_class = args.index(";", i)
            types.append(simple_name(args[i + 1:end_of_class].replace("/", ".")))
            i = end_of_class + 1
        elif args[i] == "[":
            dimensions = 0
            while i < len(args) and args[i] == "[":
                dimensions += 1
                i += 1
            if i < len(args):
                if args[i] == "L":
                    end_of_class = args.index(";", i)
                    name = simple_name(args[i + 1:end_of_class].replace("/", "."))
                    i = end_of_class + 1
                else:
                    name = PRIMITIVES.get(args[i], args[i])
                    i += 1
                types.append(name + "[]" * dimensions)
        else:
            types.append(PRIMITIVES.get(args[i], args[i]))
            i += 1
    return types


PRIMITIVE_DESCRIPTORS = {"int": "I", "long": "J", "float": "F", "double": "D", "boolean": "Z",
                          "byte": "B", "char": "C", "short": "S", "void": "V"}


def to_descriptor(types):
    """The internal descriptor of a list of java types."""
    descriptors = ""

    for type_name in types:
        type_name = type_name.strip().rstrip(";")
        if "<" in type_name:
            type_name = type_name[:type_name.index("<")]

        dimensions = 0
        while type_name.endswith("[]"):
            dimensions += 1
            type_name = type_name[:-2].strip()

        if type_name in PRIMITIVE_DESCRIPTORS:
            descriptors += PRIMITIVE_DESCRIPTORS[type_name]
        else:
            descriptors += "L" + type_name.replace(".", "/") + ";"

        descriptors += "[" * dimensions

    return descriptors


def simple_name(type_name):
    """The name of a type without the package and the generics."""
    type_name = type_name.strip()
    if "<" in type_name:
        type_name = type_name[:type_name.index("<")]
    if "." in type_name:
        type_name = type_name[type_name.rindex(".") + 1:]
    if "$" in type_name:
        type_name = type_name[type_name.rindex("$") + 1:]
    return type_name





class Jar:
    """The mapped Minecraft jar, with a cache of the javap output."""

    def __init__(self, path, out_dir):
        self.archive = zipfile.ZipFile(path)
        self.out_dir = out_dir
        os.makedirs(out_dir, exist_ok=True)
        self.classes = set(n[:-6].replace("/", ".") for n in self.archive.namelist() if n.endswith(".class"))
        self.cache = {}

    def javap(self, fqn):
        if fqn not in self.cache:
            entry = fqn.replace(".", "/") + ".class"
            if entry not in self.archive.namelist():
                self.cache[fqn] = ""
            else:
                self.archive.extract(entry, self.out_dir)
                result = subprocess.run(["javap", "-p", "-c", "-s", "-classpath", self.out_dir, fqn],
                                        capture_output=True, text=True)
                self.cache[fqn] = result.stdout
        return self.cache[fqn]

    def hierarchy(self, fqn):
        """The class and its superclasses."""
        seen = set()
        current = fqn
        while current and current not in seen:
            seen.add(current)
            output = self.javap(current)
            if not output:
                return
            yield current, output
            current = superclass(output)

    def find(self, fqn, member):
        """Look a member up in the class and in its superclasses."""
        for _, output in self.hierarchy(fqn):
            if member in members(output):
                return output
        return None

    def arities(self, fqn, method):
        """The amount of arguments of every overload of a method."""
        found = []
        for _, output in self.hierarchy(fqn):
            for line in method_lines(output, method):
                count = arity_of(line)
                if count is not None:
                    found.append((count, is_static_line(line)))
        return found

    def related(self, owner, other):
        """Whether the two owners are the same class or one of them inherits the other."""
        if owner is None or other is None:
            return True
        if owner == other:
            return True

        first = owner.replace(".", "/")
        second = other.replace(".", "/")
        for cls, _ in self.hierarchy(first.replace("/", ".")):
            if cls.replace(".", "/") == second:
                return True
        for cls, _ in self.hierarchy(second.replace("/", ".")):
            if cls.replace(".", "/") == first:
                return True
        return False

    def body(self, fqn, method):
        """The calls, the fields and the strings of a method."""
        for _, output in self.hierarchy(fqn):
            lines = method_body(output, method)
            if not lines:
                continue

            calls = []
            fields = []
            strings = []
            for line in lines:
                match = CALL_RE.search(line)
                if match:
                    calls.append((match.group(1), match.group(2), match.group(3)))
                match = FIELD_CALL_RE.search(line)
                if match:
                    fields.append((match.group(1), match.group(2)))
                match = STRING_RE.search(line)
                if match:
                    strings.append(match.group(1))

            return {"calls": calls, "fields": fields, "strings": strings}
        return None

    def method_descriptors(self, fqn, method):
        """The descriptors of every overload of a method."""
        found = []

        for _, output in self.hierarchy(fqn):
            found.extend(method_descriptors_of(output, method))

        return found

    def argument_types(self, fqn, method):
        """The types of the arguments of every overload of a method."""
        found = []
        for _, output in self.hierarchy(fqn):
            for line in method_lines(output, method):
                types = argument_types_of(line)
                if types is not None:
                    found.append(types)
        return found

    def static(self, fqn, method):
        """Whether the method is static, for every overload of it."""
        return any(is_static for _, is_static in self.arities(fqn, method))


def split_parameters(text):
    """Split a parameter list on the commas that are not inside brackets."""
    parts = []
    depth = 0
    current = ""

    for char in text:
        if char in "([":
            depth += 1
        elif char in ")]":
            depth -= 1

        if char == "," and depth == 0:
            parts.append(current)
            current = ""
        else:
            current += char

    if current.strip():
        parts.append(current)
    return [p.strip() for p in parts if p.strip()]


def type_of(parameter):
    """The type of a parameter, without its annotations and its name."""
    parameter = re.sub(r"@\w+(\([^)]*\))?\s*", "", parameter).strip()
    words = parameter.split()
    if len(words) > 1:
        parameter = " ".join(words[:-1])
    return simple_name(parameter)


def parse_declaration(line):
    """The modifiers, the name and the parameters of a declaration, if it is one."""
    stripped = line.strip()
    if not stripped or stripped.startswith(KEYWORDS):
        return None

    open_paren = stripped.find("(")
    if open_paren < 0:
        return None

    depth = 0
    close_paren = -1
    for index in range(open_paren, len(stripped)):
        if stripped[index] == "(":
            depth += 1
        elif stripped[index] == ")":
            depth -= 1
            if depth == 0:
                close_paren = index
                break
    if close_paren < 0:
        return None

    tail = stripped[close_paren + 1:].strip()
    if tail and not tail.startswith(("{", ";", "throws")):
        return None

    words = stripped[:open_paren].strip().split()
    if len(words) < 2:
        return None

    name = words[-1]
    if not re.fullmatch(r"[\w$]+", name):
        return None

    modifiers = []
    while words and words[0] in MODIFIERS:
        modifiers.append(words.pop(0))
    if not words:
        return None

    return modifiers, name, split_parameters(stripped[open_paren + 1:close_paren])


def parse_field(line):
    """The modifiers and the name of a field, if it is one."""
    stripped = line.strip()
    if not stripped or stripped.startswith(KEYWORDS) or "(" in stripped or not stripped.endswith(";"):
        return None

    body = stripped[:-1].strip()
    if "=" in body:
        body = body[:body.index("=")].strip()

    words = body.split()
    if len(words) < 2:
        return None

    name = words[-1]
    if not re.fullmatch(r"[\w$]+", name):
        return None

    modifiers = []
    while words and words[0] in MODIFIERS:
        modifiers.append(words.pop(0))

    return modifiers, name


CALL_RE = re.compile(r"// (?:Interface)?Method (?:([\w/$]+)\.)?(\w+):(\([^)]*\).*)")
FIELD_CALL_RE = re.compile(r"// Field (?:([\w/$]+)\.)?(\w+):(\S+)")
STRING_RE = re.compile(r"// String (\S+)")


def method_body(output, method):
    """The instructions of a method, as the lines of the javap output."""
    lines = output.splitlines()
    body = []
    inside = False

    for line in lines:
        if re.match(r"\s{2}[\w $<>\[\],.?]*\s\w+\(", line):
            inside = re.search(r"\b" + re.escape(method) + r"\(", line) is not None
            if inside:
                continue
        if inside:
            if re.match(r"^\s{2}\S", line) and not line.strip().startswith("Code:"):
                break
            body.append(line)

    return body


def method_descriptors_of(output, method):
    """The descriptors of every overload of a method, without the generics."""
    found = []
    lines = output.splitlines()
    inside = False

    for line in lines:
        if re.match(r"\s{2}[\w $<>\[\],.?]*\s\w+\(", line):
            inside = re.search(r"\b" + re.escape(method) + r"\(", line) is not None
            continue
        if inside:
            match = re.match(r"\s+descriptor: (\S+)", line)
            if match:
                found.append(match.group(1))
                inside = False

    return found


def collect(text, class_name):
    """The declarations of a java file, with the annotations that belong to them.

    Yields (annotations, kind, name, modifiers, parameter count, parameter types, parameters).
    """
    lines = text.splitlines()
    pending = []
    i = 0

    while i < len(lines):
        stripped = lines[i].strip()

        if stripped.startswith("@"):
            start = i
            depth = stripped.count("(") - stripped.count(")")
            while depth > 0 and i + 1 < len(lines):
                i += 1
                depth += lines[i].count("(") - lines[i].count(")")
            pending.append("\n".join(lines[start:i + 1]))
            i += 1
            continue

        if stripped and not stripped.startswith(("//", "*", "/*")):
            method = parse_declaration(lines[i])
            if method and method[1] == class_name:
                pending = []  # a constructor
            elif method:
                annotations = pending
                pending = []
                modifiers, name, params = method
                yield (annotations, "method", name, modifiers, len(params),
                       [type_of(p) for p in params], ", ".join(params))
            else:
                field = parse_field(lines[i])
                if field:
                    annotations = pending
                    pending = []
                    yield (annotations, "field", field[1], field[0], 0, [], "")
        i += 1


def injector_of(annotations):
    """The injector of a declaration, if it has one."""
    for annotation in annotations:
        for name in ALL_INJECTORS:
            if re.search(r"@" + name + r"\b", annotation):
                return name
    return None


def target_methods(annotations, mixin_targets):
    """The methods an injector injects into, with the descriptor the mixin names."""
    methods = {}

    def add(value):
        if not value:
            return
        name = value.split("(")[0]
        if name.startswith("L") and ";" in name:
            value = value[value.index(";") + 1:]
            name = value.split("(")[0]
        methods.setdefault(name, value[value.index("("):] if "(" in value else None)

    for annotation in annotations:
        for match in METHOD_RE.finditer(annotation):
            add(match.group(1))
        for match in METHOD_LIST_RE.finditer(annotation):
            for part in match.group(1).split(","):
                add(part.strip().strip('"'))
        for match in re.finditer(r"MixinTargets\.([A-Z_]+)", annotation):
            value = mixin_targets.get(match.group(1))
            if value and not value.startswith("L"):
                add(value)

    return methods


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    version = sys.argv[1]
    roots = sys.argv[2:] or ["src/main/java"]

    jar_file = jar(version)
    out_dir = f"/tmp/meteor-mixin-audit-{version}"
    minecraft = Jar(jar_file, out_dir)

    problems = []
    sources = decompiled_sources()
    descriptor_targets = []
    checked_methods = 0
    checked_targets = 0
    checked_handlers = 0

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
                targets = []
                for match in MIXIN_RE.finditer(text):
                    target = match.group(1)
                    fqn = target if "." in target else imports.get(target)
                    if not fqn:
                        continue
                    targets.append(fqn)
                    if fqn.startswith(("net.minecraft", "com.mojang")) and fqn not in minecraft.classes:
                        problems.append(f"{relative}: mixin target class {fqn} does not exist")
                for match in MIXIN_TARGETS_RE.finditer(text):
                    target = match.group(1)
                    if target.startswith(("net.minecraft", "com.mojang")) and target not in minecraft.classes:
                        problems.append(f"{relative}: mixin target class {target} does not exist")

                # the kind of the mixin and the methods it declares
                type_match = TYPE_RE.search(text)
                kind = type_match.group(1) if type_match else "class"
                class_name = type_match.group(2) if type_match else ""
                declarations = list(collect(text, class_name))
                methods = [d for d in declarations if d[1] == "method"]
                accessor_only = all(any(ACCESSOR_RE.search(a) or ACCESSOR_RE_BARE.search(a)
                                        for a in d[0]) for d in methods)

                # 2. the kind of the target
                for fqn in targets:
                    if fqn not in minecraft.classes:
                        continue
                    target_is_interface = is_interface(minecraft.javap(fqn))
                    if kind == "interface" and not accessor_only:
                        if not target_is_interface:
                            problems.append(f"{relative}: has methods of its own, so it must target an "
                                            f"interface, but {fqn} is a class")
                    elif target_is_interface:
                        problems.append(f"{relative}: targets {fqn} (an interface) but the mixin is a "
                                        f"{kind}")

                # 6. the injection points have to be in the bytecode of the target
                for annotations, declaration_kind, member, modifiers, count, types, param_types in declarations:
                    if declaration_kind != "method":
                        continue
                    if injector_of(annotations) is None:
                        continue

                    wanted = sorted(target_methods(annotations, mixin_targets))
                    if not wanted:
                        continue

                    for annotation in annotations:
                        # an invoke target
                        for match in DESCRIPTOR_RE.finditer(annotation):
                            descriptor = match.group(1)
                            semicolon = descriptor.index(";")
                            owner = descriptor[1:semicolon]
                            rest = descriptor[semicolon + 1:]
                            if "(" not in rest:
                                continue  # a field, not a call
                            called = rest.split("(")[0]
                            arg_types = rest[rest.find("(") + 1:rest.rindex(")")]

                            for fqn in targets:
                                if fqn not in minecraft.classes:
                                    continue
                                for target_method in wanted:
                                    body = minecraft.body(fqn, target_method)
                                    if body is None:
                                        continue
                                    calls = [c for c in body["calls"]
                                             if c[1] == called
                                             and c[2].startswith("(" + arg_types)
                                             and minecraft.related(owner, c[0])]
                                    if not calls:
                                        problems.append(
                                            f"{relative}: {fqn}#{target_method} does not call "
                                            f"{owner.replace('/', '.')}.{called}({arg_types})")
                                        continue
                                    ordinal = re.search(r"ordinal\s*=\s*(\d+)", annotation)
                                    if ordinal and int(ordinal.group(1)) >= len(calls):
                                        problems.append(
                                            f"{relative}: {fqn}#{target_method} calls "
                                            f"{owner.replace('/', '.')}.{called} {len(calls)} times, "
                                            f"but the injection wants the one with the ordinal "
                                            f"{ordinal.group(1)}")

                        # a field target
                        for match in re.finditer(r'value\s*=\s*"FIELD"[^@]*?target\s*=\s*"L([^"]+);(\w+):(\S+)"',
                                                 annotation):
                            owner, field = match.group(1), match.group(2)
                            for fqn in targets:
                                if fqn not in minecraft.classes:
                                    continue
                                for target_method in wanted:
                                    body = minecraft.body(fqn, target_method)
                                    if body is None:
                                        continue
                                    fields = [c for c in body["fields"]
                                              if c[1] == field and minecraft.related(owner, c[0])]
                                    if not fields:
                                        problems.append(
                                            f"{relative}: {fqn}#{target_method} does not touch the field "
                                            f"{owner.replace('/', '.')}.{field}")

                        # a string argument
                        for match in re.finditer(r'args\s*=\s*"ldc=([^"]+)"', annotation):
                            string = match.group(1)
                            for fqn in targets:
                                if fqn not in minecraft.classes:
                                    continue
                                for target_method in wanted:
                                    body = minecraft.body(fqn, target_method)
                                    if body is None:
                                        continue
                                    if string not in body["strings"]:
                                        problems.append(
                                            f"{relative}: {fqn}#{target_method} does not use the string "
                                            f"\"{string}\"")

                # 4. the injected methods
                injected = {m.group(1).split("(")[0] if "(" not in m.group(1) else m.group(1)
                            for m in METHOD_RE.finditer(text)}
                for m in re.finditer(r"MixinTargets\.([A-Z_]+)", text):
                    value = mixin_targets.get(m.group(1))
                    if value is None:
                        problems.append(f"{relative}: MixinTargets.{m.group(1)} is not defined")
                    elif value.startswith("L") and ";" in value:
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

                for fqn in targets:
                    if fqn not in minecraft.classes:
                        continue
                    for method in sorted(injected):
                        checked_methods += 1
                        if method == "<init>":
                            continue  # constructors are always there, javap does not name them

                        name = method.split("(")[0]
                        if name.startswith("L") and ";" in name:
                            # the whole member is named: Lowner/Class;member(args)ret
                            name = name[name.index(";") + 1:].split("(")[0]
                            method = method[method.index(";") + 1:]
                        if minecraft.find(fqn, name) is None:
                            problems.append(f"{relative}: {fqn}#{method} does not exist")
                            continue

                        # the mixin names the descriptor, so it has to be the one of the method
                        if "(" in method:
                            descriptor = "(" + method[method.index("(") + 1:]
                            descriptors = minecraft.method_descriptors(fqn, name)
                            if descriptors and descriptor not in descriptors:
                                problems.append(f"{relative}: {fqn}#{name} is {descriptors}, "
                                                f"but the mixin wants {descriptor}")

                for annotations, declaration_kind, member, modifiers, count, types, param_types in declarations:
                    # 5. accessors and invokers
                    for annotation in annotations:
                        accessor = ACCESSOR_RE.search(annotation)
                        if accessor:
                            accessor_name = accessor.group(2).split("(")[0]
                            for fqn in targets:
                                if fqn in minecraft.classes and minecraft.find(fqn, accessor_name) is None:
                                    problems.append(f"{relative}: {fqn}#{accessor_name} does not exist")

                    # 3. shadows
                    if not any("@Shadow" in a for a in annotations):
                        continue
                    if declaration_kind == "field":
                        for fqn in targets:
                            if fqn in minecraft.classes and minecraft.find(fqn, member) is None:
                                problems.append(f"{relative}: shadowed field {fqn}#{member} does not exist")
                    else:
                        for fqn in targets:
                            if fqn not in minecraft.classes:
                                continue
                            if minecraft.find(fqn, member) is None:
                                problems.append(f"{relative}: shadowed method {fqn}#{member} does not exist")

                # 7. and 8. the handlers of the injectors
                for annotations, declaration_kind, member, modifiers, count, types, param_types in declarations:
                    if declaration_kind != "method":
                        continue

                    injector = injector_of(annotations)
                    if injector is None:
                        continue

                    methods = target_methods(annotations, mixin_targets)
                    if not methods:
                        continue

                    # when the injection point is an invoke, the handler takes the
                    # arguments of the called method
                    invoked = None
                    for annotation in annotations:
                        for match in DESCRIPTOR_RE.finditer(annotation):
                            descriptor = match.group(1)
                            semicolon = descriptor.index(";")
                            owner = descriptor[1:semicolon].replace("/", ".")
                            called = descriptor[semicolon + 1:].split("(")[0]
                            types_of_call = descriptor_types(descriptor)
                            if types_of_call:
                                invoked = (owner, called, types_of_call)

                    handler_static = "static" in modifiers
                    for fqn in targets:
                        if fqn not in minecraft.classes:
                            continue
                        for target_method in sorted(methods):
                            if target_method.startswith("lambda$"):
                                continue  # a lambda has more arguments in the bytecode
                            arities = minecraft.arities(fqn, target_method)
                            if not arities:
                                continue
                            checked_handlers += 1

                            # 8. a static method needs a static handler
                            if injector in ("Inject", "Redirect"):
                                if len(arities) == 1 and arities[0][1] and not handler_static:
                                    problems.append(f"{relative}: {member} must be static, "
                                                    f"{fqn}#{target_method} is static")

                            # 7. the arguments of the handler
                            if injector == "Inject":
                                # the handler takes the arguments of the target, a callback and
                                # maybe captured locals, or only the callback
                                matched = False
                                wanted_descriptor = target_methods(annotations, mixin_targets).get(target_method)
                                for descriptor in minecraft.method_descriptors(fqn, target_method):
                                    if wanted_descriptor and descriptor != wanted_descriptor:
                                        continue

                                    target_types = descriptor_types(descriptor)
                                    arity = len(target_types)

                                    if re.search(r"CallbackInfo", types[0]):
                                        # the simple form: only the callback and the locals
                                        matched = True
                                        break

                                    if count < arity + 1:
                                        continue
                                    if not re.search(r"CallbackInfo", types[arity]):
                                        continue

                                    matched = True
                                    for index, (mine, theirs) in enumerate(zip(types[:arity], target_types)):
                                        # a type variable of the target is erased to its bound
                                        if len(mine) == 1 and mine.isupper():
                                            continue
                                        if mine != theirs:
                                            problems.append(
                                                f"{relative}: {member} takes {mine} as its argument "
                                                f"{index + 1}, {fqn}#{target_method} takes {theirs}")
                                    break

                                if not matched:
                                    problems.append(f"{relative}: {member} takes {count} arguments, "
                                                    f"{fqn}#{target_method} takes "
                                                    f"{[arity for arity, _ in arities]} and a callback")
                            elif injector == "Overwrite":
                                if count not in {arity for arity, _ in arities}:
                                    problems.append(f"{relative}: {member} takes {count} arguments, "
                                                    f"{fqn}#{target_method} takes "
                                                    f"{[arity for arity, _ in arities]}")

                # the @Local captures: the local has to exist in the target method
                if sources:
                    for annotations, declaration_kind, member, modifiers, count, types, param_types in declarations:
                        if declaration_kind != "method":
                            continue

                        captures = set(name for kind, name in LOCAL_RE.findall(param_types) if kind == "Local")
                        if not captures:
                            continue

                        for fqn in targets:
                            variants = sources.get(fqn.replace(".", "/"), [])
                            if not variants:
                                continue

                            for target_method in sorted(target_methods(annotations, mixin_targets)):
                                for capture in sorted(captures):
                                    found = False
                                    for source in variants:
                                        for declaration, body in source_methods(source, target_method):
                                            if re.search(r"\b" + re.escape(capture) + r"\b", body):
                                                found = True
                                                break
                                        if found:
                                            break
                                    if not found:
                                        problems.append(f"{relative}: {fqn}#{target_method} has no local "
                                                        f"\"{capture}\" to capture")


    for owner, member in descriptor_targets:
        checked_targets += 1
        if owner not in minecraft.classes:
            problems.append(f"target class {owner} does not exist")
        elif minecraft.find(owner, member) is None:
            problems.append(f"target {owner}#{member} does not exist")

    print(f"Minecraft {version}: {checked_methods} injected methods, {checked_targets} invoke targets, "
          f"{checked_handlers} handlers checked")
    if not problems:
        print("  OK: every mixin target resolves")
        return 0

    for problem in sorted(set(problems)):
        print(f"  {problem}")
    print(f"  {len(set(problems))} problem(s)")
    return 1


if __name__ == "__main__":
    sys.exit(main())
