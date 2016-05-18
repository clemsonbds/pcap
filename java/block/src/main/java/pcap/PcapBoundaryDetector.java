package pcap;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.PriorityQueue;

import block.AbstractBoundaryDetector;
import block.Solution;

public class PcapBoundaryDetector extends AbstractBoundaryDetector {
	final int HEADER_LEN = 16;
	int snap_len;
	int packet_len;
	private ByteOrder byteOrder;

	public PcapBoundaryDetector(DataInputStream stream, int snap_len, java.nio.ByteOrder byteOrder) {
		super(stream);
		this.snap_len = snap_len;
		this.packet_len = snap_len + HEADER_LEN;
		this.byteOrder = byteOrder;
	}
	
	private int readInt(byte[] bytes, int offset) {
		return java.nio.ByteBuffer.wrap(bytes, offset, 4).order(this.byteOrder).getInt();
	}
	
	private boolean validHeader(byte[] bytes, int offset) {
		long incl_len = readInt(bytes, offset + 8);
		long orig_len = readInt(bytes, offset + 12);

		if (orig_len > 0 && incl_len == Math.min(orig_len, snap_len))
			return true;

		return false;
	}
	
	public int detect() throws IOException {
		PriorityQueue<Solution> solutions = new PriorityQueue<Solution>();

		byte[] bytes = new byte[packet_len + HEADER_LEN - 1]; // snap len + header + 15 bytes
		int max_index = bytes.length - HEADER_LEN;  // don't try to interpret less than 16 bytes as a header
		
		stream.readFully(bytes); // throws EOFException if not enough data

		for (int i = 0; i <= max_index; i++)
			if (validHeader(bytes, i))
				solutions.add(new Solution(i, i + (int)readInt(bytes, i + 8) + HEADER_LEN));

//		System.out.println(solutions);

		if (solutions.isEmpty())
			throw new IOException("no solutions");
		
		int offset = 0;
		int len_chunk = bytes.length;
		
		while (len_chunk >= HEADER_LEN) {
			
			while (solutions.peek().next_index <= max_index) {
				Solution s = solutions.remove();

				while (!solutions.isEmpty() && solutions.peek().next_index == s.next_index) {
//					System.out.println("converging solution " + s + " with " + solutions.peek());
					Solution a = solutions.remove();
					// check here to avoid pushing chain of solutions +1.  might be a more elegant way to avoid
					if (s.last_index != a.last_index)
						s.last_index = s.next_index;
				}

				int header_i = s.next_index - offset;
				
				if (validHeader(bytes, header_i)) {
//					System.out.println("got valid solution " + s);
					if (solutions.isEmpty())
						return s.last_index;
					
					s.next_index += (int)readInt(bytes, header_i + 8) + HEADER_LEN;
					solutions.add(s);
//					System.out.println(solutions);
				}
			}
			
//			System.out.println("reading, " + solutions.size() + " solutions left");
			
			// place last 15 bytes at the beginning for the next iteration
			int carry_len = HEADER_LEN - 1;
			int advance_len = len_chunk - carry_len;
			offset += advance_len;

			for (int i = 0; i < carry_len; i++)
				bytes[i] = bytes[advance_len + i];
			
			// read packet_len more bytes
			int read_len = stream.read(bytes, carry_len, packet_len);
			len_chunk = read_len + carry_len;
			max_index += read_len;
		}

		throw new IOException("parallel solutions");
	}

	
}
