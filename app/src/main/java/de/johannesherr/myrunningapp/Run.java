package de.johannesherr.myrunningapp;

import java.util.ArrayList;

import android.location.Location;

public class Run {
	private final long startTime;
	private double total = 0.0;
	private final ArrayList<Location> locations;

	public Run(long startTime) {
		this.startTime = startTime;
		this.locations = new ArrayList<>();
	}


	public synchronized double avgSpeed(int num) {
		double sum = 0.0;
		int limit = Math.min(num, locations.size());
		for (int i = 0; i < limit; i++) {
			sum += locations.get(locations.size() - 1 - i).getSpeed();
		}
		return sum / limit;
	}

	public synchronized void addLocation(Location location) {
		if (!locations.isEmpty()) {
			Location loc = locations.get(locations.size() - 1);
			float distanceTo = loc.distanceTo(location);
			total += distanceTo;
		}
		locations.add(location);
	}

	public Location getLastLocation() {
		return locations.isEmpty() ? null : locations.get(locations.size() - 1);
	}

	public float getSpeed() {
		Location lastLocation = getLastLocation();
		return lastLocation == null ? null : lastLocation.getSpeed();
	}

	public double getDistance() {
		return total;
	}

	public long getStartTime() {
		return startTime;
	}
}
