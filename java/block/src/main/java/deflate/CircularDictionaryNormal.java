package deflate;

import java.io.IOException;
import java.io.OutputStream;


class CircularDictionaryNormal implements CircularDictionary {
	
	protected byte[] data;
	
	protected int index;
	
	protected int mask;
	
	
	
	public CircularDictionaryNormal(int size) {
		data = new byte[size];
		index = 0;
		
		if (size > 0 && (size & (size - 1)) == 0)  // Test if size is a power of 2
			mask = size - 1;
		else
			mask = 0;
	}
	
	
	
	public void append(int b) {
		data[index] = (byte)b;
		if (mask != 0)
			index = (index + 1) & mask;
		else
			index = (index + 1) % data.length;
	}
	
	
	public void copy(int dist, int len, OutputStream out) throws IOException {
		if (len < 0 || dist < 1 || dist > data.length)
			throw new IllegalArgumentException();

		int readIndex = mask != 0 ?
			(index - dist + data.length) & mask :
			(index - dist + data.length) % data.length;

		for (int i = 0; i < len; i++) {
			if (out != null)
				out.write(data[readIndex]);

			data[index] = data[readIndex];

			if (mask != 0) {
				readIndex = (readIndex + 1) & mask;
				index = (index + 1) & mask;
			} else {
				readIndex = (readIndex + 1) % data.length;
				index = (index + 1) % data.length;
			}
		}
	}
	
}
