package anticope.rejects.arguments;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public class EnumStringArgumentType implements ArgumentType<String> {

    private  Collection<String> EXAMPLES;
    private static final DynamicCommandExceptionType INVALID_OPTION = new DynamicCommandExceptionType(name ->
            Text.literal(name + " is not a valid option."));
    public EnumStringArgumentType(Collection<String> examples) {
        this.EXAMPLES = examples;
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String arg = reader.readUnquotedString();
        if (!EXAMPLES.contains(arg)) throw INVALID_OPTION.create(arg);
        return arg;
    }

    public static String getString(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    @Override
    public String toString() {
        return "string()";
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(EXAMPLES, builder);
    }
    
}
