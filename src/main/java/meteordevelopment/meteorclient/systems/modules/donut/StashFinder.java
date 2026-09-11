/*
 * Ember Client - Donut SMP Module
 */

package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.elements.EmberNotificationsHud;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class StashFinder extends Module {
    private static final String FIND_TYPE = "stash";

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgStorage = settings.createGroup("Saving");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> minChests = sgGeneral.add(new IntSetting.Builder()
        .name("min-chests")
        .description("Minimum chests to count as a stash.")
        .defaultValue(5)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Range to check for chests.")
        .defaultValue(10)
        .min(1)
        .sliderRange(1, 32)
        .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Send chat notification when stash found.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> saveFinds = sgStorage.add(new BoolSetting.Builder()
        .name("save-finds")
        .description("Remember stashes for this server, so they still show after relogging.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoWaypoint = sgStorage.add(new BoolSetting.Builder()
        .name("auto-waypoint")
        .description("Add a waypoint for every new stash.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Highlight found stashes.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("Stash highlight color.")
        .defaultValue(new SettingColor(255, 190, 60, 70))
        .visible(render::get)
        .build()
    );

    /** One entry per stash; a nearby chest group counts as the same stash. */
    private final List<BlockPos> foundStashes = new ArrayList<>();
    private int timer;

    public StashFinder() {
        super(Categories.Donut, "stash-finder", "Finds potential stashes on Donut SMP.");
    }

    @Override
    public void onActivate() {
        foundStashes.clear();
        timer = 0;

        for (FindsStorage.Find find : FindsStorage.get(FindsStorage.context(), FIND_TYPE)) {
            foundStashes.add(new BlockPos(find.x, find.y, find.z));
        }
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        onActivate();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        // A cube of up to 65 blocks is a lot of lookups; once a second is plenty for finding stashes.
        if (++timer < 20) return;
        timer = 0;

        BlockPos playerPos = mc.player.blockPosition();
        int chestCount = 0;
        BlockPos centerPos = null;
        int r = range.get();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    if (mc.level.getBlockState(pos).getBlock() == Blocks.CHEST) {
                        chestCount++;
                        if (centerPos == null) centerPos = pos;
                    }
                }
            }
        }

        if (chestCount < minChests.get() || centerPos == null || isKnown(centerPos)) return;

        foundStashes.add(centerPos);

        if (notify.get()) {
            info("Potential stash found! %d chests at %d, %d, %d",
                chestCount, centerPos.getX(), centerPos.getY(), centerPos.getZ());
        }
        if (saveFinds.get()) FindsStorage.add(FindsStorage.context(), FIND_TYPE, centerPos, chestCount + " chests");
        if (autoWaypoint.get()) FindsStorage.waypoint("Stash", centerPos);

        EmberNotificationsHud.push("Stash found", chestCount + " chests at " + centerPos.getX() + ", " + centerPos.getZ(), EmberNotificationsHud.GOOD);
    }

    /** The first chest found shifts as you move, so treat anything close to a known stash as the same one. */
    private boolean isKnown(BlockPos pos) {
        for (BlockPos known : foundStashes) {
            if (known.distSqr(pos) <= 32 * 32) return true;
        }
        return false;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;

        for (BlockPos pos : foundStashes) {
            event.renderer.box(pos, color.get(), color.get(), ShapeMode.Both, 0);
        }
    }

    @Override
    public String getInfoString() {
        return foundStashes.isEmpty() ? null : String.valueOf(foundStashes.size());
    }
}
