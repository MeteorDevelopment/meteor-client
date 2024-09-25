/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.process.IBaritoneProcess;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.ClientPlayerInteractionManagerAccessor;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.starscript.Script;
import meteordevelopment.starscript.Section;
import meteordevelopment.starscript.StandardLib;
import meteordevelopment.starscript.Starscript;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import meteordevelopment.starscript.utils.Error;
import meteordevelopment.starscript.utils.StarscriptError;
import meteordevelopment.starscript.value.Value;
import meteordevelopment.starscript.value.ValueMap;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteorStarscript {
    public static Starscript ss = new Starscript();

    private static final BlockPos.Mutable BP = new BlockPos.Mutable();
    private static final StringBuilder SB = new StringBuilder();

    @PreInit(dependencies = PathManagers.class)
    public static void init() {
        StandardLib.init(ss);

        // General
        ss.set("mc_version", SharedConstants.getGameVersion().getName());
        ss.set("fps", () -> Value.number(MinecraftClientAccessor.getFps()));
        ss.set("ping", MeteorStarscript::ping);
        ss.set("time", () -> Value.string(LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))));
        ss.set("cps", () -> Value.number(CPSUtils.getCpsAverage()));

        // Meteor
        ss.set("meteor", new ValueMap()
            .set("name", MeteorClient.NAME)
            .set("version", MeteorClient.VERSION != null ? (MeteorClient.DEV_BUILD.isEmpty() ? MeteorClient.VERSION.toString() : MeteorClient.VERSION + " " + MeteorClient.DEV_BUILD) : "")
            .set("modules", () -> Value.number(Modules.get().getAll().size()))
            .set("active_modules", () -> Value.number(Modules.get().getActive().size()))
            .set("is_module_active", MeteorStarscript::isModuleActive)
            .set("get_module_info", MeteorStarscript::getModuleInfo)
            .set("get_module_setting", MeteorStarscript::getModuleSetting)
            .set("prefix", MeteorStarscript::getMeteorPrefix)
        );

        // Baritone
        if (BaritoneUtils.IS_AVAILABLE) {
            ss.set("baritone", new ValueMap()
                .set("is_pathing", () -> Value.bool(BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()))
                .set("distance_to_goal", MeteorStarscript::baritoneDistanceToGoal)
                .set("process", MeteorStarscript::baritoneProcess)
                .set("process_name", MeteorStarscript::baritoneProcessName)
                .set("eta", MeteorStarscript::baritoneETA)
            );
        }

        // Camera
        ss.set("camera", new ValueMap()
            .set("pos", new ValueMap()
                .set("_toString", () -> posString(false, true))
                .set("x", () -> Value.number(mc.gameRenderer.getCamera().getPos().x))
                .set("y", () -> Value.number(mc.gameRenderer.getCamera().getPos().y))
                .set("z", () -> Value.number(mc.gameRenderer.getCamera().getPos().z))
            )
            .set("opposite_dim_pos", new ValueMap()
                .set("_toString", () -> posString(true, true))
                .set("x", () -> oppositeX(true))
                .set("y", () -> Value.number(mc.gameRenderer.getCamera().getPos().y))
                .set("z", () -> oppositeZ(true))
            )

            .set("yaw", () -> yaw(true))
            .set("pitch", () -> pitch(true))
            .set("direction", () -> direction(true))
        );

        // Player
        ss.set("player", new ValueMap()
            .set("_toString", () -> Value.string(mc.getSession().getUsername()))
            .set("health", () -> Value.number(mc.player != null ? mc.player.getHealth() : 0))
            .set("absorption", () -> Value.number(mc.player != null ? mc.player.getAbsorptionAmount() : 0))
            .set("hunger", () -> Value.number(mc.player != null ? mc.player.getHungerManager().getFoodLevel() : 0))

            .set("speed", () -> Value.number(Utils.getPlayerSpeed().horizontalLength()))
            .set("speed_all", new ValueMap()
                .set("_toString", () -> Value.string(mc.player != null ? Utils.getPlayerSpeed().toString() : ""))
                .set("x", () -> Value.number(mc.player != null ? Utils.getPlayerSpeed().x : 0))
                .set("y", () -> Value.number(mc.player != null ? Utils.getPlayerSpeed().y : 0))
                .set("z", () -> Value.number(mc.player != null ? Utils.getPlayerSpeed().z : 0))
            )

            .set("breaking_progress", () -> Value.number(mc.interactionManager != null ? ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getBreakingProgress() : 0))
            .set("biome", MeteorStarscript::biome)

            .set("dimension", () -> Value.string(PlayerUtils.getDimension().name()))
            .set("opposite_dimension", () -> Value.string(PlayerUtils.getDimension().opposite().name()))

            .set("gamemode", () -> PlayerUtils.getGameMode() != null ? Value.string(StringUtils.capitalize(PlayerUtils.getGameMode().getName())) : Value.null_())

            .set("pos", new ValueMap()
                .set("_toString", () -> posString(false, false))
                .set("x", () -> Value.number(mc.player != null ? mc.player.getX() : 0))
                .set("y", () -> Value.number(mc.player != null ? mc.player.getY() : 0))
                .set("z", () -> Value.number(mc.player != null ? mc.player.getZ() : 0))
            )
            .set("opposite_dim_pos", new ValueMap()
                .set("_toString", () -> posString(true, false))
                .set("x", () -> oppositeX(false))
                .set("y", () -> Value.number(mc.player != null ? mc.player.getY() : 0))
                .set("z", () -> oppositeZ(false))
            )

            .set("yaw", () -> yaw(false))
            .set("pitch", () -> pitch(false))
            .set("direction", () -> direction(false))

            .set("hand", () -> mc.player != null ? wrap(mc.player.getMainHandStack()) : Value.null_())
            .set("offhand", () -> mc.player != null ? wrap(mc.player.getOffHandStack()) : Value.null_())
            .set("hand_or_offhand", MeteorStarscript::handOrOffhand)
            .set("get_item", MeteorStarscript::getItem)
            .set("count_items", MeteorStarscript::countItems)

            .set("xp", new ValueMap()
                .set("level", () -> Value.number(mc.player != null ? mc.player.experienceLevel : 0))
                .set("progress", () -> Value.number(mc.player != null ? mc.player.experienceProgress : 0))
                .set("total", () -> Value.number(mc.player != null ? mc.player.totalExperience : 0))
            )

            .set("has_potion_effect", MeteorStarscript::hasPotionEffect)
            .set("get_potion_effect", MeteorStarscript::getPotionEffect)

            .set("get_stat", MeteorStarscript::getStat)
        );

        // Crosshair target
        ss.set("crosshair_target", new ValueMap()
            .set("type", MeteorStarscript::crosshairType)
            .set("value", MeteorStarscript::crosshairValue)
        );

        // Server
        ss.set("server", new ValueMap()
            .set("_toString", () -> Value.string(Utils.getWorldName()))
            .set("tps", () -> Value.number(TickRate.INSTANCE.getTickRate()))
            .set("time", () -> Value.string(Utils.getWorldTime()))
            .set("player_count", () -> Value.number(mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerList().size() : 0))
            .set("difficulty", () -> Value.string(mc.world != null ? mc.world.getDifficulty().getName() : ""))
        );
    }

    // Helpers

    public static Script compile(String source) {
        Parser.Result result = Parser.parse(source);

        if (result.hasErrors()) {
            for (Error error : result.errors) printChatError(error);
            return null;
        }

        return Compiler.compile(result);
    }

    public static Section runSection(Script script, StringBuilder sb) {
        try {
            return ss.run(script, sb);
        }
        catch (StarscriptError error) {
            printChatError(error);
            return null;
        }
    }
    public static String run(Script script, StringBuilder sb) {
        Section section = runSection(script, sb);
        return section != null ? section.toString() : null;
    }

    public static Section runSection(Script script) {
        return runSection(script, new StringBuilder());
    }

    public static String run(Script script) {
        return run(script, new StringBuilder());
    }

    // Errors

    public static void printChatError(int i, Error error) {
        String caller = getCallerName();

        if (caller != null) {
            if (i != -1) ChatUtils.errorPrefix("Starscript", "%d, %d '%c': %s (from %s)", i, error.character, error.ch, error.message, caller);
            else ChatUtils.errorPrefix("Starscript", "%d '%c': %s (from %s)", error.character, error.ch, error.message, caller);
        }
        else {
            if (i != -1) ChatUtils.errorPrefix("Starscript", "%d, %d '%c': %s", i, error.character, error.ch, error.message);
            else ChatUtils.errorPrefix("Starscript", "%d '%c': %s", error.character, error.ch, error.message);
        }
    }

    public static void printChatError(Error error) {
        printChatError(-1, error);
    }

    public static void printChatError(StarscriptError e) {
        String caller = getCallerName();

        if (caller != null) ChatUtils.errorPrefix("Starscript", "%s (from %s)", e.getMessage(), caller);
        else ChatUtils.errorPrefix("Starscript", "%s", e.getMessage());
    }

    private static String getCallerName() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if (elements.length == 0) return null;

        for (int i = 1; i < elements.length; i++) {
            String name = elements[i].getClassName();

            if (name.startsWith(Starscript.class.getPackageName())) continue;
            if (name.equals(MeteorStarscript.class.getName())) continue;

            return name.substring(name.lastIndexOf('.') + 1);
        }

        return null;
    }

    // Functions

    private static long lastRequestedStatsTime = 0;

    private static Value hasPotionEffect(Starscript ss, int argCount) {
        if (argCount < 1) ss.error("player.has_potion_effect() requires 1 argument, got %d.", argCount);
        if (mc.player == null) return Value.bool(false);

        Identifier name = popIdentifier(ss, "First argument to player.has_potion_effect() needs to a string.");

        Optional<RegistryEntry.Reference<StatusEffect>> effect = Registries.STATUS_EFFECT.getEntry(name);
        if (effect.isEmpty()) return Value.bool(false);

        StatusEffectInstance effectInstance = mc.player.getStatusEffect(effect.get());
        return Value.bool(effectInstance != null);
    }

    private static Value getPotionEffect(Starscript ss, int argCount) {
        if (argCount < 1) ss.error("player.get_potion_effect() requires 1 argument, got %d.", argCount);
        if (mc.player == null) return Value.null_();

        Identifier name = popIdentifier(ss, "First argument to player.get_potion_effect() needs to a string.");

        Optional<RegistryEntry.Reference<StatusEffect>> effect = Registries.STATUS_EFFECT.getEntry(name);
        if (effect.isEmpty()) return Value.null_();

        StatusEffectInstance effectInstance = mc.player.getStatusEffect(effect.get());
        if (effectInstance == null) return Value.null_();

        return wrap(effectInstance);
    }

    private static Value getStat(Starscript ss, int argCount) {
        if (argCount < 1) ss.error("player.get_stat() requires 1 argument, got %d.", argCount);
        if (mc.player == null) return Value.number(0);

        long time = System.currentTimeMillis();
        if ((time - lastRequestedStatsTime) / 1000.0 >= 1 && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS));
            lastRequestedStatsTime = time;
        }

        String type = argCount > 1 ? ss.popString("First argument to player.get_stat() needs to be a string.") : "custom";
        Identifier name = popIdentifier(ss, (argCount > 1 ? "Second" : "First") + " argument to player.get_stat() needs to be a string.");

        Stat<?> stat = switch (type) {
            case "mined" -> Stats.MINED.getOrCreateStat(Registries.BLOCK.get(name));
            case "crafted" -> Stats.CRAFTED.getOrCreateStat(Registries.ITEM.get(name));
            case "used" -> Stats.USED.getOrCreateStat(Registries.ITEM.get(name));
            case "broken" -> Stats.BROKEN.getOrCreateStat(Registries.ITEM.get(name));
            case "picked_up" -> Stats.PICKED_UP.getOrCreateStat(Registries.ITEM.get(name));
            case "dropped" -> Stats.DROPPED.getOrCreateStat(Registries.ITEM.get(name));
            case "killed" -> Stats.KILLED.getOrCreateStat(Registries.ENTITY_TYPE.get(name));
            case "killed_by" -> Stats.KILLED_BY.getOrCreateStat(Registries.ENTITY_TYPE.get(name));
            case "custom" -> {
                name = Registries.CUSTOM_STAT.get(name);
                yield name != null ? Stats.CUSTOM.getOrCreateStat(name) : null;
            }
            default -> null;
        };

        return Value.number(stat != null ? mc.player.getStatHandler().getStat(stat) : 0);
    }

    private static Value getModuleInfo(Starscript ss, int argCount) {
        if (argCount != 1) ss.error("meteor.get_module_info() requires 1 argument, got %d.", argCount);

        Module module = Modules.get().get(ss.popString("First argument to meteor.get_module_info() needs to be a string."));
        if (module != null && module.isActive()) {
            String info = module.getInfoString();
            return Value.string(info == null ? "" : info);
        }

        return Value.string("");
    }

    private static Value getModuleSetting(Starscript ss, int argCount) {
        if (argCount != 2) ss.error("meteor.get_module_setting() requires 2 arguments, got %d.", argCount);

        var settingName = ss.popString("Second argument to meteor.get_module_setting() needs to be a string.");
        var moduleName = ss.popString("First argument to meteor.get_module_setting() needs to be a string.");
        Module module = Modules.get().get(moduleName);
        if (module == null) {
            ss.error("Unable to get module %s for meteor.get_module_setting()", moduleName);
        }
        var setting = module.settings.get(settingName);
        if (setting == null) {
            ss.error("Unable to get setting %s for module %s for meteor.get_module_setting()", settingName, moduleName);
        }
        var value = setting.get();
        return switch (value) {
            case Double d -> Value.number(d);
            case Integer i -> Value.number(i);
            case Boolean b -> Value.bool(b);
            case List<?> list -> Value.number(list.size());
            case null, default -> Value.string(value.toString());
        };
    }

    private static Value isModuleActive(Starscript ss, int argCount) {
        if (argCount != 1) ss.error("meteor.is_module_active() requires 1 argument, got %d.", argCount);

        Module module = Modules.get().get(ss.popString("First argument to meteor.is_module_active() needs to be a string."));
        return Value.bool(module != null && module.isActive());
    }

    private static Value getItem(Starscript ss, int argCount) {
        if (argCount != 1) ss.error("player.get_item() requires 1 argument, got %d.", argCount);

        int i = (int) ss.popNumber("First argument to player.get_item() needs to be a number.");
        if (i < 0) ss.error("First argument to player.get_item() needs to be a non-negative integer.", i);
        return mc.player != null ? wrap(mc.player.getInventory().getStack(i)) : Value.null_();
    }

    private static Value countItems(Starscript ss, int argCount) {
        if (argCount != 1) ss.error("player.count_items() requires 1 argument, got %d.", argCount);

        String idRaw = ss.popString("First argument to player.count_items() needs to be a string.");
        Identifier id = Identifier.tryParse(idRaw);
        if (id == null) return Value.number(0);

        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR || mc.player == null) return Value.number(0);

        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack.getItem() == item) count += itemStack.getCount();
        }

        return Value.number(count);
    }

    private static Value getMeteorPrefix() {
        if (Config.get() == null) return Value.null_();
        return Value.string(Config.get().prefix.get());
    }

    // Other

    private static Value baritoneProcess() {
        Optional<IBaritoneProcess> process = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingControlManager().mostRecentInControl();
        return Value.string(process.isEmpty() ? "" : process.get().displayName0());
    }

    private static Value baritoneProcessName() {
        Optional<IBaritoneProcess> process = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingControlManager().mostRecentInControl();
        if (process.isEmpty()) return Value.string("");

        String className = process.get().getClass().getSimpleName();
        if (className.endsWith("Process")) className = className.substring(0, className.length() - 7);

        SB.append(className);
        int i = 0;
        for (int j = 0; j < className.length(); j++) {
            if (j > 0 && Character.isUpperCase(className.charAt(j))) {
                SB.insert(i, ' ');
                i++;
            }

            i++;
        }

        String name = SB.toString();
        SB.setLength(0);
        return Value.string(name);
    }

    // Returns the ETA in seconds
    private static Value baritoneETA() {
        if (mc.player == null) return Value.number(0);
        Optional<Double> ticksTillGoal = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().estimatedTicksToGoal();
        return ticksTillGoal.map(aDouble -> Value.number(aDouble / 20)).orElseGet(() -> Value.number(0));
    }

    private static Value oppositeX(boolean camera) {
        double x = camera ? mc.gameRenderer.getCamera().getPos().x : (mc.player != null ? mc.player.getX() : 0);
        Dimension dimension = PlayerUtils.getDimension();

        if (dimension == Dimension.Overworld) x /= 8;
        else if (dimension == Dimension.Nether) x *= 8;

        return Value.number(x);
    }

    private static Value oppositeZ(boolean camera) {
        double z = camera ? mc.gameRenderer.getCamera().getPos().z : (mc.player != null ? mc.player.getZ() : 0);
        Dimension dimension = PlayerUtils.getDimension();

        if (dimension == Dimension.Overworld) z /= 8;
        else if (dimension == Dimension.Nether) z *= 8;

        return Value.number(z);
    }

    private static Value yaw(boolean camera) {
        float yaw;
        if (camera) yaw = mc.gameRenderer.getCamera().getYaw();
        else yaw = mc.player != null ? mc.player.getYaw() : 0;
        yaw %= 360;

        if (yaw < 0) yaw += 360;
        if (yaw > 180) yaw -= 360;

        return Value.number(yaw);
    }

    private static Value pitch(boolean camera) {
        float pitch;
        if (camera) pitch = mc.gameRenderer.getCamera().getPitch();
        else pitch = mc.player != null ? mc.player.getPitch() : 0;
        pitch %= 360;

        if (pitch < 0) pitch += 360;
        if (pitch > 180) pitch -= 360;

        return Value.number(pitch);
    }

    private static Value direction(boolean camera) {
        float yaw;
        if (camera) yaw = mc.gameRenderer.getCamera().getYaw();
        else yaw = mc.player != null ? mc.player.getYaw() : 0;

        return wrap(HorizontalDirection.get(yaw));
    }

    private static Value biome() {
        if (mc.player == null || mc.world == null) return Value.string("");

        BP.set(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Identifier id = mc.world.getRegistryManager().get(RegistryKeys.BIOME).getId(mc.world.getBiome(BP).value());
        if (id == null) return Value.string("Unknown");

        return Value.string(Arrays.stream(id.getPath().split("_")).map(StringUtils::capitalize).collect(Collectors.joining(" ")));
    }

    private static Value handOrOffhand() {
        if (mc.player == null) return Value.null_();

        ItemStack itemStack = mc.player.getMainHandStack();
        if (itemStack.isEmpty()) itemStack = mc.player.getOffHandStack();

        return itemStack != null ? wrap(itemStack) : Value.null_();
    }

    private static Value ping() {
        if (mc.getNetworkHandler() == null || mc.player == null) return Value.number(0);

        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return Value.number(playerListEntry != null ? playerListEntry.getLatency() : 0);
    }

    private static Value baritoneDistanceToGoal() {
        Goal goal = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();
        return Value.number((goal != null && mc.player != null) ? goal.heuristic(mc.player.getBlockPos()) : 0);
    }

    private static Value posString(boolean opposite, boolean camera) {
        Vec3d pos;
        if (camera) pos = mc.gameRenderer.getCamera().getPos();
        else pos = mc.player != null ? mc.player.getPos() : Vec3d.ZERO;

        double x = pos.x;
        double z = pos.z;

        if (opposite) {
            Dimension dimension = PlayerUtils.getDimension();

            if (dimension == Dimension.Overworld) {
                x /= 8;
                z /= 8;
            }
            else if (dimension == Dimension.Nether) {
                x *= 8;
                z *= 8;
            }
        }

        return posString(x, pos.y, z);
    }

    private static Value posString(double x, double y, double z) {
        return Value.string(String.format("X: %.0f Y: %.0f Z: %.0f", x, y, z));
    }

    private static Value crosshairType() {
        if (mc.crosshairTarget == null) return Value.string("miss");

        return Value.string(switch (mc.crosshairTarget.getType()) {
            case MISS -> "miss";
            case BLOCK -> "block";
            case ENTITY -> "entity";
        });
    }

    private static Value crosshairValue() {
        if (mc.world == null || mc.crosshairTarget == null) return Value.null_();

        if (mc.crosshairTarget.getType() == HitResult.Type.MISS) return Value.string("");
        if (mc.crosshairTarget instanceof BlockHitResult hit) return wrap(hit.getBlockPos(), mc.world.getBlockState(hit.getBlockPos()));
        return wrap(((EntityHitResult) mc.crosshairTarget).getEntity());
    }

    // Utility

    public static Identifier popIdentifier(Starscript ss, String errorMessage) {
        try {
            return Identifier.of(ss.popString(errorMessage));
        }
        catch (InvalidIdentifierException e) {
            ss.error(e.getMessage());
            return null;
        }
    }

    // Wrapping

    public static Value wrap(ItemStack itemStack) {
        String name = itemStack.isEmpty() ? "" : Names.get(itemStack.getItem());

        int durability = 0;
        if (!itemStack.isEmpty() && itemStack.isDamageable()) durability = itemStack.getMaxDamage() - itemStack.getDamage();

        return Value.map(new ValueMap()
            .set("_toString", Value.string(itemStack.getCount() <= 1 ? name : String.format("%s %dx", name, itemStack.getCount())))
            .set("name", Value.string(name))
            .set("id", Value.string(Registries.ITEM.getId(itemStack.getItem()).toString()))
            .set("count", Value.number(itemStack.getCount()))
            .set("durability", Value.number(durability))
            .set("max_durability", Value.number(itemStack.getMaxDamage()))
        );
    }

    public static Value wrap(BlockPos blockPos, BlockState blockState) {
        return Value.map(new ValueMap()
            .set("_toString", Value.string(Names.get(blockState.getBlock())))
            .set("id", Value.string(Registries.BLOCK.getId(blockState.getBlock()).toString()))
            .set("pos", Value.map(new ValueMap()
                .set("_toString", posString(blockPos.getX(), blockPos.getY(), blockPos.getZ()))
                .set("x", Value.number(blockPos.getX()))
                .set("y", Value.number(blockPos.getY()))
                .set("z", Value.number(blockPos.getZ()))
            ))
        );
    }

    public static Value wrap(Entity entity) {
        return Value.map(new ValueMap()
            .set("_toString", Value.string(entity.getName().getString()))
            .set("id", Value.string(Registries.ENTITY_TYPE.getId(entity.getType()).toString()))
            .set("health", Value.number(entity instanceof LivingEntity e ? e.getHealth(): 0))
            .set("absorption", Value.number(entity instanceof LivingEntity e ? e.getAbsorptionAmount() : 0))
            .set("pos", Value.map(new ValueMap()
                .set("_toString", posString(entity.getX(), entity.getY(), entity.getZ()))
                .set("x", Value.number(entity.getX()))
                .set("y", Value.number(entity.getY()))
                .set("z", Value.number(entity.getZ()))
            ))
        );
    }

    public static Value wrap(HorizontalDirection dir) {
        return Value.map(new ValueMap()
            .set("_toString", Value.string(dir.name + " " + dir.axis))
            .set("name", Value.string(dir.name))
            .set("axis", Value.string(dir.axis))
        );
    }

    public static Value wrap(StatusEffectInstance effectInstance) {
        return Value.map(new ValueMap()
            .set("duration", effectInstance.getDuration())
            .set("level", effectInstance.getAmplifier() + 1)
        );
    }
}
