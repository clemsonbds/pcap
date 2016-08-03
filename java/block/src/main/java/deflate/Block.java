package deflate;

import bits.BitInput;
import block.InvalidBlockException;

public interface Block {
	
	public int length();

	public void parse(BitInput bits) throws InvalidBlockException;
	
}
