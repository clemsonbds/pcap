package block;

import java.io.DataInputStream;

public abstract class AbstractBoundaryDetector {
	protected DataInputStream stream;

	public AbstractBoundaryDetector(DataInputStream stream) {
		this.stream = stream;
	}
	
}
