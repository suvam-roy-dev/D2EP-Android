package com.pwc.d2ep.db;



import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class TaskDB {

    @PrimaryKey @NonNull
    public String taskId;

    public String visitId;
    public String status;
    public String priority;
    public String date;
    public String description;
    public String subject;
    public boolean isSynced;

}
