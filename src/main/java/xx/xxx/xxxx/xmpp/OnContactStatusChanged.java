package xx.xxx.xxxx.xmpp;

import xx.xxx.xxxx.entities.Contact;

public interface OnContactStatusChanged {
	public void onContactStatusChanged(final Contact contact, final boolean online);
}
