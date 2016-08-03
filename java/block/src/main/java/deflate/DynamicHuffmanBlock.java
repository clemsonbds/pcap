package deflate;

import java.io.IOException;
import java.util.Arrays;

import bits.BitInput;
import bits.MeasuredBitInput;
import boundary.InvalidBlockException;

public class DynamicHuffmanBlock extends HuffmanBlock {

	public DynamicHuffmanBlock(boolean isFinal) {
		super(isFinal);
	}

	@Override
	public void parseHeader(BitInput in) throws InvalidBlockException, IOException {
		MeasuredBitInput bits = new MeasuredBitInput(in);

		int numLitLenCodes = bits.readInt(5) + 257;  // hlit  + 257						// 5
		int numDistCodes = bits.readInt(5) + 1;      // hdist +   1						// 10
		
		int numCodeLenCodes = bits.readInt(4) + 4;   // hclen +   4						// 14
		int[] codeLenCodeLen = new int[19];
		codeLenCodeLen[16] = bits.readInt(3);
		codeLenCodeLen[17] = bits.readInt(3);
		codeLenCodeLen[18] = bits.readInt(3);
		codeLenCodeLen[ 0] = bits.readInt(3);											// 26
		for (int i = 0; i < numCodeLenCodes - 4; i++) {								// 255*3? 791
			if (i % 2 == 0)
				codeLenCodeLen[8 + i / 2] = bits.readInt(3);
			else
				codeLenCodeLen[7 - i / 2] = bits.readInt(3);
		}
		CodeTree codeLenCode;
		try {
			codeLenCode = new CanonicalCode(codeLenCodeLen).toCodeTree();
		} catch (IllegalStateException e) {
			throw new InvalidBlockException(e.getMessage());
		}
		
		int[] codeLens = new int[numLitLenCodes + numDistCodes];
		int runVal = -1;
		int runLen = 0;
		for (int i = 0; i < codeLens.length; i++) {
			if (runLen > 0) {
				codeLens[i] = runVal;
				runLen--;
				
			} else {
				int sym = decodeSymbol(bits, codeLenCode);
				if (sym < 16) {
					codeLens[i] = sym;
					runVal = sym;
				} else {
					if (sym == 16) {
						if (runVal == -1)
							throw new InvalidBlockException("No code length value to copy");
						runLen = bits.readInt(2) + 3;
					} else if (sym == 17) {
						runVal = 0;
						runLen = bits.readInt(3) + 3;
					} else if (sym == 18) {
						runVal = 0;
						runLen = bits.readInt(7) + 11;
					} else
						throw new AssertionError();
					i--;
				}
			}
		}
		if (runLen > 0)
			throw new InvalidBlockException("Run exceeds number of codes");
		
		// Create code trees
		int[] litLenCodeLen = Arrays.copyOf(codeLens, numLitLenCodes);
		try {
			litLenCode = new CanonicalCode(litLenCodeLen).toCodeTree();
		} catch (IllegalStateException e) {
			throw new InvalidBlockException(e.getMessage());
		}
		
		int[] distCodeLen = Arrays.copyOfRange(codeLens, numLitLenCodes, codeLens.length);
		if (distCodeLen.length == 1 && distCodeLen[0] == 0)
			distCode = null;  // Empty distance code; the block shall be all literal symbols
		else {
			// Get statistics for upcoming logic
			int oneCount = 0;
			int otherPositiveCount = 0;
			for (int x : distCodeLen) {
				if (x == 1)
					oneCount++;
				else if (x > 1)
					otherPositiveCount++;
			}
			
			// Handle the case where only one distance code is defined
			if (oneCount == 1 && otherPositiveCount == 0) {
				// Add a dummy invalid code to make the Huffman tree complete
				distCodeLen = Arrays.copyOf(distCodeLen, 32);
				distCodeLen[31] = 1;
			}
			
			try {
				distCode = new CanonicalCode(distCodeLen).toCodeTree();
			} catch (IllegalStateException e) {
				throw new InvalidBlockException(e.getMessage());
			}
		}
		
		header_len += bits.bitsRead();
	}
}
