package mil.nga.giat.mage.wearable;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import java.util.List;

public class MAGEWearBridge {

	// singleton
	private static MAGEWearBridge mageWearBridge;

	private Context mContext;

	private static final String OBSERVATION_PATH = "/observation";
	private static final String GESTURE_KEY = "gesture";
	private static final String DESCRIPTION_KEY = "description";

	private MAGEWearBridge(Context context) {
		mContext = context;
	}

	public static MAGEWearBridge getInstance(final Context context) {
		if (context == null) {
			return null;
		} else if (mageWearBridge == null) {
			mageWearBridge = new MAGEWearBridge(context);
		}
		return mageWearBridge;
	}
}
