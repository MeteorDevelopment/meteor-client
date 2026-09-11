package meteordevelopment.meteorclient.utils.network;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.chat.Component;

/**
 * Wraps the component codec so text decoded from a remote server is tagged.
 *
 * Only the top-level decode passes through here (the recursive codec refers to itself
 * internally), so the whole decoded tree is marked at once.
 */
public record ServerComponentCodec(Codec<Component> wrapped) implements Codec<Component> {
    @Override
    public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
        DataResult<Pair<Component, T>> result = wrapped.decode(ops, input);
        if (!KeyResolutionProtection.shouldTag()) return result;

        return result.map(pair -> {
            KeyResolutionProtection.markTree(pair.getFirst());
            return pair;
        });
    }

    @Override
    public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T prefix) {
        return wrapped.encode(input, ops, prefix);
    }
}
