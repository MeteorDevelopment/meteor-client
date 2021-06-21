/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.mixin.TextHandlerAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.PrimitiveIterator;
import java.util.Random;

public class BookBot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("What kind of text to write.")
        .defaultValue(Mode.Random)
        .build()
    );

    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("The name you want to give your books.")
        .defaultValue("Meteor on Crack!")
        .build()
    );

    private final Setting<Integer> pages = sgGeneral.add(new IntSetting.Builder()
        .name("pages")
        .description("The number of pages to write per book.")
        .defaultValue(50)
        .min(1).max(100)
        .sliderMax(100)
        .visible(() -> mode.get() != Mode.File)
        .build()
    );

    private final Setting<Boolean> onlyAscii = sgGeneral.add(new BoolSetting.Builder()
        .name("ascii-only")
        .description("Only uses the characters in the ASCII charset.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Random)
        .build()
    );

    private final Setting<Boolean> count = sgGeneral.add(new BoolSetting.Builder()
        .name("append-count")
        .description("Whether to append the number of the book to the title.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The amount of delay between writing books.")
        .defaultValue(20)
        .min(1)
        .sliderMin(1).sliderMax(200)
        .build()
    );

    private final File file = new File(MeteorClient.FOLDER, "bookbot.txt");
    private final MutableText editFileText = new LiteralText("Click here to edit it.")
        .setStyle(Style.EMPTY
            .withFormatting(Formatting.UNDERLINE, Formatting.RED)
            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()))
        );

    private int delayTimer, bookCount;
    private Random random;

    public BookBot() {
        super(Categories.Misc, "book-bot", "Automatically writes in books.");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton edit = theme.button("Edit File");
        edit.action = () -> {
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Util.getOperatingSystem().open(file.toURI());
        };

        return edit;
    }

    @Override
    public void onActivate() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            MutableText message = new LiteralText("");
            message.append(new LiteralText("Couldn't find bookbot.txt in your Meteor folder, it has been automatically created for you. ").formatted(Formatting.RED));
            message.append(editFileText);
            info(message);
            toggle();
            return;
        }

        random = new Random();
        delayTimer = delay.get();
        bookCount = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        FindItemResult writableBook = InvUtils.find(Items.WRITABLE_BOOK);

        // Check if there is a book to write
        if (!writableBook.found()) {
            toggle();
            return;
        }

        // Move the book into hand
        if (!writableBook.isMainHand()) {
            InvUtils.move().from(writableBook.getSlot()).toHotbar(mc.player.getInventory().selectedSlot);
            return;
        }

        // If somehow it failed, just dont do anything until it tries again
        FindItemResult finalBook = InvUtils.findInHotbar(Items.WRITABLE_BOOK);
        if (!finalBook.isMainHand()) return;

        // Check delay
        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        // Reset delay
        delayTimer = delay.get();

        // Write book

        if (mode.get() == Mode.Random) {
            int origin = onlyAscii.get() ? 0x21 : 0x00;
            int bound = onlyAscii.get() ? 0x7E : 0x10FFFF;
            
            writeBook(
                // Generate a random load of ints to use as random characters
                random.ints(origin, bound)
                    .filter(i -> !Character.isWhitespace(i) && i != '\r' && i != '\n')
                    .iterator()
            );
        } else if (mode.get() == Mode.File) {
            // Ignore if somehow the file got deleted
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                MutableText message = new LiteralText("");
                message.append(new LiteralText("Couldn't find bookbot.txt in your Meteor folder, it has been automatically created for you. ").formatted(Formatting.RED));
                message.append(editFileText);
                info(message);
                toggle();
                return;
            }

            // Handle the file being empty
            if (file.length() == 0) {
                MutableText message = new LiteralText("");
                message.append(new LiteralText("The bookbot file is empty! ").formatted(Formatting.RED));
                message.append(editFileText);
                info(message);
                toggle();
                return;
            }

            // Read each line of the file and construct a string with the needed line breaks
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder file = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    file.append(line).append('\n');
                }

                reader.close();

                // Write the file string to a book
                writeBook(file.toString().chars().iterator());
            } catch (IOException ignored) {
                error("Failed to read the file.");
            }
        }
    }

    private void writeBook(PrimitiveIterator.OfInt chars) {
        NbtList pageList = new NbtList();

        for (int pageI = 0; pageI < (mode.get() == Mode.File ? 100 : pages.get()); pageI++) {
            // Check if the stream is empty before creating a new page
            if (!chars.hasNext()) break;

            StringBuilder page = new StringBuilder();

            for (int lineI = 0; lineI < 13; lineI++) {
                // Check if the stream is empty before creating a new line
                if (!chars.hasNext()) break;

                double lineWidth = 0;
                StringBuilder line = new StringBuilder();

                while (true) {
                    // Check if the stream is empty
                    if (!chars.hasNext()) break;

                    // Get the next character
                    int nextChar = chars.nextInt();

                    // Ingore newline chars when writing lines, should already be organised
                    if (nextChar == '\r' || nextChar == '\n') break;

                    // Make sure the character will fit on the line
                    double charWidth = ((TextHandlerAccessor) mc.textRenderer.getTextHandler()).getWidthRetriever().getWidth(nextChar, Style.EMPTY);
                    if (lineWidth + charWidth > 114) break;

                    // Append it to the line
                    line.appendCodePoint(nextChar);
                    lineWidth += charWidth;
                }

                // Append the line to the page
                page.append(line).append('\n');
            }

            // Add the page to the pages nbt tag
            pageList.addElement(pageI, NbtString.of(page.toString()));
        }

        // Get the title with count
        String title = name.get();
        if (count.get() && bookCount != 0) title += " #" + bookCount;

        // Write the pages to the book and sign it
        mc.player.getMainHandStack().putSubTag("title", NbtString.of(title));
        mc.player.getMainHandStack().putSubTag("author", NbtString.of(mc.player.getGameProfile().getName()));
        mc.player.getMainHandStack().putSubTag("pages", pageList);
        mc.player.networkHandler.sendPacket(new BookUpdateC2SPacket(mc.player.getMainHandStack(), true, mc.player.getInventory().selectedSlot));

        bookCount++;
    }

    public enum Mode {
        File,
        Random
    }
}
