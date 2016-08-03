package deflate;

import java.io.IOException;

import block.InvalidBlockException;

public interface Decompressor {
	public int findIndependentStart(int start_index) throws IOException, InvalidBlockException;
}
