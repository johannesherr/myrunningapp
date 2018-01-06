package de.johannesherr.myrunningapp;

import java.util.List;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

@Dao
public interface RunsDao {

	@Query("SELECT * FROM completedrun")
	List<CompletedRun> getAll();

	@Insert
	void insertAll(CompletedRun... runs);

	@Delete
	void delete(CompletedRun run);

}
