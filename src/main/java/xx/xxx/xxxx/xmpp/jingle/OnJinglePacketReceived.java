package xx.xxx.xxxx.xmpp.jingle;

import xx.xxx.xxxx.entities.Account;
import xx.xxx.xxxx.xmpp.PacketReceived;
import xx.xxx.xxxx.xmpp.jingle.stanzas.JinglePacket;

public interface OnJinglePacketReceived extends PacketReceived {
	void onJinglePacketReceived(Account account, JinglePacket packet);
}
