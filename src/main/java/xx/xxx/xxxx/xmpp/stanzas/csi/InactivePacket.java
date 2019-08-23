package xx.xxx.xxxx.xmpp.stanzas.csi;

import xx.xxx.xxxx.xmpp.stanzas.AbstractStanza;

public class InactivePacket extends AbstractStanza {
	public InactivePacket() {
		super("inactive");
		setAttribute("xmlns", "urn:xmpp:csi:0");
	}
}
