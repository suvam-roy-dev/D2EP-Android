package com.pwc.d2ep.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {VisitDB.class, TaskDB.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract VisitDao visitDao();
    public abstract TaskDao taskDao();
}