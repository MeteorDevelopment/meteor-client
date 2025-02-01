/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.primitives.Doubles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.mixin.WorldAccessor;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.criterion.CriterionProgress;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.command.FloatRangeArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.predicate.NumberRange;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.Util;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Taken from <a href="https://github.com/xpple/clientarguments">clientarguments</a>
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2021 xpple
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author Xpple
 * @see <a href="https://github.com/xpple/clientarguments/blob/master/src/main/java/dev/xpple/clientarguments/arguments/CEntityArgument.java">CEntityArgument.java</a>
 * @see <a href="https://github.com/xpple/clientarguments/blob/master/src/main/java/dev/xpple/clientarguments/arguments/CEntitySelector.java">CEntitySelector.java</a>
 * @see <a href="https://github.com/xpple/clientarguments/blob/master/src/main/java/dev/xpple/clientarguments/arguments/CEntitySelectorOptions.java">CEntitySelectorOptions.java</a>
 * @see <a href="https://github.com/xpple/clientarguments/blob/master/src/main/java/dev/xpple/clientarguments/arguments/CEntitySelectorParser.java">CEntitySelectorParser.java</a>
 */
public class EntityArgumentType implements ArgumentType<EntityArgumentType.EntitySelector> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498");
    private static final SimpleCommandExceptionType TOO_MANY_ENTITIES_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.entity.toomany"));
    private static final SimpleCommandExceptionType TOO_MANY_PLAYERS_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.player.toomany"));
    private static final SimpleCommandExceptionType PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.player.entities"));
    private static final SimpleCommandExceptionType ENTITY_NOT_FOUND_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.entity.notfound.entity"));
    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.entity.notfound.player"));
    final boolean singleTarget;
    final boolean playersOnly;

    protected EntityArgumentType(boolean singleTarget, boolean playersOnly) {
        this.singleTarget = singleTarget;
        this.playersOnly = playersOnly;
    }

    public static EntityArgumentType entity() {
        return new EntityArgumentType(true, false);
    }

    public static <S> Entity getEntity(final CommandContext<S> context, final String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelector.class).findSingleEntity(context.getSource());
    }

    public static EntityArgumentType entities() {
        return new EntityArgumentType(false, false);
    }

    public static <S> Collection<? extends Entity> getEntities(final CommandContext<S> context, final String name) throws CommandSyntaxException {
        Collection<? extends Entity> collection = getOptionalEntities(context, name);
        if (collection.isEmpty()) {
            throw ENTITY_NOT_FOUND_EXCEPTION.create();
        }
        return collection;
    }

    public static <S> Collection<? extends Entity> getOptionalEntities(final CommandContext<S> context, final String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelector.class).findEntities(context.getSource());
    }

    public static <S> Collection<AbstractClientPlayerEntity> getOptionalPlayers(final CommandContext<S> context, final String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelector.class).findPlayers(context.getSource());
    }

    public static EntityArgumentType player() {
        return new EntityArgumentType(true, true);
    }

    public static <S> AbstractClientPlayerEntity getPlayer(final CommandContext<S> context, final String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelector.class).findSinglePlayer(context.getSource());
    }

    public static EntityArgumentType players() {
        return new EntityArgumentType(false, true);
    }

    public static <S> Collection<AbstractClientPlayerEntity> getPlayers(final CommandContext<S> context, final String name) throws CommandSyntaxException {
        List<AbstractClientPlayerEntity> list = context.getArgument(name, EntitySelector.class).findPlayers(context.getSource());
        if (list.isEmpty()) {
            throw PLAYER_NOT_FOUND_EXCEPTION.create();
        }
        return list;
    }

    @Override
    public EntitySelector parse(final StringReader stringReader) throws CommandSyntaxException {
        return this.parse(stringReader, true);
    }

    public <S> EntitySelector parse(final StringReader stringReader, final S source) throws CommandSyntaxException {
        return this.parse(stringReader, EntitySelectorParser.allowSelectors(source));
    }

    private EntitySelector parse(StringReader stringReader, boolean allowSelectors) throws CommandSyntaxException {
        EntitySelectorParser entitySelectorParser = new EntitySelectorParser(stringReader, allowSelectors);
        EntitySelector entitySelector = entitySelectorParser.parse();
        if (entitySelector.getMaxResults() > 1 && this.singleTarget) {
            if (this.playersOnly) {
                stringReader.setCursor(0);
                throw TOO_MANY_PLAYERS_EXCEPTION.createWithContext(stringReader);
            } else {
                stringReader.setCursor(0);
                throw TOO_MANY_ENTITIES_EXCEPTION.createWithContext(stringReader);
            }
        }
        if (entitySelector.includesEntities() && this.playersOnly && !entitySelector.isSelfSelector()) {
            stringReader.setCursor(0);
            throw PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION.createWithContext(stringReader);
        }
        return entitySelector;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        if (context.getSource() instanceof CommandSource commandSource) {
            StringReader stringReader = new StringReader(builder.getInput());
            stringReader.setCursor(builder.getStart());
            EntitySelectorParser entitySelectorParser = new EntitySelectorParser(stringReader, EntitySelectorParser.allowSelectors(commandSource));

            try {
                entitySelectorParser.parse();
            } catch (CommandSyntaxException ignored) {
            }

            return entitySelectorParser.fillSuggestions(builder, builderx -> {
                Collection<String> collection = commandSource.getPlayerNames();
                Iterable<String> iterable = this.playersOnly ? collection : Iterables.concat(collection, commandSource.getEntitySuggestions());
                CommandSource.suggestMatching(iterable, builderx);
            });
        }
        return Suggestions.empty();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class EntitySelectorOptions {

        private static final Map<String, SelectorOption> OPTIONS = Maps.newHashMap();
        public static final DynamicCommandExceptionType UNKNOWN_OPTION_EXCEPTION = new DynamicCommandExceptionType(option -> Text.stringifiedTranslatable("argument.entity.options.unknown", option));
        public static final DynamicCommandExceptionType INAPPLICABLE_OPTION_EXCEPTION = new DynamicCommandExceptionType(option -> Text.stringifiedTranslatable("argument.entity.options.inapplicable", option));
        public static final SimpleCommandExceptionType NEGATIVE_DISTANCE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.entity.options.distance.negative"));
        public static final SimpleCommandExceptionType NEGATIVE_LEVEL_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.entity.options.level.negative"));
        public static final SimpleCommandExceptionType TOO_SMALL_LEVEL_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.entity.options.limit.toosmall"));
        public static final DynamicCommandExceptionType IRREVERSIBLE_SORT_EXCEPTION = new DynamicCommandExceptionType(sortType -> Text.stringifiedTranslatable("argument.entity.options.sort.irreversible", sortType));
        public static final DynamicCommandExceptionType INVALID_MODE_EXCEPTION = new DynamicCommandExceptionType(gameMode -> Text.stringifiedTranslatable("argument.entity.options.mode.invalid", gameMode));
        public static final DynamicCommandExceptionType INVALID_TYPE_EXCEPTION = new DynamicCommandExceptionType(entity -> Text.stringifiedTranslatable("argument.entity.options.type.invalid", entity));

        private static void putOption(String id, SelectorHandler handler, Predicate<EntitySelectorParser> condition, Text description) {
            OPTIONS.put(id, new SelectorOption(handler, condition, description));
        }

        public static void register() {
            if (!OPTIONS.isEmpty()) {
                return;
            }
            putOption("name", reader -> {
                int cursor = reader.getReader().getCursor();
                boolean bl = reader.shouldInvertValue();
                String string = reader.getReader().readString();
                if (reader.hasNameNotEquals() && !bl) {
                    reader.getReader().setCursor(cursor);
                    throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "name");
                }
                if (bl) {
                    reader.setHasNameNotEquals(true);
                } else {
                    reader.setHasNameEquals(true);
                }

                reader.addPredicate(entity -> entity.getName().getString().equals(string) != bl);
            }, reader -> !reader.hasNameEquals(), Text.translatable("argument.entity.options.name.description"));
            putOption("distance", reader -> {
                int cursor = reader.getReader().getCursor();
                NumberRange.DoubleRange doubleRange = NumberRange.DoubleRange.parse(reader.getReader());
                if ((doubleRange.min().isEmpty() || !(doubleRange.min().get() < 0.0)) && (doubleRange.max().isEmpty() || !(doubleRange.max().get() < 0.0))) {
                    reader.setDistance(doubleRange);
                } else {
                    reader.getReader().setCursor(cursor);
                    throw NEGATIVE_DISTANCE_EXCEPTION.createWithContext(reader.getReader());
                }
            }, reader -> reader.getDistance().isDummy(), Text.translatable("argument.entity.options.distance.description"));
            putOption("level", reader -> {
                int cursor = reader.getReader().getCursor();
                NumberRange.IntRange intRange = NumberRange.IntRange.parse(reader.getReader());
                if ((intRange.min().isEmpty() || intRange.min().get() >= 0) && (intRange.max().isEmpty() || intRange.max().get() >= 0)) {
                    reader.setLevel(intRange);
                    reader.setIncludesEntities(false);
                } else {
                    reader.getReader().setCursor(cursor);
                    throw NEGATIVE_LEVEL_EXCEPTION.createWithContext(reader.getReader());
                }
            }, reader -> reader.getLevel().isDummy(), Text.translatable("argument.entity.options.level.description"));
            putOption("x", reader -> reader.setX(reader.getReader().readDouble()), reader -> reader.getX() == null, Text.translatable("argument.entity.options.x.description"));
            putOption("y", reader -> reader.setY(reader.getReader().readDouble()), reader -> reader.getY() == null, Text.translatable("argument.entity.options.y.description"));
            putOption("z", reader -> reader.setZ(reader.getReader().readDouble()), reader -> reader.getZ() == null, Text.translatable("argument.entity.options.z.description"));
            putOption("dx", reader -> reader.setDeltaX(reader.getReader().readDouble()), reader -> reader.getDeltaX() == null, Text.translatable("argument.entity.options.dx.description"));
            putOption("dy", reader -> reader.setDeltaY(reader.getReader().readDouble()), reader -> reader.getDeltaY() == null, Text.translatable("argument.entity.options.dy.description"));
            putOption("dz", reader -> reader.setDeltaZ(reader.getReader().readDouble()), reader -> reader.getDeltaZ() == null, Text.translatable("argument.entity.options.dz.description"));
            putOption("x_rotation", reader -> reader.setRotX(FloatRangeArgument.parse(reader.getReader(), true, MathHelper::wrapDegrees)), reader -> reader.getRotX() == FloatRangeArgument.ANY, Text.translatable("argument.entity.options.x_rotation.description"));
            putOption("y_rotation", reader -> reader.setRotY(FloatRangeArgument.parse(reader.getReader(), true, MathHelper::wrapDegrees)), reader -> reader.getRotY() == FloatRangeArgument.ANY, Text.translatable("argument.entity.options.y_rotation.description"));
            putOption("limit", reader -> {
                int cursor = reader.getReader().getCursor();
                int j = reader.getReader().readInt();
                if (j < 1) {
                    reader.getReader().setCursor(cursor);
                    throw TOO_SMALL_LEVEL_EXCEPTION.createWithContext(reader.getReader());
                }
                reader.setMaxResults(j);
                reader.setLimited(true);
            }, reader -> !reader.isCurrentEntity() && !reader.isLimited(), Text.translatable("argument.entity.options.limit.description"));
            putOption("sort", reader -> {
                int cursor = reader.getReader().getCursor();
                String string = reader.getReader().readUnquotedString();
                reader.setSuggestions((builder, consumer) -> CommandSource.suggestMatching(Arrays.asList("nearest", "furthest", "random", "arbitrary"), builder));

                reader.setOrder(switch (string) {
                    case "nearest" -> EntitySelectorParser.ORDER_NEAREST;
                    case "furthest" -> EntitySelectorParser.ORDER_FURTHEST;
                    case "random" -> EntitySelectorParser.ORDER_RANDOM;
                    case "arbitrary" -> EntitySelector.ORDER_ARBITRARY;
                    default -> {
                        reader.getReader().setCursor(cursor);
                        throw IRREVERSIBLE_SORT_EXCEPTION.createWithContext(reader.getReader(), string);
                    }
                });
                reader.setSorted(true);
            }, reader -> !reader.isCurrentEntity() && !reader.isSorted(), Text.translatable("argument.entity.options.sort.description"));
            putOption("gamemode", reader -> {
                reader.setSuggestions((builder, consumer) -> {
                    String stringxx = builder.getRemaining().toLowerCase(Locale.ROOT);
                    boolean blxx = !reader.hasGamemodeNotEquals();
                    boolean bl2 = true;
                    if (!stringxx.isEmpty()) {
                        if (stringxx.charAt(0) == EntitySelectorParser.SYNTAX_NOT) {
                            blxx = false;
                            stringxx = stringxx.substring(1);
                        } else {
                            bl2 = false;
                        }
                    }

                    for (GameMode gameType : GameMode.values()) {
                        if (gameType.getName().toLowerCase(Locale.ROOT).startsWith(stringxx)) {
                            if (bl2) {
                                builder.suggest("!" + gameType.getName());
                            }

                            if (blxx) {
                                builder.suggest(gameType.getName());
                            }
                        }
                    }

                    return builder.buildFuture();
                });
                int cursor = reader.getReader().getCursor();
                boolean bl = reader.shouldInvertValue();
                if (reader.hasGamemodeNotEquals() && !bl) {
                    reader.getReader().setCursor(cursor);
                    throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "gamemode");
                }
                String string = reader.getReader().readUnquotedString();
                GameMode gameType = GameMode.byName(string, null);
                if (gameType == null) {
                    reader.getReader().setCursor(cursor);
                    throw INVALID_MODE_EXCEPTION.createWithContext(reader.getReader(), string);
                }
                reader.setIncludesEntities(false);
                reader.addPredicate(entity -> {
                    if (!(entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity)) {
                        return false;
                    }
                    PlayerListEntry playerListEntry = mc.player.networkHandler.getPlayerListEntry(abstractClientPlayerEntity.getUuid());
                    if (playerListEntry == null) {
                        return false;
                    }
                    GameMode gameType2 = playerListEntry.getGameMode();
                    return bl == (gameType2 != gameType);
                });
                if (bl) {
                    reader.setHasGamemodeNotEquals(true);
                } else {
                    reader.setHasGamemodeEquals(true);
                }
            }, reader -> !reader.hasGamemodeEquals(), Text.translatable("argument.entity.options.gamemode.description"));
            putOption("team", reader -> {
                boolean bl = reader.shouldInvertValue();
                String string = reader.getReader().readUnquotedString();
                reader.addPredicate(entity -> {
                    if (!(entity instanceof LivingEntity)) {
                        return false;
                    }
                    Team team = entity.getScoreboardTeam();
                    String string2 = team == null ? "" : team.getName();
                    return string2.equals(string) != bl;
                });
                if (bl) {
                    reader.setHasTeamNotEquals(true);
                } else {
                    reader.setHasTeamEquals(true);
                }
            }, reader -> !reader.hasTeamEquals(), Text.translatable("argument.entity.options.team.description"));
            putOption("type", reader -> {
                reader.setSuggestions((builder, consumer) -> {
                    CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(), builder, String.valueOf(EntitySelectorParser.SYNTAX_NOT));
                    CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getTags().map(named -> named.getTag().id()), builder, "!#");
                    if (!reader.isTypeLimitedInversely()) {
                        CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(), builder);
                        CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getTags().map(named -> named.getTag().id()), builder, String.valueOf(EntitySelectorParser.SYNTAX_TAG));
                    }

                    return builder.buildFuture();
                });
                int cursor = reader.getReader().getCursor();
                boolean bl = reader.shouldInvertValue();
                if (reader.isTypeLimitedInversely() && !bl) {
                    reader.getReader().setCursor(cursor);
                    throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "type");
                }
                if (bl) {
                    reader.setTypeLimitedInversely();
                }

                if (reader.isTag()) {
                    TagKey<EntityType<?>> tagKey = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.fromCommandInput(reader.getReader()));
                    reader.addPredicate(entity -> entity.getType().isIn(tagKey) != bl);
                } else {
                    Identifier resourceLocation = Identifier.fromCommandInput(reader.getReader());
                    EntityType<?> entityType = Registries.ENTITY_TYPE.getOptionalValue(resourceLocation).orElseThrow(() -> {
                        reader.getReader().setCursor(cursor);
                        return INVALID_TYPE_EXCEPTION.createWithContext(reader.getReader(), resourceLocation.toString());
                    });
                    if (Objects.equals(EntityType.PLAYER, entityType) && !bl) {
                        reader.setIncludesEntities(false);
                    }

                    reader.addPredicate(entity -> Objects.equals(entityType, entity.getType()) != bl);
                    if (!bl) {
                        reader.limitToType(entityType);
                    }
                }
            }, reader -> !reader.isTypeLimited(), Text.translatable("argument.entity.options.type.description"));
            putOption("tag", reader -> {
                boolean bl = reader.shouldInvertValue();
                String string = reader.getReader().readUnquotedString();
                reader.addPredicate(entity -> {
                    if ("".equals(string)) {
                        return entity.getCommandTags().isEmpty() != bl;
                    } else {
                        return entity.getCommandTags().contains(string) != bl;
                    }
                });
            }, reader -> true, Text.translatable("argument.entity.options.tag.description"));
            putOption("nbt", reader -> {
                boolean bl = reader.shouldInvertValue();
                NbtCompound compoundTag = new StringNbtReader(reader.getReader()).parseCompound();
                reader.addPredicate(entity -> {
                    NbtCompound compoundTag2 = entity.writeNbt(new NbtCompound());
                    if (entity instanceof AbstractClientPlayerEntity abstractClientPlayer) {
                        ItemStack itemStack = abstractClientPlayer.getInventory().getMainHandStack();
                        if (!itemStack.isEmpty()) {
                            compoundTag2.put("SelectedItem", itemStack.toNbt(abstractClientPlayer.getRegistryManager()));
                        }
                    }

                    return NbtHelper.matches(compoundTag, compoundTag2, true) != bl;
                });
            }, reader -> true, Text.translatable("argument.entity.options.nbt.description"));
            putOption("scores", reader -> {
                StringReader stringReader = reader.getReader();
                Map<String, NumberRange.IntRange> map = Maps.newHashMap();
                stringReader.expect('{');
                stringReader.skipWhitespace();

                while (stringReader.canRead() && stringReader.peek() != '}') {
                    stringReader.skipWhitespace();
                    String string = stringReader.readUnquotedString();
                    stringReader.skipWhitespace();
                    stringReader.expect(EntitySelectorParser.SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR);
                    stringReader.skipWhitespace();
                    NumberRange.IntRange intRange = NumberRange.IntRange.parse(stringReader);
                    map.put(string, intRange);
                    stringReader.skipWhitespace();
                    if (stringReader.canRead() && stringReader.peek() == ',') {
                        stringReader.skip();
                    }
                }

                stringReader.expect('}');
                if (!map.isEmpty()) {
                    reader.addPredicate(entity -> {
                        try (World level = entity.getWorld()) {
                            Scoreboard scoreboard = level.getScoreboard();

                            for (Map.Entry<String, NumberRange.IntRange> entry : map.entrySet()) {
                                ScoreboardObjective objective = scoreboard.getNullableObjective(entry.getKey());
                                if (objective == null) {
                                    return false;
                                }

                                ReadableScoreboardScore readOnlyScoreInfo = scoreboard.getScore(entity, objective);
                                if (readOnlyScoreInfo == null) {
                                    return false;
                                }

                                if (!entry.getValue().test(readOnlyScoreInfo.getScore())) {
                                    return false;
                                }
                            }

                            return true;
                        } catch (IOException e) {
                            return false;
                        }
                    });
                }

                reader.setHasScores(true);
            }, reader -> !reader.hasScores(), Text.translatable("argument.entity.options.scores.description"));
            putOption("advancements", reader -> {
                StringReader stringReader = reader.getReader();
                Map<Identifier, Predicate<AdvancementProgress>> map = Maps.newHashMap();
                stringReader.expect('{');
                stringReader.skipWhitespace();

                while (stringReader.canRead() && stringReader.peek() != '}') {
                    stringReader.skipWhitespace();
                    Identifier resourceLocation = Identifier.fromCommandInput(stringReader);
                    stringReader.skipWhitespace();
                    stringReader.expect(EntitySelectorParser.SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR);
                    stringReader.skipWhitespace();
                    if (stringReader.canRead() && stringReader.peek() == '{') {
                        Map<String, Predicate<CriterionProgress>> map2 = Maps.newHashMap();
                        stringReader.skipWhitespace();
                        stringReader.expect('{');
                        stringReader.skipWhitespace();

                        while (stringReader.canRead() && stringReader.peek() != '}') {
                            stringReader.skipWhitespace();
                            String string = stringReader.readUnquotedString();
                            stringReader.skipWhitespace();
                            stringReader.expect(EntitySelectorParser.SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR);
                            stringReader.skipWhitespace();
                            boolean bl = stringReader.readBoolean();
                            map2.put(string, criterionProgress -> criterionProgress.isObtained() == bl);
                            stringReader.skipWhitespace();
                            if (stringReader.canRead() && stringReader.peek() == ',') {
                                stringReader.skip();
                            }
                        }

                        stringReader.skipWhitespace();
                        stringReader.expect('}');
                        stringReader.skipWhitespace();
                        map.put(resourceLocation, advancementProgress -> {
                            for (Map.Entry<String, Predicate<CriterionProgress>> entry : map2.entrySet()) {
                                CriterionProgress criterionProgress = advancementProgress.getCriterionProgress(entry.getKey());
                                if (criterionProgress == null || !entry.getValue().test(criterionProgress)) {
                                    return false;
                                }
                            }

                            return true;
                        });
                    } else {
                        boolean bl2 = stringReader.readBoolean();
                        map.put(resourceLocation, advancementProgress -> advancementProgress.isDone() == bl2);
                    }

                    stringReader.skipWhitespace();
                    if (stringReader.canRead() && stringReader.peek() == ',') {
                        stringReader.skip();
                    }
                }

                stringReader.expect('}');
                if (!map.isEmpty()) {
                    reader.addPredicate(entity -> false);
                    reader.setIncludesEntities(false);
                }

                reader.setHasAdvancements(true);
            }, reader -> !reader.hasAdvancements(), Text.translatable("argument.entity.options.advancements.description"));
            putOption("predicate", reader -> reader.addPredicate(entity -> false), reader -> true, Text.translatable("argument.entity.options.predicate.description"));
        }

        public static SelectorHandler getHandler(EntitySelectorParser reader, String option, int restoreCursor) throws CommandSyntaxException {
            SelectorOption selectorOption = OPTIONS.get(option);
            if (selectorOption != null) {
                if (selectorOption.condition.test(reader)) {
                    return selectorOption.handler;
                }
                throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), option);
            }
            reader.getReader().setCursor(restoreCursor);
            throw UNKNOWN_OPTION_EXCEPTION.createWithContext(reader.getReader(), option);
        }

        public static void suggestOptions(EntitySelectorParser reader, SuggestionsBuilder suggestionBuilder) {
            String string = suggestionBuilder.getRemaining().toLowerCase(Locale.ROOT);

            for (Map.Entry<String, SelectorOption> entry : OPTIONS.entrySet()) {
                if (entry.getValue().condition.test(reader) && entry.getKey().toLowerCase(Locale.ROOT).startsWith(string)) {
                    suggestionBuilder.suggest(entry.getKey() + "=", entry.getValue().description);
                }
            }
        }

        public interface SelectorHandler {
            void handle(EntitySelectorParser reader) throws CommandSyntaxException;
        }

        record SelectorOption(SelectorHandler handler, Predicate<EntitySelectorParser> condition, Text description) {}
    }

    public static class EntitySelectorParser {
        public static final char SYNTAX_SELECTOR_START = '@';
        private static final char SYNTAX_OPTIONS_START = '[';
        private static final char SYNTAX_OPTIONS_END = ']';
        public static final char SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR = '=';
        private static final char SYNTAX_OPTIONS_SEPARATOR = ',';
        public static final char SYNTAX_NOT = '!';
        public static final char SYNTAX_TAG = '#';
        private static final char SELECTOR_NEAREST_PLAYER = 'p';
        private static final char SELECTOR_ALL_PLAYERS = 'a';
        private static final char SELECTOR_RANDOM_PLAYERS = 'r';
        private static final char SELECTOR_CURRENT_ENTITY = 's';
        private static final char SELECTOR_ALL_ENTITIES = 'e';
        private static final char SELECTOR_NEAREST_ENTITY = 'n';
        public static final SimpleCommandExceptionType ERROR_INVALID_NAME_OR_UUID = new SimpleCommandExceptionType(Text.translatable("argument.entity.invalid"));
        public static final DynamicCommandExceptionType ERROR_UNKNOWN_SELECTOR_TYPE = new DynamicCommandExceptionType(type -> Text.stringifiedTranslatable("argument.entity.selector.unknown", type));
        public static final SimpleCommandExceptionType ERROR_SELECTORS_NOT_ALLOWED = new SimpleCommandExceptionType(Text.translatable("argument.entity.selector.not_allowed"));
        public static final SimpleCommandExceptionType ERROR_MISSING_SELECTOR_TYPE = new SimpleCommandExceptionType(Text.translatable("argument.entity.selector.missing"));
        public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_OPTIONS = new SimpleCommandExceptionType(Text.translatable("argument.entity.options.unterminated"));
        public static final DynamicCommandExceptionType ERROR_EXPECTED_OPTION_VALUE = new DynamicCommandExceptionType(value -> Text.stringifiedTranslatable("argument.entity.options.valueless", value));
        public static final BiConsumer<Vec3d, List<? extends Entity>> ORDER_NEAREST = (pos, entities) -> entities.sort((entity1, entity2) -> Doubles.compare(entity1.squaredDistanceTo(pos), entity2.squaredDistanceTo(pos)));
        public static final BiConsumer<Vec3d, List<? extends Entity>> ORDER_FURTHEST = (pos, entities) -> entities.sort((entity1, entity2) -> Doubles.compare(entity2.squaredDistanceTo(pos), entity1.squaredDistanceTo(pos)));
        public static final BiConsumer<Vec3d, List<? extends Entity>> ORDER_RANDOM = (pos, entities) -> Collections.shuffle(entities);
        public static final BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> SUGGEST_NOTHING = (builder, consumer) -> builder.buildFuture();
        private final StringReader reader;
        private final boolean allowSelectors;
        private int maxResults;
        private boolean includesEntities;
        private boolean worldLimited;
        private NumberRange.DoubleRange distance = NumberRange.DoubleRange.ANY;
        private NumberRange.IntRange level = NumberRange.IntRange.ANY;
        @Nullable
        private Double x;
        @Nullable
        private Double y;
        @Nullable
        private Double z;
        @Nullable
        private Double deltaX;
        @Nullable
        private Double deltaY;
        @Nullable
        private Double deltaZ;
        private FloatRangeArgument rotX = FloatRangeArgument.ANY;
        private FloatRangeArgument rotY = FloatRangeArgument.ANY;
        private final List<Predicate<Entity>> predicates = new ArrayList<>();
        private BiConsumer<Vec3d, List<? extends Entity>> order = EntitySelector.ORDER_ARBITRARY;
        private boolean currentEntity;
        @Nullable
        private String playerName;
        private int startPosition;
        @Nullable
        private UUID entityUUID;
        private BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;
        private boolean hasNameEquals;
        private boolean hasNameNotEquals;
        private boolean isLimited;
        private boolean isSorted;
        private boolean hasGamemodeEquals;
        private boolean hasGamemodeNotEquals;
        private boolean hasTeamEquals;
        private boolean hasTeamNotEquals;
        @Nullable
        private EntityType<?> type;
        private boolean typeInverse;
        private boolean hasScores;
        private boolean hasAdvancements;
        private boolean usesSelectors;

        public EntitySelectorParser(StringReader reader, boolean allowSelectors) {
            this.reader = reader;
            this.allowSelectors = allowSelectors;
        }

        public static <S> boolean allowSelectors(S source) {
            return source instanceof CommandSource;
        }

        public EntitySelector getSelector() {
            Box box;
            if (this.deltaX == null && this.deltaY == null && this.deltaZ == null) {
                if (this.distance.max().isPresent()) {
                    double d = this.distance.max().get();
                    box = new Box(-d, -d, -d, d + 1.0, d + 1.0, d + 1.0);
                } else {
                    box = null;
                }
            } else {
                box = this.createBox(this.deltaX == null ? 0.0 : this.deltaX, this.deltaY == null ? 0.0 : this.deltaY, this.deltaZ == null ? 0.0 : this.deltaZ);
            }

            Function<Vec3d, Vec3d> function;
            if (this.x == null && this.y == null && this.z == null) {
                function = vec3 -> vec3;
            } else {
                function = vec3 -> new Vec3d(this.x == null ? vec3.x : this.x, this.y == null ? vec3.y : this.y, this.z == null ? vec3.z : this.z);
            }

            return new EntitySelector(this.maxResults, this.includesEntities, this.worldLimited, List.copyOf(this.predicates), this.distance, function, box, this.order, this.currentEntity, this.playerName, this.entityUUID, this.type, this.usesSelectors);
        }

        private Box createBox(double sizeX, double sizeY, double sizeZ) {
            boolean bl = sizeX < 0.0;
            boolean bl2 = sizeY < 0.0;
            boolean bl3 = sizeZ < 0.0;
            double d = bl ? sizeX : 0.0;
            double e = bl2 ? sizeY : 0.0;
            double f = bl3 ? sizeZ : 0.0;
            double g = (bl ? 0.0 : sizeX) + 1.0;
            double h = (bl2 ? 0.0 : sizeY) + 1.0;
            double i = (bl3 ? 0.0 : sizeZ) + 1.0;
            return new Box(d, e, f, g, h, i);
        }

        private void finalizePredicates() {
            if (this.rotX != FloatRangeArgument.ANY) {
                this.predicates.add(this.createRotationPredicate(this.rotX, Entity::getPitch));
            }

            if (this.rotY != FloatRangeArgument.ANY) {
                this.predicates.add(this.createRotationPredicate(this.rotY, Entity::getYaw));
            }

            if (!this.level.isDummy()) {
                this.predicates.add(entity -> entity instanceof AbstractClientPlayerEntity abstractClientPlayer && this.level.test(abstractClientPlayer.experienceLevel));
            }
        }

        private Predicate<Entity> createRotationPredicate(FloatRangeArgument angleBounds, ToDoubleFunction<Entity> angleFunction) {
            double d = MathHelper.wrapDegrees(angleBounds.min() == null ? 0.0F : angleBounds.min());
            double e = MathHelper.wrapDegrees(angleBounds.max() == null ? 359.0F : angleBounds.max());
            return entity -> {
                double f = MathHelper.wrapDegrees(angleFunction.applyAsDouble(entity));
                return d > e ? f >= d || f <= e : f >= d && f <= e;
            };
        }

        protected void parseSelector() throws CommandSyntaxException {
            this.usesSelectors = true;
            this.suggestions = this::suggestSelector;
            if (!this.reader.canRead()) {
                throw ERROR_MISSING_SELECTOR_TYPE.createWithContext(this.reader);
            }
            int cursor = this.reader.getCursor();
            char c = this.reader.read();

            if (switch (c) {
                case SELECTOR_ALL_PLAYERS -> {
                    this.maxResults = Integer.MAX_VALUE;
                    this.includesEntities = false;
                    this.order = EntitySelector.ORDER_ARBITRARY;
                    this.limitToType(EntityType.PLAYER);
                    yield false;
                }
                case SELECTOR_ALL_ENTITIES -> {
                    this.maxResults = Integer.MAX_VALUE;
                    this.includesEntities = true;
                    this.order = EntitySelector.ORDER_ARBITRARY;
                    yield true;
                }
                case SELECTOR_NEAREST_ENTITY -> {
                    this.maxResults = 1;
                    this.includesEntities = true;
                    this.order = ORDER_NEAREST;
                    yield true;
                }
                case SELECTOR_NEAREST_PLAYER -> {
                    this.maxResults = 1;
                    this.includesEntities = false;
                    this.order = ORDER_NEAREST;
                    this.limitToType(EntityType.PLAYER);
                    yield false;
                }
                case SELECTOR_RANDOM_PLAYERS -> {
                    this.maxResults = 1;
                    this.includesEntities = false;
                    this.order = ORDER_RANDOM;
                    this.limitToType(EntityType.PLAYER);
                    yield false;
                }
                case SELECTOR_CURRENT_ENTITY -> {
                    this.maxResults = 1;
                    this.includesEntities = true;
                    this.currentEntity = true;
                    yield false;
                }
                default -> {
                    this.reader.setCursor(cursor);
                    throw ERROR_UNKNOWN_SELECTOR_TYPE.createWithContext(this.reader, "@" + c);
                }
            }) {
                this.predicates.add(Entity::isAlive);
            }

            this.suggestions = this::suggestOpenOptions;
            if (this.reader.canRead() && this.reader.peek() == SYNTAX_OPTIONS_START) {
                this.reader.skip();
                this.suggestions = this::suggestOptionsKeyOrClose;
                this.parseOptions();
            }

        }

        protected void parseNameOrUUID() throws CommandSyntaxException {
            if (this.reader.canRead()) {
                this.suggestions = this::suggestName;
            }

            int cursor = this.reader.getCursor();
            String string = this.reader.readString();

            try {
                this.entityUUID = UUID.fromString(string);
                this.includesEntities = true;
            } catch (IllegalArgumentException var4) {
                if (string.isEmpty() || string.length() > 16) {
                    this.reader.setCursor(cursor);
                    throw ERROR_INVALID_NAME_OR_UUID.createWithContext(this.reader);
                }

                this.includesEntities = false;
                this.playerName = string;
            }

            this.maxResults = 1;
        }

        protected void parseOptions() throws CommandSyntaxException {
            this.suggestions = this::suggestOptionsKey;
            this.reader.skipWhitespace();

            while (this.reader.canRead() && this.reader.peek() != SYNTAX_OPTIONS_END) {
                this.reader.skipWhitespace();
                int i = this.reader.getCursor();
                String string = this.reader.readString();
                EntitySelectorOptions.SelectorHandler handler = EntitySelectorOptions.getHandler(this, string, i);
                this.reader.skipWhitespace();
                if (!this.reader.canRead() || this.reader.peek() != SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR) {
                    this.reader.setCursor(i);
                    throw ERROR_EXPECTED_OPTION_VALUE.createWithContext(this.reader, string);
                }

                this.reader.skip();
                this.reader.skipWhitespace();
                this.suggestions = SUGGEST_NOTHING;
                handler.handle(this);
                this.reader.skipWhitespace();
                this.suggestions = this::suggestOptionsNextOrClose;
                if (this.reader.canRead()) {
                    if (this.reader.peek() != SYNTAX_OPTIONS_SEPARATOR) {
                        if (this.reader.peek() != SYNTAX_OPTIONS_END) {
                            throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext(this.reader);
                        }
                        break;
                    }

                    this.reader.skip();
                    this.suggestions = this::suggestOptionsKey;
                }
            }

            if (!this.reader.canRead()) {
                throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext(this.reader);
            }
            this.reader.skip();
            this.suggestions = SUGGEST_NOTHING;
        }

        public boolean shouldInvertValue() {
            this.reader.skipWhitespace();
            if (this.reader.canRead() && this.reader.peek() == SYNTAX_NOT) {
                this.reader.skip();
                this.reader.skipWhitespace();
                return true;
            }
            return false;
        }

        public boolean isTag() {
            this.reader.skipWhitespace();
            if (this.reader.canRead() && this.reader.peek() == SYNTAX_TAG) {
                this.reader.skip();
                this.reader.skipWhitespace();
                return true;
            }
            return false;
        }

        public StringReader getReader() {
            return this.reader;
        }

        public void addPredicate(Predicate<Entity> predicate) {
            this.predicates.add(predicate);
        }

        public void setWorldLimited() {
            this.worldLimited = true;
        }

        public NumberRange.DoubleRange getDistance() {
            return this.distance;
        }

        public void setDistance(NumberRange.DoubleRange distance) {
            this.distance = distance;
        }

        public NumberRange.IntRange getLevel() {
            return this.level;
        }

        public void setLevel(NumberRange.IntRange level) {
            this.level = level;
        }

        public FloatRangeArgument getRotX() {
            return this.rotX;
        }

        public void setRotX(FloatRangeArgument rotX) {
            this.rotX = rotX;
        }

        public FloatRangeArgument getRotY() {
            return this.rotY;
        }

        public void setRotY(FloatRangeArgument rotY) {
            this.rotY = rotY;
        }

        @Nullable
        public Double getX() {
            return this.x;
        }

        @Nullable
        public Double getY() {
            return this.y;
        }

        @Nullable
        public Double getZ() {
            return this.z;
        }

        public void setX(double x) {
            this.x = x;
        }

        public void setY(double y) {
            this.y = y;
        }

        public void setZ(double z) {
            this.z = z;
        }

        public void setDeltaX(double deltaX) {
            this.deltaX = deltaX;
        }

        public void setDeltaY(double deltaY) {
            this.deltaY = deltaY;
        }

        public void setDeltaZ(double deltaZ) {
            this.deltaZ = deltaZ;
        }

        @Nullable
        public Double getDeltaX() {
            return this.deltaX;
        }

        @Nullable
        public Double getDeltaY() {
            return this.deltaY;
        }

        @Nullable
        public Double getDeltaZ() {
            return this.deltaZ;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        public void setIncludesEntities(boolean includesEntities) {
            this.includesEntities = includesEntities;
        }

        public BiConsumer<Vec3d, List<? extends Entity>> getOrder() {
            return this.order;
        }

        public void setOrder(BiConsumer<Vec3d, List<? extends Entity>> order) {
            this.order = order;
        }

        public EntitySelector parse() throws CommandSyntaxException {
            this.startPosition = this.reader.getCursor();
            this.suggestions = this::suggestNameOrSelector;
            if (this.reader.canRead() && this.reader.peek() == SYNTAX_SELECTOR_START) {
                if (!this.allowSelectors) {
                    throw ERROR_SELECTORS_NOT_ALLOWED.createWithContext(this.reader);
                }

                this.reader.skip();
                this.parseSelector();
            } else {
                this.parseNameOrUUID();
            }

            this.finalizePredicates();
            return this.getSelector();
        }

        private static void fillSelectorSuggestions(SuggestionsBuilder builder) {
            builder.suggest("@p", Text.translatable("argument.entity.selector.nearestPlayer"));
            builder.suggest("@a", Text.translatable("argument.entity.selector.allPlayers"));
            builder.suggest("@r", Text.translatable("argument.entity.selector.randomPlayer"));
            builder.suggest("@s", Text.translatable("argument.entity.selector.self"));
            builder.suggest("@e", Text.translatable("argument.entity.selector.allEntities"));
            builder.suggest("@n", Text.translatable("argument.entity.selector.nearestEntity"));
        }

        private CompletableFuture<Suggestions> suggestNameOrSelector(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
            consumer.accept(builder);
            if (this.allowSelectors) {
                fillSelectorSuggestions(builder);
            }

            return builder.buildFuture();
        }

        private CompletableFuture<Suggestions> suggestName(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
            SuggestionsBuilder suggestionsBuilder = builder.createOffset(this.startPosition);
            consumer.accept(suggestionsBuilder);
            return builder.add(suggestionsBuilder).buildFuture();
        }

        private CompletableFuture<Suggestions> suggestSelector(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
            SuggestionsBuilder suggestionsBuilder = builder.createOffset(builder.getStart() - 1);
            fillSelectorSuggestions(suggestionsBuilder);
            builder.add(suggestionsBuilder);
            return builder.buildFuture();
        }

        private CompletableFuture<Suggestions> suggestOpenOptions(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
            builder.suggest(String.valueOf(SYNTAX_OPTIONS_START));
            return builder.buildFuture();
        }

        private CompletableFuture<Suggestions> suggestOptionsKeyOrClose(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
            builder.suggest(String.valueOf(SYNTAX_OPTIONS_END));
            EntitySelectorOptions.suggestOptions(this, builder);
            return builder.buildFuture();
        }

        private CompletableFuture<Suggestions> suggestOptionsKey(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
            EntitySelectorOptions.suggestOptions(this, builder);
            return builder.buildFuture();
        }

        private CompletableFuture<Suggestions> suggestOptionsNextOrClose(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
            builder.suggest(String.valueOf(SYNTAX_OPTIONS_SEPARATOR));
            builder.suggest(String.valueOf(SYNTAX_OPTIONS_END));
            return builder.buildFuture();
        }

        private CompletableFuture<Suggestions> suggestEquals(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
            builder.suggest(String.valueOf(SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR));
            return builder.buildFuture();
        }

        public boolean isCurrentEntity() {
            return this.currentEntity;
        }

        public void setSuggestions(BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> suggestionHandler) {
            this.suggestions = suggestionHandler;
        }

        public CompletableFuture<Suggestions> fillSuggestions(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
            return this.suggestions.apply(builder.createOffset(this.reader.getCursor()), consumer);
        }

        public boolean hasNameEquals() {
            return this.hasNameEquals;
        }

        public void setHasNameEquals(boolean hasNameEquals) {
            this.hasNameEquals = hasNameEquals;
        }

        public boolean hasNameNotEquals() {
            return this.hasNameNotEquals;
        }

        public void setHasNameNotEquals(boolean hasNameNotEquals) {
            this.hasNameNotEquals = hasNameNotEquals;
        }

        public boolean isLimited() {
            return this.isLimited;
        }

        public void setLimited(boolean isLimited) {
            this.isLimited = isLimited;
        }

        public boolean isSorted() {
            return this.isSorted;
        }

        public void setSorted(boolean isSorted) {
            this.isSorted = isSorted;
        }

        public boolean hasGamemodeEquals() {
            return this.hasGamemodeEquals;
        }

        public void setHasGamemodeEquals(boolean hasGamemodeEquals) {
            this.hasGamemodeEquals = hasGamemodeEquals;
        }

        public boolean hasGamemodeNotEquals() {
            return this.hasGamemodeNotEquals;
        }

        public void setHasGamemodeNotEquals(boolean hasGamemodeNotEquals) {
            this.hasGamemodeNotEquals = hasGamemodeNotEquals;
        }

        public boolean hasTeamEquals() {
            return this.hasTeamEquals;
        }

        public void setHasTeamEquals(boolean hasTeamEquals) {
            this.hasTeamEquals = hasTeamEquals;
        }

        public boolean hasTeamNotEquals() {
            return this.hasTeamNotEquals;
        }

        public void setHasTeamNotEquals(boolean hasTeamNotEquals) {
            this.hasTeamNotEquals = hasTeamNotEquals;
        }

        public void limitToType(EntityType<?> type) {
            this.type = type;
        }

        public void setTypeLimitedInversely() {
            this.typeInverse = true;
        }

        public boolean isTypeLimited() {
            return this.type != null;
        }

        public boolean isTypeLimitedInversely() {
            return this.typeInverse;
        }

        public boolean hasScores() {
            return this.hasScores;
        }

        public void setHasScores(boolean hasScores) {
            this.hasScores = hasScores;
        }

        public boolean hasAdvancements() {
            return this.hasAdvancements;
        }

        public void setHasAdvancements(boolean hasAdvancements) {
            this.hasAdvancements = hasAdvancements;
        }
    }

    public static class EntitySelector {

        public static final int INFINITE = Integer.MAX_VALUE;
        public static final BiConsumer<Vec3d, List<? extends Entity>> ORDER_ARBITRARY = (center, entityList) -> {};
        private static final TypeFilter<Entity, ?> ANY_TYPE = new TypeFilter<>() {
            @Override
            public Entity downcast(Entity entity) {
                return entity;
            }

            @Override
            public Class<? extends Entity> getBaseClass() {
                return Entity.class;
            }
        };
        private final int maxResults;
        private final boolean includesEntities;
        private final boolean worldLimited;
        private final List<Predicate<Entity>> contextFreePredicates;
        private final NumberRange.DoubleRange range;
        private final Function<Vec3d, Vec3d> position;
        @Nullable
        private final Box Box;
        private final BiConsumer<Vec3d, List<? extends Entity>> order;
        private final boolean currentEntity;
        @Nullable
        private final String playerName;
        @Nullable
        private final UUID entityUUID;
        private final TypeFilter<Entity, ?> type;
        private final boolean usesSelector;

        public EntitySelector(int maxResults, boolean includesEntities, boolean worldLimited, List<Predicate<Entity>> contextFreePredicates, NumberRange.DoubleRange range, Function<Vec3d, Vec3d> position, @Nullable Box Box, BiConsumer<Vec3d, List<? extends Entity>> order, boolean currentEntity, @Nullable String playerName, @Nullable UUID entityUUID, @Nullable EntityType<?> type, boolean usesSelector) {
            this.maxResults = maxResults;
            this.includesEntities = includesEntities;
            this.worldLimited = worldLimited;
            this.contextFreePredicates = contextFreePredicates;
            this.range = range;
            this.position = position;
            this.Box = Box;
            this.order = order;
            this.currentEntity = currentEntity;
            this.playerName = playerName;
            this.entityUUID = entityUUID;
            this.type = type == null ? ANY_TYPE : type;
            this.usesSelector = usesSelector;
        }

        public int getMaxResults() {
            return this.maxResults;
        }

        public boolean includesEntities() {
            return this.includesEntities;
        }

        public boolean isSelfSelector() {
            return this.currentEntity;
        }

        public boolean isWorldLimited() {
            return this.worldLimited;
        }

        public boolean usesSelector() {
            return this.usesSelector;
        }

        public <S> Entity findSingleEntity(S source) throws CommandSyntaxException {
            List<? extends Entity> list = this.findEntities(source);
            if (list.isEmpty()) {
                throw EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
            }
            if (list.size() > 1) {
                throw EntityArgumentType.TOO_MANY_ENTITIES_EXCEPTION.create();
            }
            return list.getFirst();
        }

        public <S> List<? extends Entity> findEntities(S source) throws CommandSyntaxException {
            if (!this.includesEntities) {
                return this.findPlayers(source);
            }
            if (this.playerName != null) {
                AbstractClientPlayerEntity abstractClientPlayer = Streams.stream(mc.world.getEntities())
                    .filter(entity -> entity instanceof AbstractClientPlayerEntity)
                    .map(entity -> (AbstractClientPlayerEntity) entity)
                    .filter(abstractPlayer -> abstractPlayer.getName().getString().equals(this.playerName))
                    .findAny().orElse(null);
                return abstractClientPlayer == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayer);
            }
            if (this.entityUUID != null) {
                Entity foundEntity = Streams.stream(mc.world.getEntities())
                    .filter(entity -> entity.getUuid().equals(this.entityUUID))
                    .findAny().orElse(null);
                return foundEntity == null ? Collections.emptyList() : Lists.newArrayList(foundEntity);
            }

            Vec3d Vec3d = this.position.apply(mc.player.getPos());
            Box Box = this.getAbsoluteBox(Vec3d);
            Predicate<Entity> predicate = this.getPredicate(Vec3d, Box, null);
            if (this.currentEntity) {
                return mc.player != null && predicate.test(mc.player)
                    ? Lists.newArrayList(mc.player)
                    : Collections.emptyList();
            }
            List<Entity> list = Lists.newArrayList();
            this.addEntities(list, mc.world, Box, predicate);
            return list;
        }

        private void addEntities(List<Entity> entities, ClientWorld world, @Nullable Box box, Predicate<Entity> predicate) {
            int resultLimit = this.getResultLimit();
            if (entities.size() < resultLimit) {
                if (box != null) {
                    world.collectEntitiesByType(this.type, box, predicate, entities, resultLimit);
                } else {
                    ((WorldAccessor) world).getEntityLookup().forEach(this.type, entity -> {
                        if (predicate.test(entity)) {
                            entities.add(entity);
                            if (entities.size() >= maxResults) {
                                return LazyIterationConsumer.NextIteration.ABORT;
                            }
                        }

                        return LazyIterationConsumer.NextIteration.CONTINUE;
                    });
                }
            }
        }

        private int getResultLimit() {
            return this.order == ORDER_ARBITRARY ? this.maxResults : INFINITE;
        }

        public <S> AbstractClientPlayerEntity findSinglePlayer(S source) throws CommandSyntaxException {
            List<AbstractClientPlayerEntity> list = this.findPlayers(source);
            if (list.size() != 1) {
                throw EntityArgumentType.PLAYER_NOT_FOUND_EXCEPTION.create();
            }
            return list.getFirst();
        }

        public <S> List<AbstractClientPlayerEntity> findPlayers(S source) throws CommandSyntaxException {
            AbstractClientPlayerEntity abstractClientPlayer;
            if (this.playerName != null) {
                abstractClientPlayer = Streams.stream(mc.world.getEntities())
                    .filter(entity -> entity instanceof AbstractClientPlayerEntity)
                    .map(entity -> (AbstractClientPlayerEntity) entity)
                    .filter(abstractPlayer -> abstractPlayer.getName().getString().equals(this.playerName))
                    .findAny().orElse(null);
                return abstractClientPlayer == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayer);
            }
            if (this.entityUUID != null) {
                abstractClientPlayer = Streams.stream(mc.world.getEntities())
                    .filter(entity -> entity instanceof AbstractClientPlayerEntity)
                    .map(entity -> (AbstractClientPlayerEntity) entity)
                    .filter(entity -> entity.getUuid().equals(this.entityUUID))
                    .findAny().orElse(null);
                return abstractClientPlayer == null ? Collections.emptyList() : Lists.newArrayList(abstractClientPlayer);
            }
            Vec3d Vec3dd = this.position.apply(mc.player.getPos());
            Predicate<Entity> predicate = this.getPredicate(Vec3dd, this.getAbsoluteBox(Vec3dd), null);
            if (this.currentEntity) {
                if (mc.player instanceof AbstractClientPlayerEntity player && predicate.test(player)) {
                    return Lists.newArrayList(player);
                }

                return Collections.emptyList();
            }
            List<AbstractClientPlayerEntity> entities = mc.world.getPlayers().stream()
                .filter(predicate)
                .limit(this.getResultLimit())
                .collect(Collectors.toList());

            return this.sortAndLimit(Vec3dd, entities);
        }

        @Nullable
        private Box getAbsoluteBox(Vec3d pos) {
            return this.Box != null ? this.Box.offset(pos) : null;
        }

        private Predicate<Entity> getPredicate(Vec3d pos, @Nullable Box box, @Nullable FeatureSet enabledFeatures) {
            boolean bl = enabledFeatures != null;
            boolean bl2 = box != null;
            boolean bl3 = !this.range.isDummy();
            int i = (bl ? 1 : 0) + (bl2 ? 1 : 0) + (bl3 ? 1 : 0);
            List<Predicate<Entity>> list;
            if (i == 0) {
                list = this.contextFreePredicates;
            } else {
                List<Predicate<Entity>> list2 = new ObjectArrayList<>(this.contextFreePredicates.size() + i);
                list2.addAll(this.contextFreePredicates);
                if (bl) {
                    list2.add(entity -> entity.getType().isEnabled(enabledFeatures));
                }

                if (bl2) {
                    list2.add(entity -> box.intersects(entity.getBoundingBox()));
                }

                if (bl3) {
                    list2.add(entity -> this.range.testSqrt(entity.squaredDistanceTo(pos)));
                }

                list = list2;
            }

            return Util.allOf(list);
        }

        private <T extends Entity> List<T> sortAndLimit(Vec3d pos, List<T> entities) {
            if (entities.size() > 1) {
                this.order.accept(pos, entities);
            }

            return entities.subList(0, Math.min(this.maxResults, entities.size()));
        }

        public static Text joinNames(List<? extends Entity> names) {
            return Texts.join(names, Entity::getDisplayName);
        }
    }
}
