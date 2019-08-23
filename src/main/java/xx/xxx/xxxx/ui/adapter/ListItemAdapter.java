package xx.xxx.xxxx.ui.adapter;

import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wefika.flowlayout.FlowLayout;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import xx.xxx.xxxx.R;
import xx.xxx.xxxx.entities.ListItem;
import xx.xxx.xxxx.entities.Message;
import xx.xxx.xxxx.services.NotificationService;
import xx.xxx.xxxx.services.XmppConnectionService;
import xx.xxx.xxxx.ui.StartConversationActivity;
import xx.xxx.xxxx.ui.XmppActivity;
import xx.xxx.xxxx.utils.UIHelper;

public class ListItemAdapter extends ArrayAdapter<ListItem> {

	public static TextView tvName ;
	protected XmppActivity activity;
	protected boolean showDynamicTags = false;
	private View.OnClickListener onTagTvClick = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			if (view instanceof  TextView && mOnTagClickedListener != null) {
				TextView tv = (TextView) view;
				final String tag = tv.getText().toString();
				mOnTagClickedListener.onTagClicked(tag);
			}
		}
	};
	private OnTagClickedListener mOnTagClickedListener = null;

	public ListItemAdapter(XmppActivity activity, List<ListItem> objects) {
		super(activity, 0, objects);
		this.activity = activity;
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		this.showDynamicTags = preferences.getBoolean("show_dynamic_tags",true);//dynamic tags
	}

	TextView tv;

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ListItem item = getItem(position);
		if (view == null) {
			view = inflater.inflate(R.layout.contact, parent, false);
		}
		tvName = (TextView) view.findViewById(R.id.contact_display_name);
		TextView tvJid = (TextView) view.findViewById(R.id.contact_jid);
		ImageView picture = (ImageView) view.findViewById(R.id.contact_photo);
		FlowLayout tagLayout = (FlowLayout) view.findViewById(R.id.tags);

		picture.setVisibility(View.GONE);//Se van imagenes de chat

		tv = (TextView) inflater.inflate(R.layout.list_item_tag,tagLayout,false);

		List<ListItem.Tag> tags = item.getTags(activity);
		tagLayout.setVisibility(View.VISIBLE);

		if (tags.size() == 0 || !this.showDynamicTags) {
			//tagLayout.setVisibility(View.GONE);
//			tagLayout.removeAllViewsInLayout();

			tv.setText("Desconectado");
			tv.setTextColor(Color.parseColor("#a6a6a6"));
			//tv.setBackgroundColor(Color.parseColor("#00a201"));


		} else if(tags.size() > 0 || this.showDynamicTags) {
//			tagLayout.removeAllViewsInLayout();
//			tagLayout.removeAllViewsInLayout();

			tv.setText("Disponible");
			tv.setTextColor(Color.parseColor("#03960e"));
			//tv.setBackgroundColor(Color.parseColor("#05c22b"));

		}

		if (item.getDisplayName().equals(NotificationService.nombre)) {
			//Log.e("QUIENRECIBE", item.getDisplayName());
//			tagLayout.removeAllViewsInLayout();

			tv.setText("Mensaje");
			tv.setTextColor(Color.parseColor("#039dd6"));
			//tv.setBackgroundColor(Color.parseColor("#4747fa"));
			NotificationService.nombre = null;
		}

		if(XmppConnectionService.lastRead != null && item.getDisplayName().equals(XmppConnectionService.lastRead)) {


			tv.setText("Disponible");
			tv.setTextColor(Color.parseColor("#03960e"));
			//			tv.setBackgroundColor(Color.parseColor("#05c22b"));
			XmppConnectionService.lastRead = null;
		}


		tagLayout.removeAllViewsInLayout();
		tagLayout.addView(tv);

		//3 dias de DEBUG
		//tagLayout.setVisibility(View.VISIBLE);
		//tv = (TextView) inflater.inflate(R.layout.list_item_tag,tagLayout,false);






		final String jid = item.getDisplayJid();
		if (jid != null) {
			tvJid.setVisibility(View.VISIBLE);
			tvJid.setText(jid);
		} else {
			tvJid.setVisibility(View.GONE);
		}



		tvName.setText(item.getDisplayName());
		tvName.setTextColor(Color.parseColor("#ffffff"));//user text color

		loadAvatar(item,picture);
		tvJid.setVisibility(View.GONE);//Se van imagenes de chat
		return view;
	}

	public void cambiaVisaje(){
		Log.e("Algo","algo");
	}

	public void setOnTagClickedListener(OnTagClickedListener listener) {
		this.mOnTagClickedListener = listener;
	}

	public interface OnTagClickedListener {
		void onTagClicked(String tag);
	}

	class BitmapWorkerTask extends AsyncTask<ListItem, Void, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private ListItem item = null;

		public BitmapWorkerTask(ImageView imageView) {
			imageViewReference = new WeakReference<>(imageView);
		}

		@Override
		protected Bitmap doInBackground(ListItem... params) {
			return activity.avatarService().get(params[0], activity.getPixel(48), isCancelled());
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null && !isCancelled()) {
				final ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
					imageView.setBackgroundColor(0x00000000);
				}
			}
		}
	}

	private void cambiaTexto(){
		tvName.setTextColor(Color.BLUE);
	}

	public void loadAvatar(ListItem item, ImageView imageView) {
		if (cancelPotentialWork(item, imageView)) {
			final Bitmap bm = activity.avatarService().get(item,activity.getPixel(48),true);
			if (bm != null) {
				cancelPotentialWork(item, imageView);
				imageView.setImageBitmap(bm);
				imageView.setBackgroundColor(0x00000000);
			} else {
				imageView.setBackgroundColor(UIHelper.getColorForName(item.getDisplayName()));
				imageView.setImageDrawable(null);
				final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
				final AsyncDrawable asyncDrawable = new AsyncDrawable(activity.getResources(), null, task);
				imageView.setImageDrawable(asyncDrawable);
				try {
					task.execute(item);
				} catch (final RejectedExecutionException ignored) {
				}
			}
		}
	}

	public static boolean cancelPotentialWork(ListItem item, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final ListItem oldItem = bitmapWorkerTask.item;
			if (oldItem == null || item != oldItem) {
				bitmapWorkerTask.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}

	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}




}
