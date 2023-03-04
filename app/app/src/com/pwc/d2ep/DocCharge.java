package com.pwc.d2ep;

import java.io.Serializable;

public class DocCharge implements Serializable {

    String name;
    boolean isApplied;
    double amount;

    public DocCharge(String name, boolean isApplied, double amount) {
        this.name = name;
        this.isApplied = isApplied;
        this.amount = amount;
    }
}
