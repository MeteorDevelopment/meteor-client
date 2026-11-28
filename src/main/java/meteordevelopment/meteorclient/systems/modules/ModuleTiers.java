/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import java.util.HashMap;
import java.util.Map;

/**
 * Cheat classification of every module that ships with Meteor.
 *
 * <p>The rule used for the classification:
 * <ul>
 *     <li>{@link ModuleTier#LEGIT} - only changes rendering, the GUI, chat or network handling on your side.
 *     Other players can not tell the difference to an unmodded client.</li>
 *     <li>{@link ModuleTier#BORDERLINE} - gives an advantage, but only through information the server already sends
 *     (ESP, Xray, Trajectories, ...) or through automating something you could do by hand faster
 *     (AutoEat, AutoTool, AutoWalk, ...). Other players can not tell that a module is running.</li>
 *     <li>{@link ModuleTier#CHEAT} - lets you do something vanilla does not allow, or does something automatically
 *     that other players would call cheating (Flight, Speed, Reach, KillAura, Freecam, AutoTotem, ...).</li>
 * </ul>
 *
 * <p>Modules from other addons are {@link ModuleTier#BORDERLINE} by default, so addons keep working.
 */
public final class ModuleTiers {
    private static final Map<String, ModuleTier> TIERS = new HashMap<>();

    static {
        // ---------------------------------------------------------------- Combat
        cheat(
            // combat automation and block placement other players can see
            "AnchorAura", "AntiAnvil", "AntiBed", "AutoAnvil", "AutoTrap", "AutoWeb", "BedAura",
            "Burrow", "Criticals", "CrystalAura", "HoleFiller", "KillAura", "SelfAnvil", "SelfTrap",
            "SelfWeb", "Surround", "AutoCity", "Quiver",
            // combat helpers that act without your input
            "AntiAnchor", "ArrowDodge", "AttributeSwap", "AutoArmor", "AutoEXP", "AutoTotem",
            "AutoWeapon", "BowAimbot", "BowSpam", "Offhand"
        );
        borderline(
            // stops the game when your gear is about to break, purely informational
            "AutoLog",
            // client side hitbox rendering, makes aiming easier but does nothing on the server
            "Hitboxes"
        );

        // ---------------------------------------------------------------- Player
        cheat(
            "AirPlace", "AutoClicker", "AutoGap", "BreakDelay", "FastUse", "GhostHand",
            "InstantRebreak", "LiquidInteract", "NoMiningTrace", "Reach", "SpeedMine",
            "AntiHunger", "AutoMend", "AutoTool", "Multitask"
        );
        borderline(
            // automates something you would do by hand anyway
            "AntiAFK", "AutoEat", "AutoFish", "AutoReplenish", "ChestSwap", "EXPThrower",
            "MiddleClickExtra",
            // packet tricks the server can not tell apart from a normal client
            "NoRotate", "PotionSaver", "Rotation"
        );
        legit(
            "AutoRespawn", "FakePlayer", "NameProtect", "NoInteract", "NoStatusEffects", "Portals"
        );

        // ---------------------------------------------------------------- Movement
        cheat(
            "AirJump", "AntiVoid", "AutoWasp", "Blink", "ClickTP", "ElytraBoost", "ElytraFly",
            "EntityControl", "FastClimb", "Flight", "GUIMove", "HighJump", "Jesus", "LongJump", "NoFall",
            "NoSlow", "ReverseStep", "Scaffold", "Slippy", "Speed", "Spider", "Step",
            "TridentBoost", "Velocity"
        );
        borderline(
            // things vanilla already does (auto jump) or that only stop your own input
            "Anchor", "AutoJump", "AutoWalk", "Parkour", "SafeWalk"
        );
        legit(
            "Sneak", "Sprint"
        );

        // ---------------------------------------------------------------- Render
        cheat(
            // the camera and your player are two different things, nobody else sees where you look
            "Freecam",
            // information about things you are not supposed to see, or that vanilla would not show at all
            "ESP", "Nametags", "Xray",
            "BlockESP", "Chams", "CityESP", "EntityOwner", "HoleESP", "LogoutSpots", "StorageESP",
            "Tracers", "TunnelESP", "VoidESP", "WallHack"
        );
        borderline(
            // information the server sends anyway, just rendered better
            "LightOverlay", "PopChams", "Trajectories"
        );
        legit(
            "BetterTab", "BetterTooltips", "BlockSelection", "Blur", "BossStack", "Breadcrumbs",
            "BreakIndicators", "CameraTweaks", "FreeLook", "Fullbright", "HandView",
            "ItemHighlight", "ItemPhysics", "Marker", "NoRender", "Trail", "WaypointsModule",
            "TimeChanger", "WeatherChanger", "Zoom"
        );

        // ---------------------------------------------------------------- World
        cheat(
            "BuildHeight", "EChestFarmer", "Excavator", "Flamethrower", "HighwayBuilder",
            "InfinityMiner", "LiquidFiller", "Nuker", "PacketMine", "SpawnProofer", "Timer",
            "VeinMiner", "StashFinder"
        );
        borderline(
            "AutoBreed", "AutoBrewer", "AutoMount", "AutoNametag", "AutoShearer", "AutoSign",
            "AutoSmelter", "Collisions", "EndermanLook", "MountBypass"
        );
        legit(
            "Ambience", "NoGhostBlocks"
        );

        // ---------------------------------------------------------------- Misc
        borderline(
            "BetterBeacons", "BookBot", "MessageAura", "Notebot", "PacketCanceller", "Spam",
            "Swarm"
        );
        legit(
            "AntiPacketKick", "AutoReconnect", "BetterChat", "DiscordPresence", "InventoryTweaks",
            "Notifier", "PacketLogger", "ServerSpoof", "SoundBlocker"
        );
        cheat(
            "OffhandCrash"
        );
    }

    private ModuleTiers() {
    }

    public static ModuleTier get(Class<? extends Module> klass) {
        return TIERS.getOrDefault(klass.getSimpleName(), ModuleTier.BORDERLINE);
    }

    public static ModuleTier get(Module module) {
        return get(module.getClass());
    }

    private static void cheat(String... moduleNames) {
        put(ModuleTier.CHEAT, moduleNames);
    }

    private static void borderline(String... moduleNames) {
        put(ModuleTier.BORDERLINE, moduleNames);
    }

    private static void legit(String... moduleNames) {
        put(ModuleTier.LEGIT, moduleNames);
    }

    private static void put(ModuleTier tier, String... moduleNames) {
        for (String moduleName : moduleNames) {
            ModuleTier previous = TIERS.put(moduleName, tier);
            if (previous != null) throw new IllegalStateException("Module " + moduleName + " is classified twice.");
        }
    }
}
