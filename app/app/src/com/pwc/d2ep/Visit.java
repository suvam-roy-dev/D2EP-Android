package com.pwc.d2ep;

public class Visit {
    public String getName() {
        return name;
    }
    public String getAddress(){return address;}

    public String getStartTime() {
        return startTime;
    }
    public String getDate() {
        return date;
    }

    public String getID() {
        return ID;
    }

    public String getPriority() {
        return priority;
    }

    public String getType() {
        return type;
    }
    public String getStatus(){return status;}
    public int getTasksCount(){return tasksCount;}
    public Visit(String name, String address, String startTime, String date, String ID,String priority, String status, String type, int taskCount) {
        this.name = name;
        this.address = address;
        this.startTime = startTime;
        this.date = date;
        this.ID = ID;
        this.priority = priority;
        this.status = status;
        this.type = type;
        this.tasksCount = taskCount;
    }

    private String name, address, startTime, date, ID, priority, status, type;
    private int tasksCount;


}
