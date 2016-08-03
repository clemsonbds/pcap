package bits;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

public class BitInputStream extends AbstractBitInput {
	private InputStream stream;
	private byte buffer;

	public BitInputStream(InputStream stream) {
		super(0);
		this.stream = stream;
	}

	public boolean read() throws IOException {
		if (alignment == 0)
			buffer = (byte)stream.read();

		boolean result = (buffer & (1L << alignment)) != 0;
		incrementAlignment(1);
		return result;
	}

	public int readByte() throws IOException {
		if (alignment == 0)
			return stream.read();

		return readInt(8);
	}
	
	public int available() throws IOException {
		return stream.available()*8 + ((8 - alignment) % 8);
	}
	
	public int readBits(BitSet bits, int nbits) throws IOException {
		return readBits(bits, 0, nbits);
	}

	public int readBits(BitSet bits, int offset, int nbits) throws IOException {
		int bits_read = 0;

		try {
			while (bits_read < nbits) {
				bits.set(offset+bits_read, read());
				bits_read++;
			}
		}
		catch (EOFException e) {}

		return bits_read;
	}
}
