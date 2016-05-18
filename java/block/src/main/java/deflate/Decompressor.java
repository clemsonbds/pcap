package deflate;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DataFormatException;

import stream.BitInput;


public final class Decompressor {
	
	/* Public method */
	
	public static byte[] decompress(BitInput in) throws IOException, DataFormatException {
		Decompressor decomp = new Decompressor(in);
		return decomp.output.toByteArray();
	}
	
	
	
	/* Private members */
	
	private BitInput input;
	
	private ByteArrayOutputStream output;
	
	private CircularDictionary dictionary;
	
	
	
	private Decompressor(BitInput in) throws IOException, DataFormatException {
		input = in;
		output = new ByteArrayOutputStream();
		dictionary = new CircularDictionary(32 * 1024);
		boolean isFinal = false;
		
		// Process the stream of blocks
		while (!isFinal) {

			// Block header
			isFinal = in.readBit();  // bfinal
			int type = input.readInt(2);                  // btype
			
			// Decompress by type
			if (type == 0)
				decompressUncompressedBlock();
			else if (type == 1 || type == 2) {
				CodeTree litLenCode, distCode;
				if (type == 1) {
					litLenCode = fixedLiteralLengthCode;
					distCode = fixedDistanceCode;
				} else {
					CodeTree[] temp = decodeHuffmanCodes(in);
					litLenCode = temp[0];
					distCode = temp[1];
				}
				decompressHuffmanBlock(litLenCode, distCode);
				
			} else if (type == 3)
				throw new DataFormatException("Invalid block type");
			else
				throw new AssertionError();
		}
	}
	
	
	// For handling static Huffman codes (btype = 1)
	
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
	
	
	// For handling dynamic Huffman codes (btype = 2)
	private CodeTree[] decodeHuffmanCodes(BitInput in) throws IOException, DataFormatException {
		int numLitLenCodes = input.readInt(5) + 257;  // hlit  + 257						// 5
		int numDistCodes = input.readInt(5) + 1;      // hdist +   1						// 10
		
		int numCodeLenCodes = input.readInt(4) + 4;   // hclen +   4						// 14
		int[] codeLenCodeLen = new int[19];
		codeLenCodeLen[16] = input.readInt(3);
		codeLenCodeLen[17] = input.readInt(3);
		codeLenCodeLen[18] = input.readInt(3);
		codeLenCodeLen[ 0] = input.readInt(3);											// 26
		for (int i = 0; i < numCodeLenCodes - 4; i++) {								// 255*3? 791
			if (i % 2 == 0)
				codeLenCodeLen[8 + i / 2] = input.readInt(3);
			else
				codeLenCodeLen[7 - i / 2] = input.readInt(3);
		}
		CodeTree codeLenCode;
		try {
			codeLenCode = new CanonicalCode(codeLenCodeLen).toCodeTree();
		} catch (IllegalStateException e) {
			throw new DataFormatException(e.getMessage());
		}
		
		int[] codeLens = new int[numLitLenCodes + numDistCodes];
		int runVal = -1;
		int runLen = 0;
		for (int i = 0; i < codeLens.length; i++) {
			if (runLen > 0) {
				codeLens[i] = runVal;
				runLen--;
				
			} else {
				int sym = decodeSymbol(codeLenCode);
				if (sym < 16) {
					codeLens[i] = sym;
					runVal = sym;
				} else {
					if (sym == 16) {
						if (runVal == -1)
							throw new DataFormatException("No code length value to copy");
						runLen = input.readInt(2) + 3;
					} else if (sym == 17) {
						runVal = 0;
						runLen = input.readInt(3) + 3;
					} else if (sym == 18) {
						runVal = 0;
						runLen = input.readInt(7) + 11;
					} else
						throw new AssertionError();
					i--;
				}
			}
		}
		if (runLen > 0)
			throw new DataFormatException("Run exceeds number of codes");
		
		// Create code trees
		int[] litLenCodeLen = Arrays.copyOf(codeLens, numLitLenCodes);
		CodeTree litLenCode;
		try {
			litLenCode = new CanonicalCode(litLenCodeLen).toCodeTree();
		} catch (IllegalStateException e) {
			throw new DataFormatException(e.getMessage());
		}
		
		int[] distCodeLen = Arrays.copyOfRange(codeLens, numLitLenCodes, codeLens.length);
		CodeTree distCode;
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
				throw new DataFormatException(e.getMessage());
			}
		}
		
		return new CodeTree[]{litLenCode, distCode};
	}
	
	
	/* Block decompression methods */
	
	private void decompressUncompressedBlock() throws IOException, DataFormatException {
		// Discard bits to align to byte boundary
		input.align();
		
		// Read length
		int len  = input.readInt(16);
		int nlen = input.readInt(16);
		if ((len ^ 0xFFFF) != nlen)
			throw new DataFormatException("Invalid length in uncompressed block");
		
		// Copy bytes
		for (int i = 0; i < len; i++) {
			int temp = input.readByte();
			if (temp == -1)
				throw new EOFException();
			output.write(temp);
			dictionary.append(temp);
		}
	}
	
	
	private void decompressHuffmanBlock(CodeTree litLenCode, CodeTree distCode) throws IOException, DataFormatException {
		if (litLenCode == null)
			throw new NullPointerException();
		
		while (true) {
			int sym = decodeSymbol(litLenCode);
			if (sym == 256)  // End of block
				break;
			
			if (sym < 256) {  // Literal byte
				output.write(sym);
				dictionary.append(sym);
			} else {  // Length and distance for copying
				int len = decodeRunLength(sym);
				if (distCode == null)
					throw new DataFormatException("Length symbol encountered with empty distance code");
				int distSym = decodeSymbol(distCode);
				int dist = decodeDistance(distSym);
				dictionary.copy(dist, len, output);
			}
		}
	}
	
	
	/* Symbol decoding methods */

	// read up to depth of tree
	private int decodeSymbol(CodeTree code) throws IOException {
		InternalNode currentNode = code.root;
		while (true) {
			int temp = input.readBit() ? 1 : 0;
			Node nextNode;
			if      (temp == 0) nextNode = currentNode.leftChild;
			else if (temp == 1) nextNode = currentNode.rightChild;
			else throw new AssertionError();
			
			if (nextNode instanceof Leaf)
				return ((Leaf)nextNode).symbol;
			else if (nextNode instanceof InternalNode)
				currentNode = (InternalNode)nextNode;
			else
				throw new AssertionError();
		}
	}
	
	// read 1..5 bits
	private int decodeRunLength(int sym) throws IOException, DataFormatException {
		if (sym < 257 || sym > 285)
			throw new DataFormatException("Invalid run length symbol: " + sym);
		else if (sym <= 264)
			return sym - 254;
		else if (sym <= 284) {
			int i = (sym - 261) / 4;  // Number of extra bits to read, 1..5
			return (((sym - 265) % 4 + 4) << i) + 3 + input.readInt(i);
		} else  // sym == 285
			return 258;
	}
	
	// read 1..13 bits
	private int decodeDistance(int sym) throws IOException, DataFormatException {
		if (sym <= 3)
			return sym + 1;
		else if (sym <= 29) {
			int i = sym / 2 - 1;  // Number of extra bits to read, 1..13
			return ((sym % 2 + 2) << i) + 1 + input.readInt(i);
		} else
			throw new DataFormatException("Invalid distance symbol: " + sym);
	}
	
}
