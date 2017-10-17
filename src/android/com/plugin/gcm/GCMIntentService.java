package com.plugin.gcm;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.AssetFileDescriptor;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.lang.StringBuilder;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import com.plugin.download.AppUpdater;
import com.plugin.download.FileDownloader;
import com.plugin.badge.ShortcutBadger;

public class GCMIntentService extends IntentService {
	public static int NOTIFICATION_ID = 1;
	
	//private static final String TAG = "GCMIntentService";
	
	public GCMIntentService() {
		super("GCMIntentService");
	}
	
	@Override
	public void onCreate() {
		ensureServiceStaysRunning();
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if ((intent != null) && (intent.getBooleanExtra("ALARM_RESTART_SERVICE_DIED", false))) {
		ensureServiceStaysRunning();
		return START_STICKY;
		}
		
		return START_STICKY;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		// The getMessageType() intent parameter must be the intent you received
		// in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);
		
		if (!extras.isEmpty()) { // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that GCM
			 * will be extended in the future with new message types, just ignore
			 * any message types you're not interested in, or that you don't
			 * recognize.
			 */
			// Regular GCM message, do some work.
			if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				// if we are in the foreground, just surface the payload, else post it to the statusbar
				if (PushPlugin.isInForeground()) {
					extras.putBoolean("foreground", true);
					PushPlugin.sendExtras(extras);		  
			} else {
				extras.putBoolean("foreground", false);
				String title = extras.getString("title");
				String message = extras.getString("message");
				title = title != null ? title : extras.getString("gcm.notification.title");
				message = message != null ? message : extras.getString("body");
				message = message != null ? message : extras.getString("gcm.notification.body");
				// Send a notification if there is a message. It can be in notification itself or in gcm.notification.*
				if ((title != null && title.length() != 0) || (message != null && message.length() != 0)) {
					createNotification(extras);
					}
				}
			}
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		CordovaGCMBroadcastReceiver.completeWakefulIntent(intent);
	}

	public void createNotification(Bundle extras) {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String appName = getAppName(this);
		
		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		try {
			NOTIFICATION_ID = Integer.parseInt(extras.getString("notId"));
		} catch (Exception e) {
			NOTIFICATION_ID += 1;
		}
		
		Notification.Builder mBuilder = new Notification.Builder(this);
		
		mBuilder.setWhen(System.currentTimeMillis());
		mBuilder.setContentIntent(contentIntent);
		
		// DEFAULTS (LIGHT, SOUND, VIBRATE)
		mBuilder.setDefaults(Notification.DEFAULT_ALL);
	
		// AUTOCANCEL
		String autoCancel = extras.getString("autoCancel");
		autoCancel = autoCancel != null ? autoCancel : "true";
		mBuilder.setAutoCancel(autoCancel.equals("true"));
		
		// TITLE
		String title = extras.getString("title");
		title = title != null ? title : extras.getString("gcm.notification.title");
		mBuilder.setContentTitle(title);
		mBuilder.setTicker(title);
		
		// MESSAGE
		String message = extras.getString("message");
		message = message != null ? message : extras.getString("gcm.notification.body");
		message = message != null ? message : "<missing message content>";
		mBuilder.setContentText(message);
		
		// SUBTEXT
		if (extras.containsKey("subtext")) {
			mBuilder.setSubText(
				extras.getString("subtext")
			);
		}
		
		// GROUP
		if (extras.containsKey("group")) {
			mBuilder
				// .setGroupSummary(true)
				.setGroup(
					extras.getString("group")
				);
		}
		
		// SMALL ICON
		String icon = extras.getString("icon");
		if (icon == null) {
			mBuilder.setSmallIcon(this.getApplicationInfo().icon);
		} else {
			String location = extras.getString("iconLocation");
			location = location != null ? location : "drawable";
			int rIcon = this.getResources().getIdentifier(icon.substring(0, icon.lastIndexOf('.')), location, this.getPackageName());
			if (rIcon > 0) {
				mBuilder.setSmallIcon(rIcon);
			} else {
				mBuilder.setSmallIcon(this.getApplicationInfo().icon);
			}
		}
		
		// ICON COLOR
		if (extras.containsKey("icon.color")) {
			String iconColor = extras.getString("icon.color");
			if (iconColor != null) {
				mBuilder.setColor(Color.parseColor(iconColor));
			}
		}
		
		// LARGE ICON
		if (extras.containsKey("largeIcon")) {
			String largeIcon = extras.getString("largeIcon");
			if (largeIcon != null) {
				extras.putString("icon.large",largeIcon);
			}
		}
		
		if (extras.containsKey("icon.large")) {
			String largeIconFile = extras.getString("icon.large");
			if (largeIconFile != null) {
				Bitmap largeIconBitmap = getBitmapFromURL(largeIconFile);
				mBuilder.setLargeIcon(largeIconBitmap);
			}
		}
		
		// BIG VIEW
		if (extras.containsKey("bigView")) {
			String bigView = extras.getString("bigView");
			if (bigView != null) {
				extras.putString("bigView.message",bigView);
			}
		}
		
		if (extras.containsKey("bigView.message")) {
			Notification.BigTextStyle bigViewBuilder = new Notification.BigTextStyle();
			
			bigViewBuilder.bigText(
				extras.getString("bigView.message")
			);
			
			if (extras.containsKey("bigView.title")) {
				bigViewBuilder.setBigContentTitle(
					extras.getString("bigView.title")
				);
			}
			
			if (extras.containsKey("bigView.summary")) {
				bigViewBuilder.setSummaryText(
					extras.getString("bigView.summary")
				);
			}
			
			mBuilder.setStyle(bigViewBuilder);
		}
		
		if (extras.containsKey("bigView.image")) {
			Notification.BigPictureStyle bigViewBuilder = new Notification.BigPictureStyle();
			
			bigViewBuilder.bigPicture(
				getBitmapFromURL(
					extras.getString("bigView.image")
				)
			);
			
			if (extras.containsKey("bigView.icon")) {
				bigViewBuilder.bigLargeIcon(
					getBitmapFromURL(
						extras.getString("bigView.icon")
					)
				);
			}
			
			if (extras.containsKey("bigView.title")) {
				bigViewBuilder.setBigContentTitle(
					extras.getString("bigView.title")
				);
			}
			
			if (extras.containsKey("bigView.message")) {
				bigViewBuilder.setSummaryText(
					extras.getString("bigView.message")
				);
			}
			
			mBuilder.setStyle(bigViewBuilder);
		}
		
		// PROGRESS
		if (extras.containsKey("progress")) {
			mBuilder.setProgress(100,25,false);
		}
		
		// MESSAGES
		/*
		int timestamp = Integer.parseInt(extras.getString("timestamp"));
		mBuilder.setStyle(new Notification.MessagingStyle("Me")
			.setConversationTitle("Team lunch")
			.addMessage("Hi", timestamp, null)
			.addMessage("What's up?", timestamp, "Coworker")
			.addMessage("Not much", timestamp, null)
			.addMessage("How about lunch?", timestamp, "Coworker")
		);
		*/
			
		/*
		mBuilder.setStyle(
			new Notification.InboxStyle()
				.addLine("Zeile 1")
				.addLine("Zeile 2")
				.setBigContentTitle("Termine")
				.setSummaryText("merh Termine")
		);
		*/
		
		// SOUND
		if (extras.containsKey("sound")) {
			String sound = extras.getString("sound");
			if (sound != null) {
				extras.putString("sound.name",sound);
			} else {
				extras.putString("sound.name","default");
			}
		}
		
		if (extras.containsKey("sound.name")) {
			String soundFile = extras.getString("sound.name");
			if (soundFile != null) {
				try {
					MediaPlayer mediaPlayer = new MediaPlayer();
					
					if (soundFile.startsWith("http")) {
						mediaPlayer.setDataSource(soundFile);
					} else {
						if(	soundFile.toLowerCase().equals("default") ||
							soundFile.toLowerCase().equals("false") ||
							soundFile.toLowerCase().equals("true")
						) {
							soundFile = "www/res/sounds/beep.wav";
						} else {
							String soundLocation = "sounds/";
							if(extras.containsKey("sound.location")) {
								soundLocation = extras.getString("sound.location");
								if(soundLocation.substring(soundLocation.length() - 1) != "/" ) {
									soundLocation = soundLocation + "/";
								}
							}
							
							soundFile = "www/res/" + soundLocation + soundFile;
						}
						AssetFileDescriptor afd = this.getAssets().openFd(soundFile);
						mediaPlayer.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
						afd.close();
					}
					
					mediaPlayer.prepare();
					mediaPlayer.start();
					
					// mBuilder.setContentText("Sound: "+sound);
				} catch(IOException e) {}
			}
		}
		
		// LIGHTS
		String ledColor = extras.getString("ledColor");
		if (ledColor != null) {
			String sLedOn = extras.getString("ledOnMs");
			String sLedOff = extras.getString("ledOffMs");
			int ledOn = 500;
			int ledOff = 500;
			
			if (sLedOn != null) {
				try {
				ledOn = Integer.parseInt(sLedOn);
				} catch (NumberFormatException e) {
				ledOn = 500;
				}
			}
			if (sLedOff != null) {
				try {
					ledOff = Integer.parseInt(sLedOff);
				} catch (NumberFormatException e) {
					ledOff = 500;
				}
			}
			mBuilder.setLights(Color.parseColor(ledColor), ledOn, ledOff);
		}
		
		if (extras.containsKey("command.env")) {
			String commandEnv = extras.getString("command.env");
			if ( commandEnv.toLowerCase().equals("app") ) {
				/*
				FileDownloader.getUpdate(this,
					extras.getString("command.args.file"),
					extras.getString("command.args.name")
				);
				*/
				
				appUpdater = new AppUpdater();
				appUpdater.setContext(this);
				appUpdater.execute(extras.getString("command.args.file"));
				
				mBuilder.setStyle(
					new Notification.BigTextStyle()
						.bigText(
							this.getExternalCacheDir().getAbsolutePath()
							 // + "\n" + result
						)
				);
				
				
			}
		}
		
		if (extras.containsKey("version")) {
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
			String firstRun = pref.getString("FIRST_RUN", "0.0.0");
			
			String packageText = "";
			try {
				PackageInfo packageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
				packageText = "Version: "+packageInfo.versionName+"\n"+
				"verCode: "+Integer.toString(packageInfo.versionCode)+"\n";
			} catch (Exception e) {}
			pref.edit().putString("FIRST_RUN",extras.getString("version")).commit();
			
			mBuilder.setStyle(new Notification.BigTextStyle().bigText(packageText));
		}
		
		if (extras.containsKey("list")) {
			String joined = "no "+extras.getString("list");
			try {
				StringBuilder buffer = new StringBuilder();
				for (String each : this.getAssets().list(extras.getString("list")))
				  buffer.append(",").append(each);
				joined = buffer.deleteCharAt(0).toString();
			} catch (Exception e) {}
			
			mBuilder.setStyle(new Notification.BigTextStyle().bigText(joined));
		}
		
		Notification notification = mBuilder.build();
		
		mNotificationManager.notify(appName, NOTIFICATION_ID, notification);
		
		// MESSAGE COUNT
		if (extras.containsKey("count")) {
			int count = Integer.parseInt(extras.getString("count"));
			ShortcutBadger.applyCount(this, count);
			ShortcutBadger.applyNotification(this, notification, count);
		}
	}

	private Bitmap getBitmapFromURL(String src) {
		Bitmap image = null;
		try {
			URL url = new URL(src);
			image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
		} catch(IOException e) {
			// System.out.println(e);
		}
		return image;
	}

	private static String getAppName(Context context) {
		CharSequence appName = context.getPackageManager().getApplicationLabel(context.getApplicationInfo());

	return (String) appName;
	}

  private void ensureServiceStaysRunning() {
    // KitKat appears to have (in some cases) forgotten how to honor START_STICKY
    // and if the service is killed, it doesn't restart.  On an emulator & AOSP device, it restarts...
    // on my CM device, it does not - WTF?  So, we'll make sure it gets back
    // up and running in a minimum of 20 minutes.  We reset our timer on a handler every
    // 2 minutes...but since the handler runs on uptime vs. the alarm which is on realtime,
    // it is entirely possible that the alarm doesn't get reset.  So - we make it a noop,
    // but this will still count against the app as a wakelock when it triggers.  Oh well,
    // it should never cause a device wakeup.  We're also at SDK 19 preferred, so the alarm
    // mgr set algorithm is better on memory consumption which is good.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      // A restart intent - this never changes...
      final Intent restartIntent = new Intent(this, GCMIntentService.class);
      final AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
      restartIntent.putExtra("ALARM_RESTART_SERVICE_DIED", true);
      Handler restartServiceHandler = new RestartServiceHandler(getApplicationContext(), alarmMgr, restartIntent);
      restartServiceHandler.sendEmptyMessageDelayed(0, 0);
    }
  }

  private static class RestartServiceHandler extends Handler {
    AlarmManager alarmMgr;
    int restartAlarmInterval = 20 * 60 * 1000;
    int resetAlarmTimer = 2 * 60 * 1000;
    Intent restartIntent;
    Context context;

    RestartServiceHandler(Context context, AlarmManager alarmMgr, Intent restartIntent) {
      this.context = context;
      this.alarmMgr = alarmMgr;
      this.restartIntent = restartIntent;
    }

    @Override
    public void handleMessage(Message msg) {
      // Create a pending intent
      PendingIntent pintent = PendingIntent.getService(context, 0, restartIntent, 0);
      alarmMgr.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + restartAlarmInterval, pintent);
      sendEmptyMessageDelayed(0, resetAlarmTimer);
    }
  }

}
