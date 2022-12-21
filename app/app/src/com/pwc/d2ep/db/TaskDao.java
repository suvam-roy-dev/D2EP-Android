package com.pwc.d2ep.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertTask(TaskDB... tasks);

    @Update
    public int updateTasks(TaskDB... tasks);

    @Query("SELECT * FROM tasks")
    public TaskDB[] loadAllTasks();

    @Query("SELECT * FROM tasks WHERE isSynced is 0")
    public TaskDB[] loadUnsynced();

    @Query("SELECT * FROM tasks WHERE visitId is :id")
    public TaskDB[] loadVisitTasks(String id);

    @Query("SELECT * FROM tasks WHERE taskId is :id")
    public TaskDB loadTaskDetails(String id);
}
