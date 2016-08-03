package boundary;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.PriorityQueue;

public class RecordBoundaryDetector {
	private DataInputStream stream;
	private RecordFormat format;
	private byte[] bytes;
	private int max_header_len;
	private int max_packet_len;
	
	public RecordBoundaryDetector(InputStream stream, RecordFormat format) {
		if (stream instanceof DataInputStream)
			this.stream = (DataInputStream)stream;
		else
			this.stream = new DataInputStream(stream);

		this.format = format;

		max_header_len = format.maxRecordHeaderLen();
		max_packet_len = format.maxRecordBodyLen() + max_header_len;
		bytes = new byte[max_packet_len + max_header_len - 1]; // snap len + header + 15 bytes
	}
	
	public int detect() throws IOException {
		PriorityQueue<Solution> solutions = new PriorityQueue<Solution>();

		stream.readFully(bytes); // throws EOFException if not enough data
		int max_index = bytes.length - max_header_len;  // don't try to interpret less than 16 bytes as a header

		for (int i = 0; i <= max_index; i++) {
			Record record = format.interpretRecord(bytes, i);
			
			if (record != null)
				solutions.add(new Solution(i, i + record.header_len + record.body_len));
		}

		if (solutions.isEmpty())
			throw new IOException("no solutions");
		
		int offset = 0;
		int len_chunk = bytes.length;
		
		while (len_chunk >= max_header_len) {
			
			while (solutions.peek().next_index <= max_index) {
				Solution s = solutions.remove();

				while (!solutions.isEmpty() && solutions.peek().next_index == s.next_index) {
					Solution a = solutions.remove();

					if (s.last_index != a.last_index)
						s.last_index = s.next_index;
				}

				int header_i = s.next_index - offset;
				
				if (header_i < 0) {
					throw new IOException("s.next_index = " + s.next_index + " offset = " + offset);
				}
				
				Record record = format.interpretRecord(bytes, header_i);
				if (record != null) {
					if (solutions.isEmpty())
						return s.last_index;
					
					s.next_index += record.header_len + record.body_len;
					solutions.add(s);
				}
			}
			
			// place last 15 bytes at the beginning for the next iteration
			int carry_len = max_header_len - 1;
			int advance_len = len_chunk - carry_len;
			offset += advance_len;

			for (int i = 0; i < carry_len; i++)
				bytes[i] = bytes[advance_len + i];
			
			// read packet_len more bytes
			int read_len = stream.read(bytes, carry_len, max_packet_len);
			len_chunk = read_len + carry_len;
			max_index += read_len;
		}

		throw new IOException("parallel solutions");
	}

}
