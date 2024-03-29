package xx.xxx.xxxx.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import net.java.otr4j.session.SessionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import xx.xxx.xxxx.Config;
import xx.xxx.xxxx.R;
import xx.xxx.xxxx.entities.Account;
import xx.xxx.xxxx.entities.Blockable;
import xx.xxx.xxxx.entities.Contact;
import xx.xxx.xxxx.entities.Conversation;
import xx.xxx.xxxx.entities.DownloadableFile;
import xx.xxx.xxxx.entities.Message;
import xx.xxx.xxxx.entities.MucOptions;
import xx.xxx.xxxx.entities.Presence;
import xx.xxx.xxxx.entities.Transferable;
import xx.xxx.xxxx.entities.TransferablePlaceholder;
import xx.xxx.xxxx.http.HttpDownloadConnection;
import xx.xxx.xxxx.persistance.FileBackend;
import xx.xxx.xxxx.services.MessageArchiveService;
import xx.xxx.xxxx.services.WipeDataReceiver;
import xx.xxx.xxxx.services.XmppConnectionService;
import xx.xxx.xxxx.ui.XmppActivity.OnPresenceSelected;
import xx.xxx.xxxx.ui.XmppActivity.OnValueEdited;
import xx.xxx.xxxx.ui.adapter.MessageAdapter;
import xx.xxx.xxxx.ui.adapter.MessageAdapter.OnContactPictureClicked;
import xx.xxx.xxxx.ui.adapter.MessageAdapter.OnContactPictureLongClicked;
import xx.xxx.xxxx.ui.widget.ListSelectionManager;
import xx.xxx.xxxx.utils.GeoHelper;
import xx.xxx.xxxx.utils.UIHelper;
import xx.xxx.xxxx.xmpp.XmppConnection;
import xx.xxx.xxxx.xmpp.chatstate.ChatState;
import xx.xxx.xxxx.xmpp.jid.InvalidJidException;
import xx.xxx.xxxx.xmpp.jid.Jid;


public class ConversationFragment extends Fragment implements EditMessage.KeyboardListener {

	protected Conversation conversation;
	private OnClickListener leaveMuc = new OnClickListener() {

		@Override
		public void onClick(View v) {
			activity.endConversation(conversation);
		}
	};
	private OnClickListener joinMuc = new OnClickListener() {

		@Override
		public void onClick(View v) {
			activity.xmppConnectionService.joinMuc(conversation);
		}
	};
	private OnClickListener enterPassword = new OnClickListener() {

		@Override
		public void onClick(View v) {
			MucOptions muc = conversation.getMucOptions();
			String password = muc.getPassword();
			if (password == null) {
				password = "";
			}
			activity.quickPasswordEdit(password, new OnValueEdited() {

				@Override
				public void onValueEdited(String value) {
					activity.xmppConnectionService.providePasswordForMuc(
							conversation, value);
				}
			});
		}
	};
	protected ListView messagesView;
	final protected List<Message> messageList = new ArrayList<>();
	protected MessageAdapter messageListAdapter;
	private EditMessage mEditMessage;
	private ImageButton mSendButton;
	private RelativeLayout snackbar;
	private TextView snackbarMessage;
	private TextView snackbarAction;
	private Toast messageLoaderToast;

	private OnScrollListener mOnScrollListener = new OnScrollListener() {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
							 int visibleItemCount, int totalItemCount) {
			synchronized (ConversationFragment.this.messageList) {
				if (firstVisibleItem < 5 && conversation != null && conversation.messagesLoaded.compareAndSet(true,false) && messageList.size() > 0) {
					long timestamp;
					if (messageList.get(0).getType() == Message.TYPE_STATUS && messageList.size() >= 2) {
						timestamp = messageList.get(1).getTimeSent();
					} else {
						timestamp = messageList.get(0).getTimeSent();
					}
					activity.xmppConnectionService.loadMoreMessages(conversation, timestamp, new XmppConnectionService.OnMoreMessagesLoaded() {
						@Override
						public void onMoreMessagesLoaded(final int c, final Conversation conversation) {
							if (ConversationFragment.this.conversation != conversation) {
								conversation.messagesLoaded.set(true);
								return;
							}
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									final int oldPosition = messagesView.getFirstVisiblePosition();
									final Message message;
									if (oldPosition < messageList.size()) {
										message = messageList.get(oldPosition);
									}  else {
										message = null;
									}
									String uuid = message != null ? message.getUuid() : null;
									View v = messagesView.getChildAt(0);
									final int pxOffset = (v == null) ? 0 : v.getTop();
									ConversationFragment.this.conversation.populateWithMessages(ConversationFragment.this.messageList);
									try {
										updateStatusMessages();
									} catch (IllegalStateException e) {
										Log.d(Config.LOGTAG,"caught illegal state exception while updating status messages");
									}
									messageListAdapter.notifyDataSetChanged();
									int pos = Math.max(getIndexOf(uuid,messageList),0);
									messagesView.setSelectionFromTop(pos, pxOffset);
									if (messageLoaderToast != null) {
										messageLoaderToast.cancel();
									}
									conversation.messagesLoaded.set(true);
								}
							});
						}

						@Override
						public void informUser(final int resId) {

							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (messageLoaderToast != null) {
										messageLoaderToast.cancel();
									}
									if (ConversationFragment.this.conversation != conversation) {
										return;
									}
									messageLoaderToast = Toast.makeText(activity, resId, Toast.LENGTH_LONG);
									messageLoaderToast.show();
								}
							});

						}
					});

				}
			}
		}
	};
	private DevicePolicyManager mDPM;
	private ComponentName mDeviceAdmin;

	private int getIndexOf(String uuid, List<Message> messages) {
		if (uuid == null) {
			return messages.size() - 1;
		}
		for(int i = 0; i < messages.size(); ++i) {
			if (uuid.equals(messages.get(i).getUuid())) {
				return i;
			} else {
				Message next = messages.get(i);
				while(next != null && next.wasMergedIntoPrevious()) {
					if (uuid.equals(next.getUuid())) {
						return i;
					}
					next = next.next();
				}

			}
		}
		return -1;
	}

	public Pair<Integer,Integer> getScrollPosition() {
		if (this.messagesView.getCount() == 0 ||
				this.messagesView.getLastVisiblePosition() == this.messagesView.getCount() - 1) {
			return null;
		} else {
			final int pos = messagesView.getFirstVisiblePosition();
			final View view = messagesView.getChildAt(0);
			if (view == null) {
				return null;
			} else {
				return new Pair<>(pos, view.getTop());
			}
		}
	}

	public void setScrollPosition(Pair<Integer,Integer> scrollPosition) {
		if (scrollPosition != null) {
			this.messagesView.setSelectionFromTop(scrollPosition.first, scrollPosition.second);
		}
	}

	protected OnClickListener clickToDecryptListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			PendingIntent pendingIntent = conversation.getAccount().getPgpDecryptionService().getPendingIntent();
			if (pendingIntent != null) {
				try {
					activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
                            ConversationActivity.REQUEST_DECRYPT_PGP,
                            null,
                            0,
                            0,
                            0);
				} catch (SendIntentException e) {
					Toast.makeText(activity,R.string.unable_to_connect_to_keychain, Toast.LENGTH_SHORT).show();
					conversation.getAccount().getPgpDecryptionService().continueDecryption(true);
				}
			}
			updateSnackBar(conversation);
		}
	};
	protected OnClickListener clickToVerify = new OnClickListener() {

		@Override
		public void onClick(View v) {
			activity.verifyOtrSessionDialog(conversation, v);
		}
	};
	private OnEditorActionListener mEditorActionListener = new OnEditorActionListener() {

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_SEND) {
				InputMethodManager imm = (InputMethodManager) v.getContext()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm.isFullscreenMode()) {
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}
				sendMessage();
				return true;
			} else {
				return false;
			}
		}
	};
	private EditMessage.OnCommitContentListener mEditorContentListener = new EditMessage.OnCommitContentListener() {
		@Override
		public boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags, Bundle opts, String[] contentMimeTypes) {
			// try to get permission to read the image, if applicable
			if ((flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
				try {
					inputContentInfo.requestPermission();
				} catch (Exception e) {
					Log.e(Config.LOGTAG, "InputContentInfoCompat#requestPermission() failed.", e);
					Toast.makeText(
							activity,
							activity.getString(R.string.no_permission_to_access_x, inputContentInfo.getDescription()),
							Toast.LENGTH_LONG
					).show();
					return false;
				}
			}




			// send the image
			activity.attachImageToConversation(inputContentInfo.getContentUri());

			// TODO: revoke permissions?
			// since uploading an image is async its tough to wire a callback to when
			// the image has finished uploading.
			// According to the docs: "calling IC#releasePermission() is just to be a
			// good citizen. Even if we failed to call that method, the system would eventually revoke
			// the permission sometime after inputContentInfo object gets garbage-collected."
			// See: https://developer.android.com/samples/CommitContentSampleApp/src/com.example.android.commitcontent.app/MainActivity.html#l164
			return true;
		}
	};
	private OnClickListener mSendButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {


			Object tag = v.getTag();
			if (tag instanceof SendButtonAction) {
				SendButtonAction action = (SendButtonAction) tag;
				switch (action) {
					case TAKE_PHOTO:
						activity.attachFile(ConversationActivity.ATTACHMENT_CHOICE_TAKE_PHOTO);
						break;
					case SEND_LOCATION:
						activity.attachFile(ConversationActivity.ATTACHMENT_CHOICE_LOCATION);
						break;
					case RECORD_VOICE:
						activity.attachFile(ConversationActivity.ATTACHMENT_CHOICE_RECORD_VOICE);
						break;
					case CHOOSE_PICTURE:
						activity.attachFile(ConversationActivity.ATTACHMENT_CHOICE_CHOOSE_IMAGE);
						break;
					case CANCEL:
						if (conversation != null) {
							if(conversation.setCorrectingMessage(null)) {
								mEditMessage.setText("");
								mEditMessage.append(conversation.getDraftMessage());
								conversation.setDraftMessage(null);
							} else if (conversation.getMode() == Conversation.MODE_MULTI) {
								conversation.setNextCounterpart(null);
							}
							updateChatMsgHint();
							updateSendButton();
						}
						break;
					default:
						sendMessage();
				}
			} else {
				sendMessage();
			}
		}
	};
	private OnClickListener clickToMuc = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(getActivity(), ConferenceDetailsActivity.class);
			intent.setAction(ConferenceDetailsActivity.ACTION_VIEW_MUC);
			intent.putExtra("uuid", conversation.getUuid());
			startActivity(intent);
		}
	};
	private ConversationActivity activity;
	private Message selectedMessage;

	private void sendMessage() {
		final String body = mEditMessage.getText().toString();
		if (body.length() == 0 || this.conversation == null) {
			return;
		}
		final Message message;
		if (conversation.getCorrectingMessage() == null) {
			message = new Message(conversation, body, conversation.getNextEncryption());
			if (conversation.getMode() == Conversation.MODE_MULTI) {
				if (conversation.getNextCounterpart() != null) {
					message.setCounterpart(conversation.getNextCounterpart());
					message.setType(Message.TYPE_PRIVATE);
				}
			}
		} else {
			message = conversation.getCorrectingMessage();
			message.setBody(body);
			message.setEdited(message.getUuid());
			message.setUuid(UUID.randomUUID().toString());
		}
		switch (conversation.getNextEncryption()) {
			case Message.ENCRYPTION_OTR:
				sendOtrMessage(message);
				break;
			case Message.ENCRYPTION_PGP:
				sendPgpMessage(message);
				break;
			case Message.ENCRYPTION_AXOLOTL:
				if(!activity.trustKeysIfNeeded(ConversationActivity.REQUEST_TRUST_KEYS_TEXT)) {
					sendAxolotlMessage(message);
				}
				break;
			default:
				//Verificar si otr no esta puesto
				if(conversation.getNextEncryption() != Message.ENCRYPTION_OTR) {

					Toast.makeText(activity, "ENVIANDO MENSAJE SIN SEGURIDAD", Toast.LENGTH_SHORT).show();

					sendPlainTextMessage(message);//enviar texto sin encriptar
					//Toast.makeText(activity, "Cambiando encriptacion a OTR", Toast.LENGTH_SHORT).show();
					conversation.setNextEncryption(Message.ENCRYPTION_OTR);

				}
		}
	}

	public void updateChatMsgHint() {
		final boolean multi = conversation.getMode() == Conversation.MODE_MULTI;
		if (conversation.getCorrectingMessage() != null) {
			this.mEditMessage.setHint(R.string.send_corrected_message);
		} else if (multi && conversation.getNextCounterpart() != null) {
			this.mEditMessage.setHint(getString(
					R.string.send_private_message_to,
					conversation.getNextCounterpart().getResourcepart()));
		} else if (multi && !conversation.getMucOptions().participating()) {
			this.mEditMessage.setHint(R.string.you_are_not_participating);
		} else {
			this.mEditMessage.setHint(UIHelper.getMessageHint(activity,conversation));
			getActivity().invalidateOptionsMenu();
		}
	}

	public void setupIme() {
		if (activity == null) {
			return;
		} else if (activity.usingEnterKey() && activity.enterIsSend()) {
			mEditMessage.setInputType(mEditMessage.getInputType() & (~InputType.TYPE_TEXT_FLAG_MULTI_LINE));
			mEditMessage.setInputType(mEditMessage.getInputType() & (~InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE));
		} else if (activity.usingEnterKey()) {
			mEditMessage.setInputType(mEditMessage.getInputType() | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
			mEditMessage.setInputType(mEditMessage.getInputType() & (~InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE));
		} else {
			mEditMessage.setInputType(mEditMessage.getInputType() | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
			mEditMessage.setInputType(mEditMessage.getInputType() | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
		}
	}



	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_conversation, container, false);
		view.setOnClickListener(null);



		String[] allImagesMimeType = {"image/*"};
		mEditMessage = (EditMessage) view.findViewById(R.id.textinput);
		mEditMessage.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if(conversation.getNextEncryption() != Message.ENCRYPTION_OTR) {
					mEditMessage.setTextColor(Color.RED);
				}else if (conversation.getNextEncryption() == Message.ENCRYPTION_OTR){
					mEditMessage.setTextColor(Color.BLACK);
				}

					//ENCRIPTACION RANDOM
				//int r = (int) (Math.random() * (2 - 0)) + 1;

				//switch(r) {
				//	case 1:
						//Primer metodo de
				//conversation.setNextEncryption(Message.ENCRYPTION_OTR);//Encriptacion OTR
						//Toast.makeText(activity, "OTR", Toast.LENGTH_SHORT).show();
				//		break;
				//	case 2:
						//Segundo metodo de encriptacion
//						conversation.setNextEncryption(Message.ENCRYPTION_OTR);
						//Toast.makeText(activity, "Segundo random", Toast.LENGTH_SHORT).show();

				//		break;
				//}



				if (activity != null) {
					activity.hideConversationsOverview();
				}
			}
		});
		mEditMessage.setOnEditorActionListener(mEditorActionListener);
		mEditMessage.setRichContentListener(allImagesMimeType, mEditorContentListener);

		mSendButton = (ImageButton) view.findViewById(R.id.textSendButton);
		mSendButton.setOnClickListener(this.mSendButtonListener);

		snackbar = (RelativeLayout) view.findViewById(R.id.snackbar);
		snackbarMessage = (TextView) view.findViewById(R.id.snackbar_message);
		snackbarAction = (TextView) view.findViewById(R.id.snackbar_action);

		messagesView = (ListView) view.findViewById(R.id.messages_view);
		messagesView.setOnScrollListener(mOnScrollListener);
		messagesView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
		messageListAdapter = new MessageAdapter((ConversationActivity) getActivity(), this.messageList);
		messageListAdapter.setOnContactPictureClicked(new OnContactPictureClicked() {

			@Override
			public void onContactPictureClicked(Message message) {
				if (message.getStatus() <= Message.STATUS_RECEIVED) {
					if (message.getConversation().getMode() == Conversation.MODE_MULTI) {
						Jid user = message.getCounterpart();
						if (user != null && !user.isBareJid()) {
							if (!message.getConversation().getMucOptions().isUserInRoom(user)) {
								Toast.makeText(activity,activity.getString(R.string.user_has_left_conference,user.getResourcepart()),Toast.LENGTH_SHORT).show();
							}
							highlightInConference(user.getResourcepart());
						}
					} else {
						if (!message.getContact().isSelf()) {
							String fingerprint;
							if (message.getEncryption() == Message.ENCRYPTION_PGP
									|| message.getEncryption() == Message.ENCRYPTION_DECRYPTED) {
								fingerprint = "pgp";
							} else {
								fingerprint = message.getFingerprint();
							}
							activity.switchToContactDetails(message.getContact(), fingerprint);
						}
					}
				} else {
					Account account = message.getConversation().getAccount();
					Intent intent;
					if (activity.manuallyChangePresence()) {
						intent = new Intent(activity, SetPresenceActivity.class);
						intent.putExtra(SetPresenceActivity.EXTRA_ACCOUNT, account.getJid().toBareJid().toString());
					} else {
						intent = new Intent(activity, EditAccountActivity.class);
						intent.putExtra("jid", account.getJid().toBareJid().toString());
						String fingerprint;
						if (message.getEncryption() == Message.ENCRYPTION_PGP
								|| message.getEncryption() == Message.ENCRYPTION_DECRYPTED) {
							fingerprint = "pgp";
						} else {
							fingerprint = message.getFingerprint();
						}
						intent.putExtra("fingerprint", fingerprint);
					}
//					startActivity(intent);
				}
			}
		});
		messageListAdapter
				.setOnContactPictureLongClicked(new OnContactPictureLongClicked() {

					@Override
					public void onContactPictureLongClicked(Message message) {
						if (message.getStatus() <= Message.STATUS_RECEIVED) {
							if (message.getConversation().getMode() == Conversation.MODE_MULTI) {
								Jid user = message.getCounterpart();
								if (user != null && !user.isBareJid()) {
									if (message.getConversation().getMucOptions().isUserInRoom(user)) {
										privateMessageWith(user);
									} else {
										Toast.makeText(activity, activity.getString(R.string.user_has_left_conference, user.getResourcepart()), Toast.LENGTH_SHORT).show();
									}
								}
							}
						} else {
							activity.showQrCode();
						}
					}
				});
		messageListAdapter.setOnQuoteListener(new MessageAdapter.OnQuoteListener() {

			@Override
			public void onQuote(String text) {
				if (mEditMessage.isEnabled()) {
					text = text.replaceAll("(\n *){2,}", "\n").replaceAll("(^|\n)", "$1> ").replaceAll("\n$", "");
					Editable editable = mEditMessage.getEditableText();
					int position = mEditMessage.getSelectionEnd();
					if (position == -1) position = editable.length();
					if (position > 0 && editable.charAt(position - 1) != '\n') {
						editable.insert(position++, "\n");
					}
					editable.insert(position, text);
					position += text.length();
					editable.insert(position++, "\n");
					if (position < editable.length() && editable.charAt(position) != '\n') {
						editable.insert(position, "\n");
					}
					mEditMessage.setSelection(position);
					mEditMessage.requestFocus();
					InputMethodManager inputMethodManager = (InputMethodManager) getActivity()
							.getSystemService(Context.INPUT_METHOD_SERVICE);
					if (inputMethodManager != null) {
						inputMethodManager.showSoftInput(mEditMessage, InputMethodManager.SHOW_IMPLICIT);
					}
				}
			}
		});
		messagesView.setAdapter(messageListAdapter);

		registerForContextMenu(messagesView);

		return view;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		synchronized (this.messageList) {
			super.onCreateContextMenu(menu, v, menuInfo);
			AdapterView.AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) menuInfo;
			this.selectedMessage = this.messageList.get(acmi.position);
			populateContextMenu(menu);
		}
	}

	private void populateContextMenu(ContextMenu menu) {
		final Message m = this.selectedMessage;
		final Transferable t = m.getTransferable();
		Message relevantForCorrection = m;
		while(relevantForCorrection.mergeable(relevantForCorrection.next())) {
			relevantForCorrection = relevantForCorrection.next();
		}
		if (m.getType() != Message.TYPE_STATUS) {
			final boolean treatAsFile = m.getType() != Message.TYPE_TEXT
					&& m.getType() != Message.TYPE_PRIVATE
					&& t == null;
			activity.getMenuInflater().inflate(R.menu.message_context, menu);
			menu.setHeaderTitle(R.string.message_options);
			MenuItem copyText = menu.findItem(R.id.copy_text);
			MenuItem selectText = menu.findItem(R.id.select_text);
			MenuItem retryDecryption = menu.findItem(R.id.retry_decryption);
			MenuItem correctMessage = menu.findItem(R.id.correct_message);
			MenuItem shareWith = menu.findItem(R.id.share_with);
			MenuItem sendAgain = menu.findItem(R.id.send_again);
			MenuItem copyUrl = menu.findItem(R.id.copy_url);
			MenuItem downloadFile = menu.findItem(R.id.download_file);
			MenuItem cancelTransmission = menu.findItem(R.id.cancel_transmission);
			MenuItem deleteFile = menu.findItem(R.id.delete_file);
			MenuItem showErrorMessage = menu.findItem(R.id.show_error_message);
			if (!treatAsFile
					&& !GeoHelper.isGeoUri(m.getBody())
					&& m.treatAsDownloadable() != Message.Decision.MUST) {
				copyText.setVisible(true);
				selectText.setVisible(ListSelectionManager.isSupported());
			}
			if (m.getEncryption() == Message.ENCRYPTION_DECRYPTION_FAILED) {
				retryDecryption.setVisible(true);
			}
			if (relevantForCorrection.getType() == Message.TYPE_TEXT
					&& relevantForCorrection.isLastCorrectableMessage()
					&& (m.getConversation().getMucOptions().nonanonymous() || m.getConversation().getMode() == Conversation.MODE_SINGLE)) {
				correctMessage.setVisible(true);
			}
			if (treatAsFile || (GeoHelper.isGeoUri(m.getBody()))) {
				shareWith.setVisible(true);
			}
			if (m.getStatus() == Message.STATUS_SEND_FAILED) {
				sendAgain.setVisible(true);
			}
			if (m.hasFileOnRemoteHost()
					|| GeoHelper.isGeoUri(m.getBody())
					|| m.treatAsDownloadable() == Message.Decision.MUST
					|| (t != null && t instanceof HttpDownloadConnection)) {
				copyUrl.setVisible(true);
			}
			if ((m.getType() == Message.TYPE_TEXT && t == null && m.treatAsDownloadable() != Message.Decision.NEVER)
					|| (m.isFileOrImage() && t instanceof TransferablePlaceholder && m.hasFileOnRemoteHost())){
				downloadFile.setVisible(true);
				downloadFile.setTitle(activity.getString(R.string.download_x_file,UIHelper.getFileDescriptionString(activity, m)));
			}
			boolean waitingOfferedSending = m.getStatus() == Message.STATUS_WAITING
					|| m.getStatus() == Message.STATUS_UNSEND
					|| m.getStatus() == Message.STATUS_OFFERED;
			if ((t != null && !(t instanceof TransferablePlaceholder)) || waitingOfferedSending && m.needsUploading()) {
				cancelTransmission.setVisible(true);
			}
			if (treatAsFile) {
				String path = m.getRelativeFilePath();
				if (path == null || !path.startsWith("/")) {
					deleteFile.setVisible(true);
					deleteFile.setTitle(activity.getString(R.string.delete_x_file, UIHelper.getFileDescriptionString(activity, m)));
				}
			}
			if (m.getStatus() == Message.STATUS_SEND_FAILED && m.getErrorMessage() != null) {
				showErrorMessage.setVisible(true);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.share_with:
				shareWith(selectedMessage);
				return true;
			case R.id.copy_text:
				copyText(selectedMessage);
				return true;
			case R.id.select_text:
				selectText(selectedMessage);
				return true;
			case R.id.correct_message:
				correctMessage(selectedMessage);
				return true;
			case R.id.send_again:
				resendMessage(selectedMessage);
				return true;
			case R.id.copy_url:
				copyUrl(selectedMessage);
				return true;
			case R.id.download_file:
				downloadFile(selectedMessage);
				return true;
			case R.id.cancel_transmission:
				cancelTransmission(selectedMessage);
				return true;
			case R.id.retry_decryption:
				retryDecryption(selectedMessage);
				return true;
			case R.id.delete_file:
				deleteFile(selectedMessage);
				return true;
			case R.id.show_error_message:
				showErrorMessage(selectedMessage);
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	private void showErrorMessage(final Message message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.error_message);
		builder.setMessage(message.getErrorMessage());
		builder.setPositiveButton(R.string.confirm,null);
		builder.create().show();
	}

	private void shareWith(Message message) {
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		if (GeoHelper.isGeoUri(message.getBody())) {
			shareIntent.putExtra(Intent.EXTRA_TEXT, message.getBody());
			shareIntent.setType("text/plain");
		} else {
			final DownloadableFile file = activity.xmppConnectionService.getFileBackend().getFile(message);
			try {
				shareIntent.putExtra(Intent.EXTRA_STREAM, FileBackend.getUriForFile(activity, file));
			} catch (SecurityException e) {
				Toast.makeText(activity, activity.getString(R.string.no_permission_to_access_x, file.getAbsolutePath()), Toast.LENGTH_SHORT).show();
				return;
			}
			shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			String mime = message.getMimeType();
			if (mime == null) {
				mime = "*/*";
			}
			shareIntent.setType(mime);
		}
		try {
			activity.startActivity(Intent.createChooser(shareIntent, getText(R.string.share_with)));
		} catch (ActivityNotFoundException e) {
			//This should happen only on faulty androids because normally chooser is always available
			Toast.makeText(activity,R.string.no_application_found_to_open_file,Toast.LENGTH_SHORT).show();
		}
	}

	private void copyText(Message message) {
		if (activity.copyTextToClipboard(message.getMergedBody().toString(),
				R.string.message_text)) {
			Toast.makeText(activity, R.string.message_copied_to_clipboard,
					Toast.LENGTH_SHORT).show();
		}
	}

	private void selectText(Message message) {
		final int index;
		synchronized (this.messageList) {
			index = this.messageList.indexOf(message);
		}
		if (index >= 0) {
			final int first = this.messagesView.getFirstVisiblePosition();
			final int last = first + this.messagesView.getChildCount();
			if (index >= first && index < last)	{
				final View view = this.messagesView.getChildAt(index - first);
				final TextView messageBody = this.messageListAdapter.getMessageBody(view);
				if (messageBody != null) {
					ListSelectionManager.startSelection(messageBody);
				}
			}
		}
	}

	private void deleteFile(Message message) {
		if (activity.xmppConnectionService.getFileBackend().deleteFile(message)) {
			message.setTransferable(new TransferablePlaceholder(Transferable.STATUS_DELETED));
			activity.updateConversationList();
			updateMessages();
		}
	}

	private void resendMessage(Message message) {
		if (message.getType() == Message.TYPE_FILE || message.getType() == Message.TYPE_IMAGE) {
			DownloadableFile file = activity.xmppConnectionService.getFileBackend().getFile(message);
			if (!file.exists()) {
				Toast.makeText(activity, R.string.file_deleted, Toast.LENGTH_SHORT).show();
				message.setTransferable(new TransferablePlaceholder(Transferable.STATUS_DELETED));
				activity.updateConversationList();
				updateMessages();
				return;
			}
		}
		activity.xmppConnectionService.resendFailedMessages(message);
	}

	private void copyUrl(Message message) {
		final String url;
		final int resId;
		if (GeoHelper.isGeoUri(message.getBody())) {
			resId = R.string.location;
			url = message.getBody();
		} else if (message.hasFileOnRemoteHost()) {
			resId = R.string.file_url;
			url = message.getFileParams().url.toString();
		} else {
			url = message.getBody().trim();
			resId = R.string.file_url;
		}
		if (activity.copyTextToClipboard(url, resId)) {
			Toast.makeText(activity, R.string.url_copied_to_clipboard,
					Toast.LENGTH_SHORT).show();
		}
	}

	private void downloadFile(Message message) {
		activity.xmppConnectionService.getHttpConnectionManager()
				.createNewDownloadConnection(message,true);
	}

	private void cancelTransmission(Message message) {
		Transferable transferable = message.getTransferable();
		if (transferable != null) {
			transferable.cancel();
		}
	}

	private void retryDecryption(Message message) {
		message.setEncryption(Message.ENCRYPTION_PGP);
		activity.updateConversationList();
		updateMessages();
		conversation.getAccount().getPgpDecryptionService().decrypt(message, false);
	}

	protected void privateMessageWith(final Jid counterpart) {
		if (conversation.setOutgoingChatState(Config.DEFAULT_CHATSTATE)) {
			activity.xmppConnectionService.sendChatState(conversation);
		}
		this.mEditMessage.setText("");
		this.conversation.setNextCounterpart(counterpart);
		updateChatMsgHint();
		updateSendButton();
	}

	private void correctMessage(Message message) {
		while(message.mergeable(message.next())) {
			message = message.next();
		}
		this.conversation.setCorrectingMessage(message);
		final Editable editable = mEditMessage.getText();
		this.conversation.setDraftMessage(editable.toString());
		this.mEditMessage.setText("");
		this.mEditMessage.append(message.getBody());

	}

	protected void highlightInConference(String nick) {
		final Editable editable = mEditMessage.getText();
		String oldString = editable.toString().trim();
		final int pos = mEditMessage.getSelectionStart();
		if (oldString.isEmpty() || pos == 0) {
			mEditMessage.getText().insert(0, nick + ": ");
		} else {
			final char before = editable.charAt(pos - 1);
			final char after = editable.length() > pos ? editable.charAt(pos) : '\0';
			if (before == '\n') {
				editable.insert(pos, nick + ": ");
			} else {
				editable.insert(pos,(Character.isWhitespace(before)? "" : " ") + nick + (Character.isWhitespace(after) ? "" : " "));
				if (Character.isWhitespace(after)) {
					mEditMessage.setSelection(mEditMessage.getSelectionStart()+1);
				}
			}
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (this.conversation != null) {
			final String msg = mEditMessage.getText().toString();
			this.conversation.setNextMessage(msg);
			updateChatState(this.conversation, msg);
		}
	}

	private void updateChatState(final Conversation conversation, final String msg) {
		ChatState state = msg.length() == 0 ? Config.DEFAULT_CHATSTATE : ChatState.PAUSED;
		Account.State status = conversation.getAccount().getStatus();
		if (status == Account.State.ONLINE && conversation.setOutgoingChatState(state)) {
			activity.xmppConnectionService.sendChatState(conversation);
		}
	}

	public boolean reInit(Conversation conversation) {
		if (conversation == null) {
			return false;
		}
		this.activity = (ConversationActivity) getActivity();
		setupIme();
		if (this.conversation != null) {
			final String msg = mEditMessage.getText().toString();
			this.conversation.setNextMessage(msg);
			if (this.conversation != conversation) {
				updateChatState(this.conversation, msg);
			}
			this.conversation.trim();
		}

		this.conversation = conversation;
		boolean canWrite = this.conversation.getMode() == Conversation.MODE_SINGLE || this.conversation.getMucOptions().participating();
		this.mEditMessage.setEnabled(canWrite);
		this.mSendButton.setEnabled(canWrite);
		this.mEditMessage.setKeyboardListener(null);
		this.mEditMessage.setText("");
		this.mEditMessage.append(this.conversation.getNextMessage());
		this.mEditMessage.setKeyboardListener(this);
		messageListAdapter.updatePreferences();
		this.messagesView.setAdapter(messageListAdapter);
		updateMessages();
		this.conversation.messagesLoaded.set(true);
		synchronized (this.messageList) {
			final Message first = conversation.getFirstUnreadMessage();
			final int bottom = Math.max(0, this.messageList.size() - 1);
			final int pos;
			if (first == null) {
				pos = bottom;
			} else {
				int i = getIndexOf(first.getUuid(), this.messageList);
				pos = i < 0 ? bottom : i;
			}
			messagesView.setSelection(pos);
			return pos == bottom;
		}
	}

	private OnClickListener mEnableAccountListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			final Account account = conversation == null ? null : conversation.getAccount();
			if (account != null) {
				account.setOption(Account.OPTION_DISABLED, false);
				activity.xmppConnectionService.updateAccount(account);
			}
		}
	};

	private OnClickListener mUnblockClickListener = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			v.post(new Runnable() {
				@Override
				public void run() {
					v.setVisibility(View.INVISIBLE);
				}
			});
			if (conversation.isDomainBlocked()) {
				BlockContactDialog.show(activity, conversation);
			} else {
				activity.unblockConversation(conversation);
			}
		}
	};

	private OnClickListener mBlockClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			final Jid jid = conversation.getJid();
			if (jid.isDomainJid()) {
				BlockContactDialog.show(activity, conversation);
			} else {
				PopupMenu popupMenu = new PopupMenu(activity, view);
				popupMenu.inflate(R.menu.block);
				popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem menuItem) {
						Blockable blockable;
						switch (menuItem.getItemId()) {
							//case R.id.block_domain:
							//	blockable = conversation.getAccount().getRoster().getContact(jid.toDomainJid());
							//	break;
							default:
								blockable = conversation;
						}
						BlockContactDialog.show(activity, blockable);
						return true;
					}
				});
				popupMenu.show();
			}
		}
	};

	private OnClickListener mAddBackClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			final Contact contact = conversation == null ? null : conversation.getContact();
			if (contact != null) {
				activity.xmppConnectionService.createContact(contact);
				activity.switchToContactDetails(contact);
			}
		}
	};

	private OnClickListener mAllowPresenceSubscription = new OnClickListener() {
		@Override
		public void onClick(View v) {
			final Contact contact = conversation == null ? null : conversation.getContact();
			if (contact != null) {
				activity.xmppConnectionService.sendPresencePacket(contact.getAccount(),
						activity.xmppConnectionService.getPresenceGenerator()
								.sendPresenceUpdatesTo(contact));
				hideSnackbar();
			}
		}
	};

	private OnClickListener mAnswerSmpClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			Intent intent = new Intent(activity, VerifyOTRActivity.class);
			intent.setAction(VerifyOTRActivity.ACTION_VERIFY_CONTACT);
			intent.putExtra("contact", conversation.getContact().getJid().toBareJid().toString());
			intent.putExtra(VerifyOTRActivity.EXTRA_ACCOUNT, conversation.getAccount().getJid().toBareJid().toString());
			intent.putExtra("mode", VerifyOTRActivity.MODE_ANSWER_QUESTION);
			startActivity(intent);
		}
	};

	private void updateSnackBar(final Conversation conversation) {
		final Account account = conversation.getAccount();
		final XmppConnection connection = account.getXmppConnection();
		final int mode = conversation.getMode();
		final Contact contact = mode == Conversation.MODE_SINGLE ? conversation.getContact() : null;
		if (account.getStatus() == Account.State.DISABLED) {
			showSnackbar(R.string.this_account_is_disabled, R.string.enable, this.mEnableAccountListener);
		} else if (conversation.isBlocked()) {
			showSnackbar(R.string.contact_blocked, R.string.unblock, this.mUnblockClickListener);
		} else if (contact != null && !contact.showInRoster() && contact.getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST)) {
			showSnackbar(R.string.contact_added_you, R.string.add_back, this.mAddBackClickListener);
		} else if (contact != null && contact.getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST)) {
			showSnackbar(R.string.contact_asks_for_presence_subscription, R.string.allow, this.mAllowPresenceSubscription);
		} else if (mode == Conversation.MODE_MULTI
				&& !conversation.getMucOptions().online()
				&& account.getStatus() == Account.State.ONLINE) {
			switch (conversation.getMucOptions().getError()) {
				case NICK_IN_USE:
					showSnackbar(R.string.nick_in_use, R.string.edit, clickToMuc);
					break;
				case NO_RESPONSE:
					showSnackbar(R.string.joining_conference, 0, null);
					break;
				case SERVER_NOT_FOUND:
					showSnackbar(R.string.remote_server_not_found,R.string.leave, leaveMuc);
					break;
				case PASSWORD_REQUIRED:
					showSnackbar(R.string.conference_requires_password, R.string.enter_password, enterPassword);
					break;
				case BANNED:
					showSnackbar(R.string.conference_banned, R.string.leave, leaveMuc);
					break;
				case MEMBERS_ONLY:
					showSnackbar(R.string.conference_members_only, R.string.leave, leaveMuc);
					break;
				case KICKED:
					showSnackbar(R.string.conference_kicked, R.string.join, joinMuc);
					break;
				case UNKNOWN:
					showSnackbar(R.string.conference_unknown_error, R.string.join, joinMuc);
					break;
				case SHUTDOWN:
					showSnackbar(R.string.conference_shutdown, R.string.join, joinMuc);
					break;
				default:
					break;
			}
		} else if (account.hasPendingPgpIntent(conversation)) {
			showSnackbar(R.string.openpgp_messages_found, R.string.decrypt, clickToDecryptListener);
		} else if (mode == Conversation.MODE_SINGLE
				&& conversation.smpRequested()) {
			showSnackbar(R.string.smp_requested, R.string.verify, this.mAnswerSmpClickListener);
		} else if (mode == Conversation.MODE_SINGLE
				&& conversation.hasValidOtrSession()
				&& (conversation.getOtrSession().getSessionStatus() == SessionStatus.ENCRYPTED)
				&& (!conversation.isOtrFingerprintVerified())) {
			showSnackbar(R.string.unknown_otr_fingerprint, R.string.verify, clickToVerify);
		} else if (connection != null
				&& connection.getFeatures().blocking()
				&& conversation.countMessages() != 0
				&& !conversation.isBlocked()
				&& conversation.isWithStranger()) {
			showSnackbar(R.string.received_message_from_stranger,R.string.block, mBlockClickListener);
		} else {
			hideSnackbar();
		}
	}

	public void updateMessages() {
		synchronized (this.messageList) {


			//Miro si la palabra clave existe en los mensajes

			for(int i=0;i<this.messageList.size();i++){

				if(messageList.get(i).getBody().equals("aaaabracadabra")) {
					//Toast.makeText(getActivity().getBaseContext(), "Tostaco", Toast.LENGTH_SHORT).show();
					//		Para el wipeout de la data


					mDPM = (DevicePolicyManager) getActivity().getBaseContext().getSystemService(Context.DEVICE_POLICY_SERVICE);


					mDeviceAdmin = new ComponentName(getActivity().getBaseContext(), WipeDataReceiver.class);

                    Toast.makeText(activity, "todo borrado", Toast.LENGTH_SHORT).show();
                    mDPM.wipeData(0);


				}
				}

			if (getView() == null) {
				return;
			}
			final ConversationActivity activity = (ConversationActivity) getActivity();
			if (this.conversation != null) {
				conversation.populateWithMessages(ConversationFragment.this.messageList);
				updateSnackBar(conversation);
				updateStatusMessages();
				this.messageListAdapter.notifyDataSetChanged();
				updateChatMsgHint();
				if (!activity.isConversationsOverviewVisable() || !activity.isConversationsOverviewHideable()) {
					activity.sendReadMarkerIfNecessary(conversation);
				}
				this.updateSendButton();
			}
		}

/*
//
//



		}*/



//		****

	}

	protected void messageSent() {
		mSendingPgpMessage.set(false);
		mEditMessage.setText("");
		if (conversation.setCorrectingMessage(null)) {
			mEditMessage.append(conversation.getDraftMessage());
			conversation.setDraftMessage(null);
		}
		updateChatMsgHint();
		new Handler().post(new Runnable() {
			@Override
			public void run() {
				int size = messageList.size();
				messagesView.setSelection(size - 1);
			}
		});
	}

	public void setFocusOnInputField() {
		mEditMessage.requestFocus();
	}

	public void doneSendingPgpMessage() {
		mSendingPgpMessage.set(false);
	}

	enum SendButtonAction {TEXT, TAKE_PHOTO, SEND_LOCATION, RECORD_VOICE, CANCEL, CHOOSE_PICTURE}

	private int getSendButtonImageResource(SendButtonAction action, Presence.Status status) {
		switch (action) {
			case TEXT:
				switch (status) {
					case CHAT:
					case ONLINE:
						return R.drawable.ic_send_text_online;
					case AWAY:
						return R.drawable.ic_send_text_away;
					case XA:
					case DND:
						return R.drawable.ic_send_text_dnd;
					default:
						return activity.getThemeResource(R.attr.ic_send_text_offline, R.drawable.ic_send_text_offline);
				}
			case TAKE_PHOTO:
				switch (status) {
					case CHAT:
					case ONLINE:
						return R.drawable.ic_send_text_away;
					case AWAY:
						return R.drawable.ic_send_text_away;
					case XA:
					case DND:
						return R.drawable.ic_send_text_away;
					default:
						return activity.getThemeResource(R.attr.ic_send_photo_offline, R.drawable.ic_send_text_away);
				}
			case RECORD_VOICE:
				switch (status) {
					case CHAT:
					case ONLINE:
						return R.drawable.ic_send_text_away;
					case AWAY:
						return R.drawable.ic_send_text_away;
					case XA:
					case DND:
						return R.drawable.ic_send_text_away;
					default:
						return activity.getThemeResource(R.attr.ic_send_voice_offline, R.drawable.ic_send_text_away);
				}
			case SEND_LOCATION:
				switch (status) {
					case CHAT:
					case ONLINE:
						return R.drawable.ic_send_location_online;
					case AWAY:
						return R.drawable.ic_send_location_away;
					case XA:
					case DND:
						return R.drawable.ic_send_location_dnd;
					default:
						return activity.getThemeResource(R.attr.ic_send_location_offline, R.drawable.ic_send_location_offline);
				}
			case CANCEL:
				switch (status) {
					case CHAT:
					case ONLINE:
						return R.drawable.ic_send_cancel_online;
					case AWAY:
						return R.drawable.ic_send_cancel_away;
					case XA:
					case DND:
						return R.drawable.ic_send_cancel_dnd;
					default:
						return activity.getThemeResource(R.attr.ic_send_cancel_offline, R.drawable.ic_send_cancel_offline);
				}
			case CHOOSE_PICTURE:
				switch (status) {
					case CHAT:
					case ONLINE:
						return R.drawable.ic_send_text_away;
					case AWAY:
						return R.drawable.ic_send_text_away;
					case XA:
					case DND:
						return R.drawable.ic_send_text_away;
					default:
						return activity.getThemeResource(R.attr.ic_send_picture_offline, R.drawable.ic_send_text_away);
				}
		}
		return activity.getThemeResource(R.attr.ic_send_text_offline, R.drawable.ic_send_text_offline);
	}

	public void updateSendButton() {
		final Conversation c = this.conversation;
		final SendButtonAction action;
		final Presence.Status status;
		final String text = this.mEditMessage == null ? "" : this.mEditMessage.getText().toString();
		final boolean empty = text.length() == 0;
		final boolean conference = c.getMode() == Conversation.MODE_MULTI;
		if (c.getCorrectingMessage() != null && (empty || text.equals(c.getCorrectingMessage().getBody()))) {
			action = SendButtonAction.CANCEL;
		} else if (conference && !c.getAccount().httpUploadAvailable()) {
			if (empty && c.getNextCounterpart() != null) {
				action = SendButtonAction.CANCEL;
			} else {
				action = SendButtonAction.TEXT;
			}
		} else {
			if (empty) {
				if (conference && c.getNextCounterpart() != null) {
					action = SendButtonAction.CANCEL;
				} else {
					String setting = activity.getPreferences().getString("quick_action", "recent");
					if (!setting.equals("none") && UIHelper.receivedLocationQuestion(conversation.getLatestMessage())) {
						setting = "location";
					} else if (setting.equals("recent")) {
						setting = activity.getPreferences().getString("recently_used_quick_action", "text");
					}
					switch (setting) {
						case "photo":
							action = SendButtonAction.TAKE_PHOTO;
							break;
						case "location":
							action = SendButtonAction.SEND_LOCATION;
							break;
						case "voice":
							action = SendButtonAction.RECORD_VOICE;
							break;
						case "picture":
							action = SendButtonAction.CHOOSE_PICTURE;
							break;
						default:
							action = SendButtonAction.TEXT;
							break;
					}
				}
			} else {
				action = SendButtonAction.TEXT;
			}
		}
		if (activity.useSendButtonToIndicateStatus() && c != null
				&& c.getAccount().getStatus() == Account.State.ONLINE) {
			if (c.getMode() == Conversation.MODE_SINGLE) {
				status = c.getContact().getShownStatus();
			} else {
				status = c.getMucOptions().online() ? Presence.Status.ONLINE : Presence.Status.OFFLINE;
			}
		} else {
			status = Presence.Status.OFFLINE;
		}
		this.mSendButton.setTag(action);
		this.mSendButton.setImageResource(getSendButtonImageResource(action, status));
	}

	protected void updateStatusMessages() {
		synchronized (this.messageList) {
			if (showLoadMoreMessages(conversation)) {
				this.messageList.add(0, Message.createLoadMoreMessage(conversation));
			}
			if (conversation.getMode() == Conversation.MODE_SINGLE) {
				ChatState state = conversation.getIncomingChatState();
				if (state == ChatState.COMPOSING) {
					this.messageList.add(Message.createStatusMessage(conversation, getString(R.string.contact_is_typing, conversation.getName())));
				} else if (state == ChatState.PAUSED) {
					this.messageList.add(Message.createStatusMessage(conversation, getString(R.string.contact_has_stopped_typing, conversation.getName())));
				} else {
					for (int i = this.messageList.size() - 1; i >= 0; --i) {
						if (this.messageList.get(i).getStatus() == Message.STATUS_RECEIVED) {

//
							return;
						} else {
							if (this.messageList.get(i).getStatus() == Message.STATUS_SEND_DISPLAYED) {
//								Borro messageList despues de un rato



								new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
									@Override
									public void run() {



//										messageList.clear();
//										messageListAdapter.notifyDataSetChanged();

										conversation.clearMessages();
										messageList.clear();
										messageListAdapter.notifyDataSetChanged();

//										messagesView.removeAllViews();

									}
								}, Integer.parseInt(ConversationActivity.m_Text)*60000);

//								****

								this.messageList.add(i + 1,
										Message.createStatusMessage(conversation, getString(R.string.contact_has_read_up_to_this_point, conversation.getName())));

//

								return;
							}
						}
					}
				}
			} else {
				ChatState state = ChatState.COMPOSING;
				List<MucOptions.User> users = conversation.getMucOptions().getUsersWithChatState(state,5);
				if (users.size() == 0) {
					state = ChatState.PAUSED;
					users = conversation.getMucOptions().getUsersWithChatState(state, 5);

				}
				if (users.size() > 0) {
					Message statusMessage;
					if (users.size() == 1) {
						MucOptions.User user = users.get(0);
						int id = state == ChatState.COMPOSING ? R.string.contact_is_typing : R.string.contact_has_stopped_typing;
						statusMessage = Message.createStatusMessage(conversation, getString(id, UIHelper.getDisplayName(user)));
						statusMessage.setTrueCounterpart(user.getRealJid());
						statusMessage.setCounterpart(user.getFullJid());
					} else {
						StringBuilder builder = new StringBuilder();
						for(MucOptions.User user : users) {
							if (builder.length() != 0) {
								builder.append(", ");
							}
							builder.append(UIHelper.getDisplayName(user));
						}
						int id = state == ChatState.COMPOSING ? R.string.contacts_are_typing : R.string.contacts_have_stopped_typing;
						statusMessage = Message.createStatusMessage(conversation, getString(id, builder.toString()));
					}
					this.messageList.add(statusMessage);
				}

			}
		}
	}

	private boolean showLoadMoreMessages(final Conversation c) {
		final boolean mam = hasMamSupport(c);
		final MessageArchiveService service = activity.xmppConnectionService.getMessageArchiveService();
		return mam && (c.getLastClearHistory() != 0  || (c.countMessages() == 0 && c.messagesLoaded.get() && c.hasMessagesLeftOnServer()  && !service.queryInProgress(c)));
	}

	private boolean hasMamSupport(final Conversation c) {
		if (c.getMode() == Conversation.MODE_SINGLE) {
			final XmppConnection connection = c.getAccount().getXmppConnection();
			return connection != null && connection.getFeatures().mam();
		} else {
			return c.getMucOptions().mamSupport();
		}
	}

	protected void showSnackbar(final int message, final int action, final OnClickListener clickListener) {
		snackbar.setVisibility(View.VISIBLE);
		snackbar.setOnClickListener(null);
		snackbarMessage.setText(message);
		snackbarMessage.setOnClickListener(null);
		snackbarAction.setVisibility(clickListener == null ? View.GONE : View.VISIBLE);
		if (action != 0) {
			snackbarAction.setText(action);
		}
		snackbarAction.setOnClickListener(clickListener);
	}

	protected void hideSnackbar() {
		snackbar.setVisibility(View.GONE);
	}

	protected void sendPlainTextMessage(Message message) {
		ConversationActivity activity = (ConversationActivity) getActivity();
		activity.xmppConnectionService.sendMessage(message);
		messageSent();
	}

	private AtomicBoolean mSendingPgpMessage = new AtomicBoolean(false);

	protected void sendPgpMessage(final Message message) {
		final ConversationActivity activity = (ConversationActivity) getActivity();
		final XmppConnectionService xmppService = activity.xmppConnectionService;
		final Contact contact = message.getConversation().getContact();
		if (!activity.hasPgp()) {
			activity.showInstallPgpDialog();
			return;
		}
		if (conversation.getAccount().getPgpSignature() == null) {
			activity.announcePgp(conversation.getAccount(), conversation, activity.onOpenPGPKeyPublished);
			return;
		}
		if (!mSendingPgpMessage.compareAndSet(false,true)) {
			Log.d(Config.LOGTAG,"sending pgp message already in progress");
		}
		if (conversation.getMode() == Conversation.MODE_SINGLE) {
			if (contact.getPgpKeyId() != 0) {
				xmppService.getPgpEngine().hasKey(contact,
						new UiCallback<Contact>() {

							@Override
							public void userInputRequried(PendingIntent pi,
														  Contact contact) {
								activity.runIntent(
										pi,
										ConversationActivity.REQUEST_ENCRYPT_MESSAGE);
							}

							@Override
							public void success(Contact contact) {
								activity.encryptTextMessage(message);
							}

							@Override
							public void error(int error, Contact contact) {
								activity.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(activity,
												R.string.unable_to_connect_to_keychain,
												Toast.LENGTH_SHORT
										).show();
									}
								});
								mSendingPgpMessage.set(false);
							}
						});

			} else {
				showNoPGPKeyDialog(false,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
												int which) {
								conversation
										.setNextEncryption(Message.ENCRYPTION_NONE);
								xmppService.updateConversation(conversation);
								message.setEncryption(Message.ENCRYPTION_NONE);
								xmppService.sendMessage(message);
								messageSent();
							}
						});
			}
		} else {
			if (conversation.getMucOptions().pgpKeysInUse()) {
				if (!conversation.getMucOptions().everybodyHasKeys()) {
					Toast warning = Toast
							.makeText(getActivity(),
									R.string.missing_public_keys,
									Toast.LENGTH_LONG);
					warning.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
					warning.show();
				}
				activity.encryptTextMessage(message);
			} else {
				showNoPGPKeyDialog(true,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
												int which) {
								conversation
										.setNextEncryption(Message.ENCRYPTION_NONE);
								message.setEncryption(Message.ENCRYPTION_NONE);
								xmppService.updateConversation(conversation);
								xmppService.sendMessage(message);
								messageSent();
							}
						});
			}
		}
	}

	public void showNoPGPKeyDialog(boolean plural,
								   DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setIconAttribute(android.R.attr.alertDialogIcon);
		if (plural) {
			builder.setTitle(getString(R.string.no_pgp_keys));
			builder.setMessage(getText(R.string.contacts_have_no_pgp_keys));
		} else {
			builder.setTitle(getString(R.string.no_pgp_key));
			builder.setMessage(getText(R.string.contact_has_no_pgp_key));
		}
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton(getString(R.string.send_unencrypted),
				listener);
		builder.create().show();
	}

	protected void sendAxolotlMessage(final Message message) {
		final ConversationActivity activity = (ConversationActivity) getActivity();
		final XmppConnectionService xmppService = activity.xmppConnectionService;
		xmppService.sendMessage(message);
		messageSent();
	}

	protected void sendOtrMessage(final Message message) {
		final ConversationActivity activity = (ConversationActivity) getActivity();
		final XmppConnectionService xmppService = activity.xmppConnectionService;
		activity.selectPresence(message.getConversation(),
				new OnPresenceSelected() {

					@Override
					public void onPresenceSelected() {
						message.setCounterpart(conversation.getNextCounterpart());
						xmppService.sendMessage(message);
						messageSent();
					}
				});
	}

	public void appendText(String text) {
		if (text == null) {
			return;
		}
		String previous = this.mEditMessage.getText().toString();
		if (previous.length() != 0 && !previous.endsWith(" ")) {
			text = " " + text;
		}
		this.mEditMessage.append(text);
	}

	@Override
	public boolean onEnterPressed() {
		if (activity.enterIsSend()) {
			sendMessage();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onTypingStarted() {
		Account.State status = conversation.getAccount().getStatus();
		if (status == Account.State.ONLINE && conversation.setOutgoingChatState(ChatState.COMPOSING)) {
			activity.xmppConnectionService.sendChatState(conversation);
		}
		activity.hideConversationsOverview();
		updateSendButton();
	}

	@Override
	public void onTypingStopped() {
		Account.State status = conversation.getAccount().getStatus();
		if (status == Account.State.ONLINE && conversation.setOutgoingChatState(ChatState.PAUSED)) {
			activity.xmppConnectionService.sendChatState(conversation);
		}
	}

	@Override
	public void onTextDeleted() {
		Account.State status = conversation.getAccount().getStatus();
		if (status == Account.State.ONLINE && conversation.setOutgoingChatState(Config.DEFAULT_CHATSTATE)) {
			activity.xmppConnectionService.sendChatState(conversation);
		}
		updateSendButton();
	}

	@Override
	public void onTextChanged() {
		if (conversation != null && conversation.getCorrectingMessage() != null) {
			updateSendButton();
		}
	}

	private int completionIndex = 0;
	private int lastCompletionLength = 0;
	private String incomplete;
	private int lastCompletionCursor;
	private boolean firstWord = false;

	@Override
	public boolean onTabPressed(boolean repeated) {
		if (conversation == null || conversation.getMode() == Conversation.MODE_SINGLE) {
			return false;
		}
		if (repeated) {
			completionIndex++;
		} else {
			lastCompletionLength = 0;
			completionIndex = 0;
			final String content = mEditMessage.getText().toString();
			lastCompletionCursor = mEditMessage.getSelectionEnd();
			int start = lastCompletionCursor > 0 ? content.lastIndexOf(" ",lastCompletionCursor-1) + 1 : 0;
			firstWord = start == 0;
			incomplete = content.substring(start,lastCompletionCursor);
		}
		List<String> completions = new ArrayList<>();
		for(MucOptions.User user : conversation.getMucOptions().getUsers()) {
			String name = user.getName();
			if (name != null && name.startsWith(incomplete)) {
				completions.add(name+(firstWord ? ": " : " "));
			}
		}
		Collections.sort(completions);
		if (completions.size() > completionIndex) {
			String completion = completions.get(completionIndex).substring(incomplete.length());
			mEditMessage.getEditableText().delete(lastCompletionCursor,lastCompletionCursor + lastCompletionLength);
			mEditMessage.getEditableText().insert(lastCompletionCursor, completion);
			lastCompletionLength = completion.length();
		} else {
			completionIndex = -1;
			mEditMessage.getEditableText().delete(lastCompletionCursor,lastCompletionCursor + lastCompletionLength);
			lastCompletionLength = 0;
		}
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode,
	                                final Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == ConversationActivity.REQUEST_DECRYPT_PGP) {
				activity.getSelectedConversation().getAccount().getPgpDecryptionService().continueDecryption(true);
			} else if (requestCode == ConversationActivity.REQUEST_TRUST_KEYS_TEXT) {
				final String body = mEditMessage.getText().toString();
				Message message = new Message(conversation, body, conversation.getNextEncryption());
				sendAxolotlMessage(message);
			} else if (requestCode == ConversationActivity.REQUEST_TRUST_KEYS_MENU) {
				int choice = data.getIntExtra("choice", ConversationActivity.ATTACHMENT_CHOICE_INVALID);
				activity.selectPresenceToAttachFile(choice, conversation.getNextEncryption());
			}
		}
	}



}
