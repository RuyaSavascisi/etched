package gg.moonflower.etched.api.sound.stream;

import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * @author Ocelot
 */
public class RawAudioStream implements AudioStream {

    private final AudioFormat format;
    private final InputStream input;

    public RawAudioStream(AudioFormat format, InputStream input) {
        this.format = format;
        this.input = input;
    }

    private static ByteBuffer convertAudioBytes(byte[] audio_bytes, boolean two_bytes_data, ByteOrder order) {
        ByteBuffer dest = BufferUtils.createByteBuffer(audio_bytes.length);
        dest.order(ByteOrder.nativeOrder());
        ByteBuffer src = ByteBuffer.wrap(audio_bytes);
        src.order(order);
        if (two_bytes_data) {
            ShortBuffer dest_short = dest.asShortBuffer();
            ShortBuffer src_short = src.asShortBuffer();
            dest_short.put(src_short);
        } else {
            dest.put(src);
        }
        dest.rewind();
        return dest;
    }

    @Override
    public AudioFormat getFormat() {
        return this.format;
    }

    @Override
    public ByteBuffer read(int amount) throws IOException {
        byte[] buf = new byte[amount];
        int read, total = 0;

        while (total < buf.length && (read = this.input.read(buf, total, buf.length - total)) != -1) {
            total += read;
        }

        byte[] result = buf;
        if (buf.length != total) {
            result = new byte[total];
            System.arraycopy(buf, 0, result, 0, result.length);
        }

        return convertAudioBytes(result, this.format.getSampleSizeInBits() == 16, this.format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void close() throws IOException {
        this.input.close();
    }
}
