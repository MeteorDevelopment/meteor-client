/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.screens;

import motordevelopment.motorclient.gui.GuiTheme;
import motordevelopment.motorclient.gui.WindowScreen;
import motordevelopment.motorclient.gui.widgets.containers.WTable;
import motordevelopment.motorclient.gui.widgets.input.WTextBox;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static motordevelopment.motorclient.MotorClient.mc;

public class EditBookTitleAndAuthorScreen extends WindowScreen {
    private final ItemStack itemStack;
    private final Hand hand;

    public EditBookTitleAndAuthorScreen(GuiTheme theme, ItemStack itemStack, Hand hand) {
        super(theme, "Edit title & author");
        this.itemStack = itemStack;
        this.hand = hand;
    }

    @Override
    public void initWidgets() {
        WTable t = add(theme.table()).expandX().widget();

        t.add(theme.label("Title"));
        WTextBox title = t.add(theme.textBox(itemStack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT).title().get(mc.shouldFilterText()))).minWidth(220).expandX().widget();
        t.row();

        t.add(theme.label("Author"));
        WTextBox author = t.add(theme.textBox(itemStack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT).author())).minWidth(220).expandX().widget();
        t.row();

        t.add(theme.button("Done")).expandX().widget().action = () -> {
            WrittenBookContentComponent component = itemStack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
            WrittenBookContentComponent newComponent = new WrittenBookContentComponent(RawFilteredPair.of(title.get()), author.get(), component.generation(), component.pages(), component.resolved());
            itemStack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, newComponent);

            BookScreen.Contents contents = new BookScreen.Contents(itemStack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT).getPages(mc.shouldFilterText()));
            List<String> pages = new ArrayList<>(contents.getPageCount());
            for (int i = 0; i < contents.getPageCount(); i++) pages.add(contents.getPage(i).getString());

            mc.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(hand == Hand.MAIN_HAND ? mc.player.getInventory().selectedSlot : 40, pages, Optional.of(title.get())));

            close();
        };
    }
}
