/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

//Created by Astro Orbis 29/03/2021 from github.com/MeteorDevelopment/meteor-book-dupe-addon

package minegame159.meteorclient.commands.commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Hand;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

//Credit to the original author (https://github.com/Gaider10/BookDupe) (i think) for some of this code.

public class BookDupeCommand extends Command {

    private final SimpleCommandExceptionType BOOK_NOT_FOUND_EXCEPTION = new SimpleCommandExceptionType(new LiteralText("No writeable book found in inventory."));
    private final ItemStack DUPE_BOOK = new ItemStack(Items.WRITABLE_BOOK, 1);

    public BookDupeCommand() {
        super("dupe", "Dupes using a held, writable book.");

        StringBuilder stringBuilder = new StringBuilder();

        for(int i = 0; i < 21845; i++){
            stringBuilder.append((char) 2048);
        }

        String str1 = stringBuilder.toString();



        ListTag listTag = new ListTag();
        listTag.addTag(0, StringTag.of(str1));

        for(int i = 1; i < 40; i++){
            listTag.addTag(i, StringTag.of("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        }

        DUPE_BOOK.putSubTag("title", StringTag.of("a"));
        DUPE_BOOK.putSubTag("pages", listTag);
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (InvUtils.getHand(Items.WRITABLE_BOOK) != Hand.MAIN_HAND) ChatUtils.prefixError("BOOK DUPE", "No book found, you must be holding a writable book!");
            else mc.player.networkHandler.sendPacket(new BookUpdateC2SPacket(DUPE_BOOK, true, mc.player.inventory.selectedSlot));

            return SINGLE_SUCCESS;
        });
    }

}
