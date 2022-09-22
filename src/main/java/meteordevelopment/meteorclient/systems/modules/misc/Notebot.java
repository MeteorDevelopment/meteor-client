/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.notebot.NBSDecoder;
import meteordevelopment.meteorclient.utils.notebot.NotebotUtils;
import meteordevelopment.meteorclient.utils.notebot.nbs.Layer;
import meteordevelopment.meteorclient.utils.notebot.nbs.Note;
import meteordevelopment.meteorclient.utils.notebot.nbs.Song;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.Instrument;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Notebot extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgNoteMap = settings.createGroup("Note Map", false);
    private final SettingGroup sgRender = settings.createGroup("Render", true);

    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("The delay when loading a song.")
        .defaultValue(1)
        .sliderRange(1, 20)
        .min(1)
        .build()
    );

    private final Setting<Integer> concurrentTuneBlocks = sgGeneral.add(new IntSetting.Builder()
        .name("concurrent-tune-blocks")
        .description("How many noteblocks can be tuned at the same time. On Paper it is recommended to set it to 1 to avoid bugs.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<NotebotUtils.NotebotMode> mode = sgGeneral.add(new EnumSetting.Builder<NotebotUtils.NotebotMode>()
        .name("mode")
        .description("Select mode of notebot")
        .defaultValue(NotebotUtils.NotebotMode.ExactInstruments)
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

    private final Setting<Boolean> swingArm = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-arm")
        .description("Should swing arm on hit")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> checkNoteblocksAgainDelay = sgGeneral.add(new IntSetting.Builder()
        .name("check-noteblocks-again-delay")
        .description("How much delay should be between end of tuning and checking again")
        .defaultValue(10)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Boolean> renderText = sgRender.add(new BoolSetting.Builder()
        .name("render-text")
        .description("Whether or not to render the text above noteblocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderBoxes = sgRender.add(new BoolSetting.Builder()
        .name("render-boxes")
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

    private final Setting<SettingColor> untunedSideColor = sgRender.add(new ColorSetting.Builder()
        .name("untuned-side-color")
        .description("The color of the sides of the untuned blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 10))
        .build()
    );

    private final Setting<SettingColor> untunedLineColor = sgRender.add(new ColorSetting.Builder()
        .name("untuned-line-color")
        .description("The color of the lines of the untuned blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    private final Setting<SettingColor> tunedSideColor = sgRender.add(new ColorSetting.Builder()
        .name("tuned-side-color")
        .description("The color of the sides of the tuned blocks being rendered.")
        .defaultValue(new SettingColor(0, 204, 0, 10))
        .build()
    );

    private final Setting<SettingColor> tunedLineColor = sgRender.add(new ColorSetting.Builder()
        .name("tuned-line-color")
        .description("The color of the lines of the tuned blocks being rendered.")
        .defaultValue(new SettingColor(0, 204, 0, 255))
        .build()
    );

    private final Setting<SettingColor> tuneHitSideColor = sgRender.add(new ColorSetting.Builder()
        .name("hit-side-color")
        .description("The color of the sides being rendered on noteblock tune hit.")
        .defaultValue(new SettingColor(255, 153, 0, 10))
        .build()
    );

    private final Setting<SettingColor> tuneHitLineColor = sgRender.add(new ColorSetting.Builder()
        .name("hit-line-color")
        .description("The color of the lines being rendered on noteblock tune hit.")
        .defaultValue(new SettingColor(255, 153, 0, 255))
        .build()
    );

    private final Setting<SettingColor> scannedNoteblockSideColor = sgRender.add(new ColorSetting.Builder()
        .name("scanned-noteblock-side-color")
        .description("The color of the sides of the scanned noteblocks being rendered.")
        .defaultValue(new SettingColor(255, 255, 0, 30))
        .build()
    );

    private final Setting<SettingColor> scannedNoteblockLineColor = sgRender.add(new ColorSetting.Builder()
        .name("scanned-noteblock-line-color")
        .description("The color of the lines of the scanned noteblocks being rendered.")
        .defaultValue(new SettingColor(255, 255, 0, 255))
        .build()
    );

    private final Setting<Double> noteTextScale = sgRender.add(new DoubleSetting.Builder()
        .name("note-text-scale")
        .description("The scale.")
        .defaultValue(1.5)
        .min(0)
        .build()
    );

    private final Setting<Boolean> showScannedNoteblocks = sgRender.add(new BoolSetting.Builder()
        .name("show-scanned-noteblocks")
        .description("Show scanned Noteblocks")
        .defaultValue(false)
        .build()
    );

    private CompletableFuture<Boolean> loadingSongFuture = null;

    private final Map<Integer, List<Note>> song = new HashMap<>(); // tick -> notes
    private final List<Note> uniqueNotes = new ArrayList<>();
    private final Map<Note, BlockPos> noteBlockPositions = new HashMap<>(); // Currently used noteblocks by the song
    private final Map<Note, List<BlockPos>> scannedNoteblocks = new HashMap<>(); // Found noteblocks
    private final List<BlockPos> clickedBlocks = new ArrayList<>();
    private Stage stage = Stage.None;
    private boolean isPlaying = false;
    private int currentTick = 0;
    private int ticks = 0;
    private boolean noSongsFound = true;
    private WLabel status;
    private int lastTick = -1;

    private boolean anyNoteblockTuned = false;
    private final Map<BlockPos, Integer> tuneHits = new HashMap<>(); // noteblock -> target hits number

    private int waitTicks = -1;


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
        if (loadingSongFuture != null) {
            loadingSongFuture.cancel(true);
            loadingSongFuture = null;
        }
        clickedBlocks.clear();
        tuneHits.clear();
        anyNoteblockTuned = false;
        currentTick = 0;
        lastTick = -1;
        isPlaying = false;
        stage = Stage.None;
        song.clear();
        noteBlockPositions.clear();
        uniqueNotes.clear();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderBoxes.get()) return;

        if (stage != Stage.SetUp && stage != Stage.Tune && stage != Stage.WaitingToCheckNoteblocks && !isPlaying) return;

        if (showScannedNoteblocks.get()) {
            for (List<BlockPos> blockPosList : scannedNoteblocks.values()) {
                for (BlockPos blockPos : blockPosList) {
                    double x1 = blockPos.getX();
                    double y1 = blockPos.getY();
                    double z1 = blockPos.getZ();
                    double x2 = blockPos.getX() + 1;
                    double y2 = blockPos.getY() + 1;
                    double z2 = blockPos.getZ() + 1;

                    event.renderer.box(x1, y1, z1, x2, y2, z2, scannedNoteblockSideColor.get(), scannedNoteblockLineColor.get(), shapeMode.get(), 0);
                }
            }
        } else {
            for (var entry : noteBlockPositions.entrySet()) {
                Note note = entry.getKey();
                BlockPos blockPos = entry.getValue();

                BlockState state = mc.world.getBlockState(blockPos);
                if (state.getBlock() != Blocks.NOTE_BLOCK) continue;

                int level = state.get(NoteBlock.NOTE);

                double x1 = blockPos.getX();
                double y1 = blockPos.getY();
                double z1 = blockPos.getZ();
                double x2 = blockPos.getX() + 1;
                double y2 = blockPos.getY() + 1;
                double z2 = blockPos.getZ() + 1;

                // Render boxes around noteblocks in use

                Color sideColor;
                Color lineColor;
                if (clickedBlocks.contains(blockPos)) {
                    sideColor = tuneHitSideColor.get();
                    lineColor = tuneHitLineColor.get();
                } else {
                    if (note.getNoteLevel() == level) {
                        sideColor = tunedSideColor.get();
                        lineColor = tunedLineColor.get();
                    } else {
                        sideColor = untunedSideColor.get();
                        lineColor = untunedLineColor.get();
                    }
                }

                event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, shapeMode.get(), 0);
            }
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!renderText.get()) return;

        if (stage != Stage.SetUp && stage != Stage.Tune && stage != Stage.WaitingToCheckNoteblocks && !isPlaying) return;

        Vec3 pos = new Vec3();

        for (BlockPos blockPos : noteBlockPositions.values()) {
            BlockState state = mc.world.getBlockState(blockPos);
            if (state.getBlock() != Blocks.NOTE_BLOCK) continue;

            double x = blockPos.getX() + 0.5;
            double y = blockPos.getY() + 1;
            double z = blockPos.getZ() + 0.5;

            pos.set(x, y, z);

            // Render level text logic

            String levelText = String.valueOf(state.get(NoteBlock.NOTE));
            String tuneHitsText = null;
            if (tuneHits.containsKey(blockPos)) {
                tuneHitsText = " -" + tuneHits.get(blockPos);
            }

            if (!NametagUtils.to2D(pos, noteTextScale.get(), true)) {
                continue;
            }

            TextRenderer text = TextRenderer.get();

            NametagUtils.begin(pos);
            text.beginBig();

            double xScreen = text.getWidth(levelText) / 2.0;
            if (tuneHitsText != null) {
                xScreen += text.getWidth(tuneHitsText) / 2.0;
            }

            double hX = text.render(levelText, -xScreen, 0, Color.GREEN);
            if (tuneHitsText != null) {
                text.render(tuneHitsText, hX, 0, Color.RED);
            }
            text.end();

            NametagUtils.end();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        ticks++;
        clickedBlocks.clear();

        if (stage == Stage.WaitingToCheckNoteblocks) {
            waitTicks--;
            if (waitTicks == 0) {
                waitTicks = -1;
                info("Checking noteblocks again...");

                setupTuneHitsMap();
                stage = Stage.Tune;
            }
        }
        else if (stage == Stage.SetUp) {
            setupBlocks();
            setupNoteblocksMap();
            setupTuneHitsMap();
            stage = Stage.Tune;
        }
        else if (stage == Stage.Tune) {
            tune();
        }
        else if (stage == Stage.Preview || stage == Stage.Playing) {
            if (!isPlaying) return;

            if (mc.player == null || currentTick > lastTick) {
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

    private void setupNoteblocksMap() {
        noteBlockPositions.clear();

        List<Note> uniqueNotesToUse = new ArrayList<>(uniqueNotes);
        Map<Instrument, List<BlockPos>> incorrectNoteBlocks = new HashMap<>();

        // Check if there are already tuned noteblocks
        for (var entry : scannedNoteblocks.entrySet()) {
            Note note = entry.getKey();
            List<BlockPos> noteblocks = new ArrayList<>(entry.getValue());

            if (uniqueNotesToUse.contains(note)) {
                noteBlockPositions.put(note, noteblocks.remove(0));
                uniqueNotesToUse.remove(note);
            }

            if (!noteblocks.isEmpty()) {
                // Add excess for mapping process note -> block pos

                if (!incorrectNoteBlocks.containsKey(note.getInstrument())) {
                    incorrectNoteBlocks.put(note.getInstrument(), new ArrayList<>());
                }

                incorrectNoteBlocks.get(note.getInstrument()).addAll(noteblocks);
            }
        }

        // Map note -> block pos
        for (var entry : incorrectNoteBlocks.entrySet()) {
            List<BlockPos> positions = entry.getValue();

            if (mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
                Instrument inst = entry.getKey();

                List<Note> foundNotes = uniqueNotesToUse.stream()
                    .filter(note -> note.getInstrument() == inst)
                    .collect(Collectors.toList());

                if (foundNotes.isEmpty()) continue;

                for (BlockPos pos : positions) {
                    if (foundNotes.isEmpty()) break;

                    Note note = foundNotes.remove(0);
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

    private void setupTuneHitsMap() {
        tuneHits.clear();

        for (var entry : noteBlockPositions.entrySet()) {
            int targetLevel = entry.getKey().getNoteLevel();
            BlockPos blockPos = entry.getValue();

            BlockState blockState = mc.world.getBlockState(blockPos);
            int currentLevel = blockState.get(NoteBlock.NOTE);

            if (targetLevel != currentLevel) {
                tuneHits.put(blockPos, calcNumberOfHits(currentLevel, targetLevel));
            }
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();

        // Align Center
        WButton alignCenter = table.add(theme.button("Align Center")).expandCellX().center().widget();
        alignCenter.action = () -> {
            if (mc.player == null) return;
            mc.player.setPosition(Vec3d.ofBottomCenter(mc.player.getBlockPos()));
        };

        table.row();

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
        if (isPlaying) return String.format("Playing song. %d/%d", currentTick, lastTick);
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
        if (stage == Stage.SetUp || stage == Stage.Tune || stage == Stage.WaitingToCheckNoteblocks) {
            resetVariables();
        } else {
            isPlaying = false;
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
        if (!loadFileToMap(file, () -> stage = Stage.SetUp)) {
            if (autoPlay.get()) {
                playRandomSong();
            }
        }
    }

    public void previewSong(File file) {
        if (!isActive()) toggle();
        loadFileToMap(file, () -> {
            stage = Stage.Preview;
            play();
        });
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

    private boolean loadFileToMap(File file, Runnable callback) {
        if (!file.exists() || !file.isFile()) {
            error("File not found");
            return false;
        }
        String extension = FilenameUtils.getExtension(file.getName());

        resetVariables();
        info("Loading song \"%s\".", getFileLabel(file.toPath()));

        if (extension.equals("txt")) loadingSongFuture = CompletableFuture.supplyAsync(() -> loadTextFile(file));
        else if (extension.equals("nbs")) loadingSongFuture = CompletableFuture.supplyAsync(() -> loadNbsFile(file));
        loadingSongFuture.completeOnTimeout(false, 10, TimeUnit.SECONDS);

        stage = Stage.LoadingSong;
        long time1 = System.currentTimeMillis();
        loadingSongFuture.thenAccept(success -> {
            if (success) {
                long time2 = System.currentTimeMillis();
                long diff = time2 - time1;

                lastTick = Collections.max(song.keySet());
                info("Song '"+getFileLabel(file.toPath())+"' has been loaded to the memory! Took "+diff+"ms");
                callback.run();
            } else {
                error("Could not load song '"+getFileLabel(file.toPath())+"'");
                if (autoPlay.get()) {
                    playRandomSong();
                }
            }
        });
        return true;
    }

    private boolean loadTextFile(File file) {
        List<String> data;
        try {
            data = Files.readAllLines(file.toPath());
        } catch (IOException e) {
            error("Error while reading \"%s\"", file.getName());
            return false;
        }
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
        int min = (int) (-mc.interactionManager.getReachDistance()) - 2;
        int max = (int) mc.interactionManager.getReachDistance() + 2;

        // Scan for noteblocks horizontally
        // 6^3 kek
        for (int y = min; y < max; y++) {
            for (int x = min; x < max; x++) {
                for (int z = min; z < max; z++) {
                    BlockPos pos = mc.player.getBlockPos().add(x, y + 1, z);

                    BlockState blockState = mc.world.getBlockState(pos);
                    if (blockState.getBlock() != Blocks.NOTE_BLOCK) continue;

                    // Copied from ServerPlayNetworkHandler#onPlayerInteractBlock
                    Vec3d vec3d2 = Vec3d.ofCenter(pos);
                    double sqrt = mc.player.getEyePos().squaredDistanceTo(vec3d2);
                    if (sqrt > ServerPlayNetworkHandler.MAX_BREAK_SQUARED_DISTANCE) continue;

                    if (!isValidScanSpot(pos)) continue;

                    Note note = NotebotUtils.getNoteFromNoteBlock(blockState, mode.get());
                    if (!scannedNoteblocks.containsKey(note)) {
                        scannedNoteblocks.put(note, new ArrayList<>());
                    }

                    scannedNoteblocks.get(note).add(pos);
                }
            }

        }
    }

    private void setupBlocks() {
        song.values().forEach(notes -> {
            notes.forEach(note -> {
                if (!uniqueNotes.contains(note)) {
                    uniqueNotes.add(note);
                }
            });
        });
        scanForNoteblocks();
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

    private void tune() {
        if (tuneHits.isEmpty()) {
            if (anyNoteblockTuned) {
                anyNoteblockTuned = false;
                waitTicks = checkNoteblocksAgainDelay.get();
                stage = Stage.WaitingToCheckNoteblocks;

                info("Delaying check for noteblocks");
            } else {
                stage = Stage.Playing;
                info("Loading done.");
                play();
            }
            return;
        }

        if (ticks < tickDelay.get()) {
            return;
        }

        tuneBlocks();
        ticks = 0;
    }

    private void tuneBlocks() {
        if (mc.world == null || mc.player == null) {
            disable();
        }

        if (swingArm.get()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        int iterations = 0;
        var iterator = tuneHits.entrySet().iterator();

        // Concurrent tuning :o
        while (iterator.hasNext()){
            var entry = iterator.next();
            BlockPos pos = entry.getKey();
            int hitsNumber = entry.getValue();

            if (autoRotate.get()) {
                Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, () -> tuneNoteblockWithPackets(pos));
            } else {
                this.tuneNoteblockWithPackets(pos);
            }

            clickedBlocks.add(pos);

            hitsNumber--;
            entry.setValue(hitsNumber);

            if (hitsNumber == 0) {
                iterator.remove();
            }

            iterations++;

            if (iterations == concurrentTuneBlocks.get()) return;
        }
    }

    private void tuneNoteblockWithPackets(BlockPos pos) {
        // We don't need to raycast here. Server handles this packet fine
        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN, pos, false), 0));

        anyNoteblockTuned = true;
    }

    private static int calcNumberOfHits(int from, int to) {
        if (from > to) {
            return (25 - from) + to;
        } else {
            return to - from;
        }
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

            if (swingArm.get()) {
                mc.player.swingHand(Hand.MAIN_HAND);
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

    private @Nullable Instrument getMappedInstrument(Instrument inst) {
        if (mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
            return ((NotebotUtils.NullableInstrument) sgNoteMap.getByIndex(inst.ordinal()).get()).toMinecraftInstrument();
        } else {
            return inst;
        }
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
        LoadingSong,
        SetUp,
        Tune,
        WaitingToCheckNoteblocks,
        Playing,
        Preview
    }
}
