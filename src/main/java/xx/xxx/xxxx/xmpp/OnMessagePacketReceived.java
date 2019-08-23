package xx.xxx.xxxx.xmpp;

import xx.xxx.xxxx.entities.Account;
import xx.xxx.xxxx.xmpp.stanzas.MessagePacket;

public interface OnMessagePacketReceived extends PacketReceived {
	public void onMessagePacketReceived(Account account, MessagePacket packet);
}
