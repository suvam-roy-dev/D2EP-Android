package com.pwc.d2ep.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface VisitDao{

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertVisits(VisitDB... visits);

    @Update
    public int updateVisits(VisitDB... visits);

    @Query("SELECT * FROM visits")
    public VisitDB[] loadAllVisits();

    @Query("SELECT * FROM visits WHERE isSynced is 0")
    public VisitDB[] loadUnsynced();

    @Query("SELECT * FROM visits WHERE visitId is :id")
    public VisitDB loadVisitDetails(String id);
}
