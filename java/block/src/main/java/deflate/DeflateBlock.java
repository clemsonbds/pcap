package deflate;

import java.io.IOException;
import java.io.OutputStream;

import bits.BitInput;
import boundary.InvalidBlockException;

public abstract class DeflateBlock {
	protected int header_len = 0;
	protected int payload_len = 0;

	boolean isFinal;

	public DeflateBlock(boolean isFinal) {
		this.isFinal = isFinal;
		header_len = 3;
	}
	
	public int length() {
		return header_len + payload_len;
	}

//	public abstract void parse(BitInput bits) throws InvalidBlockException, IOException;
	public abstract void parseHeader(BitInput in) throws InvalidBlockException, IOException;
	public abstract void findPayloadLength(BitInput in) throws InvalidBlockException, IOException;
	public abstract void decode(BitInput in, CircularDictionary dict, OutputStream out) throws InvalidBlockException, IOException;

}
