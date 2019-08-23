package xx.xxx.xxxx.xmpp.stanzas.csi;

import xx.xxx.xxxx.xmpp.stanzas.AbstractStanza;

public class ActivePacket extends AbstractStanza {
	public ActivePacket() {
		super("active");
		setAttribute("xmlns", "urn:xmpp:csi:0");
	}
}
