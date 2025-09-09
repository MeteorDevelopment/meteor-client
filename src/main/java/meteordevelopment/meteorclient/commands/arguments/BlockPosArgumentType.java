/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
 * @see <a href="https://github.com/xpple/clientarguments/blob/master/src/main/java/dev/xpple/clientarguments/arguments/CBlockPosArgument.java">CBlockPosArgument.java</a>
 * @see <a href="https://github.com/xpple/clientarguments/blob/master/src/main/java/dev/xpple/clientarguments/arguments/CCoordinates.java">CCoordinates.java</a>
 * @see <a href="https://github.com/xpple/clientarguments/blob/master/src/main/java/dev/xpple/clientarguments/arguments/CWorldCoordinates.java">CWorldCoordinates.java</a>
 * @see <a href="https://github.com/xpple/clientarguments/blob/master/src/main/java/dev/xpple/clientarguments/arguments/CLocalCoordinates.java">CLocalCoordinates.java</a>
 */
public class BlockPosArgumentType implements ArgumentType<BlockPosArgumentType.PosArgument> {
    private static final BlockPosArgumentType INSTANCE = new BlockPosArgumentType();
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "~0.5 ~1 ~-5");
    public static final SimpleCommandExceptionType UNLOADED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.pos.unloaded"));
    public static final SimpleCommandExceptionType OUT_OF_WORLD_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.pos.outofworld"));
    public static final SimpleCommandExceptionType OUT_OF_BOUNDS_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.pos.outofbounds"));

    private BlockPosArgumentType() {}

    public static BlockPosArgumentType blockPos() {
        return INSTANCE;
    }

    public static <S> BlockPos getLoadedBlockPos(CommandContext<S> context, String name) throws CommandSyntaxException {
        ClientWorld clientLevel = mc.world;
        return getLoadedBlockPos(context, clientLevel, name);
    }

    public static <S> BlockPos getLoadedBlockPos(CommandContext<S> context, ClientWorld level, String name) throws CommandSyntaxException {
        BlockPos blockPos = getBlockPos(context, name);
        ChunkPos chunkPos = new ChunkPos(blockPos);
        if (!level.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
            throw UNLOADED_EXCEPTION.create();
        } else if (!level.isInBuildLimit(blockPos)) {
            throw OUT_OF_WORLD_EXCEPTION.create();
        } else {
            return blockPos;
        }
    }

    public static <S> BlockPos getBlockPos(CommandContext<S> context, String name) {
        return context.getArgument(name, PosArgument.class).getBlockPos(context.getSource());
    }

    public static <S> BlockPos getValidBlockPos(CommandContext<S> context, String name) throws CommandSyntaxException {
        BlockPos blockPos = getBlockPos(context, name);
        if (!World.isValid(blockPos)) {
            throw OUT_OF_BOUNDS_EXCEPTION.create();
        } else {
            return blockPos;
        }
    }

    public PosArgument parse(StringReader stringReader) throws CommandSyntaxException {
        return stringReader.canRead() && stringReader.peek() == '^' ? LookingPosArgument.parse(stringReader) : DefaultPosArgument.parse(stringReader);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof CommandSource)) {
            return Suggestions.empty();
        } else {
            String string = builder.getRemaining();
            Collection<CommandSource.RelativePosition> collection;
            if (!string.isEmpty() && string.charAt(0) == '^') {
                collection = Collections.singleton(CommandSource.RelativePosition.ZERO_LOCAL);
            } else {
                collection = ((CommandSource) context.getSource()).getBlockPositionSuggestions();
            }

            return CommandSource.suggestPositions(string, collection, builder, CommandManager.getCommandValidator(this::parse));
        }
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public interface PosArgument {
        <S> Vec3d getPosition(S var1);

        <S> Vec2f getRotation(S var1);

        default <S> BlockPos getBlockPos(S source) {
            return BlockPos.ofFloored(this.getPosition(source));
        }

        boolean isXRelative();

        boolean isYRelative();

        boolean isZRelative();
    }

    public static class DefaultPosArgument implements PosArgument {
        private final CoordinateArgument x;
        private final CoordinateArgument y;
        private final CoordinateArgument z;

        public DefaultPosArgument(CoordinateArgument x, CoordinateArgument y, CoordinateArgument z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public <S> Vec3d getPosition(S source) {
            Vec3d vec3 = mc.player.getPos();
            return new Vec3d(this.x.toAbsoluteCoordinate(vec3.x), this.y.toAbsoluteCoordinate(vec3.y), this.z.toAbsoluteCoordinate(vec3.z));
        }

        @Override
        public <S> Vec2f getRotation(S source) {
            Vec2f vec2 = mc.player.getRotationClient();
            return new Vec2f((float) this.x.toAbsoluteCoordinate(vec2.x), (float) this.y.toAbsoluteCoordinate(vec2.y));
        }

        @Override
        public boolean isXRelative() {
            return this.x.isRelative();
        }

        @Override
        public boolean isYRelative() {
            return this.y.isRelative();
        }

        @Override
        public boolean isZRelative() {
            return this.z.isRelative();
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DefaultPosArgument defaultPosArgument)) {
                return false;
            }
            return this.x.equals(defaultPosArgument.x) && this.y.equals(defaultPosArgument.y) && this.z.equals(defaultPosArgument.z);
        }

        public static DefaultPosArgument parse(StringReader reader) throws CommandSyntaxException {
            int cursor = reader.getCursor();
            CoordinateArgument worldCoordinate = CoordinateArgument.parse(reader);
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();
                CoordinateArgument worldCoordinate2 = CoordinateArgument.parse(reader);
                if (reader.canRead() && reader.peek() == ' ') {
                    reader.skip();
                    CoordinateArgument worldCoordinate3 = CoordinateArgument.parse(reader);
                    return new DefaultPosArgument(worldCoordinate, worldCoordinate2, worldCoordinate3);
                }
            }
            reader.setCursor(cursor);
            throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
        }

        public static DefaultPosArgument parse(StringReader reader, boolean centerIntegers) throws CommandSyntaxException {
            int cursor = reader.getCursor();
            CoordinateArgument worldCoordinate = CoordinateArgument.parse(reader, centerIntegers);
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();
                CoordinateArgument worldCoordinate2 = CoordinateArgument.parse(reader, false);
                if (reader.canRead() && reader.peek() == ' ') {
                    reader.skip();
                    CoordinateArgument worldCoordinate3 = CoordinateArgument.parse(reader, centerIntegers);
                    return new DefaultPosArgument(worldCoordinate, worldCoordinate2, worldCoordinate3);
                }
            }
            reader.setCursor(cursor);
            throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
        }

        public static DefaultPosArgument absolute(double x, double y, double z) {
            return new DefaultPosArgument(new CoordinateArgument(false, x), new CoordinateArgument(false, y), new CoordinateArgument(false, z));
        }

        public static DefaultPosArgument absolute(Vec2f vec) {
            return new DefaultPosArgument(new CoordinateArgument(false, vec.x), new CoordinateArgument(false, vec.y), new CoordinateArgument(true, 0.0));
        }

        public static DefaultPosArgument current() {
            return new DefaultPosArgument(new CoordinateArgument(true, 0.0), new CoordinateArgument(true, 0.0), new CoordinateArgument(true, 0.0));
        }

        @Override
        public int hashCode() {
            int i = this.x.hashCode();
            i = 31 * i + this.y.hashCode();
            return 31 * i + this.z.hashCode();
        }
    }

    public static class LookingPosArgument implements PosArgument {
        private final double x;
        private final double y;
        private final double z;

        public LookingPosArgument(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public <S> Vec3d getPosition(S source) {
            Vec2f vec2 = mc.player.getRotationClient();
            Vec3d vec3 = EntityAnchorArgumentType.EntityAnchor.FEET.positionAt(mc.player);
            float f = MathHelper.cos((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
            float g = MathHelper.sin((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
            float h = MathHelper.cos(-vec2.x * (float) (Math.PI / 180.0));
            float i = MathHelper.sin(-vec2.x * (float) (Math.PI / 180.0));
            float j = MathHelper.cos((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
            float k = MathHelper.sin((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
            Vec3d vec32 = new Vec3d(f * h, i, g * h);
            Vec3d vec33 = new Vec3d(f * j, k, g * j);
            Vec3d vec34 = vec32.crossProduct(vec33).multiply(-1.0);
            double d = vec32.x * this.z + vec33.x * this.y + vec34.x * this.x;
            double e = vec32.y * this.z + vec33.y * this.y + vec34.y * this.x;
            double l = vec32.z * this.z + vec33.z * this.y + vec34.z * this.x;
            return new Vec3d(vec3.x + d, vec3.y + e, vec3.z + l);
        }

        @Override
        public <S> Vec2f getRotation(S source) {
            return Vec2f.ZERO;
        }

        @Override
        public boolean isXRelative() {
            return true;
        }

        @Override
        public boolean isYRelative() {
            return true;
        }

        @Override
        public boolean isZRelative() {
            return true;
        }

        public static LookingPosArgument parse(StringReader reader) throws CommandSyntaxException {
            int cursor = reader.getCursor();
            double d = readCoordinate(reader, cursor);
            if (!reader.canRead() || reader.peek() != ' ') {
                reader.setCursor(cursor);
                throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
            }
            reader.skip();
            double e = readCoordinate(reader, cursor);
            if (!reader.canRead() || reader.peek() != ' ') {
                reader.setCursor(cursor);
                throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
            }
            reader.skip();
            double f = readCoordinate(reader, cursor);
            return new LookingPosArgument(d, e, f);
        }

        private static double readCoordinate(StringReader reader, int startingCursorPos) throws CommandSyntaxException {
            if (!reader.canRead()) {
                throw CoordinateArgument.MISSING_COORDINATE.createWithContext(reader);
            }
            if (reader.peek() != '^') {
                reader.setCursor(startingCursorPos);
                throw Vec3ArgumentType.MIXED_COORDINATE_EXCEPTION.createWithContext(reader);
            }
            reader.skip();
            return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LookingPosArgument lookingPosArgument)) {
                return false;
            }
            return this.x == lookingPosArgument.x && this.y == lookingPosArgument.y && this.z == lookingPosArgument.z;
        }

        public int hashCode() {
            return Objects.hash(this.x, this.y, this.z);
        }
    }
}
