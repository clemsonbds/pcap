package deflate;

import java.io.IOException;
import java.io.OutputStream;

public interface CircularDictionary {
	public void append(int b);
	public void copy(int dist, int len, OutputStream out) throws IOException;
}
