package me.dantaeusb.zetter.server.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

public class PaintingLookupArgument implements ArgumentType<PaintingInput> {
    private static final Collection<String> EXAMPLES = Arrays.asList("12", PaintingData.CODE_PREFIX + "12", "Painting", "\"My Painting\"");

    private static final Pattern ID = Pattern.compile("\\p{javaDigit}+");
    private static final Pattern CODE = Pattern.compile(PaintingData.CODE_PREFIX + "\\p{javaDigit}+");

    private static final DynamicCommandExceptionType ERROR_INVALID_PAINTING_CODE = new DynamicCommandExceptionType((code) -> {
        return Component.translatable("console.zetter.error.invalid_painting_code", code);
    });

    public PaintingLookupArgument() {

    }

    public static PaintingLookupArgument painting() {
        return new PaintingLookupArgument();
    }

    public PaintingInput parse(StringReader reader) throws CommandSyntaxException {
        String input = StringReader.isQuotedStringStart(reader.peek()) ? reader.readQuotedString() : reader.readUnquotedString();

        try {
            if (ID.matcher(input).matches()) {
                return PaintingInput.fromId(Integer.parseInt(input));
            } else if (CODE.matcher(input).matches()) {
                return PaintingInput.fromCode(input);
            } else {
                return PaintingInput.fromTitle(input);
            }
        } catch (NumberFormatException e) {
            throw ERROR_INVALID_PAINTING_CODE.create(input);
        }
    }

    public static <S> PaintingInput getPaintingInput(CommandContext<S> context, String argumentName) {
        return context.getArgument(argumentName, PaintingInput.class);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
