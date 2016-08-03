package bits;

import java.io.IOException;

public abstract class AbstractBitInput implements BitInput {
	protected int alignment;

	protected AbstractBitInput(int alignment) {
		this.alignment = alignment;
	}
	
	public int readInt(int nbits) throws IOException {
		if (nbits < 0 || nbits >= 32)
			throw new IllegalArgumentException();

		int result = 0;

		for (int i = 0; i < nbits; i++)
			result |= (read() ? 1 : 0) << i;
		
		return result;
	}

	protected void incrementAlignment() {
		incrementAlignment(1);
	}

	protected void incrementAlignment(int nbits) {
		alignment = (alignment + nbits) % 8;
	}
	
	public int getAlignment() {
		return alignment;
	}

	public int align() throws IOException {
		int nbits = (8-alignment) % 8;
		skip(nbits);
		return nbits;
	}

	public void skip(int nbits) throws IOException {
		while (nbits-- > 0)
			read();
	}
}
