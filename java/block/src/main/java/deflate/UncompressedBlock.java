package deflate;

import java.io.IOException;
import java.io.OutputStream;

import bits.BitInput;
import boundary.InvalidBlockException;

public class UncompressedBlock extends DeflateBlock {
	
	public UncompressedBlock(boolean isFinal) {
		super(isFinal);
	}
	
	public void parseHeader(BitInput bits) throws InvalidBlockException, IOException {
		header_len += bits.align(); // align to byte boundary
		header_len += 32; // 16 for len, 16 for one's complement of len

		payload_len = bits.readInt(16);
		int complement = bits.readInt(16);
		if ((payload_len ^ 0xFFFF) != complement)
			throw new InvalidBlockException("Invalid length in uncompressed block");
	}

	@Override
	public void findPayloadLength(BitInput in) throws InvalidBlockException, IOException {
		// no op, already got it without parsing
	}

	@Override
	public void decode(BitInput in, CircularDictionary dict, OutputStream out) throws InvalidBlockException, IOException {
		// Copy bytes straight to output
		for (int i = 0; i < payload_len; i++) {
			int temp = in.readByte();
			if (out != null)
				out.write(temp);
			if (dict != null)
				dict.append(temp);
		}
		
	}

}
