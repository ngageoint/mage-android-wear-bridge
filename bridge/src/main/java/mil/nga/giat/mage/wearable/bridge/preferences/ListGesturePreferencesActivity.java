package mil.nga.giat.mage.wearable.bridge.preferences;


import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import mil.nga.giat.chronostouch.gesture.ChronosGesture;
import mil.nga.giat.chronostouch.gesture.ChronosGestureManager;
import mil.nga.giat.mage.wearable.bridge.R;

public class ListGesturePreferencesActivity extends ListActivity {
	protected static final int CREATE_NEW_GESTURE = 1;

	protected ChronosGestureManager mGestureManager;
	protected GesturesAdapter mAdapter;
	protected TextView mEmpty;

	protected EditText mInput;

	private ChronosGesture mCurrentEditGesture;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGestureManager = ChronosGestureManager.getInstance(getApplicationContext());

		setContentView(R.layout.gesture_list);

		mAdapter = new GesturesAdapter(this);
		setListAdapter(mAdapter);

		mEmpty = (TextView) findViewById(android.R.id.empty);

		registerForContextMenu(getListView());
	}

	@Override
	protected void onResume() {
		super.onResume();
		mAdapter.clear();
		mAdapter.addAll(mGestureManager.getGestures());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_gesture_menu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.create_gesture) {
			startActivity(new Intent(this, CreateGesturePreferencesActivity.class));
			return true;
		} else if (id == android.R.id.home) {
			onBackPressed();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mCurrentEditGesture != null) {
			outState.putParcelable("currentGesture", mCurrentEditGesture);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		mCurrentEditGesture = state.getParcelable("currentGesture");
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent editGesture = new Intent(this, CreateGesturePreferencesActivity.class);
		editGesture.putExtra("chronosgesture", mAdapter.getItem(position));

		startActivityForResult(editGesture, CREATE_NEW_GESTURE);

	}

	private class GesturesAdapter extends ArrayAdapter<ChronosGesture> {
		private final LayoutInflater mInflater;

		public GesturesAdapter(Context context) {
			super(context, 0);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.gesture_list_item, parent, false);
			}

			final ChronosGesture chronosGesture = getItem(position);
			final TextView label = (TextView) convertView;

			Spannable spannable = new SpannableString(chronosGesture.getChronosData().getName() + " - " + chronosGesture.getChronosData().getType());
			label.setTag(chronosGesture);
			label.setText(spannable);
			label.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(getResources(), chronosGesture.getGesture().toBitmap(200, 200, 0, Color.RED)), null, null, null);

			return convertView;
		}
	}
}

