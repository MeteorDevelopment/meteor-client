/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Native file dialogs. The dialog is asynchronous and its callback runs on SDL's own thread, so the result is handed
 * back on the client thread.
 */
public final class FileDialogs {
    // SDL reads the filters until it is done with the dialog, so they are built once per extension list and kept.
    private static final Map<String, SDL_DialogFileFilter.Buffer> filters = new HashMap<>();

    // The upcall can not be freed while it is executing, so the previous dialog's callback is released when the next
    // dialog opens. Dialogs are modal, so there is only ever one in flight.
    private static SDL_DialogFileCallback callback;

    private FileDialogs() {}

    /**
     * @param extensions SDL filter extensions, e.g. {@code "txt"}, {@code "txt;json"} or {@code "*"}
     * @param onSelected called on the client thread with the picked file
     */
    public static void openFile(String extensions, Consumer<? super File> onSelected) {
        SDLDialog.SDL_ShowOpenFileDialog(callback(onSelected), NULL, NULL, get(extensions), (ByteBuffer) null, false);
    }

    /**
     * @param extensions SDL filter extensions, e.g. {@code "png"}, {@code "txt;json"} or {@code "*"}
     * @param onSelected called on the client thread with the picked file
     */
    public static void saveFile(String extensions, Consumer<? super File> onSelected) {
        SDLDialog.SDL_ShowSaveFileDialog(callback(onSelected), NULL, NULL, get(extensions), (ByteBuffer) null);
    }

    private static SDL_DialogFileCallback callback(Consumer<? super File> onSelected) {
        if (callback != null) callback.free();

        return callback = new SDL_DialogFileCallback() {
            @Override
            public void invoke(long userdata, long filelist, int filter) {
                if (filelist == NULL) {
                    MeteorClient.LOG.error("File dialog error: {}", SDLError.SDL_GetError());
                    return;
                }

                long path = MemoryUtil.memGetAddress(filelist);
                if (path == NULL) return; // Canceled

                File file = new File(MemoryUtil.memUTF8(path));
                mc.execute(() -> onSelected.accept(file));
            }
        };
    }

    private static SDL_DialogFileFilter.Buffer get(String extensions) {
        return filters.computeIfAbsent(extensions, extensions1 -> {
            String name = extensions1.equals("*") ? "All files" : "*." + extensions1.replace(";", ", *.");

            return SDL_DialogFileFilter.calloc(1)
                .name(MemoryUtil.memUTF8(name))
                .pattern(MemoryUtil.memUTF8(extensions1));
        });
    }
}
