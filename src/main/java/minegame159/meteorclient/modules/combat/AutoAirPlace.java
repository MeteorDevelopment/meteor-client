package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Chat;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoAirPlace extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
            .name("turn-off")
            .description("Toggles when one placed.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
            .name("self-toggle")
            .description("Toggles when you run out of obsidian.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> onlyObby = sgGeneral.add(new BoolSetting.Builder()
            .name("only-obsidian")
            .description("Whether or not to only airplace obsidian.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> reachDist = sgGeneral.add(new DoubleSetting.Builder()
            .name("reach-distance")
            .description("The distance the block will be airplaced at.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build()
    );
    //all of this alignment stuff is just so i can "debug" it in real time instead of having to reaload the game each time
    private final Setting<Double> alignX = sgGeneral.add(new DoubleSetting.Builder()
            .name("x axis alignment")
            .description("how far along the x axis the center of the reach sphere will be shifted.")
            .defaultValue(0.5)
            .min(-1)
            .sliderMax(1)
            .build()
    );
    private final Setting<Double> alignZ = sgGeneral.add(new DoubleSetting.Builder()
            .name("z axis alignment")
            .description("how far along the z axis the center of the reach sphere will be shifted.")
            .defaultValue(0.5)
            .min(-1)
            .sliderMax(1)
            .build()
    );
    private final Setting<Double> alignY = sgGeneral.add(new DoubleSetting.Builder()
            .name("y axis alignment")
            .description("how far along the y axis the center of the reach sphere will be shifted.")
            .defaultValue(1)
            .min(-1)
            .sliderMax(1)
            .build()
    );


    public AutoAirPlace(){
        super(Category.Combat, "auto-air-place", "Automatically places a block in the air at the end of your reach distance.");
    }

    BlockPos targetPos;
    int blockSlot;
    int obsidianSlot;
    int prevSlot;
    boolean sentMessage = false;

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if (onlyObby.get() == false) {
            blockSlot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack handStack = mc.player.getMainHandStack(); //if you're already holding a block, it will place that block instead of searching your hotbar
                if (!handStack.isEmpty() && (handStack.getItem() instanceof BlockItem)){
                    blockSlot = mc.player.inventory.selectedSlot;
                } else if (mc.player.inventory.getStack(i).getItem() instanceof BlockItem) {
                    blockSlot = i;
                    break;
                }
            }

            if (blockSlot == -1 && selfToggle.get()) {
                if (!sentMessage) {
                    Chat.warning(this, "No blocks found… disabling.");
                    sentMessage = true;
                }
                this.toggle();
                return;
            } else if (blockSlot == -1) return;
            prevSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = blockSlot;
            targetPos = mc.player.getBlockPos().up();

            double YawRadians = (mc.gameRenderer.getCamera().getYaw() + 90) * (Math.PI / 180);//converts pitch and yaw from degrees to radians
            double PitchRadians = (mc.gameRenderer.getCamera().getPitch() + 90) * (Math.PI / 180);
            double placeholderX = reachDist.get() * Math.sin(PitchRadians) * Math.cos(YawRadians) + alignX.get();//x, y and z coords where it will attempt to airplace the block
            double placeholderZ = reachDist.get() * Math.sin(PitchRadians) * Math.sin(YawRadians) + alignZ.get();
            double placeholderY = reachDist.get() * Math.cos(PitchRadians) + alignY.get();


            if (mc.world.getBlockState(targetPos.add(placeholderX, placeholderY, placeholderZ)).getMaterial().isReplaceable()) {
                mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, targetPos.add(placeholderX, placeholderY, placeholderZ), false));
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            if (turnOff.get()) toggle();
            mc.player.inventory.selectedSlot = prevSlot;
        } else if (onlyObby.get() == true){
            obsidianSlot = -1;
            for(int i = 0; i < 9; i++){
                if (mc.player.inventory.getStack(i).getItem() == Blocks.OBSIDIAN.asItem()){
                    obsidianSlot = i;
                    break;
                }
            }
            if (obsidianSlot == -1 && selfToggle.get()) {
                if (!sentMessage) {
                    Chat.warning(this, "No obsidian found… disabling.");
                    sentMessage = true;
                }
                this.toggle();
                return;
            } else if (obsidianSlot == -1) return;
            prevSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = obsidianSlot;
            targetPos = mc.player.getBlockPos().up();

            double YawRadians = (mc.gameRenderer.getCamera().getYaw() + 90) * (Math.PI / 180);//converts pitch and yaw from degrees to radians
            double PitchRadians = (mc.gameRenderer.getCamera().getPitch() + 90) * (Math.PI / 180);
            double placeholderX = reachDist.get() * Math.sin(PitchRadians) * Math.cos(YawRadians) + alignX.get();//x, y and z coords where it will attempt to airplace the block
            double placeholderZ = reachDist.get() * Math.sin(PitchRadians) * Math.sin(YawRadians) + alignZ.get();
            double placeholderY = reachDist.get() * Math.cos(PitchRadians) + alignY.get();


            if(mc.world.getBlockState(targetPos.add(placeholderX, placeholderY, placeholderZ)).getMaterial().isReplaceable()){
                mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, targetPos.add(placeholderX , placeholderY, placeholderZ), false));
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            if (turnOff.get()) toggle();
            mc.player.inventory.selectedSlot = prevSlot;
        }
    });
}
