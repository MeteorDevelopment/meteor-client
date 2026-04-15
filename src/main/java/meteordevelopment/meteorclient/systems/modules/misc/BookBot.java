/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Predicate;

public class BookBot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("What kind of text to write.")
        .defaultValue(Mode.Random)
        .build()
    );

    private final Setting<RandomType> randomType = sgGeneral.add(new EnumSetting.Builder<RandomType>()
        .name("random-type")
        .description("What kind of random to use.")
        .defaultValue(RandomType.Utf8)
        .visible(() -> mode.get() == Mode.Random)
        .build()
    );

    private final Setting<Integer> pages = sgGeneral.add(new IntSetting.Builder()
        .name("pages")
        .description("The number of pages to write per book.")
        .defaultValue(50)
        .range(1, 100)
        .sliderRange(1, 100)
        .visible(() -> mode.get() != Mode.File && randomType.get() != RandomType.PaperMC)
        .build()
    );

    private final Setting<Integer> characters = sgGeneral.add(new IntSetting.Builder()
        .name("characters")
        .description("How many characters to write per page.")
        .defaultValue(128)
        .range(1, 1024)
        .sliderRange(1, 1024)
        .visible(() -> mode.get() == Mode.Random && randomType.get() != RandomType.PaperMC)
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

    private final Setting<Boolean> sign = sgGeneral.add(new BoolSetting.Builder()
        .name("sign")
        .description("Whether to sign the book.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("The name you want to give your books.")
        .defaultValue("Meteor on Crack!")
        .visible(sign::get)
        .build()
    );

    private final Setting<Boolean> count = sgGeneral.add(new BoolSetting.Builder()
        .name("append-count")
        .description("Whether to append the number of the book to the title.")
        .defaultValue(true)
        .visible(sign::get)
        .build()
    );

    private final Setting<Boolean> wordWrap = sgGeneral.add(new BoolSetting.Builder()
        .name("word-wrap")
        .description("Prevents words from being cut in the middle of lines.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.File)
        .build()
    );

    private File file = new File(MeteorClient.FOLDER, "bookbot.txt");
    private final PointerBuffer filters;

    private int delayTimer, bookCount;
    private Random random;

    public BookBot() {
        super(Categories.Misc, "book-bot", "Automatically writes in books.");

        if (!file.exists()) {
            file = null;
        }

        filters = BufferUtils.createPointerBuffer(1);

        ByteBuffer txtFilter = MemoryUtil.memASCII("*.txt");

        filters.put(txtFilter);
        filters.rewind();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WHorizontalList list = theme.horizontalList();

        WButton selectFile = list.add(theme.button("Select File")).widget();

        WLabel fileName = list.add(theme.label((file != null && file.exists()) ? file.getName() : "No file selected.")).widget();

        selectFile.action = () -> {
            String path = TinyFileDialogs.tinyfd_openFileDialog(
                "Select File",
                new File(MeteorClient.FOLDER, "bookbot.txt").getAbsolutePath(),
                filters,
                null,
                false
            );

            if (path != null) {
                file = new File(path);
                fileName.set(file.getName());
            }
        };

        return list;
    }

    @Override
    public void onActivate() {
        if ((file == null || !file.exists()) && mode.get() == Mode.File) {
            info("No file selected, please select a file in the GUI.");
            toggle();
            return;
        }

        random = new Random();
        delayTimer = delay.get();
        bookCount = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Predicate<ItemStack> bookPredicate = i -> {
            WritableBookContent component = i.get(DataComponents.WRITABLE_BOOK_CONTENT);
            return i.getItem() == Items.WRITABLE_BOOK && (component == null || component.pages().isEmpty());
        };

        FindItemResult writableBook = InvUtils.find(bookPredicate);

        // Check if there is a book to write
        if (!writableBook.found()) {
            toggle();
            return;
        }

        // Move the book into hand
        if (!InvUtils.testInMainHand(bookPredicate)) {
            InvUtils.move().from(writableBook.slot()).toHotbar(mc.player.getInventory().getSelectedSlot());
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
            switch (randomType.get()) {
                case Ascii ->
                    writeBook(random.ints(0x21, 0x80).filter(i -> !Character.isWhitespace(i) && i != '\r' && i != '\n').iterator());
                case Utf8 ->
                    writeBook(random.ints(0x21, 0xD800).filter(i -> !Character.isWhitespace(i) && i != '\r' && i != '\n').iterator());
                case PaperMC -> writePaperMcBook();
            }
        } else if (mode.get() == Mode.File) {
            // Ignore if somehow the file got deleted
            if ((file == null || !file.exists()) && mode.get() == Mode.File) {
                info("No file selected, please select a file in the GUI.");
                toggle();
                return;
            }

            // Handle the file being empty
            if (file.length() == 0) {
                MutableComponent message = Component.literal("");
                message.append(Component.literal("The bookbot file is empty! ").withStyle(ChatFormatting.RED));
                message.append(Component.literal("Click here to edit it.")
                    .setStyle(Style.EMPTY
                        .applyFormats(ChatFormatting.UNDERLINE, ChatFormatting.RED)
                        .withClickEvent(new ClickEvent.OpenFile(file.getAbsolutePath()))
                    )
                );
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
            } catch (IOException _) {
                error("Failed to read the file.");
            }
        }
    }

    private void writeBook(PrimitiveIterator.OfInt chars) {
        ArrayList<String> pages = new ArrayList<>();
        ArrayList<Filterable<Component>> filteredPages = new ArrayList<>();
        int maxPages = mode.get() == Mode.File ? 100 : this.pages.get();

        if (wordWrap.get() && mode.get() == Mode.File) {
            StringBuilder text = new StringBuilder();
            while (chars.hasNext()) {
                text.appendCodePoint(chars.nextInt());
            }

            // Use mc's own word wrapping logic
            List<FormattedText> wrappedLines = mc.font.splitIgnoringLanguage(Component.literal(text.toString()), 114);
            processLinesToPages(wrappedLines, pages, filteredPages, maxPages);
        } else {
            int pageIndex = 0;
            final StringBuilder page = new StringBuilder();

            while (pageIndex != maxPages) {
                for (int i = 0; i < characters.get() && chars.hasNext(); i++) {
                    page.appendCodePoint(chars.nextInt());
                }

                if (!page.isEmpty()) {
                    String builtPage = page.toString();
                    filteredPages.add(Filterable.passThrough(Component.nullToEmpty(builtPage)));
                    pages.add(builtPage);
                    page.setLength(0);
                }

                pageIndex++;
            }
        }

        createBook(pages, filteredPages);
    }

    /**
     * @author S
     */
    private void writePaperMcBook() {
        ArrayList<String> pages = new ArrayList<>();
        ArrayList<Filterable<Component>> filteredPages = new ArrayList<>();
        final StringBuilder page = new StringBuilder();

        PrimitiveIterator.OfInt oneByte = random.ints(0x21, 0x80).iterator();
        PrimitiveIterator.OfInt twoBytes = random.ints(0x0080, 0x0800).iterator();
        PrimitiveIterator.OfInt threeBytes = random.ints(0x0800, 0xD800).iterator();

        for (int pageIndex = 0; pageIndex < 100; pageIndex++) {
            if (pageIndex < 50) {
                page.appendCodePoint(threeBytes.nextInt());
                for (int i = 1; i < 1024; i++) {
                    page.appendCodePoint(oneByte.nextInt());
                }
            } else if (pageIndex == 50) {
                for (int i = 0; i < 110; i++) {
                    page.appendCodePoint(threeBytes.nextInt());
                }
                page.appendCodePoint(twoBytes.nextInt());
                for (int i = 0; i < 913; i++) {
                    page.appendCodePoint(oneByte.nextInt());
                }
            } else {
                for (int i = 0; i < 1024; i++) {
                    page.appendCodePoint(threeBytes.nextInt());
                }
            }

            String builtPage = page.toString();
            filteredPages.add(Filterable.passThrough(Component.nullToEmpty(builtPage)));
            pages.add(builtPage);
            page.setLength(0);
        }

        createBook(pages, filteredPages);
    }

    private void processLinesToPages(List<FormattedText> lines, ArrayList<String> pages, ArrayList<Filterable<Component>> filteredPages, int maxPages) {
        int pageIndex = 0;
        int lineIndex = 0;
        StringBuilder currentPage = new StringBuilder();

        for (FormattedText line : lines) {
            String lineText = line.getString();

            if (!currentPage.isEmpty()) {
                currentPage.append('\n');
            }
            currentPage.append(lineText);
            lineIndex++;

            if (lineIndex == 14) {
                filteredPages.add(Filterable.passThrough(Component.nullToEmpty(currentPage.toString())));
                pages.add(currentPage.toString());
                currentPage.setLength(0);
                pageIndex++;
                lineIndex = 0;

                if (pageIndex == maxPages) break;
            }
        }

        if (!currentPage.isEmpty() && pageIndex < maxPages) {
            filteredPages.add(Filterable.passThrough(Component.nullToEmpty(currentPage.toString())));
            pages.add(currentPage.toString());
        }
    }

    private void createBook(ArrayList<String> pages, ArrayList<Filterable<Component>> filteredPages) {
        // Get the title with count
        String title = name.get();
        if (count.get() && bookCount != 0) title += " #" + bookCount;

        // Write data to book
        mc.player.getMainHandItem().set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), mc.player.getGameProfile().name(), 0, filteredPages, true));

        // Send book update to server
        mc.player.connection.send(new ServerboundEditBookPacket(mc.player.getInventory().getSelectedSlot(), pages, sign.get() ? Optional.of(title) : Optional.empty()));

        bookCount++;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();

        if (file != null && file.exists()) {
            tag.putString("file", file.getAbsolutePath());
        }

        return tag;
    }

    @Override
    public Module fromTag(CompoundTag tag) {
        if (tag.contains("file")) {
            file = new File(tag.getStringOr("file", ""));
        }

        return super.fromTag(tag);
    }

    public enum Mode {
        File,
        Random
    }

    public enum RandomType {
        Ascii,
        Utf8,
        PaperMC
    }
}
