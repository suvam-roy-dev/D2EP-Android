package com.pwc.d2ep;

public class Visit {
    public String getName() {
        return name;
    }

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

    public Visit(String name, String startTime, String date, String ID,String priority, String type) {
        this.name = name;
        this.startTime = startTime;
        this.date = date;
        this.ID = ID;
        this.priority = priority;
        this.type = type;
    }

    private String name, startTime, date, ID, priority, type;


}
