package com.pwc.d2ep;

import java.io.Serializable;

public class DocCharge implements Serializable {

    String name,id, masterID;
    boolean isApplied;
    double amount;
    boolean isFloating;
    boolean isTax;

    public DocCharge(String name, String id, String masterID, boolean isApplied, double amount) {
        this.name = name;
        this.id = id;
        this.isApplied = isApplied;
        this.amount = amount;
        this.masterID =masterID;
    }
    public DocCharge(String name, String id, String masterID, boolean isApplied, double amount, boolean isFloating, boolean isTax) {
        this.name = name;
        this.id = id;
        this.isApplied = isApplied;
        this.amount = amount;
        this.masterID =masterID;
        this.isFloating = isFloating;
        this.isTax = isTax;
    }
}
