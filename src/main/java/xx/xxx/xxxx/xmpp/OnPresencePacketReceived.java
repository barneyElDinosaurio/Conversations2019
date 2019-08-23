package xx.xxx.xxxx.xmpp;

import xx.xxx.xxxx.entities.Account;
import xx.xxx.xxxx.xmpp.stanzas.PresencePacket;

public interface OnPresencePacketReceived extends PacketReceived {
	public void onPresencePacketReceived(Account account, PresencePacket packet);
}
