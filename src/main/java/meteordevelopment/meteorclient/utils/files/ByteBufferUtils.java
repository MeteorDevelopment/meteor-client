/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.files;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.IntFunction;

import static java.nio.file.Files.*;

@NullMarked
public final class ByteBufferUtils {
    private ByteBufferUtils() {}

    public static ByteBuffer readFully(Path path, IntFunction<ByteBuffer> allocator) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            long size = size(path);
            if (size > Integer.MAX_VALUE) {
                throw new IOException("File too large to read into ByteBuffer: " + path);
            }
            ByteBuffer buffer = allocator.apply((int) size);
            while (buffer.hasRemaining()) {
                int bytesRead = channel.read(buffer);
                if (bytesRead == -1) break; // EOF
            }
            buffer.flip();
            return buffer;
        }
    }

    public static ByteBuffer readFully(ReadableByteChannel channel, IntFunction<ByteBuffer> allocator) throws IOException {
        ByteBuffer buffer = requireCapacity(allocator.apply(8192), 8192);

        while (true) {
            int bytesRead = channel.read(buffer);

            if (bytesRead == -1) break;

            if (bytesRead == 0) {
                // Avoid busy-spin on non-blocking channels.
                // If buffer is full, grow; otherwise caller should probably be using blocking I/O.
                if (!buffer.hasRemaining()) {
                    buffer = grow(buffer, allocator);
                    continue;
                }
                // In a "readFully" API, returning early is usually better than spinning forever.
                // Alternative: Thread.onSpinWait(); continue;
                break;
            }

            if (!buffer.hasRemaining()) {
                buffer = grow(buffer, allocator);
            }
        }

        buffer.flip();
        return buffer;
    }

    private static ByteBuffer grow(ByteBuffer buffer, IntFunction<ByteBuffer> allocator) {
        int oldCap = buffer.capacity();
        int newCap = oldCap << 1;
        if (newCap <= 0) throw new OutOfMemoryError("Buffer too large (overflow): " + oldCap);

        ByteBuffer newBuffer = requireCapacity(allocator.apply(newCap), newCap);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }

    private static ByteBuffer requireCapacity(ByteBuffer buf, int minCap) {
        if (buf.capacity() < minCap) {
            throw new IllegalArgumentException("Allocator returned capacity " + buf.capacity() + " < " + minCap);
        }
        return buf;
    }

}
