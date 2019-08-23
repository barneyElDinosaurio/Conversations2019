package xx.xxx.xxxx.xmpp;

import xx.xxx.xxxx.entities.Account;
import xx.xxx.xxxx.xmpp.stanzas.IqPacket;

public interface OnIqPacketReceived extends PacketReceived {
	public void onIqPacketReceived(Account account, IqPacket packet);
}
