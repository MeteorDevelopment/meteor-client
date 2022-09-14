/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
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
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.Instrument;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


public class Notebot extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgNoteMap = settings.createGroup("Note Map", false);
    private final SettingGroup sgRender = settings.createGroup("Render", false);

    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("The delay when loading a song.")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<NotebotUtils.NotebotMode> mode = sgGeneral.add(new EnumSetting.Builder<NotebotUtils.NotebotMode>()
        .name("mode")
        .description("Select mode of notebot")
        .defaultValue(NotebotUtils.NotebotMode.AnyInstrument)
        .build()
    );

    private final Setting<Boolean> polyphonic = sgGeneral.add(new BoolSetting.Builder()
        .name("polyphonic")
        .description("Whether or not to allow multiple notes to be played at the same time")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoRotate = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-rotate")
        .description("Should client look at note block when it wants to hit it")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoPlay = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-play")
        .description("Auto plays random songs")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> roundOutOfRange = sgGeneral.add(new BoolSetting.Builder()
        .name("round-out-of-range")
        .description("Rounds out of range notes")
        .defaultValue(false)
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



    private final TreeMap<Integer, List<Note>> song = new TreeMap<>(Comparator.naturalOrder()); // tick -> notes
    private final List<Note> uniqueNotes = new ArrayList<>();
    private final Map<Note, BlockPos> noteBlockPositions = new HashMap<>();
    private final Map<Instrument, List<BlockPos>> scannedNoteblocks = new HashMap<>();
    private Stage stage = Stage.None;
    private boolean isPlaying = false;
    private int currentNote = 0;
    private int currentTick = 0;
    private int ticks = 0;
    private boolean noSongsFound = true;
    private WLabel status;


    public Notebot() {
        super(Categories.Misc, "notebot", "Plays noteblock nicely");

        for (Instrument inst : Instrument.values()) {
            sgNoteMap.add(new EnumSetting.Builder<NotebotUtils.NullableInstrument>()
                .name(beautifyText(inst.name()))
                .defaultValue(NotebotUtils.NullableInstrument.fromMinecraftInstrument(inst))
                .visible(() -> mode.get() == NotebotUtils.NotebotMode.ExactInstruments)
                .build()
            );
        }
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
        currentTick = 0;
        isPlaying = false;
        stage = Stage.None;
        song.clear();
        noteBlockPositions.clear();
        uniqueNotes.clear();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;

        if (stage != Stage.SetUp && stage != Stage.Tune && !isPlaying) return;

        noteBlockPositions.values().forEach((blockPos) -> {
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

            if (mc.player == null || currentTick > song.lastKey()) {
                stop();
                return;
            }

            if (song.containsKey(currentTick)) {
                if (stage == Stage.Preview) onTickPreview();
                else onTickPlay();
            }

            currentTick++;

            if (status != null) status.set(getStatus());
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();

        // Label
        status = table.add(theme.label(getStatus())).expandCellX().widget();

        // Random Song
        WButton randomSong = table.add(theme.button("Random Song")).right().widget();
        randomSong.action = this::playRandomSong;

        // Pause
        WButton pause = table.add(theme.button(isPlaying ? "Pause" : "Resume")).right().widget();
        pause.action = () -> {
            pause();
            pause.set(isPlaying ? "Pause" : "Resume");
            status.set(getStatus());
        };

        // Stop
        WButton stop = table.add(theme.button("Stop")).right().widget();
        stop.action = this::forceStop;

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
        if (isPlaying) return String.format("Playing song. %d/%d", currentTick, song.lastKey());
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

    public void forceStop() {
        info("Stopping.");
        if (stage == Stage.SetUp || stage == Stage.Tune) {
            resetVariables();
        } else {
            isPlaying = false;
            currentNote = 0;
            currentTick = 0;
        }
        if (status != null) status.set(getStatus());
    }

    public void stop() {
        if (autoPlay.get()) {
            playRandomSong();
        } else {
            forceStop();
        }
    }

    public void playRandomSong() {
        File[] files = MeteorClient.FOLDER.toPath().resolve("notebot").toFile().listFiles();

        File randomSong = files[ThreadLocalRandom.current().nextInt(files.length)];
        if (isValidFile(randomSong.toPath())) {
            loadSong(randomSong);
        } else {
            playRandomSong();
        }
    }

    public void disable() {
        resetVariables();
        info("Stopping.");
        if (!isActive()) toggle();
    }

    public void loadSong(File file) {
        if (!isActive()) toggle();
        if (!loadFileToMap(file)) {
            if (autoPlay.get()) {
                playRandomSong();
            }
            return;
        }
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

    private void addNote(int tick, Note value) {
        if (!song.containsKey(tick)) {
            song.put(tick, new ArrayList<>());
        }

        if (polyphonic.get()) {
            song.get(tick).add(value);
        } else if (song.get(tick).isEmpty()) {
            song.get(tick).add(value);
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
            int type = 0;
            try {
                key = Integer.parseInt(parts[0]);
                val = Integer.parseInt(parts[1]);
                if (parts.length > 2) {
                    type = Integer.parseInt(parts[2]);
                }
            } catch (NumberFormatException e) {
                warning("Invalid character at line %d", i);
                continue;
            }

            if (val < 0 || val > 24) {
                if (roundOutOfRange.get()) {
                    val = val < 0 ? 0 : 24;
                } else {
                    warning("Note at tick %d out of range.", key);
                    continue;
                }
            }

            if (mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
                Instrument newInstrument;
                try {
                    newInstrument = getMappedInstrument(Instrument.values()[type]);
                } catch (IndexOutOfBoundsException exception) {
                    continue;
                }
                if (newInstrument != null) {
                    int nbsInstrument = NotebotUtils.toNBSInstrument(newInstrument);
                    addNote(key, new Note(nbsInstrument, val + NotebotUtils.NOTE_OFFSET));
                }
            } else {
                addNote(key, new Note(-1, val + NotebotUtils.NOTE_OFFSET));
            }
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
            for (var entry : layer.getHashMap().entrySet()) {
                int tick = entry.getKey();
                Note note = entry.getValue();
                tick *= nbsSong.getDelay();
                if (note == null) continue;
                int instr = note.getRawInstrument();
                if (NotebotUtils.fromNBSInstrument(instr) == null) continue;
                int n = note.getNoteLevel();
                if (n < 0 || n > 24) {
                    if (roundOutOfRange.get()) {
                        note.setNoteLevel(n < 0 ? 0 : 24);
                    } else {
                        warning("Note at tick %d out of range.", tick);
                        continue;
                    }
                }
                if (mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
                    Instrument newInstrument = getMappedInstrument(note.getInstrument());
                    if (newInstrument != null) {
                        note.setInstrument(newInstrument);
                        addNote(tick, note);
                    }
                } else {
                    note.setRawInstrument(-1);
                    addNote(tick, note);
                }
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
                    BlockPos pos = mc.player.getBlockPos().add(x, y+1, z);

                    BlockState blockState = mc.world.getBlockState(pos);
                    if (blockState.getBlock() != Blocks.NOTE_BLOCK) continue;

                    float reach = mc.interactionManager.getReachDistance() + 0.7f; // We need to expand the player range to reach the corners of blocks
                    reach = reach * reach; //^2
                    if (pos.getSquaredDistance(mc.player.getEyePos()) > reach) continue;
                    if (!isValidScanSpot(pos)) continue;

                    Instrument inst = blockState.get(NoteBlock.INSTRUMENT);
                    if (!scannedNoteblocks.containsKey(inst)) {
                        scannedNoteblocks.put(inst, new ArrayList<>());
                    }

                    scannedNoteblocks.get(inst).add(pos);
                }
            }

        }
    }

    private boolean setupBlocks() {
        song.values().forEach(notes -> {
            notes.forEach(note -> {
                if (!uniqueNotes.contains(note)) {
                    uniqueNotes.add(note);
                }
            });
        });
        scanForNoteblocks();

        int scannedNoteBlocksSize = countScannedNoteBlocks();
        if (uniqueNotes.size() > scannedNoteBlocksSize) {
            error("Too many notes. %d is the maximum.", scannedNoteBlocksSize);
            return false;
        }
        currentNote = 0;
        stage = Stage.SetUp;
        return true;
    }

    private void onTickPreview() {
        for (Note note : song.get(currentTick)) {
            if (mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
                mc.player.playSound(note.getInstrument().getSound(), 2f, (float) Math.pow(2.0D, (note.getNoteLevel() - 12) / 12.0D));
            } else {
                mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP, 2f, (float) Math.pow(2.0D, (note.getNoteLevel() - 12) / 12.0D));
            }
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

        if (currentNote == 0) {
            List<Note> uniqueNotesToUse = new ArrayList<>(uniqueNotes);

            for (var entry : scannedNoteblocks.entrySet()) {
                List<BlockPos> positions = entry.getValue();

                if (mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
                    Instrument inst = entry.getKey();

                    List<Note> notes = uniqueNotesToUse.stream()
                        .filter(note -> note.getInstrument() == inst)
                        .collect(Collectors.toList());

                    if (notes.isEmpty()) continue;

                    for (BlockPos pos : positions) {
                        if (notes.isEmpty()) break;

                        Note note = notes.remove(0);
                        noteBlockPositions.put(note, pos);

                        uniqueNotesToUse.remove(note);
                    }
                } else {
                    for (BlockPos pos : positions) {
                        if (uniqueNotesToUse.isEmpty()) break;

                        Note note = uniqueNotesToUse.remove(0);
                        noteBlockPositions.put(note, pos);
                    }
                }
            }

            if (!uniqueNotesToUse.isEmpty()) {
                for (Note note : uniqueNotesToUse) {
                    warning("Missing note: "+note.getInstrument()+", "+note.getNoteLevel());
                }
                warning(uniqueNotesToUse.size()+" missing notes!");
            }
        }

        stage = Stage.Tune;
    }

    private void tune() {
        if (ticks < tickDelay.get()) {
            return;
        }
        ticks = 0;
        BlockPos pos = noteBlockPositions.get(uniqueNotes.get(currentNote));
        if (pos == null) {
            currentNote++;
            stage = Stage.SetUp;
            return;
        }
        if (autoRotate.get()) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, this::tuneRotate);
        } else {
            this.tuneRotate();
        }
    }

    private void tuneRotate() {
        BlockPos pos = noteBlockPositions.get(uniqueNotes.get(currentNote));
        if (pos == null) {
            return;
        }
        if (!tuneBlock(pos, uniqueNotes.get(currentNote))) {
            disable();
        }
    }

    private boolean tuneBlock(BlockPos pos, Note targetNote) {
        if (mc.world == null || mc.player == null) {
            return false;
        }

        BlockState block = mc.world.getBlockState(pos);
        if (block.getBlock() != Blocks.NOTE_BLOCK) {
            stage = Stage.SetUp;
            return true;
        }

        Note note = NotebotUtils.getNoteFromNoteBlock(block, mode.get());
        if (note.equals(targetNote)) {
            currentNote++;
            stage = Stage.SetUp;
            return true;
        }

        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(pos), rayTraceCheck(pos), pos, true), 0));
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private void onTickPlay() {
        List<Note> notes = song.get(currentTick);
        if (!notes.isEmpty()) {
            BlockPos firstPos = noteBlockPositions.get(notes.get(0));
            if (firstPos != null) {
                if (autoRotate.get()) {
                    Rotations.rotate(Rotations.getYaw(firstPos), Rotations.getPitch(firstPos));
                }
            }

            for (Note note : notes) {
                BlockPos pos = noteBlockPositions.get(note);
                if (pos == null) {
                    return;
                }

                if (polyphonic.get()) {
                    playRotate(pos);
                } else {
                    this.playRotate(pos);
                }
            }
        }
    }

    private void playRotate(BlockPos pos) {
        if (mc.interactionManager == null) return;
        try {
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

    private @Nullable Instrument getMappedInstrument(Instrument inst) {
        if (mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
            return ((NotebotUtils.NullableInstrument) sgNoteMap.getByIndex(inst.ordinal()).get()).toMinecraftInstrument();
        } else {
            return inst;
        }
    }

    private int countScannedNoteBlocks() {
        int i = 0;
        for (List<BlockPos> blockPosList : scannedNoteblocks.values()) {
            i+=blockPosList.size();
        }

        return i;
    }

    private String beautifyText(String text) {
        text = text.toLowerCase(Locale.ROOT);

        String[] arr = text.split("_");
        StringBuilder sb = new StringBuilder();

        for (String s : arr) {
            sb.append(Character.toUpperCase(s.charAt(0)))
                .append(s.substring(1));
        }
        return sb.toString().trim();
    }

    private enum Stage {
        None,
        SetUp,
        Tune,
        Playing,
        Preview
    }
}
