/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.MacroArgumentType;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.macros.Macro;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.command.CommandSource;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class MacroCommand extends Command {
    private static final SimpleCommandExceptionType ADD_EMPTY_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("Cannot add empty line."));
    private Macro macroToBind;
    private boolean awaitRelease;

    public MacroCommand() {
        super("macro", "Allows you to execute macros.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("list").executes(context -> {
            info("--- Macros ((highlight)%s(default)) ---", Macros.get().getAll().size());

            for (Macro macro : Macros.get()) {
                HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, getTooltip(macro));

                MutableText text = Text.literal(macro.name.get()).formatted(Formatting.WHITE);
                text.setStyle(text.getStyle().withHoverEvent(hoverEvent));

                MutableText sep = Text.literal(" - ");
                sep.setStyle(sep.getStyle().withHoverEvent(hoverEvent));
                text.append(sep.formatted(Formatting.GRAY));

                MutableText key = Text.literal(macro.keybind.toString());
                key.setStyle(key.getStyle().withHoverEvent(hoverEvent));
                text.append(key.formatted(Formatting.GRAY));

                ChatUtils.sendMsg(text);
            }

            return SINGLE_SUCCESS;
        }));

        builder.then(argument("macro", MacroArgumentType.create())
            .executes(context -> {
                Macro macro = MacroArgumentType.get(context);
                macro.onAction();
                return SINGLE_SUCCESS;
            })

            .then(literal("bind").executes(context -> {
                info("Press a key to bind the macro to.");
                Macro macro = MacroArgumentType.get(context);
                macroToBind = macro;
                awaitRelease = true;
                MeteorClient.EVENT_BUS.subscribe(this);
                return SINGLE_SUCCESS;
            }))

            .then(literal("edit").then(argument("line", IntegerArgumentType.integer(0))
                .then(argument("content", StringArgumentType.greedyString()).executes(context -> {
                    Macro macro = MacroArgumentType.get(context);
                    int line = IntegerArgumentType.getInteger(context, "line");
                    String content = StringArgumentType.getString(context, "content");
                    boolean shouldAppend = line >= macro.messages.get().size();
                    boolean shouldRemove = content.isEmpty();

                    if (shouldAppend && shouldRemove) throw ADD_EMPTY_EXCEPTION.create();

                    if (shouldAppend) {
                        macro.messages.get().add(content);
                        info("Added message to macro.");
                    } else if (shouldRemove) {
                        macro.messages.get().remove(line);
                        info("Removed line from macro messages.");
                    } else {
                        macro.messages.get().set(line, content);
                        info("Changed line from macro messages.");
                    }

                    return SINGLE_SUCCESS;
                })))
            )

            .then(literal("delete").executes(context -> {
                Macro macro = MacroArgumentType.get(context);
                Macros.get().remove(macro);
                info("Successfully removed macro.");
                return SINGLE_SUCCESS;
            }))
        );

        builder.then(literal("create").then(argument("name", StringArgumentType.string()).executes(context -> {
            String name = StringArgumentType.getString(context, "name");
            Macro macro = new Macro();
            macro.name.set(name);
            Macros.get().add(macro);
            info("Macro created.");
            info("Run '%smacro %s bind' to set a keybind.", Config.get().prefix.get(), name);
            info("Run '%smacro %s edit {line} {content}' to edit the macro's content.", Config.get().prefix.get(), name);
            return SINGLE_SUCCESS;
        })));
    }

    private MutableText getTooltip(Macro macro) {
        MutableText tooltip = Text.literal(macro.name.get()).formatted(Formatting.BLUE, Formatting.GOLD).append(ScreenTexts.LINE_BREAK);
        for (String line : macro.messages.get()) {
            tooltip.append(ScreenTexts.LINE_BREAK).append(Text.literal(line).formatted(Formatting.WHITE));
        }
        return tooltip;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onKeyBinding(KeyEvent event) {
        if (event.action == KeyAction.Release && onBinding(true, event.key, event.modifiers)) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onButtonBinding(MouseButtonEvent event) {
        if (event.action == KeyAction.Release && onBinding(false, event.button, 0)) event.cancel();
    }

    private boolean onBinding(boolean isKey, int value, int modifiers) {
        if (macroToBind == null) return false;

        if (awaitRelease) {
            if (!isKey || (value != GLFW.GLFW_KEY_ENTER && value != GLFW.GLFW_KEY_KP_ENTER)) return false;

            awaitRelease = false;
            return false;
        }

        if (macroToBind.keybind.get().canBindTo(isKey, value, modifiers)) {
            macroToBind.keybind.get().set(isKey, value, modifiers);
            info("Bound to (highlight)%s(default).", macroToBind.keybind.get());
        }
        else if (value == GLFW.GLFW_KEY_ESCAPE) {
            macroToBind.keybind.set(Keybind.none());
            info("Removed bind.");
        }
        else return false;

        macroToBind = null;
        MeteorClient.EVENT_BUS.unsubscribe(this);

        return true;
    }
}
