package bits;

import java.io.IOException;
import java.util.BitSet;

public class BitArrayInputStream extends AbstractBitInput {
	private BitSet bitset;
	private int position;

	public BitArrayInputStream(BitSet bitset, int position, int alignment) {
		super(alignment);
		this.bitset = bitset;
		this.position = position;
	}

	public boolean read() throws IOException {
		boolean result = bitset.get(position++);
		incrementAlignment(1);
		return result;
	}

	public int readByte() throws IOException {
		return readInt(8);
	}

	public int available() {
		return bitset.size() - position;
	}
}
