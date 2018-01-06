package de.johannesherr.myrunningapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class FragmentDev extends Fragment {

	private TextView text;
	private TextView status;
	private Run run;
	private Button button;
	private final Handler handler = new Handler();
	private Runnable runnable;
	private RunsDatabase runsDatabase;
	private PowerManager.WakeLock wakeLock;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {

		MainActivity activity = (MainActivity) getActivity();
		runsDatabase = activity.getDatabase();
		wakeLock = activity.getWakeLock();

		View fragment = inflater.inflate(R.layout.fragment_dev, container, false);

		button = fragment.findViewById(R.id.button);

		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (run == null) {
					startTracking();
				} else {
					stopTracking();
				}
			}
		});

		text = fragment.findViewById(R.id.textView);
		text.setText("Just Do It.");
		status = fragment.findViewById(R.id.status);

		activity.setLocationCallback(new MyLocationCallback());

		return fragment;
	}

	private void startTracking() {
		this.run = new Run(System.currentTimeMillis());
		button.setText("Stop Run");
		wakeLock.acquire();
		runnable = new Runnable() {
			@Override
			public void run() {
				if (run != null) {
					displayRun(run, text);
					handler.postDelayed(runnable, 100);
				}
			}
		};
		handler.postDelayed(runnable, 100);
	}

	private void stopTracking() {
		RunsDao runsDao = runsDatabase.runsDao();
		CompletedRun completedRun = new CompletedRun();
		completedRun.setTimestamp(run.getStartTime());
		completedRun.setLocations(locationsToString());
		runsDao.insertAll(completedRun);
		this.run = null;
		button.setText("Start Run");
		wakeLock.release();
	}

	private String locationsToString() {
		StringBuilder sb = new StringBuilder();
		ArrayList<Location> locations = run.getLocations();
		for (int i = 0; i < locations.size(); i++) {
			Location location = locations.get(i);
			sb.append(String.format("(%s,%s,%s)", location.getLatitude(), location.getLongitude(), location.getTime()));
			if (i + 1 < locations.size()) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	private static void displayRun(Run run, TextView text) {
		double avg1 = run.avgSpeed(30);
		double avg10 = run.avgSpeed(300);

		List<Double> splitsMPerSec = run.computeSplits();

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < splitsMPerSec.size(); i++) {
			Double split = splitsMPerSec.get(i);
			sb.append(String.format(Locale.US,
			                        "%2d: %.2f m/km, %.1f km/h\n",
			                        i + 1,
			                        Utils.mPerSec_to_mPerKM(split),
			                        Utils.mPerSec_to_kmPerH(split)
			));
		}

		long durationMS = System.currentTimeMillis() - run.getStartTime();

		text.setText(String.format(Locale.US,
		                           "Duration: %dm %.1fs\n" +
						                           "Latitude: %s\nLongitude: %s\nAltitude: %.2f\n" +
						                           "Accuracy: %.2f\n" +
						                           "Distance: %.0fm, %.3fkm \n" +
						                           "Speed: %.2f m/s, %.2f km/h, %.2f m/km\n" +
						                           "Avg-Speed ( 1m): %.2f m/s, %.2f km/h, %.2f m/km\n" +
						                           "Avg-Speed (10m): %.2f m/s, %.2f km/h, %.2f m/km\n\n" +
						                           "Splits:\n" +
						                           "%s",

		                           durationMS / 1000 / 60,
		                           (durationMS / 100.0) % 600 / 10,

		                           run.getLastLocation() == null ? "-" : run.getLastLocation().getLatitude(),
		                           run.getLastLocation() == null ? "-" : run.getLastLocation().getLongitude(),
		                           run.getLastLocation() == null ? -1 : run.getLastLocation().getAltitude(),

		                           run.getLastLocation() == null ? -1 : run.getLastLocation().getAccuracy(),
		                           run.getDistance(),
		                           run.getDistance() / 1000,
		                           run.getSpeed(),
		                           Utils.mPerSec_to_kmPerH(run.getSpeed()),
		                           Utils.mPerSec_to_mPerKM(run.getSpeed()),

		                           avg1,
		                           Utils.mPerSec_to_kmPerH(avg1),
		                           Utils.mPerSec_to_mPerKM(avg1),

		                           avg10,
		                           Utils.mPerSec_to_kmPerH(avg10),
		                           Utils.mPerSec_to_mPerKM(avg10),

		                           sb
		));
	}

	private class MyLocationCallback extends LocationCallback {
		@Override
		public void onLocationAvailability(LocationAvailability locationAvailability) {
			status.setText(String.format("locationAvailable = %s%n", locationAvailability.isLocationAvailable()));
		}

		@Override
		public void onLocationResult(LocationResult locationResult) {
			Location location = locationResult.getLastLocation();
			status.setText(String.format(Locale.US,
			                             "Latitude: %s\nLongitude: %s\nAccuracy: %.0f",
			                             location.getLatitude(), location.getLongitude(), location.getAccuracy()));

			if (run != null) {
				run.addLocation(location);
			}
		}
	}
}
