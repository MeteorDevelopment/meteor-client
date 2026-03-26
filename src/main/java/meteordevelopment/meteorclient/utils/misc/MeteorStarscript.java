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
import net.minecraft.IdentifierException;
import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import org.meteordev.starscript.Script;
import org.meteordev.starscript.Section;
import org.meteordev.starscript.StandardLib;
import org.meteordev.starscript.Starscript;
import org.meteordev.starscript.compiler.Compiler;
import org.meteordev.starscript.compiler.Parser;
import org.meteordev.starscript.utils.Error;
import org.meteordev.starscript.utils.StarscriptError;
import org.meteordev.starscript.value.Value;
import org.meteordev.starscript.value.ValueMap;

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

    private static final BlockPos.MutableBlockPos BP = new BlockPos.MutableBlockPos();
    private static final StringBuilder SB = new StringBuilder();

    @PreInit(dependencies = PathManagers.class)
    public static void init() {
        StandardLib.init(ss);

        // General
        ss.set("mc_version", SharedConstants.getCurrentVersion().name());
        ss.set("fps", () -> Value.number(MinecraftClientAccessor.meteor$getFps()));
        ss.set("ping", MeteorStarscript::ping);
        ss.set("time", () -> Value.string(LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))));
        ss.set("cps", () -> Value.number(CPSUtils.getCpsAverage()));

        // Meteor
        ss.set("meteor", new ValueMap()
            .set("name", MeteorClient.NAME)
            .set("version", MeteorClient.VERSION != null ? (MeteorClient.BUILD_NUMBER.isEmpty() ? MeteorClient.VERSION.toString() : MeteorClient.VERSION + " " + MeteorClient.BUILD_NUMBER) : "")
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
                .set("x", () -> Value.number(mc.gameRenderer.getMainCamera().position().x))
                .set("y", () -> Value.number(mc.gameRenderer.getMainCamera().position().y))
                .set("z", () -> Value.number(mc.gameRenderer.getMainCamera().position().z))
            )
            .set("opposite_dim_pos", new ValueMap()
                .set("_toString", () -> posString(true, true))
                .set("x", () -> oppositeX(true))
                .set("y", () -> Value.number(mc.gameRenderer.getMainCamera().position().y))
                .set("z", () -> oppositeZ(true))
            )

            .set("yaw", () -> yaw(true))
            .set("pitch", () -> pitch(true))
            .set("direction", () -> direction(true))
        );

        // Player
        ss.set("player", new ValueMap()
            .set("_toString", () -> Value.string(mc.getUser().getName()))
            .set("health", () -> Value.number(mc.player != null ? mc.player.getHealth() : 0))
            .set("absorption", () -> Value.number(mc.player != null ? mc.player.getAbsorptionAmount() : 0))
            .set("hunger", () -> Value.number(mc.player != null ? mc.player.getFoodData().getFoodLevel() : 0))
            .set("saturation", () -> Value.number(mc.player != null ? mc.player.getFoodData().getSaturationLevel() : 0))

            .set("speed", () -> Value.number(Utils.getPlayerSpeed().horizontalDistance()))
            .set("speed_all", new ValueMap()
                .set("_toString", () -> Value.string(mc.player != null ? Utils.getPlayerSpeed().toString() : ""))
                .set("x", () -> Value.number(mc.player != null ? Utils.getPlayerSpeed().x : 0))
                .set("y", () -> Value.number(mc.player != null ? Utils.getPlayerSpeed().y : 0))
                .set("z", () -> Value.number(mc.player != null ? Utils.getPlayerSpeed().z : 0))
            )

            .set("breaking_progress", () -> Value.number(mc.gameMode != null ? ((ClientPlayerInteractionManagerAccessor) mc.gameMode).meteor$getBreakingProgress() : 0))
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

            .set("hand", () -> mc.player != null ? wrap(mc.player.getMainHandItem()) : Value.null_())
            .set("offhand", () -> mc.player != null ? wrap(mc.player.getOffhandItem()) : Value.null_())
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
            .set("player_count", () -> Value.number(mc.getConnection() != null ? mc.getConnection().getOnlinePlayers().size() : 0))
            .set("difficulty", () -> Value.string(mc.level != null ? mc.level.getDifficulty().getSerializedName() : ""))
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

        Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.get(name);
        if (effect.isEmpty()) return Value.bool(false);

        MobEffectInstance effectInstance = mc.player.getEffect(effect.get());
        return Value.bool(effectInstance != null);
    }

    private static Value getPotionEffect(Starscript ss, int argCount) {
        if (argCount < 1) ss.error("player.get_potion_effect() requires 1 argument, got %d.", argCount);
        if (mc.player == null) return Value.null_();

        Identifier name = popIdentifier(ss, "First argument to player.get_potion_effect() needs to a string.");

        Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.get(name);
        if (effect.isEmpty()) return Value.null_();

        MobEffectInstance effectInstance = mc.player.getEffect(effect.get());
        if (effectInstance == null) return Value.null_();

        return wrap(effectInstance);
    }

    private static Value getStat(Starscript ss, int argCount) {
        if (argCount < 1) ss.error("player.get_stat() requires 1 argument, got %d.", argCount);
        if (mc.player == null) return Value.number(0);

        long time = System.currentTimeMillis();
        if ((time - lastRequestedStatsTime) / 1000.0 >= 1 && mc.getConnection() != null) {
            mc.getConnection().getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
            lastRequestedStatsTime = time;
        }

        String type = argCount > 1 ? ss.popString("First argument to player.get_stat() needs to be a string.") : "custom";
        Identifier name = popIdentifier(ss, (argCount > 1 ? "Second" : "First") + " argument to player.get_stat() needs to be a string.");

        Stat<?> stat = switch (type) {
            case "mined" -> Stats.BLOCK_MINED.get(BuiltInRegistries.BLOCK.getValue(name));
            case "crafted" -> Stats.ITEM_CRAFTED.get(BuiltInRegistries.ITEM.getValue(name));
            case "used" -> Stats.ITEM_USED.get(BuiltInRegistries.ITEM.getValue(name));
            case "broken" -> Stats.ITEM_BROKEN.get(BuiltInRegistries.ITEM.getValue(name));
            case "picked_up" -> Stats.ITEM_PICKED_UP.get(BuiltInRegistries.ITEM.getValue(name));
            case "dropped" -> Stats.ITEM_DROPPED.get(BuiltInRegistries.ITEM.getValue(name));
            case "killed" -> Stats.ENTITY_KILLED.get(BuiltInRegistries.ENTITY_TYPE.getValue(name));
            case "killed_by" -> Stats.ENTITY_KILLED_BY.get(BuiltInRegistries.ENTITY_TYPE.getValue(name));
            case "custom" -> {
                name = BuiltInRegistries.CUSTOM_STAT.getValue(name);
                yield name != null ? Stats.CUSTOM.get(name) : null;
            }
            default -> null;
        };

        return Value.number(stat != null ? mc.player.getStats().getValue(stat) : 0);
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
        return mc.player != null ? wrap(mc.player.getInventory().getItem(i)) : Value.null_();
    }

    private static Value countItems(Starscript ss, int argCount) {
        if (argCount != 1) ss.error("player.count_items() requires 1 argument, got %d.", argCount);

        String idRaw = ss.popString("First argument to player.count_items() needs to be a string.");
        Identifier id = Identifier.tryParse(idRaw);
        if (id == null) return Value.number(0);

        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == Items.AIR || mc.player == null) return Value.number(0);

        int count = 0;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack itemStack = mc.player.getInventory().getItem(i);
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
        double x = camera ? mc.gameRenderer.getMainCamera().position().x : (mc.player != null ? mc.player.getX() : 0);
        Dimension dimension = PlayerUtils.getDimension();

        if (dimension == Dimension.Overworld) x /= 8;
        else if (dimension == Dimension.Nether) x *= 8;

        return Value.number(x);
    }

    private static Value oppositeZ(boolean camera) {
        double z = camera ? mc.gameRenderer.getMainCamera().position().z : (mc.player != null ? mc.player.getZ() : 0);
        Dimension dimension = PlayerUtils.getDimension();

        if (dimension == Dimension.Overworld) z /= 8;
        else if (dimension == Dimension.Nether) z *= 8;

        return Value.number(z);
    }

    private static Value yaw(boolean camera) {
        float yaw;
        if (camera) yaw = mc.gameRenderer.getMainCamera().yRot();
        else yaw = mc.player != null ? mc.player.getYRot() : 0;
        yaw %= 360;

        if (yaw < 0) yaw += 360;
        if (yaw > 180) yaw -= 360;

        return Value.number(yaw);
    }

    private static Value pitch(boolean camera) {
        float pitch;
        if (camera) pitch = mc.gameRenderer.getMainCamera().xRot();
        else pitch = mc.player != null ? mc.player.getXRot() : 0;
        pitch %= 360;

        if (pitch < 0) pitch += 360;
        if (pitch > 180) pitch -= 360;

        return Value.number(pitch);
    }

    private static Value direction(boolean camera) {
        float yaw;
        if (camera) yaw = mc.gameRenderer.getMainCamera().yRot();
        else yaw = mc.player != null ? mc.player.getYRot() : 0;

        return wrap(HorizontalDirection.get(yaw));
    }

    private static Value biome() {
        if (mc.player == null || mc.level == null) return Value.string("");

        BP.set(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        return mc.level.registryAccess().lookup(Registries.BIOME)
            .map(biomeRegistry -> {
                Identifier id = biomeRegistry.getKey(mc.level.getBiome(BP).value());
                if (id == null) return Value.string("Unknown");
                return Value.string(Arrays.stream(id.getPath().split("_")).map(StringUtils::capitalize).collect(Collectors.joining(" ")));
            })
            .orElse(Value.string("Unknown"));
    }

    private static Value handOrOffhand() {
        if (mc.player == null) return Value.null_();

        ItemStack itemStack = mc.player.getMainHandItem();
        if (itemStack.isEmpty()) itemStack = mc.player.getOffhandItem();

        return itemStack != null ? wrap(itemStack) : Value.null_();
    }

    private static Value ping() {
        if (mc.getConnection() == null || mc.player == null) return Value.number(0);

        PlayerInfo playerListEntry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return Value.number(playerListEntry != null ? playerListEntry.getLatency() : 0);
    }

    private static Value baritoneDistanceToGoal() {
        Goal goal = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();
        return Value.number((goal != null && mc.player != null) ? goal.heuristic(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()) : 0);
    }

    private static Value posString(boolean opposite, boolean camera) {
        Vec3 pos;
        if (camera) pos = mc.gameRenderer.getMainCamera().position();
        else pos = mc.player != null ? mc.player.position() : Vec3.ZERO;

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
        if (mc.hitResult == null) return Value.string("miss");

        return Value.string(switch (mc.hitResult.getType()) {
            case MISS -> "miss";
            case BLOCK -> "block";
            case ENTITY -> "entity";
        });
    }

    private static Value crosshairValue() {
        if (mc.level == null || mc.hitResult == null) return Value.null_();

        if (mc.hitResult.getType() == HitResult.Type.MISS) return Value.string("");
        if (mc.hitResult instanceof BlockHitResult hit) return wrap(hit.getBlockPos(), mc.level.getBlockState(hit.getBlockPos()));
        return wrap(((EntityHitResult) mc.hitResult).getEntity());
    }

    // Utility

    public static Identifier popIdentifier(Starscript ss, String errorMessage) {
        try {
            return Identifier.parse(ss.popString(errorMessage));
        }
        catch (IdentifierException e) {
            ss.error(e.getMessage());
            return null;
        }
    }

    // Wrapping

    public static Value wrap(ItemStack itemStack) {
        String name = itemStack.isEmpty() ? "" : Names.get(itemStack.getItem());

        int durability = 0;
        if (!itemStack.isEmpty() && itemStack.isDamageableItem()) durability = itemStack.getMaxDamage() - itemStack.getDamageValue();

        return Value.map(new ValueMap()
            .set("_toString", Value.string(itemStack.getCount() <= 1 ? name : String.format("%s %dx", name, itemStack.getCount())))
            .set("name", Value.string(name))
            .set("id", Value.string(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString()))
            .set("count", Value.number(itemStack.getCount()))
            .set("durability", Value.number(durability))
            .set("max_durability", Value.number(itemStack.getMaxDamage()))
        );
    }

    public static Value wrap(BlockPos blockPos, BlockState blockState) {
        return Value.map(new ValueMap()
            .set("_toString", Value.string(Names.get(blockState.getBlock())))
            .set("id", Value.string(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString()))
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
            .set("id", Value.string(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()))
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

    public static Value wrap(MobEffectInstance effectInstance) {
        return Value.map(new ValueMap()
            .set("duration", effectInstance.getDuration())
            .set("level", effectInstance.getAmplifier() + 1)
        );
    }
}
