package de.johannesherr.myrunningapp;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class CompletedRun {

	@PrimaryKey(autoGenerate = true)
	private int uid;
	private long timestamp;
	private String locations;

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getLocations() {
		return locations;
	}

	public void setLocations(String locations) {
		this.locations = locations;
	}

	@Override
	public String toString() {
		return "CompletedRun{" +
						"uid=" + uid +
						", timestamp=" + timestamp +
						", locations='" + locations + '\'' +
						'}';
	}
}
