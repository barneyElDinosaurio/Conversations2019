package xx.xxx.xxxx.crypto.axolotl;

public class CryptoFailedException extends Exception {

	public CryptoFailedException(String msg) {
		super(msg);
	}

	public CryptoFailedException(Exception e){
		super(e);
	}
}
