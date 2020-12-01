package minegame159.meteorclient.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import minegame159.meteorclient.Config;
import minegame159.meteorclient.commands.CommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CommandSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestor.class)
public abstract class MixinCommandSuggestor {

    @Shadow private ParseResults<CommandSource> parse;

    @Shadow @Final private TextFieldWidget textField;

    @Shadow @Final private MinecraftClient client;

    @Shadow private CommandSuggestor.SuggestionWindow window;

    @Shadow private boolean completingSuggestions;

    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow protected abstract void show();

    @Inject(method = "refresh",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;canRead()Z"),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILHARD)
    public void onRefresh(CallbackInfo ci, String string, StringReader reader) {
        String prefix = Config.INSTANCE.getPrefix();

        // Assuming the prefix can be more than one character, return if the first n characters of prefix do not
        // match the input.
        if (!reader.canRead(prefix.length())) return;
        while (prefix.length() > 0) {
            if (reader.read() != prefix.charAt(0)) return;
            prefix = prefix.substring(1);
        }

        assert this.client.player != null;
        // Pretty much copy&paste from the refresh method
        CommandDispatcher<CommandSource> commandDispatcher = CommandManager.getDispatcher();
        if (this.parse == null) {
            this.parse = commandDispatcher.parse(reader, CommandManager.getCommandSource());
        }

        int cursor = textField.getCursor();
        if (cursor >= 1 && !this.completingSuggestions) {
            this.pendingSuggestions = commandDispatcher.getCompletionSuggestions(this.parse, cursor);
            this.pendingSuggestions.thenRun(() -> {
                if (this.pendingSuggestions.isDone()) {
                    this.show();
                }
            });
        }

        ci.cancel();
    }

}
