package de.johannesherr.myrunningapp;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {CompletedRun.class}, version = 2)
public abstract class RunsDatabase extends RoomDatabase {

	public abstract RunsDao runsDao();

}
