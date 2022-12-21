package com.pwc.d2ep.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "visits")
public class VisitDB {

    @PrimaryKey @NonNull
    public String visitId;

    public String name;
    public String status;
    public String priority;
    public String time;
    public String distributorName;
    public String distributorAddress;
    public String retailerName;
    public String retailerAddress;
    public String beatPlanning;
    public String beatLocation;
    public String ownerID;
    public boolean isSynced;

}
