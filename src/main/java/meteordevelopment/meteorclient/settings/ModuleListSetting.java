/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.CollectionItemArgumentType;
import meteordevelopment.meteorclient.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModuleListSetting extends Setting<List<Module>> {
    private static List<String> suggestions;

    public ModuleListSetting(String name, String description, List<Module> defaultValue, Consumer<List<Module>> onChanged, Consumer<Setting<List<Module>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("add")
            .then(Command.argument("module", ModuleArgumentType.create())
                .executes(context -> {
                    Module module = ModuleArgumentType.get(context);
                    if (!this.get().contains(module)) {
                        this.get().add(module);
                        output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", module.title, this.title));
                        this.onChanged();
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );

        builder.then(Command.literal("remove")
            .then(Command.argument("module", new CollectionItemArgumentType<>(this::get, module -> module.name))
                .executes(context -> {
                    Module module = context.getArgument("module", Module.class);
                    if (this.get().remove(module)) {
                        this.onChanged();
                        output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", module.title, this.title));
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }

    @Override
    public List<String> getSuggestions() {
        if (suggestions == null) {
            suggestions = new ArrayList<>(Modules.get().getAll().size());
            for (Module module : Modules.get().getAll()) suggestions.add(module.name);
        }

        return suggestions;
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtList modulesTag = new NbtList();
        for (Module module : get()) modulesTag.add(NbtString.of(module.name));
        tag.put("modules", modulesTag);

        return tag;
    }

    @Override
    public List<Module> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("modules", 8);
        for (NbtElement tagI : valueTag) {
            Module module = Modules.get().get(tagI.asString());
            if (module != null) get().add(module);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<Module>, ModuleListSetting> {
        public Builder() {
            super(new ArrayList<>(0));
        }

        @SafeVarargs
        public final Builder defaultValue(Class<? extends Module>... defaults) {
            List<Module> modules = new ArrayList<>();

            for (Class<? extends Module> klass : defaults) {
                if (Modules.get().get(klass) != null) modules.add(Modules.get().get(klass));
            }

            return defaultValue(modules);
        }

        @Override
        public ModuleListSetting build() {
            return new ModuleListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
