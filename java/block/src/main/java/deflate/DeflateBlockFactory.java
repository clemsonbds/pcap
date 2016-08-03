package deflate;

import java.io.IOException;
import java.util.BitSet;

import bits.BitArrayInputStream;
import bits.BitInput;
import boundary.InvalidBlockException;

public class DeflateBlockFactory {
	// requires bitset with index 0 aligned on byte boundary
	public DeflateBlock parseHeader(BitInput bits) throws InvalidBlockException, IOException {
		boolean isFinal = bits.read();
		int blockType = bits.readInt(2);
		DeflateBlock block;

		switch (blockType) {
		case 0:
			block = new UncompressedBlock(isFinal);
			break;
		case 1:
			block = new FixedHuffmanBlock(isFinal);
			break;
		case 2:
			block = new DynamicHuffmanBlock(isFinal);
			break;
		default:
			throw new InvalidBlockException("invalid block type");
		}

		block.parseHeader(bits);
		return block;
	}

	public DeflateBlock parseHeader(BitSet bitset, int offset, int byte_alignment) throws InvalidBlockException, IOException {
		BitArrayInputStream stream = new BitArrayInputStream(bitset, offset, byte_alignment);
		return parseHeader(stream);
	}
}
