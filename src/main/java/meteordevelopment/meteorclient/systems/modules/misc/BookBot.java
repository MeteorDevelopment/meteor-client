/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.mixin.TextHandlerAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.text.Style;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BookBot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("What kind of text to write.")
        .defaultValue(Mode.Random)
        .build()
    );

    private final Setting<String> textPath = sgGeneral.add(new StringSetting.Builder()
        .name("text-path")
        .defaultValue(new File(MeteorClient.FOLDER, "bookbot.txt").getAbsolutePath())
        .visible(() -> false)
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
        .range(1, 100)
        .sliderRange(1, 100)
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
        .sliderRange(1, 200)
        .build()
    );

    private List<String> lines;
    private int delayTimer, bookCount;
    private Random random;

    public BookBot() {
        super(Categories.Misc, "book-bot", "Automatically writes in books.");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        return Utils.fileSelectWidget(textPath, theme);
    }

    @Override
    public void onActivate() {
        if (mode.get() == Mode.File) {
            try {
                lines = Files.readAllLines(Path.of(textPath.get()));
            } catch (IOException e) {
                error("No file selected, please select a file in the GUI.");
                toggle();
            }
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
            error("You need a writable book to use this module.");
            toggle();
            return;
        }

        // Move the book into hand
        if (!InvUtils.testInMainHand(Items.WRITABLE_BOOK)) {
            InvUtils.move().from(writableBook.slot()).toHotbar(mc.player.getInventory().selectedSlot);
            return;
        }

        // Check delay
        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        // Reset delay
        delayTimer = delay.get();

        // Write book
        if (mode.get() == Mode.Random) {
            int origin = onlyAscii.get() ? 0x21 : 0x0800;
            int bound = onlyAscii.get() ? 0x7E : 0x10FFFF;

            // Generate a random load of ints to use as random characters
            writeBook(
                random.ints(origin, bound)
                    .filter(i -> !Character.isWhitespace(i) && i != '\r' && i != '\n')
                    .iterator()
            );
        } else if (mode.get() == Mode.File) {
            if (lines == null || lines.isEmpty()) {
                error("The bookbot file is empty or not found. (%s)", textPath.get());
                toggle();
            } else writeBook(String.join("\n", lines).chars().iterator());
        }
    }

    private void writeBook(PrimitiveIterator.OfInt chars) {
        ArrayList<String> pages = new ArrayList<>();

        for (int pageI = 0; pageI < (mode.get() == Mode.File ? 100 : this.pages.get()); pageI++) {
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

                    // Ignore newline chars when writing lines, should already be organised
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

            // Append page to the page list
            pages.add(page.toString());
        }

        // Get the title with count
        String title = name.get();
        if (count.get() && bookCount != 0) title += " #" + bookCount;

        // Write data to book
        mc.player.getMainHandStack().setSubNbt("title", NbtString.of(title));
        mc.player.getMainHandStack().setSubNbt("author", NbtString.of(mc.player.getGameProfile().getName()));

        // Write pages NBT
        NbtList pageNbt = new NbtList();
        pages.stream().map(NbtString::of).forEach(pageNbt::add);
        if (!pages.isEmpty()) mc.player.getMainHandStack().setSubNbt("pages", pageNbt);

        // Send book update to server
        mc.player.networkHandler.sendPacket(new BookUpdateC2SPacket(mc.player.getInventory().selectedSlot, pages, Optional.of(title)));

        bookCount++;
    }

    public enum Mode {
        File,
        Random
    }
}
