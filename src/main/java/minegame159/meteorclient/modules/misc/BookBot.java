package minegame159.meteorclient.modules.misc;

//Created by squidoodly 06/07/2020 AT FUCKING 12:00AM KILL ME

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.zero.alpine.listener.EventHandler; //YAY!! COMMENTS!! I LOVE COMMENTS!!
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;//Cum
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.Utils;// Have fun getting rid of all these!!
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.container.SlotActionType;
import net.minecraft.item.Items;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.util.Hand;
//FUCK YOU GHOST TYPES
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;//Can you even say bitch in code on GitHub? I hope so, for your sake. :kekw:
import java.io.IOException;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BookBot extends ToggleModule {
    public enum Mode{ //Edna Mode
        File,
        Random,
        Ascii
    }
    //Didn't add it to the module list cuz I didn't know if it was gonna work.
    public BookBot(){
        super(Category.Misc, "book-bot", "Writes books full of characters or from a file."); //Grammar who?
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();//Obligatory comment.

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>() //WEEEEEEEEEEEEEEEEEEEE (Wanted to add a comment on everything but nothing to say so fuck you.)
            .name("mode")
            .description("The mode of the book bot.")
            .defaultValue(Mode.Ascii)
            .build()
    );
    //Idk how to add the name into the book so you're gonna have to do it or tell me.
    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
            .name("name")
            .description("The name you want to give the books")
            .defaultValue("Meteor on Crack!") //METEOR ON CRACK!!!
            .build()
    );

    private final Setting<String> fileName = sgGeneral.add(new StringSetting.Builder()
            .name("file-name")
            .description("The name of the text file (.txt included)") //Some retard will do it without and complain like a tard.
            .defaultValue("book.txt")
            .build()
    );

    private final Setting<Integer> noOfPages = sgGeneral.add(new IntSetting.Builder()
            .name("no-of-pages")
            .description("The number of pages to write per book.") //Fuck making it individual per book.
            .defaultValue(100)
            .min(1)
            .max(100)
            .sliderMax(100) //Max number of pages possible.
            .build()
    );

    private final Setting<Integer> noOfBooks = sgGeneral.add(new IntSetting.Builder()
            .name("no-of-books")
            .description("The number of books to make(or until the file runs out)")
            .defaultValue(1)
            .min(1)
            .sliderMax(9999) //For when you are trying to type the fucking bible or some shit.
            .max(999999999) //Don't ask why anyone would want to but they can.
            .build()
    );
    //Please don't ask my why they are global. I have no answer for you.
    private static final Random RANDOM = new Random();
    private ListTag pages = new ListTag();
    private String joinedPages;
    private int booksLeft;

    @Override
    public void onActivate() { //WHY THE FUCK DOES OnActivate NOT CORRECT TO onActivate? Fucking retard.
        //We need to enter the loop somehow. ;)
        booksLeft = noOfBooks.get();
        super.onActivate(); //Almost forgot this. lol
    }

    @Override
    public void onDeactivate() {
        //Reset everything for next time. Don't know if it's needed but we're gonna do it anyway.
        booksLeft = 0;
        joinedPages = "";
        pages = new ListTag();
        super.onDeactivate(); //Same with this. 12:00 be like :quirky:
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        //Make sure we aren't in the inventory.
        if(mc.currentScreen instanceof ContainerScreen) return;
        //If there are no books left to write we are done.
        if(booksLeft == 0){
            this.onDeactivate();
            return;
        }
        //If the player isn't holding a book
        if(mc.player.getMainHandStack().getItem() != Items.WRITABLE_BOOK){
            //Find one
            InvUtils.FindItemResult itemResult = InvUtils.findItemWithCount(Items.WRITABLE_BOOK);
            //If it's in their hotbar then just switch to it (no need to switch back later)
            if(itemResult.slot <= 8 && itemResult.slot != -1){
                mc.player.inventory.selectedSlot = itemResult.slot;
            }else if(mc.player.inventory.selectedSlot > 8){ //Else if it's in their inventory then swap their current item with the writable book
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(itemResult.slot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(mc.player.inventory.selectedSlot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(itemResult.slot), 0, SlotActionType.PICKUP);
            }else{ //Otherwise we are out and we can just wait for more books.
                //I'm always waiting. Watching. Get more books. I dare you. :))))
                return;
            }
        }
        if(mode.get() == Mode.Random){
            //Generates a random stream of integers??
            IntStream charGenerator = RANDOM.ints(0x80, 0x10ffff - 0x800).map(i -> i < 0xd800 ? i : i + 0x800);
            //Convert that stream into a string and tidies up.
            joinedPages = charGenerator.mapToObj(i -> String.valueOf((char) i)).collect(Collectors.joining()).replaceFirst("\r\n", "\n");
            //Pass that string into the book writing thing.
            writeBook(joinedPages);
        }else if(mode.get() == Mode.Ascii){
            //Generates a random stream of integers??
            IntStream charGenerator = RANDOM.ints(0x20, 0x7f);
            //Converts that stream into a string and tidies up.
            joinedPages = charGenerator.mapToObj(i -> String.valueOf((char) i)).collect(Collectors.joining()).replaceFirst("\r\n", "\n");
            //Pass that string to the book writing thing.
            writeBook(joinedPages);
        }else if(mode.get() == Mode.File){
            //If it is the first time writing a book
            if(booksLeft == noOfBooks.get()) {
                //Fetch the file and initialise the IntList
                File file = new File(MeteorClient.FOLDER, fileName.get());
                IntList chars = new IntArrayList();
                //Check if the file exists.
                if (!file.exists()) {
                    Utils.sendMessage("#redThe file you specified doesn't exist in the meteor folder."); //You dumb bitch.
                    return;
                }
                //Try to read the file
                try {
                    //Create the reader
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    chars.clear();

                    //Put all the characters into the IntList. (Cuz why read words as words? That would make too much sense.)
                    int c;
                    while ((c = reader.read()) != -1) chars.add(c);
                    reader.close();

                    //Make the second list??? Idk, you coded this, you should know. Idk why I have to explain this shit to you. It just works. (I hope)
                    int[] chars2 = new int[chars.size()];
                    chars.getElements(0, chars2, 0, chars.size());

                    //Turn the IntList into a string (Like they should have fucking been since the start because, ya know, they are FUCKING WORDS IN THE FILE YOU TWAT)
                    joinedPages = IntStream.of(chars2).toString(); //At least it's easy to convert them or I think I would have cut my dick off.
                    //Pass the string to the book writing thing.
                    writeBook(joinedPages);
                } catch (IOException ignored) { //EZ ignore. > 1 blocked message
                    //If it fails then send a message
                    Utils.sendMessage("#redFailed to read the file.");
                    //When you try your best but you don't succeed.
                }
            }else if(booksLeft < noOfBooks.get()){ //If it's not the first time writing to a book
                if(joinedPages.length() > 0) { //Make sure there is something to write.
                    //We can just pass the main string as pages get removed as they are added to the ListTag and then the ListTag is used to make a whole book.
                    //The same page shouldn't be written twice as the string is removed as soon as it is added.
                    writeBook(joinedPages);
                }else{
                    onDeactivate();//Hope this is the right one.
                }
            }
        }
    });
    //Idk if it;s just cuz it's late and I'm starting to doubt myself but I'm like 90% sure I fucked this up. Please fix it so I can call myself a dev again.
    private void writeBook(String bitchNutz){ //BITCHNUTZ LMAO
        //Initialise all the shit I need.
        int page = 0;
        int j;
        int line;
        int i;
        //If the book isn't full
        while(page < noOfPages.get()) {
            j = 0;
            line = 0;
            //If the page isn't full
            while (line < 13) { //13 is the most number of lines I am comfortable fitting on a page
                i = 1;
                //Make sure we don't go too far.
                if(mc.textRenderer.getStringWidth(bitchNutz.substring(0, bitchNutz.length() - 1)) >= 113) {
                    //Make the line as long as it's allowed
                    while (mc.textRenderer.getStringWidth(bitchNutz.substring(0, i)) <= 113) { //113 is the widest a line can be in pixels
                        i += 1;
                    }
                    //Make sure the line isn't too long just in case we overshot with the last one.
                    if (mc.textRenderer.getStringWidth(bitchNutz.substring(0, i)) > 113) {
                        i -= 1;
                    }
                    //Add to j as it will be the final index of the page
                    j += i;
                    //Remove the last page from the string we are working on
                    bitchNutz = bitchNutz.substring(i, bitchNutz.length() - 1);
                    //Add to the line count
                    line += 1;
                }else{ //If we go too far then just fucking end it.
                    //Go to the end of the thing and just exit the script.
                    j = bitchNutz.length() - 1;
                    //Originally had line = 13 cuz am smart. :sunglasses:
                    break;
                }
            }
            //Add a page to the ListTag
            pages.add(new StringTag(joinedPages.substring(0, j)));
            //Make sure if we are at the end of the document then we just leave the thing and the book is counted and it's over.
            if(bitchNutz.length() - 1 - j > 0) { //Just in case it's a perfect length? I don't really know why I added this but it's here and it's probably for a reason so it's staying.
                //Remove a page from the main string.
                joinedPages = bitchNutz.substring(j, bitchNutz.length() - 1);
            }else{
                break; //Make sure we don't keep writing pages if whatever happened above happens. Idk anymore
            }
            //Add to the page counter. We can afford to do this no matter what cuz why not?
            page += 1;
        }
        //Idk how to close the book properly.
        //.................................................HELP.......................................................
        mc.player.getMainHandStack().getOrCreateTag().put("pages", pages);
        //Plz make meteor the fucking author if you can. :kekw:
        mc.player.networkHandler.sendPacket(new BookUpdateC2SPacket(mc.player.getMainHandStack(), true, Hand.MAIN_HAND)); //Idk what signed means, so but I changed it because I hoped it would change the constructor it used cuz I'm smart. Unsurprisingly it didn't change the constructor but hopefully I did a good anyway?
        //To keep track of how many books we have left.
        booksLeft -= 1;
        //Book go Brrrrrrrrrr
    }
} //IT TOOK ME 30 FUCKING MINUTES TO COMMENT THIS. I WANT TO DIE. SEND HELP. CODING METEOR IS BECOMING AN ADDICTION. PLEASE. CAN SOMEONE HEAR ME? ANYONE?
