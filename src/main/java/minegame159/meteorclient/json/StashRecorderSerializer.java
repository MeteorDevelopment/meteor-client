package minegame159.meteorclient.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import minegame159.meteorclient.modules.misc.StashRecorder;

import java.lang.reflect.Type;

public class StashRecorderSerializer implements JsonSerializer<StashRecorder> {
    @Override
    public JsonElement serialize(StashRecorder src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src.chunks);
    }
}
