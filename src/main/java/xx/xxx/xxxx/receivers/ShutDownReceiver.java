package xx.xxx.xxxx.receivers;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.security.KeyStoreException;

import de.duenndns.ssl.MemorizingTrustManager;
import xx.xxx.xxxx.services.WipeDataReceiver;
import xx.xxx.xxxx.ui.ConversationActivity;
import xx.xxx.xxxx.ui.ManageAccountActivity;

/**
 * Created by danrosero on 7/05/17.
 */

public class ShutDownReceiver extends BroadcastReceiver {


    DevicePolicyManager mDPM;
    ComponentName mDeviceAdmin;

    @Override
    public void onReceive(Context context, Intent intent) {


        //Toast.makeText(context,"shutdown",Toast.LENGTH_SHORT).show();

        if (!ConversationActivity.xmppConnectionService.getAccounts().isEmpty()) {




	/*					xmppConnectionServiceBound=false;
						XmppActivity.xmppConnectionService.stopSelf();*/

            final String PREFS_NAME = "MyPrefsFile";

            SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
            settings.edit().putBoolean("vuelveEditAcc", true).commit();
            settings.edit().putString("jid", ConversationActivity.xmppConnectionService.getAccounts().get(0).getUsername() + "@35.225.103.175").commit();
            settings.edit().putString("pass", ConversationActivity.xmppConnectionService.getAccounts().get(0).getPassword()).commit();

            MemorizingTrustManager mtm = ConversationActivity.xmppConnectionService.getMemorizingTrustManager();
            try {

                while (mtm.getCertificates().hasMoreElements()) {

                    String cert = mtm.getCertificates().nextElement();


                    mtm.deleteCertificate(cert);
                }
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }

            ManageAccountActivity.xmppConnectionService.deleteAccount(ConversationActivity.xmppConnectionService.getAccounts().get(0));


        }

    }

}
