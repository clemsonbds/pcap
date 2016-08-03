package deflate;

import java.io.InputStream;

import boundary.Decompressor;

public abstract class AbstractDecompressor implements Decompressor {
	protected InputStream stream;
	
	public AbstractDecompressor(InputStream stream2) {
		this.stream = stream2;
	}
	
}
