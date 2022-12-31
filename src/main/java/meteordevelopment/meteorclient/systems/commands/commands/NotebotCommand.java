/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.NotebotSongArgumentType;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.Notebot;
import meteordevelopment.meteorclient.utils.notebot.song.Note;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.enums.Instrument;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class NotebotCommand extends Command {
    private final static SimpleCommandExceptionType INVALID_SONG = new SimpleCommandExceptionType(Text.literal("Invalid song."));

    int ticks = -1;
    private final Map<Integer, List<Note>> song = new HashMap<>(); // tick -> notes

    public NotebotCommand() {
        super("notebot", "Allows you load notebot files");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("help").executes(ctx -> {
            Util.getOperatingSystem().open("https://github.com/MeteorDevelopment/meteor-client/wiki/Notebot-Guide");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("status").executes(ctx -> {
            Notebot notebot = Modules.get().get(Notebot.class);
            info(notebot.getStatus());
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("pause").executes(ctx -> {
            Notebot notebot = Modules.get().get(Notebot.class);
            notebot.pause();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("resume").executes(ctx -> {
            Notebot notebot = Modules.get().get(Notebot.class);
            notebot.pause();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("stop").executes(ctx -> {
            Notebot notebot = Modules.get().get(Notebot.class);
            notebot.stop();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("randomsong").executes(ctx -> {
            Notebot notebot = Modules.get().get(Notebot.class);
            notebot.playRandomSong();
            return SINGLE_SUCCESS;
        }));

        builder.then(
            literal("play").then(
                argument("song", NotebotSongArgumentType.create()).executes(ctx -> {
                    Notebot notebot = Modules.get().get(Notebot.class);
                    Path songPath = ctx.getArgument("song", Path.class);
                    if (songPath == null || !songPath.toFile().exists()) {
                        throw INVALID_SONG.create();
                    }
                    notebot.loadSong(songPath.toFile());
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(
            literal("preview").then(
                argument("song", NotebotSongArgumentType.create()).executes(ctx -> {
                    Notebot notebot = Modules.get().get(Notebot.class);
                    Path songPath = ctx.getArgument("song", Path.class);
                    if (songPath == null || !songPath.toFile().exists()) {
                        throw INVALID_SONG.create();
                    }
                    notebot.previewSong(songPath.toFile());
                    return SINGLE_SUCCESS;
        })));

        builder.then(literal("record").then(literal("start").executes(ctx -> {
            ticks = -1;
            song.clear();
            MeteorClient.EVENT_BUS.subscribe(this);
            info("Recording started");
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("record").then(literal("cancel").executes(ctx -> {
            MeteorClient.EVENT_BUS.unsubscribe(this);
            info("Recording cancelled");
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("record").then(literal("save").then(argument("name", StringArgumentType.greedyString()).executes(ctx -> {
            String name = ctx.getArgument("name", String.class);
            if (name == null || name.equals("")) {
                throw INVALID_SONG.create();
            }
            Path path = MeteorClient.FOLDER.toPath().resolve(String.format("notebot/%s.txt", name));
            saveRecording(path);
            return SINGLE_SUCCESS;
        }))));
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (ticks == -1) return;
        ticks++;
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlaySoundS2CPacket sound && sound.getSound().value().getId().getPath().contains("note_block")) {
            if (ticks == -1) ticks = 0;
            List<Note> notes = song.computeIfAbsent(ticks, tick -> new ArrayList<>());
            var note = getNote(sound);
            if (note != null) {
                notes.add(note);
            }
        }
    }

    private void saveRecording(Path path) {
        if (song.size() < 1) {
            MeteorClient.EVENT_BUS.unsubscribe(this);
            return;
        }
        try {
            MeteorClient.EVENT_BUS.unsubscribe(this);

            FileWriter file = new FileWriter(path.toFile());
            for (var entry : song.entrySet()) {
                int tick = entry.getKey();
                List<Note> notes = entry.getValue();

                for (var note : notes) {
                    Instrument instrument = note.getInstrument();
                    int noteLevel = note.getNoteLevel();

                    file.write(String.format("%d:%d:%d\n", tick, noteLevel, instrument.ordinal()));
                }
            }

            file.close();
            info("Song saved.");
        } catch (IOException e) {
            info("Couldn't create the file.");
            MeteorClient.EVENT_BUS.unsubscribe(this);
        }

    }

    private Note getNote(PlaySoundS2CPacket soundPacket) {
        float pitch = soundPacket.getPitch();

        // Bruteforce note level
        int noteLevel = -1;
        for (int n = 0; n < 25; n++) {
            if ((float) Math.pow(2.0D, (n - 12) / 12.0D) - 0.01 < pitch &&
                (float) Math.pow(2.0D, (n - 12) / 12.0D) + 0.01 > pitch) {
                noteLevel = n;
                break;
            }
        }

        if (noteLevel == -1) {
            error("Error while bruteforcing a note level! Sound: " + soundPacket.getSound().value() + " Pitch: " + pitch);
            return null;
        }

        Instrument instrument = getInstrumentFromSound(soundPacket.getSound().value());
        if (instrument == null) {
            error("Can't find the instrument from sound! Sound: " + soundPacket.getSound().value());
            return null;
        }

        return new Note(instrument, noteLevel);
    }

    private Instrument getInstrumentFromSound(SoundEvent sound) {
        String path = sound.getId().getPath();
        if (path.contains("harp"))
            return Instrument.HARP;
        else if (path.contains("basedrum"))
            return Instrument.BASEDRUM;
        else if (path.contains("snare"))
            return Instrument.SNARE;
        else if (path.contains("hat"))
            return Instrument.HAT;
        else if (path.contains("bass"))
            return Instrument.BASS;
        else if (path.contains("flute"))
            return Instrument.FLUTE;
        else if (path.contains("bell"))
            return Instrument.BELL;
        else if (path.contains("guitar"))
            return Instrument.GUITAR;
        else if (path.contains("chime"))
            return Instrument.CHIME;
        else if (path.contains("xylophone"))
            return Instrument.XYLOPHONE;
        else if (path.contains("iron_xylophone"))
            return Instrument.IRON_XYLOPHONE;
        else if (path.contains("cow_bell"))
            return Instrument.COW_BELL;
        else if (path.contains("didgeridoo"))
            return Instrument.DIDGERIDOO;
        else if (path.contains("bit"))
            return Instrument.BIT;
        else if (path.contains("banjo"))
            return Instrument.BANJO;
        else if (path.contains("pling"))
            return Instrument.PLING;
        return null;
    }
}
