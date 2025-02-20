/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import meteordevelopment.meteorclient.commands.arguments.*;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;

import static meteordevelopment.meteorclient.commands.Command.*;

public class SettingCommandRegistry {
    private static final Reference2ObjectMap<Class<? extends Setting<?>>, Factory<?>> REGISTRY = new Reference2ObjectOpenHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends Setting<?>> void register(Class<T> settingClass, Factory<T> factory) {
        REGISTRY.put(settingClass,
            // cast from Setting<?> to concrete implementation
            (builder, setting, output) -> factory.build(builder, (T) setting, output)
        );
    }

    @SuppressWarnings("unchecked")
    public static <T extends Setting<?>> Factory<T> get(T setting) {
        return (Factory<T>) REGISTRY.get(setting.getClass());
    }

    @FunctionalInterface
    public interface Factory<T extends Setting<?>> {
        void build(LiteralArgumentBuilder<CommandSource> builder, T setting, Consumer<String> output);
    }

    static {
        REGISTRY.defaultReturnValue((builder, setting, output) -> {});

        //register(BlockDataSetting.class, (builder, setting, output) -> {}); // todo
        register(BlockListSetting.class, (builder, setting, output) -> {
            builder.then(literal("remove")
                .then(argument("block", new CollectionItemArgumentType<>(setting::get, t -> Registries.BLOCK.getId(t).toString()))
                    .executes(context -> {
                        Block block = context.getArgument("block", Block.class);
                        if (setting.get().remove(block)) {
                            String blockName = Registries.BLOCK.getId(block).toString();
                            output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", blockName, setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );

            builder.then(literal("add")
                .then(argument("block", RegistryEntryArgumentType.block()).executes(context -> {
                    Block block = RegistryEntryArgumentType.getBlock(context, "block").value();
                    String blockName = Registries.BLOCK.getId(block).toString();
                    if ((setting.filter == null || setting.filter.test(block)) && !setting.get().contains(block)) {
                        setting.get().add(block);
                        output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", blockName, setting.title));
                        setting.onChanged();
                    } else {
                        output.accept(String.format("Could not add (highlight)%s(default) to (highlight)%s(default).", blockName, setting.title));
                    }
                    return SINGLE_SUCCESS;
                }))
            );
        });
        register(BlockPosSetting.class, (builder, setting, output) -> {
            builder.then(literal("set")
                .then(argument("pos", BlockPosArgumentType.blockPos())
                    .executes(context -> {
                        BlockPos pos = context.getArgument("pos", BlockPos.class); // todo i know this is broken, i dont care.
                        setting.set(pos);
                        output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default), (highlight)%s(default), (highlight)%s(default).", setting.title, pos.getX(), pos.getY(), pos.getZ()));
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(BlockSetting.class, (builder, setting, output) -> {
            builder.then(literal("set")
                .then(argument("block", RegistryEntryArgumentType.block())
                    .executes(context -> {
                        RegistryEntry<Block> blockEntry = RegistryEntryArgumentType.getBlock(context, "block");
                        if (setting.set(blockEntry.value())) {
                            output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", setting.title, blockEntry.getIdAsString()));
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(BoolSetting.class, (builder, setting, output) -> {
            builder.then(literal("toggle").executes(context -> {
                setting.set(!setting.get());
                output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", setting.title, setting.get()));
                return SINGLE_SUCCESS;
            }));

            builder.then(literal("set")
                .then(argument("value", BoolArgumentType.bool())
                    .executes(context -> {
                        setting.set(BoolArgumentType.getBool(context, "value"));
                        output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", setting.title, setting.get()));
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(ColorListSetting.class, (builder, setting, output) -> {
            builder.then(literal("add")
                .then(argument("color", ColorArgumentType.color())
                    .executes(context -> {
                        SettingColor color = ColorArgumentType.get(context, "color");
                        setting.get().add(color);
                        output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", color, setting.title));
                        setting.onChanged();
                        return SINGLE_SUCCESS;
                    })
                )
            );

            builder.then(literal("remove")
                .then(argument("color", new CollectionItemArgumentType<>(setting::get))
                    .executes(context -> {
                        SettingColor color = context.getArgument("color", SettingColor.class);
                        if (setting.get().remove(color)) {
                            output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", color, setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(ColorSetting.class, (builder, setting, output) -> {
            builder.then(literal("set")
                .then(argument("color", ColorArgumentType.color())
                    .executes(context -> {
                        SettingColor color = ColorArgumentType.get(context, "color");
                        setting.set(color);
                        output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", setting.title, setting.get()));
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(DoubleSetting.class, (builder, setting, output) -> {
            builder.then(literal("set")
                .then(argument("value", DoubleArgumentType.doubleArg(setting.min, setting.max))
                    .executes(context -> {
                        if (setting.set(DoubleArgumentType.getDouble(context, "value"))) {
                            String formatStr = "%." + setting.decimalPlaces + "f";
                            output.accept(String.format("Set (highlight)%s(default) to (hightlight)" + formatStr + "(default).", setting.title, setting.get()));
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(EnchantmentListSetting.class, (builder, setting, output) -> {
            builder.then(literal("add")
                .then(argument("enchantment", RegistryEntryReferenceArgumentType.enchantment())
                    .executes(context -> {
                        RegistryEntry<Enchantment> entry = RegistryEntryReferenceArgumentType.getEnchantment(context, "enchantment");
                        if (setting.get().add(entry.getKey().orElseThrow())) {
                            output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", Names.get(entry), setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );

            builder.then(literal("remove")
                .then(argument("enchantment", new CollectionItemArgumentType<>(setting::get, Names::get))
                    .executes(context -> {
                        @SuppressWarnings("unchecked")
                        RegistryKey<Enchantment> entry = context.getArgument("enchantment", RegistryKey.class);
                        if (setting.get().remove(entry)) {
                            output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", Names.get(entry), setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(EntityTypeListSetting.class, (builder, setting, output) -> {
            builder.then(literal("add")
                .then(argument("entity", RegistryEntryArgumentType.entityType())
                    .executes(context -> {
                        RegistryEntry.Reference<EntityType<?>> entry = RegistryEntryArgumentType.getEntityType(context, "entity");
                        if ((setting.filter == null || setting.filter.test(entry.value()) && setting.get().add(entry.value()))) {
                            output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", Names.get(entry.value()), setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );

            builder.then(literal("remove")
                .then(argument("entity", new CollectionItemArgumentType<>(setting::get, Names::get))
                    .executes(context -> {
                        EntityType<?> entityType = context.getArgument("entity", EntityType.class);
                        if (setting.get().remove(entityType)) {
                            output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", Names.get(entityType), setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        //register(EnumSetting.class, (builder, setting, output) -> {}); // todo
        register(FontFaceSetting.class, (builder, setting, output) -> {
            builder.then(literal("set")
                .then(argument("font", FontFaceArgumentType.fontFace())
                    .executes(context -> {
                        FontFace fontFace = FontFaceArgumentType.get(context, "font");
                        if (setting.set(fontFace)) {
                            output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", setting.title, fontFace));
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        //register(GenericSetting.class, (builder, setting, output) -> {}); // todo
        register(IntSetting.class, (builder, setting, output) -> {
            builder.then(literal("set")
                .then(argument("value", IntegerArgumentType.integer(setting.min, setting.max))
                    .executes(context -> {
                        int value = IntegerArgumentType.getInteger(context, "value");
                        if (setting.set(value)) {
                            output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", setting.title, value));
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(ItemListSetting.class, (builder, setting, output) -> {
            builder.then(literal("add")
                .then(argument("item", RegistryEntryArgumentType.item())
                    .executes(context -> {
                        RegistryEntry<Item> entry = RegistryEntryArgumentType.getItem(context, "item");
                        if ((setting.filter == null || setting.filter.test(entry.value())) && !setting.get().contains(entry.value())) {
                            setting.get().add(entry.value());
                            output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", Names.get(entry.value()), setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );

            builder.then(literal("remove")
                .then(argument("item", new CollectionItemArgumentType<>(setting::get, Names::get))
                    .executes(context -> {
                        Item item = context.getArgument("item", Item.class);
                        if (setting.get().remove(item)) {
                            setting.onChanged();
                            output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", Names.get(item), setting.title));
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(ItemSetting.class, (builder, setting, output) -> {
            builder.then(literal("set")
                .then(argument("item", RegistryEntryArgumentType.item())
                    .executes(context -> {
                        Item item = RegistryEntryArgumentType.getItem(context, "item").value();
                        if (setting.set(item)) {
                            output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", setting.title, Names.get(item)));
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(KeybindSetting.class, (builder, setting, output) -> {
            builder.then(literal("set")
                .then(argument("keybind", IntegerArgumentType.integer())
                    .executes(context -> {
                        int value = IntegerArgumentType.getInteger(context, "keybind");
                        setting.set(Keybind.fromKey(value));
                        output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", setting.title, setting.get()));
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(ModuleListSetting.class, (builder, setting, output) -> {
            builder.then(literal("add")
                .then(argument("module", ModuleArgumentType.create())
                    .executes(context -> {
                        Module module = ModuleArgumentType.get(context);
                        if (!setting.get().contains(module)) {
                            setting.get().add(module);
                            output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", module.title, setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );

            builder.then(literal("remove")
                .then(argument("module", new CollectionItemArgumentType<>(setting::get, module -> module.name))
                    .executes(context -> {
                        Module module = context.getArgument("module", Module.class);
                        if (setting.get().remove(module)) {
                            setting.onChanged();
                            output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", module.title, setting.title));
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        //register(PacketListSetting.class, (builder, setting, output) -> {}); // todo
        register(ParticleTypeListSetting.class, (builder, setting, output) -> {
            builder.then(literal("add")
                .then(argument("particle", RegistryEntryArgumentType.particleType())
                    .executes(context -> {
                        RegistryEntry<ParticleType<?>> entry = RegistryEntryArgumentType.getParticleType(context, "particle");
                        if (!setting.get().contains(entry.value())) {
                            setting.get().add(entry.value());
                            output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", Names.get(entry.value()), setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );

            builder.then(literal("remove")
                .then(argument("particle", new CollectionItemArgumentType<>(setting::get, Names::get))
                    .executes(context -> {
                        ParticleType<?> particleType = context.getArgument("particle", ParticleType.class);
                        if (setting.get().remove(particleType)) {
                            setting.onChanged();
                            output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", Names.get(particleType), setting.title));
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        //register(PotionSetting.class, (builder, setting, output) -> {}); // todo
        //register(ProvidedStringSetting.class, (builder, setting, output) -> {}); // todo
        register(ScreenHandlerListSetting.class, (builder, setting, output) -> {
            builder.then(literal("add")
                .then(argument("screenHandler", RegistryEntryArgumentType.screenHandler())
                    .executes(context -> {
                        RegistryEntry.Reference<ScreenHandlerType<?>> entry = RegistryEntryArgumentType.getScreenHandler(context, "screenHandler");
                        setting.get().add(entry.value());
                        output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", entry.getIdAsString(), setting.title));
                        setting.onChanged();
                        return SINGLE_SUCCESS;
                    })
                )
            );

            builder.then(literal("remove")
                .then(argument("screenHandler", new CollectionItemArgumentType<>(setting::get))
                    .executes(context -> {
                        ScreenHandlerType<?> screenHandler = context.getArgument("screenHandler", ScreenHandlerType.class);
                        if (setting.get().remove(screenHandler)) {
                            output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", screenHandler, setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        //register(SoundEventListSetting.class, (builder, setting, output) -> {}); // todo
        register(StatusEffectAmplifierMapSetting.class, (builder, setting, output) -> {
            builder.then(literal("set")
                .then(argument("effect", RegistryEntryArgumentType.statusEffect())
                    .then(argument("amplifier", IntegerArgumentType.integer(0))
                        .executes(context -> {
                            StatusEffect effect = RegistryEntryArgumentType.getStatusEffect(context, "effect").value();
                            int amplifier = IntegerArgumentType.getInteger(context, "amplifier");
                            setting.get().put(effect, amplifier);
                            output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", Names.get(effect), amplifier));
                            setting.onChanged();
                            return SINGLE_SUCCESS;
                        })
                    )
                )
            );

            builder.then(literal("remove")
                .then(argument("effect", new CollectionItemArgumentType<>(() -> setting.get().keySet(), Names::get))
                    .executes(context -> {
                        StatusEffect effect = context.getArgument("effect", StatusEffect.class);
                        if (setting.get().containsKey(effect)) {
                            setting.get().removeInt(effect);
                            output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", Names.get(effect), setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(StatusEffectListSetting.class, (builder, setting, output) -> {
            builder.then(literal("add")
                .then(argument("effect", RegistryEntryArgumentType.statusEffect())
                    .executes(context -> {
                        StatusEffect effect = RegistryEntryArgumentType.getStatusEffect(context, "effect").value();
                        if (!setting.get().contains(effect)) {
                            setting.get().add(effect);
                            output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", Names.get(effect), setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );

            builder.then(literal("remove")
                .then(argument("effect", new CollectionItemArgumentType<>(setting::get, Names::get))
                    .executes(context -> {
                        StatusEffect effect = context.getArgument("effect", StatusEffect.class);
                        if (setting.get().remove(effect)) {
                            output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", Names.get(effect), setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(StorageBlockListSetting.class, (builder, setting, output) -> {
            builder.then(literal("add")
                .then(argument("blockEntity", RegistryEntryArgumentType.blockEntityType())
                    .executes(context -> {
                        RegistryEntry<BlockEntityType<?>> entry = RegistryEntryArgumentType.getBlockEntityType(context, "blockEntity");
                        if (!setting.get().contains(entry.value())) {
                            setting.get().add(entry.value());
                            output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", entry.getIdAsString(), setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );

            builder.then(literal("remove")
                .then(argument("blockEntity", new CollectionItemArgumentType<>(setting::get))
                    .executes(context -> {
                        BlockEntityType<?> blockEntityType = context.getArgument("effect", BlockEntityType.class);
                        if (setting.get().remove(blockEntityType)) {
                            output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", blockEntityType, setting.title));
                            setting.onChanged();
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(StringListSetting.class, (builder, setting, output) -> {
            builder.then(literal("add")
                .then(argument("string", StringArgumentType.string())
                    .executes(context -> {
                        String string = StringArgumentType.getString(context, "string");
                        setting.get().add(string);
                        output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", string, setting.title));
                        setting.onChanged();
                        return SINGLE_SUCCESS;
                    })
                )
            );

            builder.then(literal("remove")
                .then(argument("string", new CollectionItemArgumentType<>(setting::get))
                    .executes(context -> {
                        String string = context.getArgument("string", String.class);
                        if (setting.get().remove(string)) {
                            setting.onChanged();
                            output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", string, setting.title));
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        register(StringSetting.class, (builder, setting, output) -> {
            builder.then(literal("set")
                .then(argument("string", StringArgumentType.string())
                    .executes(context -> {
                        if (setting.set(StringArgumentType.getString(context, "string"))) {
                            output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", setting.title, setting.get()));
                        }
                        return SINGLE_SUCCESS;
                    })
                )
            );
        });
        //register(Vector3dSetting.class, (builder, setting, output) -> {}); // todo
    }
}
