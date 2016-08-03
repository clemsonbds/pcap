package deflate;

import java.io.IOException;
import java.io.OutputStream;


final class CircularDictionaryTest extends CircularDictionaryNormal {
	
	public int lookbacks;

	// simplified for 32768 size
	
	public CircularDictionaryTest() {
		super(32768);

		for (int i = 0; i < 32768; i++)
			data[i] = 1;

		lookbacks = 32768;
	}
	
	public void append(int b) {
		if (data[index] == 1)
			lookbacks--;
		data[index] = 0;
		index = (index + 1) & mask;
	}

	public int getMatches() {
		int result = 0;
		for (int i = 0; i < data.length; i++)
			if (data[i] == 1)
				result++;
		return result;
	}
	
	public void copy(int dist, int len, OutputStream out) throws IOException {
		if (len < 0 || dist < 1 || dist > data.length)
			throw new IllegalArgumentException();
		int readIndex = (index - dist + data.length) & mask;
//		System.out.println("starting copy from distance " + dist + ", index " + readIndex + ", of len " + len);
		for (int i = 0; i < len; i++) {
//			System.out.println("copying index " + readIndex + " [" + data[readIndex] + "] to index " + index + " [" + data[index] + "]");
			if (data[index] == 1 && data[readIndex] == 0)
				lookbacks--;
			else if (data[index] == 0 && data[readIndex] == 1)
				lookbacks++;
			
			data[index] = data[readIndex];
			
			readIndex = (readIndex + 1) & mask;
			index = (index + 1) & mask;
		}
	}
	
}
