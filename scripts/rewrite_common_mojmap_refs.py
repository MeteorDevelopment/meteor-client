#!/usr/bin/env python3

from __future__ import annotations

import re
from pathlib import Path


IMPORT_REPLACEMENTS = {
    "import net.minecraft.item.*;": "import net.minecraft.world.item.*;",
    "import net.minecraft.block.*;": "import net.minecraft.world.level.block.*;",
    "import net.minecraft.block.entity.*;": "import net.minecraft.world.level.block.entity.*;",
    "import net.minecraft.entity.*;": "import net.minecraft.world.entity.*;",
    "import net.minecraft.entity.projectile.*;": "import net.minecraft.world.entity.projectile.*;",
    "import net.minecraft.entity.projectile.thrown.*;": "import net.minecraft.world.entity.projectile.*;",
    "import net.minecraft.text.*;": "import net.minecraft.network.chat.*;",
    "import net.minecraft.screen.*;": "import net.minecraft.world.inventory.*;",
    "import net.minecraft.client.render.*;": "import net.minecraft.client.renderer.*;",
    "import net.minecraft.network.packet.c2s.play.*;": "import net.minecraft.network.protocol.game.*;",
    "import net.minecraft.network.packet.s2c.play.*;": "import net.minecraft.network.protocol.game.*;",
    "import net.minecraft.client.gui.screen.option.*;": "import net.minecraft.client.gui.screens.options.*;",
    "import net.minecraft.client.gui.screen.world.*;": "import net.minecraft.client.gui.screens.worldselection.*;",
}

STRING_REPLACEMENTS = {
    "net.minecraft.level.": "net.minecraft.world.",
    "Minecraft.getInstance().getNetworkHandler()": "Minecraft.getInstance().getConnection()",
    "Minecraft.getInstance().getSession()": "Minecraft.getInstance().getUser()",
    "Minecraft.getInstance().world": "Minecraft.getInstance().level",
    "keyboardHandlerHandler": "keyboardHandler",
    "mouseHandlerHandler": "mouseHandler",
    "getConnection().getConnection()": "getConnection()",
}

TOKEN_REPLACEMENTS = [
    (re.compile(r"(?<![\w.])mc\.interactionManager\b"), "mc.gameMode"),
    (re.compile(r"(?<![\w.])minecraft\.interactionManager\b"), "minecraft.gameMode"),
    (re.compile(r"(?<![\w.])mc\.worldRenderer\b"), "mc.levelRenderer"),
    (re.compile(r"(?<![\w.])minecraft\.worldRenderer\b"), "minecraft.levelRenderer"),
    (re.compile(r"(?<![\w.])mc\.textRenderer\b"), "mc.font"),
    (re.compile(r"(?<![\w.])minecraft\.textRenderer\b"), "minecraft.font"),
    (re.compile(r"(?<![\w.])mc\.mouse\b"), "mc.mouseHandler"),
    (re.compile(r"(?<![\w.])minecraft\.mouse\b"), "minecraft.mouseHandler"),
    (re.compile(r"(?<![\w.])mc\.keyboard\b"), "mc.keyboardHandler"),
    (re.compile(r"(?<![\w.])minecraft\.keyboard\b"), "minecraft.keyboardHandler"),
    (re.compile(r"(?<![\w.])mc\.currentScreen\b"), "mc.screen"),
    (re.compile(r"(?<![\w.])minecraft\.currentScreen\b"), "minecraft.screen"),
    (re.compile(r"(?<![\w.])mc\.world\b"), "mc.level"),
    (re.compile(r"(?<![\w.])minecraft\.world\b"), "minecraft.level"),
    (re.compile(r"(?<![\w.])mc\.getNetworkHandler\(\)"), "mc.getConnection()"),
    (re.compile(r"(?<![\w.])minecraft\.getNetworkHandler\(\)"), "minecraft.getConnection()"),
    (re.compile(r"(?<![\w.])mc\.getSession\(\)"), "mc.getUser()"),
    (re.compile(r"(?<![\w.])minecraft\.getSession\(\)"), "minecraft.getUser()"),
    (re.compile(r"(?<![\w.])mc\.getServer\(\)"), "mc.getSingleplayerServer()"),
    (re.compile(r"(?<![\w.])minecraft\.getServer\(\)"), "minecraft.getSingleplayerServer()"),
    (re.compile(r"(?<![\w.])mc\.getCurrentServerEntry\(\)"), "mc.getCurrentServer()"),
    (re.compile(r"(?<![\w.])minecraft\.getCurrentServerEntry\(\)"), "minecraft.getCurrentServer()"),
    (re.compile(r"(?<![\w.])mc\.getSkinProvider\(\)"), "mc.getSkinManager()"),
    (re.compile(r"(?<![\w.])minecraft\.getSkinProvider\(\)"), "minecraft.getSkinManager()"),
    (re.compile(r"(?<![\w.])mc\.getApiServices\(\)"), "mc.services()"),
    (re.compile(r"(?<![\w.])minecraft\.getApiServices\(\)"), "minecraft.services()"),
]


def main() -> None:
    root = Path("src/main/java")
    changed_files = 0

    for path in root.rglob("*.java"):
        original = path.read_text()
        updated = original

        for old, new in STRING_REPLACEMENTS.items():
            updated = updated.replace(old, new)

        for old, new in IMPORT_REPLACEMENTS.items():
            updated = updated.replace(old, new)

        for pattern, replacement in TOKEN_REPLACEMENTS:
            updated = pattern.sub(replacement, updated)

        if updated != original:
            path.write_text(updated)
            changed_files += 1

    print({"changed_files": changed_files})


if __name__ == "__main__":
    main()
