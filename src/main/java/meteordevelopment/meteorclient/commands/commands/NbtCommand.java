/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.BlockPosArgumentType;
import meteordevelopment.meteorclient.commands.arguments.EntityArgumentType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.BlockDataObject;
import net.minecraft.command.CommandSource;
import net.minecraft.command.DataCommandObject;
import net.minecraft.command.EntityDataObject;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class NbtCommand extends Command {
    private static final SimpleCommandExceptionType GET_MULTIPLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.data.get.multiple"));

    private static final Text copyButton = Text.literal("NBT").setStyle(Style.EMPTY
        .withFormatting(Formatting.UNDERLINE)
        .withHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            Text.literal("Copy the NBT data to your clipboard.")
        )));

    private final List<DataObjectType> OBJECT_TYPES = List.of(
        BLOCK_DATA_OBJECT_TYPE,
        ENTITY_DATA_OBJECT_TYPE
    );

    public NbtCommand() {
        super("nbt", "View NBT data for entities and block entities.");
    }

    public static MutableText createCopyButton(String toCopy) {
        return Text.empty().append(copyButton).setStyle(Style.EMPTY
            .withClickEvent(new ClickEvent(
                ClickEvent.Action.COPY_TO_CLIPBOARD,
                toCopy
            )));
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        for (DataObjectType objectType : OBJECT_TYPES) {
            builder.then(objectType.addArgumentsToBuilder(
                literal("get"),
                innerBuilder -> innerBuilder.executes(context -> executeGet(objectType.getObject(context)))
                    .then(argument("path", NbtPathArgumentType.nbtPath()).executes(context -> executeGet(
                        objectType.getObject(context),
                        context.getArgument("path", NbtPathArgumentType.NbtPath.class)
                    )))
            ));

            builder.then(objectType.addArgumentsToBuilder(
                literal("copy"),
                innerBuilder -> innerBuilder.executes(context -> executeCopy(objectType.getObject(context)))
                    .then(argument("path", NbtPathArgumentType.nbtPath()).executes(context -> executeCopy(
                        objectType.getObject(context),
                        context.getArgument("path", NbtPathArgumentType.NbtPath.class)
                    )))
            ));
        }

        builder.then(literal("get")
            .executes(context -> executeGet(new EntityDataObject(mc.player)))
            .then(argument("path", NbtPathArgumentType.nbtPath()).executes(context -> executeGet(
                new EntityDataObject(mc.player),
                context.getArgument("path", NbtPathArgumentType.NbtPath.class)
            )))
        );
        builder.then(literal("copy")
            .executes(context -> executeCopy(new EntityDataObject(mc.player)))
            .then(argument("path", NbtPathArgumentType.nbtPath()).executes(context -> executeCopy(
                new EntityDataObject(mc.player),
                context.getArgument("path", NbtPathArgumentType.NbtPath.class)
            )))
        );
    }

    private static NbtElement get(DataCommandObject object, NbtPathArgumentType.NbtPath path) throws CommandSyntaxException {
        Collection<NbtElement> collection = path.get(object.getNbt());
        Iterator<NbtElement> iterator = collection.iterator();
        NbtElement element = iterator.next();
        if (iterator.hasNext()) {
            throw GET_MULTIPLE_EXCEPTION.create();
        }
        return element;
    }

    private int executeGet(DataCommandObject object, NbtPathArgumentType.NbtPath path) throws CommandSyntaxException {
        NbtElement element = get(object, path);

        Text text = Text.empty()
            .append(createCopyButton(element.toString()))
            .append(NbtHelper.toPrettyPrintedText(element));

        info(text);

        return SINGLE_SUCCESS;
    }

    private int executeGet(DataCommandObject object) throws CommandSyntaxException {
        NbtCompound compound = object.getNbt();

        Text text = Text.empty()
            .append(createCopyButton(compound.toString()))
            .append(NbtHelper.toPrettyPrintedText(compound));

        info(text);

        return SINGLE_SUCCESS;
    }

    private int executeCopy(DataCommandObject object, NbtPathArgumentType.NbtPath path) throws CommandSyntaxException {
        NbtElement element = get(object, path);
        String elementAsString = element.toString();

        Text text = Text.empty()
            .append(createCopyButton(elementAsString))
            .append(NbtHelper.toPrettyPrintedText(element));

        info(text);
        mc.keyboard.setClipboard(elementAsString);

        return SINGLE_SUCCESS;
    }

    private int executeCopy(DataCommandObject object) throws CommandSyntaxException {
        NbtCompound compound = object.getNbt();
        String compoundAsString = compound.toString();

        Text text = Text.empty()
            .append(createCopyButton(compoundAsString))
            .append(NbtHelper.toPrettyPrintedText(compound));

        info(text);
        mc.keyboard.setClipboard(compoundAsString);

        return SINGLE_SUCCESS;
    }

    public static final DataObjectType BLOCK_DATA_OBJECT_TYPE = new DataObjectType() {
        private static final SimpleCommandExceptionType INVALID_BLOCK_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.data.block.invalid"));

        @Override
        public <S> DataCommandObject getObject(CommandContext<S> context) throws CommandSyntaxException {
            BlockPos blockPos = BlockPosArgumentType.getLoadedBlockPos(context, "sourcePos");
            BlockEntity blockEntity = mc.world.getBlockEntity(blockPos);
            if (blockEntity == null) {
                throw INVALID_BLOCK_EXCEPTION.create();
            } else {
                return new BlockDataObject(blockEntity, blockPos);
            }
        }

        @Override
        public ArgumentBuilder<CommandSource, ?> addArgumentsToBuilder(ArgumentBuilder<CommandSource, ?> argument, Function<ArgumentBuilder<CommandSource, ?>, ArgumentBuilder<CommandSource, ?>> argumentAdder) {
            return argument.then(literal("block").then(
                argumentAdder.apply(argument("sourcePos", BlockPosArgumentType.blockPos()))
            ));
        }
    };

    public static final DataObjectType ENTITY_DATA_OBJECT_TYPE = new DataObjectType() {
        @Override
        public <S> DataCommandObject getObject(CommandContext<S> context) throws CommandSyntaxException {
            return new EntityDataObject(EntityArgumentType.getEntity(context, "source"));
        }

        @Override
        public ArgumentBuilder<CommandSource, ?> addArgumentsToBuilder(ArgumentBuilder<CommandSource, ?> argument, Function<ArgumentBuilder<CommandSource, ?>, ArgumentBuilder<CommandSource, ?>> argumentAdder) {
            return argument.then(literal("entity").then(
                argumentAdder.apply(argument("source", EntityArgumentType.entity()))
            ));
        }
    };

    public interface DataObjectType {
        <S> DataCommandObject getObject(CommandContext<S> context) throws CommandSyntaxException;

        ArgumentBuilder<CommandSource, ?> addArgumentsToBuilder(ArgumentBuilder<CommandSource, ?> argument, Function<ArgumentBuilder<CommandSource, ?>, ArgumentBuilder<CommandSource, ?>> argumentAdder);
    }
}
