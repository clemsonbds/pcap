package deflate;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import bits.BitArrayInputStream;
import bits.BitInput;
import bits.BitInputStream;
import boundary.AbstractDetector;
import boundary.InvalidBlockException;
import boundary.Solution;

public class DeflateBoundaryDetector extends AbstractDetector {
	private final int MAX_COMPRESSED_PAYLOAD_LEN = 32768*8; // bits, this may not be correct for uncompressible data
	private final int MAX_HEADER_LEN = 8000; // bits, verify this
	private final int BLOCK_LEN = MAX_HEADER_LEN + MAX_COMPRESSED_PAYLOAD_LEN;
	
	public DeflateBoundaryDetector(InputStream stream) {
		super(stream);
	}

	public int detect() throws IOException {
		BitInputStream in = new BitInputStream(stream);

		int sample_len = 2*BLOCK_LEN - 1;
		BitSet bits = new BitSet(sample_len);
		int offset = 0;
		int bits_read = in.readBits(bits, sample_len);

		if (bits_read < sample_len) {
			throw new EOFException();
		}
		
		int max_index = bits.length() - BLOCK_LEN;

		PriorityQueue<Solution> solutions = new PriorityQueue<Solution>();
		Set<Integer> tested = new TreeSet<Integer>();
		identifyInitialCandidates(bits, max_index, solutions, tested);

		if (solutions.isEmpty())
			throw new IOException("no solutions");

		while (bits_read >= BLOCK_LEN) {
			Solution s = narrowCandidates(bits, max_index, offset, solutions, tested);
			
			if (s != null)
				return s.last_index;
//			System.out.println("reading, " + solutions.size() + " solutions left: " + solutions);

			offset += BLOCK_LEN;
			int carry_len = BLOCK_LEN - 1;
			
			for (int i = 0; i < carry_len; i++)
				bits.set(i, bits.get(i+BLOCK_LEN));

			bits_read = in.readBits(bits, carry_len, BLOCK_LEN) + carry_len;
			
		}

		throw new IOException("parallel solutions");
	}

	// identifies the valid-looking start points, but can't identify end points for any except uncompressed blocks
	private void identifyInitialCandidates(BitSet bits, int max_index, PriorityQueue<Solution> solutions, Set<Integer> tested) throws IOException {
		DeflateBlockFactory factory = new DeflateBlockFactory();

		// do this backwards to avoid adding solutions that point to other solutions in the first round
		for (int i = max_index; i >= 0; i--) {
			// we know this was called on a byte alignment, make this easy
			int offset_alignment = i % 8;
			try {
				BitInput stream = new BitArrayInputStream(bits, i, offset_alignment);
				DeflateBlock block = factory.parseHeader(stream);
				block.findPayloadLength(stream);
//				System.out.println("found valid block at bit index " + i);
				if (!tested.contains(i)) {
					solutions.add(new Solution(i, i+block.length()));
					tested.add(i);
				}
			}
			catch (InvalidBlockException e) {
//				System.out.println("invalid block at index " + i);
			}
		}
		
	}

	private Solution narrowCandidates(BitSet bits, int max_index, int offset, PriorityQueue<Solution> solutions, Set<Integer> tested) throws IOException {
		DeflateBlockFactory factory = new DeflateBlockFactory();
							
		while (solutions.peek().next_index - offset <= max_index) {
			Solution s = solutions.remove();

			while (!solutions.isEmpty() && solutions.peek().next_index == s.next_index) {
//					System.out.println("converging solution " + s + " with " + solutions.peek());
				Solution a = solutions.remove();
				// check here to avoid pushing chain of solutions +1.  might be a more elegant way to avoid
				if (s.last_index != a.last_index)
					s.last_index = s.next_index;
			}

			// this removes false solutions that point to already discarded solutions, no need to test against current solutions
			if (tested.contains(s.next_index))
				continue;
			
			try {
				int next_index = s.next_index - offset;
				BitInput stream = new BitArrayInputStream(bits, next_index, s.next_index % 8);
				DeflateBlock block = factory.parseHeader(stream);
				block.findPayloadLength(stream); // could still be invalid

				// could this be returned earlier?  one too many tests?
				if (solutions.isEmpty())
					return s;

				s.next_index += block.length();
				System.out.println("found valid block at bit index " + next_index + ", new solution is " + s);
				solutions.add(s);
			}
			catch (InvalidBlockException e) {
//				System.out.println("invalid block at index " + i);
			}
		}
		
		return null;
	}
}
