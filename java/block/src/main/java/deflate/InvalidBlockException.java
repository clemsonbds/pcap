package deflate;

public class InvalidBlockException extends Exception {
	public InvalidBlockException(String reason) {
		super(reason);
	}
}
