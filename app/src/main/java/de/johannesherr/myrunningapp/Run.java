package de.johannesherr.myrunningapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.location.Location;

public class Run {
	private final long startTime;
	private double total = 0.0;
	private final ArrayList<Location> locations;

	public Run(long startTime) {
		this.startTime = startTime;
		this.locations = new ArrayList<>();
	}

	public synchronized List<Double> computeSplits() {
		if (locations.isEmpty()) return Collections.singletonList(0.0);

		double curDist = 0;
		double totalDist = 0;
		Iterator<Location> iterator = locations.iterator();
		Location prev = iterator.next();
		long curStart = prev.getTime();

		List<Double> splitsMPerSec = new ArrayList<>();
		for (int km = 0; iterator.hasNext();) {
			Location loc = iterator.next();
			float distance = prev.distanceTo(loc);
			prev = loc;
			curDist += distance;
			totalDist += distance;
			if (((int) totalDist) / 1000 >= km + 1 || !iterator.hasNext()) {
				km = (int) totalDist / 1000;

				splitsMPerSec.add(Utils.getMeterPerSecond(curDist, loc.getTime() - curStart));

				curDist = 0;
				curStart = loc.getTime();
			}
		}
		System.out.printf("totalDist = %s%n", totalDist);
		return splitsMPerSec;
	}


	public synchronized double avgSpeed(int num) {
		double sum = 0.0;
		int limit = Math.min(num, locations.size());
		for (int i = 0; i < limit; i++) {
			sum += locations.get(locations.size() - 1 - i).getSpeed();
		}
		return limit == 0 ? 0 : sum / limit;
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
		return lastLocation == null ? 0.0f : lastLocation.getSpeed();
	}

	public double getDistance() {
		return total;
	}

	public long getStartTime() {
		return startTime;
	}

	public ArrayList<Location> getLocations() {
		return locations;
	}
}
