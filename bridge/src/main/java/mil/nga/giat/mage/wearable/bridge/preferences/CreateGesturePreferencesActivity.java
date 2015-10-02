package mil.nga.giat.mage.wearable.bridge.preferences;

import android.app.Activity;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.internal.util.Predicate;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.chronostouch.gesture.ChronosData;
import mil.nga.giat.chronostouch.gesture.ChronosGesture;
import mil.nga.giat.chronostouch.gesture.ChronosGestureManager;
import mil.nga.giat.chronostouch.gesture.OnDelayedGestureListener;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.wearable.bridge.R;

public class CreateGesturePreferencesActivity extends Activity {

	private static final String LOG_NAME = CreateGesturePreferencesActivity.class.getName();

	protected Gesture mGesture;
	protected String mGestureType;
	protected String mGestureName;

	protected ArrayAdapter<String> mSpinnerArrayAdapter;

	protected MenuItem mSaveGestureMenuItem;

	protected Boolean mIsEdit = false;
	protected ChronosGesture originalGesture = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.gesture_create);

		TextView gestureNameInput = (TextView) findViewById(R.id.gesture_name);
		Spinner typeSpinner = (Spinner) findViewById(R.id.type_spinner);
		mSpinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getTypes().toArray(new String[getTypes().size()]));
		mSpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		typeSpinner.setAdapter(mSpinnerArrayAdapter);
		final GestureOverlayView gestureOverlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);

		ChronosGesture cg = (ChronosGesture)getIntent().getParcelableExtra("chronosgesture");
		if(cg != null) {
			mIsEdit = true;
			mGestureName = cg.getChronosData().getName();
			mGestureType = cg.getChronosData().getType();
			mGesture = cg.getGesture();
			populateControls();
			originalGesture = new ChronosGesture(mGesture, new ChronosData(mGesture.getID(), mGestureName, mGestureType));
		}

		gestureNameInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				mGestureName = s.toString();
				toggleCreateButton();
			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});

		typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mGestureType = parent.getItemAtPosition(position).toString();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		gestureOverlay.addOnGestureListener(new OnDelayedGestureListener(
				new Predicate<GestureOverlayView>() {
					@Override
					public boolean apply(GestureOverlayView gestureOverlayView) {
						mGesture = null;
						toggleCreateButton();
						return true;
					}
				},
				new Predicate<Object>() {
					@Override
					public boolean apply(Object gesture) {
						mGesture = (Gesture) gesture;
						toggleCreateButton();
						return true;
					}
				}));
	}

	public List<String> getTypes() {
		List<String> types = new ArrayList<String>();

		JsonObject dynamicFormJson = EventHelper.getInstance(getApplicationContext()).getCurrentEvent().getForm();
		JsonArray dynamicFormFields = dynamicFormJson.get("fields").getAsJsonArray();

		final String typeFieldName = "type";
		JsonObject typeField = null;
		for (int i = 0; i < dynamicFormFields.size(); i++) {
			JsonObject field = dynamicFormFields.get(i).getAsJsonObject();
			String name = field.get("name").getAsString();
			if (name != null && name.equals(typeFieldName)) {
				typeField = field;
				continue;
			}
		}

		if(typeField != null && typeField.has("choices")) {
			JsonArray choices = typeField.getAsJsonArray("choices");
			for (int i = 0; i < choices.size(); i++) {
				JsonObject choice = choices.get(i).getAsJsonObject();
				if(choice != null && !choice.isJsonNull() && choice.has("title")) {
					String title = choice.get("title").getAsString();
					types.add(title);
				}
			}
		}

		return types;
	}

	public void saveGesture() {
		TextView gestureNameInput = (TextView) findViewById(R.id.gesture_name);
		gestureNameInput.setError(null);
		if (mGestureName != null && !mGestureName.isEmpty()) {
			if (mGesture != null) {
				ChronosGestureManager.getInstance(getApplicationContext()).addGesture(new ChronosGesture(mGesture, new ChronosData(mGesture.getID(), mGestureName, mGestureType)));
			} else {
				Log.e(LOG_NAME, "Should be unreachable via GUI controls.");
			}
		} else {
			Log.e(LOG_NAME, "Should be unreachable via GUI controls.");
			gestureNameInput.setError("Can not be empty");
		}
		finish();
	}

	public void deleteGesture() {
		if(mIsEdit) {
			ChronosGestureManager.getInstance(getApplicationContext()).removeGesture(originalGesture);
		}
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.create_gesture_menu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mSaveGestureMenuItem = menu.findItem(R.id.save_gesture);
		MenuItem deleteGestureMenuItem = menu.findItem(R.id.delete_gesture);
		if(mIsEdit) {
			mSaveGestureMenuItem.setTitle("Save");
			deleteGestureMenuItem.setTitle("Delete");
		} else {
			mSaveGestureMenuItem.setTitle("Create");
			deleteGestureMenuItem.setTitle("Discard");
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.save_gesture) {
			saveGesture();
			return true;
		} else if (id == R.id.delete_gesture) {
			deleteGesture();
			return true;
		} else if(id == android.R.id.home) {
			onBackPressed();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	protected void toggleCreateButton() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(mSaveGestureMenuItem != null) {
					if (mGesture != null && (mGestureName != null && !mGestureName.isEmpty())) {
						mSaveGestureMenuItem.setEnabled(true);
					} else {
						mSaveGestureMenuItem.setEnabled(false);
					}
				}
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
		overlay.removeAllOnGestureListeners();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mGesture != null) {
			outState.putString("gestureName", mGestureName);
			outState.putString("gestureType", mGestureType);
			outState.putParcelable("gesture", mGesture);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		mGestureName = savedInstanceState.getString("gestureName");
		mGestureType = savedInstanceState.getString("gestureType");
		mGesture = savedInstanceState.getParcelable("gesture");
		populateControls();

		toggleCreateButton();
	}

	protected void populateControls() {
		if(mGestureName != null) {
			TextView gestureNameInput = (TextView) findViewById(R.id.gesture_name);
			gestureNameInput.setText(mGestureName);
		}
		if(mGestureType != null && mSpinnerArrayAdapter != null) {
			Spinner typeSpinner = (Spinner) findViewById(R.id.type_spinner);
			typeSpinner.setSelection(mSpinnerArrayAdapter.getPosition(mGestureType));
		}
		if (mGesture != null) {
			final GestureOverlayView gestureOverlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
			gestureOverlay.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					gestureOverlay.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					gestureOverlay.setGesture(mGesture);
				}
			});
		}
	}
}
