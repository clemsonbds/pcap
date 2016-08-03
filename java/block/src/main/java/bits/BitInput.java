package bits;

import java.io.IOException;
import java.util.BitSet;


public interface BitInput {
	// Reads a bit from the stream. Returns 0 or 1 if a bit is available, or throws an EOFException if the end
	// of stream is reached. The end of stream always occurs on a byte boundary.
	public boolean read() throws IOException;

	// Reads 1..32 bits and interprets as an integer.
	public int readInt(int nbits) throws IOException;
	
	// read one byte.
	public int readByte() throws IOException;

	// Find number of bits available for reading.
	public int available() throws IOException;
	
	// Align to byte boundary, discarding bits.  Does nothing if currently aligned.  Returns number of bits discarded.
	public int align() throws IOException;
	
	// Read and discard some bits.
	public void skip(int nbits) throws IOException;
}
