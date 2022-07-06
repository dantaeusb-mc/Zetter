package me.dantaeusb.zetter.menu.painting.parameters;

import me.dantaeusb.zetter.menu.painting.tools.Brush;
import me.dantaeusb.zetter.menu.painting.tools.Bucket;
import me.dantaeusb.zetter.menu.painting.tools.Pencil;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Tuple;
import org.apache.commons.lang3.SerializationException;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractToolParameters {
    protected HashMap<String, Object> values = new HashMap<>();

    public static void writePacketData(AbstractToolParameters toolParameters, FriendlyByteBuf buffer) {
        buffer.writeCollection(toolParameters.values.entrySet(), AbstractToolParameters::writeEntry);
    }

    /**
     * Uses Java's ObjectOutputStream which apparently is extremely inefficient
     * for primitives. It takes 79 bytes to send float and int.
     * Though works good for enums/strings
     * @todo: fix that wasteful serializer
     * @param buffer
     * @param entry
     */
    private static void writeEntry(FriendlyByteBuf buffer, Map.Entry<String, Object> entry) {
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

    public static AbstractToolParameters readPacketData(FriendlyByteBuf buffer, String toolCode) {
        // @todo: varies!
        AbstractToolParameters toolParameters;

        switch (toolCode) {
            case Brush.CODE:
                toolParameters = new BrushParameters();
                break;
            case Bucket.CODE:
                toolParameters = new BucketParameters();
                break;
            case Pencil.CODE:
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
     * This is very dangerous as we perform class lookup I suppose
     *
     * @param buffer
     * @return
     */
    private static Tuple<String, Object> readEntry(FriendlyByteBuf buffer) {
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

    public void serialize() {

    }
}
