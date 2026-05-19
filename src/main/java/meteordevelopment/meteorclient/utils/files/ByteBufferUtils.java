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
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.IntFunction;

@NullMarked
public final class ByteBufferUtils {
    private ByteBufferUtils() {
    }

    public static ByteBuffer readFully(Path path, IntFunction<ByteBuffer> allocator) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            long size = channel.size();
            if (size > Integer.MAX_VALUE) {
                throw new IOException("File too large to read into ByteBuffer: " + path);
            }
            return readFully(channel, (int) size, allocator);
        }
    }

    public static ByteBuffer readFully(ReadableByteChannel channel, IntFunction<ByteBuffer> allocator) throws IOException {
        // special-case if content size is known
        if (channel instanceof SeekableByteChannel seekableChannel) {
            long size = seekableChannel.size();
            if (size > Integer.MAX_VALUE) {
                throw new IOException("Channel content too large to read into ByteBuffer");
            }
            return readFully(seekableChannel, (int) size, allocator);
        }

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

    private static ByteBuffer readFully(SeekableByteChannel channel, int size, IntFunction<ByteBuffer> allocator) throws IOException {
        ByteBuffer buffer = requireCapacity(allocator.apply(size), size);
        while (buffer.hasRemaining()) {
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1) break; // EOF
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
