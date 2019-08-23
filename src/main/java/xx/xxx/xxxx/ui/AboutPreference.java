package xx.xxx.xxxx.ui;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.Toast;

import xx.xxx.xxxx.utils.PhoneHelper;

public class AboutPreference extends Preference {
	public AboutPreference(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		setSummary();
	}

	public AboutPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		setSummary();
	}

    @Override
    protected void onClick() {
        super.onClick();
       // final Intent intent = new Intent(getContext(), AboutActivity.class);
        //getContext().startActivity(intent);
		//Toast.makeText(getContext(), "Gracias", Toast.LENGTH_SHORT).show();
	}

    private void setSummary() {
		setSummary("Conversations " + PhoneHelper.getVersionName(getContext()));
	}
}

