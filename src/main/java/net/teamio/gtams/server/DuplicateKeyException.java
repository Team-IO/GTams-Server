package net.teamio.gtams.server;

public class DuplicateKeyException extends RuntimeException {

	public DuplicateKeyException() {
	}

	public DuplicateKeyException(String arg0) {
		super(arg0);
	}

	public DuplicateKeyException(Throwable arg0) {
		super(arg0);
	}

	public DuplicateKeyException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public DuplicateKeyException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
