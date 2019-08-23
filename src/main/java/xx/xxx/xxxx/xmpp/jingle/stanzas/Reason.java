package xx.xxx.xxxx.xmpp.jingle.stanzas;

import xx.xxx.xxxx.xml.Element;

public class Reason extends Element {
	private Reason(String name) {
		super(name);
	}

	public Reason() {
		super("reason");
	}
}
