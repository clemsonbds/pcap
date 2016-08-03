package deflate;

import java.io.IOException;
import java.io.OutputStream;

import bits.BitInput;
import bits.MeasuredBitInput;
import boundary.InvalidBlockException;

public abstract class HuffmanBlock extends DeflateBlock {
	CodeTree litLenCode;
	CodeTree distCode;
	
	public HuffmanBlock(boolean isFinal) {
		super(isFinal);
	}

	public void findPayloadLength(BitInput in) throws InvalidBlockException, IOException {
		decode(in, null, null);
	}
	
	@Override
	public void decode(BitInput in, CircularDictionary dict, OutputStream out) throws InvalidBlockException, IOException {
		if (litLenCode == null)
			throw new NullPointerException();

		MeasuredBitInput bits = new MeasuredBitInput(in);
		
		while (true) {
			int sym = decodeSymbol(bits, litLenCode);
			if (sym == 256)  // End of block
				break;
			
			if (sym < 256) {  // Literal byte
				if (out != null)
					out.write(sym);
				if (dict != null)
					dict.append(sym);
			} else {  // Length and distance for copying
				if (distCode == null)
					throw new InvalidBlockException("Length symbol encountered with empty distance code");
				int len = decodeRunLength(bits, sym);
				int distSym = decodeSymbol(bits, distCode);
				int dist = decodeDistance(bits, distSym);

				if (len < 0 || dist < 1 || dist > 32768)
					throw new InvalidBlockException("LZ77 encoded lookup is incorrect");

				if (dict != null)
					dict.copy(dist, len, out);
			}
		}
		
		payload_len = bits.bitsRead();
	}

	// read up to depth of tree
	protected int decodeSymbol(BitInput bits, CodeTree code) throws IOException {
		InternalNode currentNode = code.root;
		while (true) {
			int temp = bits.read() ? 1 : 0;
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
	private int decodeRunLength(BitInput bits, int sym) throws InvalidBlockException, IOException {
		if (sym < 257 || sym > 285)
			throw new InvalidBlockException("Invalid run length symbol: " + sym);
		else if (sym <= 264)
			return sym - 254;
		else if (sym <= 284) {
			int i = (sym - 261) / 4;  // Number of extra bits to read, 1..5
			return (((sym - 265) % 4 + 4) << i) + 3 + bits.readInt(i);
		} else  // sym == 285
			return 258;
	}
	
	// read 1..13 bits
	private int decodeDistance(BitInput bits, int sym) throws InvalidBlockException, IOException {
		if (sym <= 3)
			return sym + 1;
		else if (sym <= 29) {
			int i = sym / 2 - 1;  // Number of extra bits to read, 1..13
			return ((sym % 2 + 2) << i) + 1 + bits.readInt(i);
		} else
			throw new InvalidBlockException("Invalid distance symbol: " + sym);
	}

}
