package xx.xxx.xxxx.xmpp.jingle;

import xx.xxx.xxxx.entities.DownloadableFile;

public interface OnFileTransmissionStatusChanged {
	void onFileTransmitted(DownloadableFile file);

	void onFileTransferAborted();
}
