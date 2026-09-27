#!/usr/bin/env python3
"""Generate assets/meteor-client/lang/en_us.json from the Meteor source tree.

Module/HUD/command names and descriptions are read from their declarations; settings
are read from each `<X>Setting.Builder().name(...).description(...)` chain, scoped by
the owner that the corresponding Settings collection is assigned to at runtime.
"""
import json
import re
import sys
import glob
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src/main/java/meteordevelopment/meteorclient")
OUT = os.path.join(ROOT, "src/main/resources/assets/meteor-client/lang/en_us.json")

METEOR = "meteor-client"
GLOBAL = "global"

# Files whose Settings collection is assigned an explicit owner id in code.
EXPLICIT_OWNER = {
    "systems/config/Config.java": "config",
    "systems/hud/Hud.java": "hud",
    "systems/proxies/Proxies.java": "proxies",
    "systems/proxies/Proxy.java": "proxy",
    "systems/macros/Macro.java": "macro",
    "systems/profiles/Profile.java": "profile",
    "systems/waypoints/Waypoint.java": "waypoint",
    "systems/modules/render/marker/BaseMarker.java": "marker",
    "pathing/BaritoneSettings.java": "baritone",
    "pathing/NopPathManager.java": "nop-path-manager",
    "gui/GuiTheme.java": "gui-theme",
    "systems/hud/screens/HudElementScreen.java": "hud-editor",
    "systems/modules/render/blockesp/ESPBlockDataScreen.java": "esp-block-data",
    # Subclasses inheriting a Settings collection from a parent with an explicit owner
    "gui/themes/meteor/MeteorGuiTheme.java": "gui-theme",
    "systems/modules/render/marker/CuboidMarker.java": "marker",
    "systems/modules/render/marker/Sphere2dMarker.java": "marker",
    "systems/hud/elements/TextHud.java": "text",
}

valid = re.compile(r"^[a-z0-9][a-z0-9-]*(?:\.[a-z0-9][a-z0-9-]*)+$")

# Vanilla-format keys consumed directly by Minecraft's KeyMapping / resource system.
# They must keep their original dotted format and are exempt from the Meteor key schema.
PRESERVED = {
    "key.meteor-client.open-gui": "Open GUI",
    "key.meteor-client.open-commands": "Open Commands",
    "key.category.meteor-client.meteor-client": "Meteor Client",
}


def slug(s):
    s = re.sub(r"[^a-z0-9]+", "-", s.strip().lower())
    s = re.sub(r"^-+|-+$", "", s)
    return s or "unnamed"


def name_to_title(name):
    """Mirror Utils.nameToTitle: split on '-', uppercase the first char of each segment
    and leave the remainder untouched (commons-lang StringUtils.capitalize semantics)."""
    return " ".join(w[:1].upper() + w[1:] for w in name.split("-"))


def rel(f):
    return os.path.relpath(f, SRC).replace(os.sep, "/")


def main():
    entries = dict(PRESERVED)

    def add(key, value):
        if key in entries and entries[key] != value:
            print(f"WARN duplicate key {key}: {entries[key]!r} vs {value!r}", file=sys.stderr)
        entries[key] = value

    files = sorted(glob.glob(os.path.join(SRC, "**", "*.java"), recursive=True))
    unmapped = []

    for f in files:
        t = open(f, encoding="utf-8").read()
        r = rel(f)

        # --- modules ---
        for m in re.finditer(
            r'super\s*\(\s*Categories\.\w+\s*,\s*"([^"]*)"\s*,\s*"([^"]*)"', t
        ):
            mid, desc = m.group(1), m.group(2)
            add(f"{METEOR}.module.{slug(mid)}.name", name_to_title(mid))
            add(f"{METEOR}.module.{slug(mid)}.description", desc)

        # --- hud elements ---
        for m in re.finditer(
            r'new\s+HudElementInfo<[^>]*>\([^,]*,\s*"([^"]*)"\s*,\s*"([^"]*)"', t
        ):
            hid, desc = m.group(1), m.group(2)
            add(f"{METEOR}.hud.{slug(hid)}.name", name_to_title(hid))
            add(f"{METEOR}.hud.{slug(hid)}.description", desc)

        # --- commands ---
        if "/commands/commands/" in f:
            for m in re.finditer(r'super\s*\(\s*"([^"]*)"\s*,\s*"([^"]*)"', t):
                cid, desc = m.group(1), m.group(2)
                add(f"{METEOR}.command.{slug(cid)}.name", name_to_title(cid))
                add(f"{METEOR}.command.{slug(cid)}.description", desc)

        # --- settings ---
        n_builders = len(re.findall(r"new\s+\w*Setting\.Builder", t))
        if n_builders == 0:
            continue

        # determine owner
        mod = re.search(r'super\s*\(\s*Categories\.\w+\s*,\s*"([^"]*)"', t)
        hud = re.search(r'new\s+HudElementInfo<[^>]*>\([^,]*,\s*"([^"]*)"', t)
        if mod:
            owner = mod.group(1)
        elif hud:
            owner = hud.group(1)
        elif r in EXPLICIT_OWNER:
            owner = EXPLICIT_OWNER[r]
        else:
            owner = None
            unmapped.append(r)
        scope = slug(owner) if owner else GLOBAL

        # group variable -> group name
        groupvars = {}
        for m in re.finditer(r"(\w+)\s*=\s*settings\.getDefaultGroup\(\)", t):
            groupvars[m.group(1)] = "General"
        for m in re.finditer(r"(\w+)\s*=\s*settings\.createGroup\(\"([^\"]*)\"", t):
            groupvars[m.group(1)] = m.group(2)

        # each `<group>.add(new <X>Setting.Builder()...build())` block
        for m in re.finditer(r"(\w+)\.add\(\s*new\s+\w*Setting\.Builder", t):
            var = m.group(1)
            gname = groupvars.get(var, "General")
            span = t[m.end():m.end() + 2000]
            b = span.find(".build()")
            block = span[: b + 7] if b >= 0 else span
            nm = re.search(r'\.name\("([^"]*)"\)', block)
            dm = re.search(r'\.description\("([^"]*)"\)', block)
            if not nm:
                continue
            gslug = slug(gname)
            add(f"{METEOR}.setting.{scope}.{gslug}.{slug(nm.group(1))}.name", name_to_title(nm.group(1)))
            if dm:
                add(f"{METEOR}.setting.{scope}.{gslug}.{slug(nm.group(1))}.description", dm.group(1))

    # --- categories ---
    cats = os.path.join(SRC, "systems/modules/Categories.java")
    ct = open(cats, encoding="utf-8").read()
    for m in re.finditer(r'new\s+Category\("([^"]*)"', ct):
        add(f"{METEOR}.category.{slug(m.group(1))}.name", m.group(1))

    # --- validate + write ---
    bad = [k for k in entries if not valid.match(k)]
    for k in bad:
        print(f"INVALID KEY: {k}", file=sys.stderr)
    if unmapped:
        print("UNMAPPED SETTINGS FILES (owner unknown):", file=sys.stderr)
        for u in sorted(set(unmapped)):
            print("  " + u, file=sys.stderr)
    if bad or unmapped:
        sys.exit(2)

    ordered = {k: entries[k] for k in sorted(entries)}
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(ordered, fh, indent=2, ensure_ascii=False)
        fh.write("\n")
    print(f"Wrote {len(ordered)} keys to {os.path.relpath(OUT, ROOT)}")


if __name__ == "__main__":
    main()
