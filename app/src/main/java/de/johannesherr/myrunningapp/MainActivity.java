package de.johannesherr.myrunningapp;

import java.util.Locale;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

	private static final int ACTIVATE_GPS = 43;
	private TextView text;
	private TextView status;
	private Run run;
	private Button button;
	private PowerManager.WakeLock wakeLock;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyLock");

		if (missingPermissions()) {
			ActivityCompat.requestPermissions(this,
			                                  new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
							                                  Manifest.permission.ACCESS_COARSE_LOCATION},
			                                  42);
		} else {
			observeLocation();
		}

		button = (Button) findViewById(R.id.button);
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

		text = (TextView) MainActivity.this.findViewById(R.id.textView);
		text.setText("waiting...");
		status = (TextView) MainActivity.this.findViewById(R.id.status);
	}

	private void stopTracking() {
		this.run = null;
		button.setText("Start Run");
		wakeLock.release();
	}

	private void startTracking() {
		this.run = new Run(System.currentTimeMillis());
		button.setText("Stop Run");
		wakeLock.acquire();
	}

	private void observeLocation() {
		final FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
		final SettingsClient settingsClient = LocationServices.getSettingsClient(this);

		final LocationRequest request = new LocationRequest();
		request.setInterval(2000);
		request.setFastestInterval(2000);
		request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
						.addLocationRequest(request)
						.build();
		Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(settingsRequest);

		task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
			@Override
			public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
				if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
					System.out.println("FAIL");
					return;
				}

				client.requestLocationUpdates(request, new LocationCallback() {

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
							displayRun(run, text);
						}
					}
				}, null);
			}
		});

		task.addOnFailureListener(this, new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				if (e instanceof ResolvableApiException) {
					try {
						ResolvableApiException resolvable = (ResolvableApiException) e;
						resolvable.startResolutionForResult(MainActivity.this, ACTIVATE_GPS);
					} catch (IntentSender.SendIntentException sendEx) {
						// Ignore the error.
					}
				}
			}
		});
	}

	private static void displayRun(Run run, TextView text) {
		double avg1 = run.avgSpeed(30);
		double avg10 = run.avgSpeed(300);

		long durationMS = System.currentTimeMillis() - run.getStartTime();
		text.setText(String.format(Locale.US,
		                           "Duration: %dm %ds\n" +
						                           "Latitude: %s\nLongitude: %s\nAltitude: %.2f\n" +
						                           "Accuracy: %.2f\n" +
						                           "Distance: %.0fm, %.3fkm \n" +
						                           "Speed: %.2f m/s, %.2f km/h, %.2f m/km\n" +
						                           "Avg-Speed ( 1m): %.2f m/s, %.2f km/h, %.2f m/km\n" +
						                           "Avg-Speed (10m): %.2f m/s, %.2f km/h, %.2f m/km\n",

		                           durationMS / 1000 / 60,
		                           (durationMS / 1000) % 60,

		                           run.getLastLocation().getLatitude(),
		                           run.getLastLocation().getLongitude(),
		                           run.getLastLocation().getAltitude(),

		                           run.getLastLocation().getAccuracy(),
		                           run.getDistance(),
		                           run.getDistance() / 1000,
		                           run.getSpeed(),
		                           run.getSpeed() * 3600 / 1000,
		                           1000 / run.getSpeed() / 60,

		                           avg1,
		                           avg1 * 3600 / 1000,
		                           1000 / avg1 / 60,

		                           avg10,
		                           avg10 * 3600 / 1000,
		                           1000 / avg10 / 60
		));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ACTIVATE_GPS) {
			observeLocation();
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		observeLocation();
	}

	private boolean missingPermissions() {
		return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
						ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (wakeLock.isHeld()) {
			wakeLock.release();
		}
	}
}
