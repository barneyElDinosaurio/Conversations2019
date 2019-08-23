package xx.xxx.xxxx.xmpp;

import xx.xxx.xxxx.entities.Account;

public interface OnMessageAcknowledged {
	public void onMessageAcknowledged(Account account, String id);
}
