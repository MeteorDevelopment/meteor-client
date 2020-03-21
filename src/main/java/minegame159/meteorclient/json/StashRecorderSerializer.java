package minegame159.meteorclient.json;

import com.google.gson.*;
import minegame159.meteorclient.modules.misc.StashRecorder;
import net.minecraft.util.math.ChunkPos;

import java.lang.reflect.Type;

public class StashRecorderSerializer implements JsonSerializer<StashRecorder> {
    @Override
    public JsonElement serialize(StashRecorder src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray a = new JsonArray();

        for (ChunkPos chunkPos : src.chunkStorageCounts.keySet()) {
            JsonObject o = new JsonObject();
            o.addProperty("chunkX", chunkPos.x);
            o.addProperty("chunkZ", chunkPos.z);
            o.addProperty("count", src.chunkStorageCounts.get(chunkPos));
            a.add(o);
        }

        return a;
    }
}
