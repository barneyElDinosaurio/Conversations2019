package xx.xxx.xxxx.xmpp;

import xx.xxx.xxxx.crypto.axolotl.AxolotlService;

public interface OnKeyStatusUpdated {
	public void onKeyStatusUpdated(AxolotlService.FetchStatus report);
}
