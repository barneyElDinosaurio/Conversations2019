package xx.xxx.xxxx.xmpp.stanzas.streammgmt;

import xx.xxx.xxxx.xmpp.stanzas.AbstractStanza;

public class RequestPacket extends AbstractStanza {

	public RequestPacket(int smVersion) {
		super("r");
		this.setAttribute("xmlns", "urn:xmpp:sm:" + smVersion);
	}

}
