package deflate;

import java.io.IOException;
import java.util.Arrays;

import bits.BitInput;
import boundary.InvalidBlockException;

public class FixedHuffmanBlock extends HuffmanBlock {

	public FixedHuffmanBlock(boolean isFinal) {
		super(isFinal);
	}

	private static CodeTree fixedLiteralLengthCode;
	private static CodeTree fixedDistanceCode;
	
	static {
		int[] llcodelens = new int[288];
		Arrays.fill(llcodelens,   0, 144, 8);
		Arrays.fill(llcodelens, 144, 256, 9);
		Arrays.fill(llcodelens, 256, 280, 7);
		Arrays.fill(llcodelens, 280, 288, 8);
		fixedLiteralLengthCode = new CanonicalCode(llcodelens).toCodeTree();
		
		int[] distcodelens = new int[32];
		Arrays.fill(distcodelens, 5);
		fixedDistanceCode = new CanonicalCode(distcodelens).toCodeTree();
	}

	public void parseHeader(BitInput bits) throws InvalidBlockException, IOException {
		litLenCode = fixedLiteralLengthCode;
		distCode = fixedDistanceCode;
	}
}
