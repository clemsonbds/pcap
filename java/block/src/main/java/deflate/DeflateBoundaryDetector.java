package deflate;

import java.io.DataInputStream;
import java.util.BitSet;
import java.util.PriorityQueue;

import block.AbstractBoundaryDetector;
import block.Solution;
import stream.BitInput;
import stream.BitInputStream;

public class DeflateBoundaryDetector extends AbstractBoundaryDetector {
	private final int HEADER_LEN = 8000;
	
	public DeflateBoundaryDetector(DataInputStream stream, int block_len) {
		super(stream);
	}

	public int detect() {
		BitInputStream in = new BitInputStream(stream);

		
		
		return 0;
	}
	
	private void identifyInitialCandidates(byte[] bytes, PriorityQueue<Solution> solutions) {
		BitSet bits = BitSet.valueOf(bytes);
		
		int max_index = bits.length() - HEADER_LEN;
		
//		for (int i = 0; i <= max_index; i++)
//			if (validHeader(bits, i)) {
//				int end = findBoundary()
//			}
//				solutions.add();
	}
}
