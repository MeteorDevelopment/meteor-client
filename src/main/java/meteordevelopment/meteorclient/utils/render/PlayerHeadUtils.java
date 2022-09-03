package meteordevelopment.meteorclient.utils.render;

import com.google.gson.Gson;
import com.mojang.util.UUIDTypeAdapter;
import meteordevelopment.meteorclient.systems.accounts.TexturesJson;
import meteordevelopment.meteorclient.systems.accounts.UuidToProfileResponse;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.network.Http;

import java.util.Base64;
import java.util.UUID;

public class PlayerHeadUtils {
    public static PlayerHeadTexture STEVE_HEAD;

    @PostInit
    public static void init() {
        STEVE_HEAD = new PlayerHeadTexture();
    }

    public static PlayerHeadTexture fetchHead(UUID id) {
        if (id == null) return null;

        String url = getSkinUrl(id);
        return url != null ? new PlayerHeadTexture(url) : null;
    }

    public static String getSkinUrl(UUID id) {
        UuidToProfileResponse res2 = Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + UUIDTypeAdapter.fromUUID(id)).sendJson(UuidToProfileResponse.class);
        if (res2 == null) return null;

        String base64Textures = res2.getPropertyValue("textures");
        if (base64Textures == null) return null;

        TexturesJson textures = new Gson().fromJson(new String(Base64.getDecoder().decode(base64Textures)), TexturesJson.class);
        if (textures.textures.SKIN == null) return null;

        return textures.textures.SKIN.url;
    }
}
