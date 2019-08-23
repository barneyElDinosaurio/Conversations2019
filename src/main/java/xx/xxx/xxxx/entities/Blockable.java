package xx.xxx.xxxx.entities;

import xx.xxx.xxxx.xmpp.jid.Jid;

public interface Blockable {
	boolean isBlocked();
	boolean isDomainBlocked();
	Jid getBlockedJid();
	Jid getJid();
	Account getAccount();
}
