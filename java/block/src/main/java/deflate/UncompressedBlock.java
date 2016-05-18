package deflate;

import java.util.BitSet;
import java.util.zip.DataFormatException;

public class UncompressedBlock extends DeflateBlock {

	public UncompressedBlock(boolean isFinal) {
		super(isFinal);
	}

}
