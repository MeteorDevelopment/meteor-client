/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EditBookTitleAndAuthorScreen extends WindowScreen {
    private final ItemStack itemStack;
    private final InteractionHand hand;

    public EditBookTitleAndAuthorScreen(GuiTheme theme, ItemStack itemStack, InteractionHand hand) {
        super(theme, "Edit title & author");
        this.itemStack = itemStack;
        this.hand = hand;
    }

    @Override
    public void initWidgets() {
        WTable t = add(theme.table()).expandX().widget();

        t.add(theme.label("Title"));
        WTextBox title = t.add(theme.textBox(itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT).title().get(mc.shouldFilterText()))).minWidth(220).expandX().widget();
        t.row();

        t.add(theme.label("Author"));
        WTextBox author = t.add(theme.textBox(itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT).author())).minWidth(220).expandX().widget();
        t.row();

        t.add(theme.button("Done")).expandX().widget().action = () -> {
            WrittenBookContent component = itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            WrittenBookContent newComponent = new WrittenBookContentComponent(Filterable.of(title.get()), author.get(), component.generation(), component.pages(), component.resolved());
            itemStack.set(DataComponents.WRITTEN_BOOK_CONTENT, newComponent);

            BookViewScreen.BookAccess contents = new BookViewScreen.Contents(itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT).getPages(mc.shouldFilterText()));
            List<String> pages = new ArrayList<>(contents.getPageCount());
            for (int i = 0; i < contents.getPageCount(); i++) pages.add(contents.getPage(i).getString());

            mc.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(hand == InteractionHand.MAIN_HAND ? mc.player.getInventory().getSelectedSlot() : 40, pages, Optional.of(title.get())));

            close();
        };
    }
}
