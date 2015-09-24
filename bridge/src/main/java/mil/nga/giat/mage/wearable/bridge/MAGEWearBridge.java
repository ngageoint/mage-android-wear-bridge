package mil.nga.giat.mage.wearable.bridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Map;

import mil.nga.giat.chronostouch.gesture.ChronosGesture;
import mil.nga.giat.chronostouch.pipe.DataManager;

public class MAGEWearBridge {

	private static final String LOG_NAME = MAGEWearBridge.class.getName();

	// singleton
	private static MAGEWearBridge mageWearBridge;

	private Context mContext;

	private static final String OBSERVATION_PATH = "/observation";
	private static final String GESTURE_KEY = "gesture";
	private static final String DESCRIPTION_KEY = "description";

	private MAGEWearBridge(Context context) {
		mContext = context;
	}

	/**
	 * Called via reflection from mage client
	 *
	 * @param context
	 * @return
	 */
	public static MAGEWearBridge getInstance(final Context context) {
		if (context == null) {
			return null;
		} else if (mageWearBridge == null) {
			mageWearBridge = new MAGEWearBridge(context);
		}
		return mageWearBridge;
	}

	/**
	 * Called via reflection from mage client
	 */
	public void startBridge() {
		Log.d(LOG_NAME, "starting bridge");

		final DataManager dataLayerManager = DataManager.getInstance(mContext);
		dataLayerManager.addListenPath(OBSERVATION_PATH);
		dataLayerManager.addListenKey(GESTURE_KEY);

		dataLayerManager.addListenKey(DESCRIPTION_KEY);
		LocalBroadcastManager.getInstance(mContext).registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(LOG_NAME, "received message");

				Map<String, Object> dataMap = dataLayerManager.getData().get(OBSERVATION_PATH);
				ChronosGesture chronosGesture = (ChronosGesture) dataMap.get(GESTURE_KEY);
				String description = (String) dataMap.get(DESCRIPTION_KEY);


			}
		}, new IntentFilter(OBSERVATION_PATH));
	}
}
