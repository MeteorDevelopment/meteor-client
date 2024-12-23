/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.commands;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.component.Component;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.visitor.NbtTextFormatter;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author Crosby
 */
public class ComponentMapWriter {
    private final DynamicOps<NbtElement> nbtOps;

    public ComponentMapWriter(CommandRegistryAccess commandRegistryAccess) {
        this.nbtOps = mc.getNetworkHandler().getRegistryManager().getOps(NbtOps.INSTANCE); // todo figure out why codecs dislike CommandRegistryAccess
    }

    public DataResult<String> write(ComponentMap componentMap) {
        StringWriter writer = new StringWriter();
        write(componentMap, writer);
        return writer.build();
    }

    public DataResult<String> write(ComponentChanges componentChanges) {
        StringWriter writer = new StringWriter();
        write(componentChanges, writer);
        return writer.build();
    }

    public <T> DataResult<String> write(ComponentType<T> componentType, T object) {
        StringWriter writer = new StringWriter();
        writeComponentValue(writer, componentType, object);
        return writer.build();
    }

    public DataResult<Text> writePrettyPrinted(ComponentMap componentMap) {
        ComponentTextFormatter writer = new ComponentTextFormatter();
        write(componentMap, writer);
        return writer.build();
    }

    public DataResult<Text> writePrettyPrinted(ComponentChanges componentChanges) {
        ComponentTextFormatter writer = new ComponentTextFormatter();
        write(componentChanges, writer);
        return writer.build();
    }

    public <T> DataResult<Text> writePrettyPrinted(ComponentType<T> componentType, T object) {
        ComponentTextFormatter writer = new ComponentTextFormatter();
        writeComponentValue(writer, componentType, object);
        return writer.build();
    }

    public void write(ComponentMap componentMap, Writer writer) {
        writer.acceptStart();
        for (Iterator<Component<?>> it = componentMap.iterator(); it.hasNext();) {
            Component<?> component = it.next();
            Identifier identifier = Registries.DATA_COMPONENT_TYPE.getId(component.type());
            writer.acceptIdentifier(identifier);
            writer.acceptValueSeparator();
            writeComponentValue(writer, component);
            if (it.hasNext()) writer.acceptComponentSeparator();
        }
        writer.acceptEnd();
    }

    private <T> void writeComponentValue(Writer writer, Component<T> component) {
        component.encode(this.nbtOps).ifSuccess(writer::acceptComponentValue);
    }

    public void write(ComponentChanges componentChanges, Writer writer) {
        writer.acceptStart();
        for (Iterator<Map.Entry<ComponentType<?>, Optional<?>>> it = componentChanges.entrySet().iterator(); it.hasNext();) {
            Map.Entry<ComponentType<?>, Optional<?>> entry = it.next();
            Identifier identifier = Registries.DATA_COMPONENT_TYPE.getId(entry.getKey());
            if (entry.getValue().isPresent()) {
                writer.acceptIdentifier(identifier);
                writer.acceptValueSeparator();
                writeComponentValue(writer, entry.getKey(), entry.getValue().get());
            } else {
                writer.acceptRemoved();
                writer.acceptIdentifier(identifier);
            }
            if (it.hasNext()) writer.acceptComponentSeparator();
        }
        writer.acceptEnd();
    }

    private  <T> void writeComponentValue(Writer writer, ComponentType<T> type, Object value) {
        Codec<T> codec = type.getCodec();
        if (codec != null) {
            codec.encodeStart(this.nbtOps, (T) value)
                .ifSuccess(writer::acceptComponentValue)
                .ifError(error -> error.resultOrPartial().ifPresentOrElse(
                    writer::acceptComponentValue,
                    () -> writer.onError(error)
                ));
        }
    }

    public interface Writer {
        void acceptStart();
        void acceptIdentifier(Identifier identifier);
        void acceptValueSeparator();
        void acceptComponentValue(NbtElement element);
        void acceptRemoved();
        void acceptComponentSeparator();
        void acceptEnd();
        void onError(DataResult.Error<NbtElement> error);
    }

    public static class StringWriter implements Writer {
        private final StringBuilder builder = new StringBuilder();
        private @Nullable DataResult.Error<NbtElement> error = null;

        @Override
        public void acceptStart() {
            this.builder.append('[');
        }

        @Override
        public void acceptRemoved() {
            this.builder.append('!');
        }

        @Override
        public void acceptIdentifier(Identifier identifier) {
            this.builder.append(identifier);
        }

        @Override
        public void acceptValueSeparator() {
            this.builder.append('=');
        }

        @Override
        public void acceptComponentValue(NbtElement element) {
            this.builder.append(new StringNbtWriter().apply(element));
        }

        @Override
        public void acceptComponentSeparator() {
            this.builder.append(',');
        }

        @Override
        public void acceptEnd() {
            this.builder.append(']');
        }

        @Override
        public void onError(DataResult.Error<NbtElement> error) {
            this.error = error;
        }

        public DataResult<String> build() {
            return this.error == null
                ? DataResult.success(this.builder.toString())
                : DataResult.error(this.error.messageSupplier(), this.builder.toString());
        }
    }

    public static class ComponentTextFormatter implements Writer {
        private static final Text VALUE_SEPARATOR = Text.literal(": ");
        private static final Text REMOVED = Text.literal("!").formatted(Formatting.RED);
        private static final Formatting NAME_COLOR = Formatting.AQUA;
        private final MutableText result = Text.empty();
        private @Nullable DataResult.Error<NbtElement> error = null;

        @Override
        public void acceptStart() {
            this.result.append("[");
        }

        @Override
        public void acceptRemoved() {
            this.result.append(REMOVED);
        }

        @Override
        public void acceptIdentifier(Identifier identifier) {
            this.result.append(Text.literal(identifier.toString()).formatted(NAME_COLOR));
        }

        @Override
        public void acceptValueSeparator() {
            this.result.append(VALUE_SEPARATOR);
        }

        @Override
        public void acceptComponentValue(NbtElement element) {
            this.result.append(new NbtTextFormatter("").apply(element));
        }

        @Override
        public void acceptComponentSeparator() {
            this.result.append(Texts.DEFAULT_SEPARATOR_TEXT);
        }

        @Override
        public void acceptEnd() {
            this.result.append("]");
        }

        @Override
        public void onError(DataResult.Error<NbtElement> error) {
            this.error = error;
        }

        public DataResult<Text> build() {
            return this.error == null
                ? DataResult.success(this.result)
                : DataResult.error(this.error.messageSupplier(), this.result);
        }
    }
}
