package stream;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;


/**
 * A stream of bits that can be read.
 */
public class BitInputStream extends FilterInputStream implements BitInput {
	private int bitPosition;  // Always between 1 and 8, inclusive
	private BitSet bitbuffer;
	
	public BitInputStream(InputStream in) {
		super(in);

		bitPosition = 8;
	}

	public boolean readBit() throws IOException {
		if (bitPosition == 8)
			readNextByte();

		return bitbuffer.get(bitPosition++);
	}

	public int readBits(BitSet bits, int nbits) throws IOException {
		int bits_read = 0;

		try {
			while (bits_read < nbits) {
				bits.set(bits_read, readBit());
				bits_read++;
			}
		}
		catch (EOFException e) {}

		return bits_read;
	}

	public int availableBits() throws IOException {
		return available() * 8 + (8 - bitPosition);
	}

	public int getBitPosition() {
		return bitPosition;
	}	

	private void readNextByte() throws IOException {
		byte[] b = new byte[1];

		if (in.read(b) == -1)
			throw new EOFException();
		
		bitbuffer = BitSet.valueOf(b);
		bitPosition = 0;
	}
	
	public void align() throws IOException {
		align(0);
	}

	public void align(int position) throws IOException {
		while (bitPosition != position)
			readBit();
	}

	public int readByte() throws IOException {
		return readInt(8);
	}

	public int readInt(int nbits) throws IOException {
		if (nbits < 0 || nbits >= 32)
			throw new IllegalArgumentException();
		
		int result = 0;
		for (int i = 0; i < nbits; i++)
			result |= readBit() ? 1 : 0 << i;
		return result;
	}

	// overridden methods to allow byte reading without messing up bit tracking.  discards current byte
	@Override
	public int read() throws IOException {
		bitPosition = 8;
		return super.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		bitPosition = 8;
		return super.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		bitPosition = 8;
		return super.read(b, off, len);
	}

	@Override
	public void reset() throws IOException {
		bitPosition = 8;
		super.reset();
	}
	
	@Override
	public long skip(long n) throws IOException {
		bitPosition = 8;
		return super.skip(n);
	}

}
