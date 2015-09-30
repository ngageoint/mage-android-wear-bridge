package mil.nga.giat.mage.wearable.bridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.gesture.Gesture;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import mil.nga.giat.chronostouch.gesture.ChronosGesture;
import mil.nga.giat.chronostouch.gesture.ChronosGestureManager;
import mil.nga.giat.chronostouch.pipe.DataManager;
import mil.nga.giat.chronostouch.utils.ParcelableToByteArrayUtil;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;

public class MAGEWearBridge {

	private static final String LOG_NAME = MAGEWearBridge.class.getName();

	// singleton
	private static MAGEWearBridge mageWearBridge;
	private static GestureReceiver gestureReceiver;

	protected Context mContext;
	protected DataManager dataLayerManager;

	protected static final String OBSERVATION_PATH = "/observation";
	protected static final String GESTURE_KEY = "gesture";
	protected static final String DESCRIPTION_KEY = "description";

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
			gestureReceiver = mageWearBridge.new GestureReceiver();
		}
		return mageWearBridge;
	}

	/**
	 * Called via reflection from mage client
	 */
	public void startBridge() {
		Log.d(LOG_NAME, "starting bridge");

		dataLayerManager = DataManager.getInstance(mContext);
		dataLayerManager.addListenPath(OBSERVATION_PATH);
		dataLayerManager.addListenKey(GESTURE_KEY);

		dataLayerManager.addListenKey(DESCRIPTION_KEY);
		LocalBroadcastManager.getInstance(mContext).unregisterReceiver(gestureReceiver);
		LocalBroadcastManager.getInstance(mContext).registerReceiver(gestureReceiver, new IntentFilter(OBSERVATION_PATH));
	}

	public class GestureReceiver extends BroadcastReceiver {

		protected Observation getNewObservation(Context context) {
			Observation o = new Observation();
			o.setEvent(EventHelper.getInstance(context).getCurrentEvent());
			o.setTimestamp(new Date());
			List<ObservationProperty> properties = new ArrayList<ObservationProperty>();
			properties.add(new ObservationProperty("timestamp", DateFormatFactory.ISO8601().format(o.getTimestamp())));
			o.addProperties(properties);

			List<Location> tLocations =	LocationHelper.getInstance(context).getCurrentUserLocations(context, 1, true);
			if(!tLocations.isEmpty()) {
				o.setGeometry(tLocations.get(0).getGeometry());
			} else {
				Log.e(LOG_NAME, "Could not get location for observation.");
			}

			try {
				User u = UserHelper.getInstance(context).readCurrentUser();
				o.setUserId(u.getRemoteId());
			} catch(UserException ue) {
				Log.e(LOG_NAME, "Could not get user for observation.");
			}

			return o;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(LOG_NAME, "wear message received");

			Map<String, Object> dataMap = dataLayerManager.getData().get(OBSERVATION_PATH);
			Gesture gesture = ParcelableToByteArrayUtil.getParcelable((byte[])dataMap.get(GESTURE_KEY), Gesture.CREATOR);

			List<ChronosGesture> tGestures = ChronosGestureManager.getInstance(mContext).recognize(gesture);
			if (!tGestures.isEmpty()) {
				ChronosGesture chronosGesture = tGestures.get(0);

				String description = (String) dataMap.get(DESCRIPTION_KEY);

				// check if there is a description field
				JsonObject dynamicFormJson = EventHelper.getInstance(context).getCurrentEvent().getForm();
				JsonArray dynamicFormFields = dynamicFormJson.get("fields").getAsJsonArray();

				final String descriptionFieldTitle = "Description";
				final String typeFieldName = "type";

				JsonObject descriptionField = null;
				JsonObject typeField = null;
				for (int i = 0; i < dynamicFormFields.size(); i++) {
					JsonObject field = dynamicFormFields.get(i).getAsJsonObject();
					String name = field.get("name").getAsString();
					if (name != null && name.equals(typeFieldName)) {
						typeField = field;
						continue;
					}
					String title = field.get("title").getAsString();
					if (title != null) {
						if(title.equalsIgnoreCase(descriptionFieldTitle)) {
							descriptionField = field;
						} else if(descriptionField == null && title.toUpperCase().contains(descriptionFieldTitle.toUpperCase())) {
							descriptionField = field;
						}
						continue;
					}
				}

				Observation o = getNewObservation(context);

				List<ObservationProperty> properties = new ArrayList<ObservationProperty>();
				if (typeField != null) {
					properties.add(new ObservationProperty(typeField.get("name").getAsString(), chronosGesture.getChronosData().getType()));
				}

				if (descriptionField != null && description != null && !description.isEmpty()) {
					properties.add(new ObservationProperty(descriptionField.get("name").getAsString(), description));
				}

				o.addProperties(properties);

				try {
					ObservationHelper.getInstance(context).create(o);
				} catch (ObservationException oe) {
					Log.e(LOG_NAME, "Could not create observation.");
				}
			}
		}
	}
}
