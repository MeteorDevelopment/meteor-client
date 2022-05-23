/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.notebot.NBSDecoder;
import meteordevelopment.meteorclient.utils.notebot.NotebotUtils;
import meteordevelopment.meteorclient.utils.notebot.nbs.Layer;
import meteordevelopment.meteorclient.utils.notebot.nbs.Note;
import meteordevelopment.meteorclient.utils.notebot.nbs.Song;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


public class Notebot extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render", false);

    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("The delay when loading a song.")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<NotebotUtils.InstrumentType> instrument = sgGeneral.add(new EnumSetting.Builder<NotebotUtils.InstrumentType>()
        .name("instrument")
        .description("Select which instrument will be played")
        .defaultValue(NotebotUtils.InstrumentType.NotDrums)
        .build()
    );

    private final Setting<Boolean> polyphonic = sgGeneral.add(new BoolSetting.Builder()
        .name("polyphonic")
        .description("Whether or not to allow multiple notes to be played at the same time")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether or not to render the outline around the noteblocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 10))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    private final List<BlockPos> possibleBlockPos = new ArrayList<>();
    private final List<ImmutablePair<Integer, Integer>> song = new ArrayList<>();
    private final List<Integer> uniqueNotes = new ArrayList<>();
    private final HashMap<Integer, BlockPos> blockPositions = new HashMap<>();
    private final List<BlockPos> scannedNoteblocks = new ArrayList<>();
    private Stage stage = Stage.None;
    private boolean isPlaying = false;
    private int currentNote = 0;
    private int currentIndex = 0;
    private int offset = 0;
    private int ticks = 0;
    private boolean noSongsFound = true;
    private WLabel status;


    public Notebot() {
        super(Categories.Misc, "notebot", "Plays noteblock nicely");

        for (int y = -5; y < 5; y++) {
            for (int x = -5; x < 5; x++) {
                if (y != 0 || x != 0) {
                    BlockPos pos = new BlockPos(x, 0, y);
                    if (pos.getSquaredDistance(0, 0, 0) < (4.3 * 4.3) - 0.5) {
                        possibleBlockPos.add(pos);
                    }
                }
            }
        }

        possibleBlockPos.sort(Comparator.comparingDouble(vec -> vec.getSquaredDistance(new Vec3i(0, 0, 0))));
    }

    @Override
    public String getInfoString() {
        return stage.toString();
    }

    @Override
    public void onActivate() {
        ticks = 0;
        resetVariables();
    }

    private void resetVariables() {
        currentNote = 0;
        currentIndex = 0;
        offset = 0;
        isPlaying = false;
        stage = Stage.None;
        song.clear();
        blockPositions.clear();
        uniqueNotes.clear();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;

        if (stage != Stage.SetUp && stage != Stage.Tune && !isPlaying) return;

        blockPositions.values().forEach((blockPos) -> {
            double x1 = blockPos.getX();
            double y1 = blockPos.getY();
            double z1 = blockPos.getZ();
            double x2 = blockPos.getX() + 1;
            double y2 = blockPos.getY() + 1;
            double z2 = blockPos.getZ() + 1;

            event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        });
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        ticks++;

        if (stage == Stage.SetUp) {
            setup();
        }
        else if (stage == Stage.Tune) {
            tune();
        }
        else if (stage == Stage.Preview || stage == Stage.Playing) {
            if (!isPlaying) return;

            if (mc.player == null || currentIndex >= song.size()) {
                stop();
                return;
            }

            while (song.get(currentIndex).left < currentNote) {
                currentIndex++;
            }

            while (song.get(currentIndex).left == currentNote) {
                if (stage == Stage.Preview) onTickPreview();
                else onTickPlay();

                currentIndex++;

                if (currentIndex >= song.size()) return;
            }

            currentNote++;

            if (status != null) status.set(getStatus());
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();

        // Label
        status = table.add(theme.label(getStatus())).expandCellX().widget();

        // Pause
        WButton pause = table.add(theme.button(isPlaying ? "Pause" : "Resume")).right().widget();
        pause.action = () -> {
            pause();
            pause.set(isPlaying ? "Pause" : "Resume");
            status.set(getStatus());
        };

        // Stop
        WButton stop = table.add(theme.button("Stop")).right().widget();
        stop.action = this::stop;

        table.row();

        noSongsFound = true;

        try {
            Files.list(MeteorClient.FOLDER.toPath().resolve("notebot")).forEach(path -> {
                if (isValidFile(path)) {
                    noSongsFound = false;
                    table.add(theme.label(getFileLabel(path))).expandCellX();
                    WButton load = table.add(theme.button("Load")).right().widget();
                    load.action = () -> {
                        loadSong(path.toFile());
                        status.set(getStatus());
                    };
                    WButton preview = table.add(theme.button("Preview")).right().widget();
                    preview.action = () -> {
                        previewSong(path.toFile());
                        status.set(getStatus());
                    };
                    table.row();
                }
            });
        } catch (IOException e) {
            table.add(theme.label("Missing meteor-client/notebot folder.")).expandCellX();
            table.row();
        }

        if (noSongsFound) {
            table.add(theme.label("No songs found.")).expandCellX();
            table.row();

            WButton guide = table.add(theme.button("Guide")).expandX().widget();
            guide.action = () -> Util.getOperatingSystem().open("https://github.com/MeteorDevelopment/meteor-client/wiki/Notebot-Guide");
        }

        return table;
    }


    public String getStatus() {
        if (!this.isActive()) return "Module disabled.";
        if (song.isEmpty()) return "No song loaded.";
        if (isPlaying) return String.format("Playing song. %d/%d", currentIndex, song.size());
        if (stage == Stage.Playing || stage == Stage.Preview) return "Ready to play.";
        if (stage == Stage.SetUp || stage == Stage.Tune) return "Setting up the noteblocks.";
        else return String.format("Stage: %s.", stage.toString());
    }

    private String getFileLabel(Path file) {
        return file
            .getFileName()
            .toString()
            .replace(".txt", "")
            .replace(".nbs", "");
    }

    private boolean isValidFile(Path file) {
        String extension = FilenameUtils.getExtension(file.toFile().getName());
        if (extension.equals("txt")) return true;
        else return extension.equals("nbs");
    }

    public void play() {
        if (mc.player == null) return;
        if (mc.player.getAbilities().creativeMode && stage != Stage.Preview) {
            error("You need to be in survival mode.");
        } else if (stage == Stage.Preview || stage == Stage.Playing) {
            isPlaying = true;
            info("Playing.");
        } else {
            error("No song loaded.");
        }
    }

    public void pause() {
        if (!isActive()) toggle();
        if (isPlaying) {
            info("Pausing.");
            isPlaying = false;
        } else {
            info("Resuming.");
            isPlaying = true;
        }
    }

    public void stop() {
        info("Stopping.");
        if (stage == Stage.SetUp || stage == Stage.Tune) {
            resetVariables();
        } else {
            isPlaying = false;
            currentNote = 0;
            currentIndex = 0;
        }
        if (status != null) status.set(getStatus());
    }

    public void disable() {
        resetVariables();
        info("Stopping.");
        if (!isActive()) toggle();
    }

    public void loadSong(File file) {
        if (!isActive()) toggle();
        if (!loadFileToMap(file)) return;
        if (!setupBlocks()) return;
        info("Loading song \"%s\".", getFileLabel(file.toPath()));
    }

    public void previewSong(File file) {
        if (!isActive()) toggle();
        if (loadFileToMap(file)) {
            info("Song \"%s\" loaded.", getFileLabel(file.toPath()));
            stage = Stage.Preview;
            play();
        }
    }

    private void addNote(int tick, int value) {
        if (polyphonic.get()) {
            song.add(new ImmutablePair<>(tick, value));
        } else if (song.size() == 0) {
            song.add(new ImmutablePair<>(tick, value));
        } else if (song.get(song.size() - 1).left != tick) {
            song.add(new ImmutablePair<>(tick, value));
        }
    }

    private boolean loadFileToMap(File file) {
        if (!file.exists() || !file.isFile()) {
            error("File not found");
            return false;
        }
        String extension = FilenameUtils.getExtension(file.getName());
        boolean success = false;
        if (extension.equals("txt")) success = loadTextFile(file);
        else if (extension.equals("nbs")) success = loadNbsFile(file);
        if (success) {
            song.sort(Comparator.comparingInt(o -> o.left));
        }
        return success;
    }

    private boolean loadTextFile(File file) {
        List<String> data;
        try {
            data = Files.readAllLines(file.toPath());
        } catch (IOException e) {
            error("Error while reading \"%s\"", file.getName());
            return false;
        }
        resetVariables();
        for (int i = 0; i < data.size(); i++) {
            String[] parts = data.get(i).split(":");
            if (parts.length < 2) {
                warning("Malformed line %d", i);
                continue;
            }
            int key;
            int val;
            try {
                key = Integer.parseInt(parts[0]);
                val = Integer.parseInt(parts[1]);
                if (parts.length > 2) {
                    int type = Integer.parseInt(parts[2]);
                    if (!NotebotUtils.isValidIntrumentTextFile(type, instrument.get())) continue;
                }
            } catch (NumberFormatException e) {
                warning("Invalid character at line %d", i);
                continue;
            }
            addNote(key, val);
        }
        return true;
    }

    private boolean loadNbsFile(File file) {
        Song nbsSong = NBSDecoder.parse(file);
        if (nbsSong == null) {
            error("Couldn't parse the file. Only classic and opennbs v5 are supported");
            return false;
        }
        List<Layer> layers = new ArrayList<>(nbsSong.getLayerHashMap().values());
        resetVariables();
        for (Layer layer : layers) {
            for (int tick : layer.getHashMap().keySet()) {
                Note note = layer.getNote(tick);
                tick *= nbsSong.getDelay();
                if (note == null) continue;
                byte instr = note.getInstrument();
                if (!NotebotUtils.isValidInstrumentNbsFile(instr, instrument.get())) continue;
                int n = Byte.toUnsignedInt(note.getKey());
                n -= 33; // amazing conversion
                if (n < 0 || n > 24) {
                    warning("Note at tick %d out of range.", tick);
                    continue;
                }
                addNote(tick, n);
            }
        }
        return true;
    }

    private void scanForNoteblocks() {
        if (mc.interactionManager == null || mc.world == null || mc.player == null) return;
        scannedNoteblocks.clear();
        int min = (int) (-mc.interactionManager.getReachDistance()) - 1;
        int max = (int) mc.interactionManager.getReachDistance() + 1;
        // 5^3 kek
        for (int x = min; x < max; x++) {
            for (int y = min; y < max; y++) {
                for (int z = min; z < max; z++) {
                    BlockPos pos = mc.player.getBlockPos().add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() != Blocks.NOTE_BLOCK) continue;
                    float reach = mc.interactionManager.getReachDistance();
                    reach = reach * reach; //^2
                    if (pos.getSquaredDistance(mc.player.getPos()) > reach) continue;
                    if (!isValidScanSpot(pos) || !NotebotUtils.isValidInstrument(pos, instrument.get())) continue;
                    scannedNoteblocks.add(pos);
                }
            }

        }
    }

    private boolean setupBlocks() {
        song.forEach((v) -> {
            if (!uniqueNotes.contains(v.right)) {
                uniqueNotes.add(v.right);
            }
        });
        scanForNoteblocks();
        if (uniqueNotes.size() > possibleBlockPos.size() + scannedNoteblocks.size()) {
            error("Too many notes. %d is the maximum.", possibleBlockPos.size());
            return false;
        }
        currentNote = 0;
        offset = 0;
        stage = Stage.SetUp;
        return true;
    }

    private void onTickPreview() {
        if (song.get(currentIndex).left == currentNote) {
            mc.player.playSound(NotebotUtils.getInstrumentSound(instrument.get()), 2f, (float) Math.pow(2.0D, (song.get(currentIndex).right - 12) / 12.0D));
        }
    }

    private void setup() {
        if (ticks < tickDelay.get()) return;

        ticks = 0;

        if (currentNote >= uniqueNotes.size()) {
            stage = Stage.Playing;
            info("Loading done.");
            play();
            return;
        }

        int index = currentNote + offset;

        BlockPos pos;

        if (index < scannedNoteblocks.size()) {
            pos = scannedNoteblocks.get(index);

            if (mc.world.getBlockState(pos).getBlock() != Blocks.NOTE_BLOCK) {
                offset++;
            } else {
                blockPositions.put(uniqueNotes.get(currentNote), pos);
                stage = Stage.Tune;
            }

            return;
        }

        FindItemResult noteBlock = InvUtils.findInHotbar(Items.NOTE_BLOCK);

        if (!noteBlock.found()) {
            error("Not enough noteblocks");
            disable();
            return;
        }

        index -= scannedNoteblocks.size();

        try {
            pos = mc.player.getBlockPos().add(possibleBlockPos.get(index));
        } catch (IndexOutOfBoundsException e) {
            error("Not enough valid positions.");
            disable();
            return;
        }

        if (!isValidEmptySpot(pos) || !NotebotUtils.isValidInstrument(pos, instrument.get())) {
            offset++;
            return;
        }

        if (!BlockUtils.place(pos, noteBlock, true, 100, true)) {
            offset++;
        } else {
            blockPositions.put(uniqueNotes.get(currentNote), pos);
            stage = Stage.Tune;
        }
    }

    private void tune() {
        if (ticks < tickDelay.get()) return;
        ticks = 0;
        BlockPos pos = blockPositions.get(uniqueNotes.get(currentNote));
        if (pos == null) return;
        Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, this::tuneRotate);
    }

    private void tuneRotate() {
        BlockPos pos = blockPositions.get(uniqueNotes.get(currentNote));
        if (pos == null) return;
        if (!tuneBlock(pos, uniqueNotes.get(currentNote))) {
            disable();
        }
    }

    private boolean tuneBlock(BlockPos pos, int note) {
        if (mc.world == null || mc.player == null) return false;

        BlockState block = mc.world.getBlockState(pos);
        if (block.getBlock() != Blocks.NOTE_BLOCK) {
            offset++;
            stage = Stage.SetUp;
            return true;
        }

        if (block.get(NoteBlock.NOTE).equals(note)) {
            currentNote++;
            stage = Stage.SetUp;
            return true;
        }

        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(
            Utils.vec3d(pos), rayTraceCheck(pos), pos, true)));
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private void onTickPlay() {
        if (song.get(currentIndex).left == currentNote) {
            int note = song.get(currentIndex).right;
            BlockPos pos = blockPositions.get(note);
            if (polyphonic.get()) {
                Rotations.setCamRotation(Rotations.getYaw(pos), Rotations.getPitch(pos));
                playRotate();
            } else {
                Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, this::playRotate);
            }
        }
    }

    private void playRotate() {
        if (mc.interactionManager == null) return;
        try {
            int note = song.get(currentIndex).right;
            BlockPos pos = blockPositions.get(note);

            mc.interactionManager.attackBlock(pos, Direction.DOWN);
        } catch (NullPointerException ignored) {
        }
    }

    private boolean isValidEmptySpot(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isAir()) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;
        return mc.world.getBlockState(pos.down()).getBlock() != Blocks.NOTE_BLOCK;
    }

    private boolean isValidScanSpot(BlockPos pos) {
        if (mc.world.getBlockState(pos).getBlock() != Blocks.NOTE_BLOCK) return false;
        return mc.world.getBlockState(pos.up()).isAir();
    }

    // Stolen from crystal aura :)
    private Direction rayTraceCheck(BlockPos pos) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        for (Direction direction : Direction.values()) {
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d(pos.getX() + 0.5 + direction.getVector().getX() * 0.5,
                pos.getY() + 0.5 + direction.getVector().getY() * 0.5,
                pos.getZ() + 0.5 + direction.getVector().getZ() * 0.5), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                return direction;
            }
        }

        if (pos.getY() > eyesPos.y) return Direction.DOWN;

        return Direction.UP;
    }

    private enum Stage {
        None,
        SetUp,
        Tune,
        Playing,
        Preview
    }
}
