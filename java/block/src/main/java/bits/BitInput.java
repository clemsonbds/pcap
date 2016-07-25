package stream;

import java.io.IOException;
import java.util.BitSet;


public interface BitInput {
	
	// Reads a bit from the stream. Returns 0 or 1 if a bit is available, or throws an EOFException if the end
	// of stream is reached. The end of stream always occurs on a byte boundary.
	public boolean readBit() throws IOException;
	
	public int readBits(BitSet bits, int nbits) throws IOException;
	
	public int availableBits() throws IOException;
	
	// Returns the current bit position, which is between 0 and 7 inclusive. The number of bits remaining in the current byte is 8 minus this number.
	public int getBitPosition();

	// align to the index specified, default 0 (on a byte boundary)
	public void align() throws IOException;
	public void align(int position) throws IOException;

	// temporary, this is part of DataInput and should be implemented by a DataBitInputStream class
	public int readByte() throws IOException;
	public int readInt(int nbits) throws IOException;
}
