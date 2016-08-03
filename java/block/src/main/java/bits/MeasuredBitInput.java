package bits;

import java.io.IOException;

public class MeasuredBitInput implements BitInput {
	private BitInput in;
	private int bits_read = 0;
	
	public MeasuredBitInput(BitInput in) {
		this.in = in;
	}
	
	public int bitsRead() {
		return bits_read;
	}
	
	public boolean read() throws IOException {
		bits_read++;
		return in.read();
	}

	public int readByte() throws IOException {
		bits_read += 8;
		return in.readByte();
	}
	
	public int readInt(int nbits) throws IOException {
		bits_read += nbits;
		return in.readInt(nbits);
	}

	public int available() throws IOException {
		return in.available();
	}
	
	public int align() throws IOException {
		int ret = in.align();
		bits_read += ret;
		return ret;
	}

	public void skip(int nbits) throws IOException {
		bits_read += nbits;
		in.skip(nbits);
	}
}
