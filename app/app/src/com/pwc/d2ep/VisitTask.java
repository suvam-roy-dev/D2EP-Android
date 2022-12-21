package com.pwc.d2ep;

public class VisitTask {

    public String getTaskId() {
        return taskId;
    }

    public String getVisitId() {
        return visitId;
    }

    public String getSubject() {
        return subject;
    }

    public String getDate() {
        return date;
    }

    public String getPriority() {
        return priority;
    }

    public VisitTask(String taskId, String visitId, String subject, String date, String priority) {
        this.taskId = taskId;
        this.visitId = visitId;
        this.subject = subject;
        this.date = date;
        this.priority = priority;
    }

    String taskId, visitId, subject, date, priority;

}
