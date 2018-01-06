package de.johannesherr.myrunningapp;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

	private static final int ACTIVATE_GPS = 43;
	private PowerManager.WakeLock wakeLock;
	private RunsDatabase runsDatabase;
	private LocationCallback locationCallback;
	private Runnable freRunnable;
	private AtomicBoolean vib;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyLock");

		runsDatabase = Room.databaseBuilder(this, RunsDatabase.class, "my-runs")
						.allowMainThreadQueries()
						.fallbackToDestructiveMigration()
						.build();

		setContentView(R.layout.activity_main);

		if (missingPermissions()) {
			ActivityCompat.requestPermissions(this,
			                                  new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
							                                  Manifest.permission.ACCESS_COARSE_LOCATION},
			                                  42);
		} else {
			observeLocation();
		}

		TabHost tabHost = findViewById(R.id.tab_host);
		tabHost.setup();

		addTab(tabHost, "T1", "Dev", R.id.tab1);
		addTab(tabHost, "T2", "List", R.id.tab2);
		addTab(tabHost, "THist", "Hist", R.id.tabHist);
		addTab(tabHost, "T3", "Freq", R.id.tab3);

		final TextView textView = findViewById(R.id.completed_runs_text);
		updateList(textView);

		final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		Button refresh = findViewById(R.id.refreshButton);
		refresh.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateList(textView);
			}
		});

		final TextView freqTextView = findViewById(R.id.freqText);
		final AtomicInteger freq = new AtomicInteger(180);
		SeekBar seekBar = findViewById(R.id.seekBar);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				progress = Math.max(progress, 1);
				if (fromUser) {
					freq.set(progress);
				}
				freqTextView.setText(String.valueOf(progress));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		seekBar.setProgress(freq.get());

		final AtomicInteger halfIt = new AtomicInteger(1);
		final ToggleButton halfButton = findViewById(R.id.halfBtn);
		halfButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					halfIt.set(2);
				} else {
					halfIt.set(1);
				}
			}
		});

		final Handler repFrefHandler = new Handler();
		vib = new AtomicBoolean();
		freRunnable = new Runnable() {
			@Override
			public void run() {
				if (vib.get()) {
					repFrefHandler.postDelayed(freRunnable, 60_000 / (freq.get() / halfIt.get()));
					vibrator.vibrate(10);
				}
			}
		};
		final ToggleButton freqBtn = findViewById(R.id.freqBtn);
		freqBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				vib.set(isChecked);
				if (isChecked) {
					repFrefHandler.post(freRunnable);
				}
			}
		});
	}

	private void updateList(TextView textView) {
		List<CompletedRun> completedRuns = getRuns();
		textView.setText(String.format(Locale.US, "%d runs in database\n%s", completedRuns.size(), oldruns(completedRuns)));
	}

	public List<CompletedRun> getRuns() {
		return runsDatabase.runsDao().getAll();
	}

	private String oldruns(List<CompletedRun> runs) {
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.US);
		for (CompletedRun completedRun : runs) {
			String desc = getRunDescription(sdf, completedRun);

			sb.append(desc);
		}
		return sb.toString();
	}

	public static class RunInfo {
		public final double dist;
		public final int mins;

		public RunInfo(double dist, int mins) {
			this.dist = dist;
			this.mins = mins;
		}
	}
	
	public static String getRunDescription(SimpleDateFormat sdf, CompletedRun completedRun) {
		RunInfo runInfo = getRunInfo(completedRun);

		return String.format(Locale.US,
		                     "%s: %.1fkm, %dmin\n",
		                     sdf.format(completedRun.getTimestamp()), runInfo.dist / 1000, runInfo.mins);
	}

	public static RunInfo getRunInfo(CompletedRun completedRun) {
		double dist = 0;
		int mins = 0;
		List<double[]> locs = Utils.parse(completedRun.getLocations());

		if (!locs.isEmpty()) {
			double[] cur = locs.get(0);
			for (int i = 1; i < locs.size(); i++) {
				double[] loc = locs.get(i);
				float[] results = new float[3];
				Location.distanceBetween(cur[0], cur[1], loc[0], loc[1], results);
				dist += results[0];
				cur = loc;
			}

			double[] last = locs.get(locs.size() - 1);
			mins = ((int) (last[2] - completedRun.getTimestamp())) / 60_000;
		}

		return new RunInfo(dist, mins);
	}

	private void addTab(TabHost tabHost, String tag, String label, int id) {
		TabHost.TabSpec spec = tabHost.newTabSpec(tag);
		spec.setIndicator(label);
		spec.setContent(id);
		tabHost.addTab(spec);
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
		runsDatabase.close();
		vib.set(false);
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
						if (locationCallback != null) {
							locationCallback.onLocationAvailability(locationAvailability);
						}
					}

					@Override
					public void onLocationResult(LocationResult locationResult) {
						if (locationCallback != null) {
							locationCallback.onLocationResult(locationResult);
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

	public RunsDatabase getDatabase() {
		return runsDatabase;
	}

	public PowerManager.WakeLock getWakeLock() {
		return wakeLock;
	}

	public void setLocationCallback(LocationCallback locationCallback) {
		this.locationCallback = locationCallback;
	}
}
