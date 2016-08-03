package examples;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import boundary.RecordFormat;
import boundary.Decompressor;
import boundary.InvalidBlockException;
import deflate.DeflateBoundaryDetector;
import deflate.DeflateDecompressor;

public class DeflateRandomReadExample {

	public static void main(String[] args) throws IOException, InvalidBlockException {
		File file = new File("/Users/jason/git/pcap/test_data/netflix.pcap.gz");
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream bis = new BufferedInputStream(fis);  // so we can traverse twice

		RecordFormat detector = new DeflateBoundaryDetector(bis);
		Decompressor decomp = new DeflateDecompressor(bis);
		int bit_offset = 0;
		
		while (true) {
			bis.mark(8*32768);
			int block_bit_index = detector.detect();
			bis.reset();
	
			System.out.println("block start index is at " + (block_bit_index + bit_offset));
	
			bis.mark(1024*32768);
			int read_bit_index = decomp.findIndependentStart(block_bit_index);
			bis.reset();
			
			System.out.println("first read index is at " + (read_bit_index + bit_offset));

			int skip_bytes = block_bit_index/8+2;
			bit_offset += skip_bytes*8;
			bis.skip(skip_bytes);
		}
	}

}
