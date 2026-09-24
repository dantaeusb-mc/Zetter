package me.dantaeusb.zetter.painting.parameters;

import me.dantaeusb.zetter.painting.Tool;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Tuple;
import org.apache.commons.lang3.SerializationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractToolParameters implements Cloneable {
    private static final int CODE_MAX_LENGTH = 128;
    private static final int STRING_VALUE_MAX_LENGTH = 128;

    private static final byte TYPE_FLOAT = 0;
    private static final byte TYPE_INTEGER = 1;
    private static final byte TYPE_BOOLEAN = 2;
    private static final byte TYPE_STRING = 3;

    protected HashMap<String, Object> values = new HashMap<>();

    @Override
    public AbstractToolParameters clone() throws CloneNotSupportedException {
        AbstractToolParameters copy = (AbstractToolParameters) super.clone();
        copy.values = new HashMap<>();

        for (Map.Entry<String, Object> valueEntry : this.values.entrySet()) {
            if (
                valueEntry.getValue() instanceof Number
                    || valueEntry.getValue() instanceof String
                    || valueEntry.getValue() instanceof Boolean
            ) {
                copy.values.put(valueEntry.getKey(), valueEntry.getValue());
            } else if (valueEntry.getValue() instanceof CloneableParameter) {
                copy.values.put(valueEntry.getKey(), ((CloneableParameter) valueEntry.getValue()).clone());
            } else {
                throw new CloneNotSupportedException("Value of parameter " + valueEntry.getKey() + " is not cloneable!");
            }
        }

        return copy;
    }

    public static void writePacketData(AbstractToolParameters toolParameters, FriendlyByteBuf buffer) {
        buffer.writeCollection(toolParameters.values.entrySet(), AbstractToolParameters::writeEntry);
    }

    /**
     * Values are tagged with their type and written as primitives: every action
     * carries a full copy of its parameters, so the encoding has to be compact
     *
     * @param buffer
     * @param entry
     */
    private static void writeEntry(FriendlyByteBuf buffer, Map.Entry<String, Object> entry) {
        buffer.writeUtf(entry.getKey(), CODE_MAX_LENGTH);

        final Object value = entry.getValue();

        if (value instanceof Float floatValue) {
            buffer.writeByte(TYPE_FLOAT);
            buffer.writeFloat(floatValue);
        } else if (value instanceof Integer integerValue) {
            buffer.writeByte(TYPE_INTEGER);
            buffer.writeVarInt(integerValue);
        } else if (value instanceof Boolean booleanValue) {
            buffer.writeByte(TYPE_BOOLEAN);
            buffer.writeBoolean(booleanValue);
        } else if (value instanceof String stringValue) {
            buffer.writeByte(TYPE_STRING);
            buffer.writeUtf(stringValue, STRING_VALUE_MAX_LENGTH);
        } else {
            throw new SerializationException(
                "Unable to write parameter " + entry.getKey() + ": unsupported type " + value.getClass().getName()
            );
        }
    }

    public static AbstractToolParameters readPacketData(FriendlyByteBuf buffer, Tool tool) {
        // @todo: varies!
        AbstractToolParameters toolParameters;

        switch (tool) {
            case BRUSH:
                toolParameters = new BrushParameters();
                break;
            case BUCKET:
                toolParameters = new BucketParameters();
                break;
            case SPONGE:
                toolParameters = new SpongeParameters();
                break;
            case PENCIL:
            default:
                toolParameters = new PencilParameters();
                break;
        }

        final List<Tuple<String, Object>> rawParameters = buffer.readCollection(
            NonNullList::createWithCapacity,
            AbstractToolParameters::readEntry
        );

        toolParameters.values = (HashMap<String, Object>) rawParameters.stream().collect(Collectors.toMap(
            Tuple::getA,
            Tuple::getB
        ));

        return toolParameters;
    }

    /**
     * Only the types writeEntry knows are accepted, so nothing here can name a
     * class to instantiate
     *
     * @param buffer
     * @return
     */
    private static Tuple<String, Object> readEntry(FriendlyByteBuf buffer) {
        final String key = buffer.readUtf(CODE_MAX_LENGTH);
        final byte type = buffer.readByte();

        return switch (type) {
            case TYPE_FLOAT -> new Tuple<>(key, buffer.readFloat());
            case TYPE_INTEGER -> new Tuple<>(key, buffer.readVarInt());
            case TYPE_BOOLEAN -> new Tuple<>(key, buffer.readBoolean());
            case TYPE_STRING -> new Tuple<>(key, buffer.readUtf(STRING_VALUE_MAX_LENGTH));
            default -> throw new SerializationException("Unknown type " + type + " for parameter " + key);
        };
    }
}
