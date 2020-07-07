package minegame159.meteorclient.commands.commands;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.container.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.util.Hand;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Book extends Command {
    private static final int MAX_PAGES = 100;
    private static final int LINES_PER_PAGE = 210;
    private static final int PIXELS_PER_LINE = 113;

    private static final Random RANDOM = new Random();
    private static final File FILE = new File(MeteorClient.FOLDER, "book.txt");
    private static final IntList CHARS = new IntArrayList();

    public Book() {
        super("book", "Fills books with characters");
    }

    @Override
    public void run(String[] args) {
        if (args.length <= 0) {
            Utils.sendMessage("#redSpecify one of #grayrandom#red, #grayascii #redor #grayfile#red.");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "random": {
                int limit = parseLimit(args);
                if (limit == -1) return;
                fillBook(RANDOM.ints(0x80, 0x10ffff - 0x800).map(i -> i < 0xd800 ? i : i + 0x800), limit);
                break;
            }
            case "ascii": {
                int limit = parseLimit(args);
                if (limit == -1) return;
                fillBook(RANDOM.ints(0x20, 0x7f), limit);
                break;
            }
            case "file": {
                if (!FILE.exists()) {
                    Utils.sendMessage("#redPlace a 'book.txt' file inside 'meteor-client' folder.");
                    return;
                }

                try {
                    BufferedReader reader = new BufferedReader(new FileReader(FILE));
                    CHARS.clear();

                    int c;
                    while ((c = reader.read()) != -1) CHARS.add(c);
                    reader.close();

                    int[] chars = new int[CHARS.size()];
                    CHARS.getElements(0, chars, 0, CHARS.size());
                    fillBook(IntStream.of(chars), 100);
                } catch (IOException ignored) {
                    Utils.sendMessage("#redFailed to read the file.");
                }
                break;
            }
        }
    }

    private int parseLimit(String[] args) {
        if (args.length <= 1) {
            Utils.sendMessage("#redSpecify number of pages #gray(1-100)#red.");
            return -1;
        }

        int limit = 0;
        boolean ok = true;

        try {
            limit = Integer.parseInt(args[1]);
            if (limit < 1 || limit > 100) ok = false;
        } catch (NumberFormatException ignored) {
            ok = false;
        }

        if (ok) {
            return limit;
        } else {
            Utils.sendMessage("#redSpecify number of pages #gray(1-100)#red.");
            return -1;
        }
    }

    private void fillBook(IntStream charGenerator, int limit) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        InvUtils.FindItemResult itemResult = InvUtils.findItemWithCount(Items.WRITABLE_BOOK);
        if(itemResult.slot <= 8){
            player.inventory.selectedSlot = itemResult.slot;
        }else{
            if(player.inventory.getEmptySlot() < 8){
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(itemResult.slot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(player.inventory.getEmptySlot()), 0, SlotActionType.PICKUP);
            }else{
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(itemResult.slot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(player.inventory.selectedSlot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(itemResult.slot), 0, SlotActionType.PICKUP);
            }
        }
        ItemStack heldItem = player.getMainHandStack();

        String joinedPages = charGenerator.mapToObj(i -> String.valueOf((char) i)).collect(Collectors.joining());
        joinedPages = joinedPages.replaceAll("\r\n", "\n");

        ListTag pages = new ListTag();
        /*
        int start = 0;
        int end = 0;
        for (int page = 0; page < limit;) {
            end++;

            if (end >= (page + 1) * CHARS_PER_PAGE) {
                boolean brejk = false;
                if (end >= joinedPages.length()) {
                    end = joinedPages.length();
                    brejk = true;
                }
                pages.add(StringTag.of(joinedPages.substring(start, end)));
                start = end + 1;
                page++;
                if (brejk) break;
            }
        }
         */
        //Separate the pages
        for(int page = 1; page <= limit; page++){
            pages.add(StringTag.of(""));
        }

        heldItem.getOrCreateTag().put("pages", pages);
        player.networkHandler.sendPacket(new BookUpdateC2SPacket(heldItem, false, Hand.MAIN_HAND));
    }
}
