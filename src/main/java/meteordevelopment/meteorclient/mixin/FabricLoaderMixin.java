package meteordevelopment.meteorclient.mixin;

import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(value = FabricLoaderImpl.class, remap = false)
public class FabricLoaderMixin {

    // 【修改点1】这里改为“白名单”，即你想要给服务器看的 Mod
    @Unique
    private static final Set<String> WHITELIST_MODS = new HashSet<>(Arrays.asList(
            // --- 必须保留的基础组件 (否则无法进服) ---
            "minecraft",
            "fabricloader",
            "java",
            
            // --- 你想展示给服务器的“良民”证明 ---
            "sodium",           // 钠 (优化Mod)
            "fabric-api"        // Fabric API (建议保留，否则看起来很假，因为钠依赖它)
    ));

    @Inject(method = "getAllMods", at = @At("RETURN"), cancellable = true)
    private void filterModList(CallbackInfoReturnable<Collection<ModContainer>> cir) {
        // 1. 检查是否为网络线程 (保持不变)
        boolean isNetworking = false;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("net.fabricmc.fabric.impl.networking") ||
                    className.contains("net.fabricmc.fabric.mixin.networking")) {
                isNetworking = true;
                break;
            }
        }

        // 2. 如果不是网络请求，直接放行 (本地游戏必须加载所有Mod)
        if (!isNetworking)
            return;

        // 3. 开始过滤 (逻辑反转)
        Collection<ModContainer> originalList = cir.getReturnValue();
        List<ModContainer> filteredList = new ArrayList<>();
        
        // 用于调试：看看实际隐藏了多少
        int hiddenCount = 0;

        for (ModContainer mod : originalList) {
            String modId = mod.getMetadata().getId();

            // 【修改点2】判断逻辑反转：
            // 如果 modId 在白名单里 -> 放行
            // 如果 不在白名单里 -> 隐藏
            if (WHITELIST_MODS.contains(modId)) {
                filteredList.add(mod);
            } else {
                hiddenCount++;
                // 你可以在这里打印被隐藏的 modId 方便调试，但为了刷屏少一点，建议只统计数量
            }
        }

        // 4. 打印简略日志
        if (hiddenCount > 0) {
            System.out.println("[Anti-Leak] 白名单模式生效。已向服务器隐藏 " + hiddenCount + " 个 Mod，仅展示: " + WHITELIST_MODS);
        }

        // 5. 替换返回值
        cir.setReturnValue(filteredList);
    }
}