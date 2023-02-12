package me.dantaeusb.zetter.painting.parameters;

import me.dantaeusb.zetter.painting.Tools;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Tuple;
import org.apache.commons.lang3.SerializationException;

import javax.lang.model.type.PrimitiveType;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractToolParameters implements Cloneable {
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

    public static void writePacketData(AbstractToolParameters toolParameters, PacketBuffer buffer) {
        buffer.writeCollection(toolParameters.values.entrySet(), AbstractToolParameters::writeEntry);
    }

    /**
     * Uses Java's ObjectOutputStream which apparently is extremely inefficient
     * for primitives. It takes 79 bytes to send float and int.
     * Though works good for enums/strings
     *
     * @param buffer
     * @param entry
     * @todo: fix that wasteful serializer
     */
    private static void writeEntry(PacketBuffer buffer, Map.Entry<String, Object> entry) {
        try {
            buffer.writeUtf(entry.getKey(), 128);
            ByteArrayOutputStream streamOutput = new ByteArrayOutputStream();

            ObjectOutputStream stream = new ObjectOutputStream(streamOutput);
            stream.writeObject(entry.getValue());
            streamOutput.close();

            final byte[] output = streamOutput.toByteArray();

            buffer.writeInt(output.length);
            buffer.writeBytes(streamOutput.toByteArray());
        } catch (IOException e) {
            throw new SerializationException("Unable to write value for parameter " + entry.getKey());
        }
    }

    public static AbstractToolParameters readPacketData(PacketBuffer buffer, Tools tool) {
        // @todo: varies!
        AbstractToolParameters toolParameters;

        switch (tool) {
            case BRUSH:
                toolParameters = new BrushParameters();
                break;
            case BUCKET:
                toolParameters = new BucketParameters();
                break;
            case PENCIL:
            default:
                toolParameters = new PencilParameters();
                break;
        }

        final List<Tuple<String, Object>> rawParameters = buffer.readCollection(
            NonNullList::create,
            AbstractToolParameters::readEntry
        );

        toolParameters.values = (HashMap<String, Object>) rawParameters.stream().collect(Collectors.toMap(
            Tuple::getA,
            Tuple::getB
        ));

        return toolParameters;
    }

    /**
     * This is very dangerous as we perform class lookup I suppose
     *
     * @param buffer
     * @return
     */
    private static Tuple<String, Object> readEntry(PacketBuffer buffer) {
        final String key = buffer.readUtf(128);
        final int length = buffer.readInt();

        final byte[] input = new byte[length];
        buffer.readBytes(length).nioBuffer().get(input);

        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(input);
            ObjectInputStream objectStream = new ObjectInputStream(byteStream);
            final Object value = objectStream.readObject();

            return new Tuple<>(key, value);
        } catch (IOException e) {
            throw new SerializationException("Unable to read value for parameter " + key);
        } catch (ClassNotFoundException e) {
            throw new SerializationException("Incorrect or forbidden type for parameter " + key);
        }
    }
}
