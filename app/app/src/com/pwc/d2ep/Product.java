package com.pwc.d2ep;

public class Product {
    String name, uom, ID;
    double available, allocated, inHand, cost;

    public Product(String name, String ID, String uom, double available, double allocated, double inHand, double cost) {
        this.name = name;
        this.uom = uom;
        this.available = available;
        this.allocated = allocated;
        this.inHand = inHand;
        this.cost = cost;
        this.ID = ID;
    }

    public String getName() {
        return name;
    }

    public String getUom() {
        return uom;
    }

    public double getAvailable() {
        return available;
    }

    public double getAllocated() {
        return allocated;
    }

    public double getInHand() {
        return inHand;
    }

    public double getCost() {
        return cost;
    }

    public String getID() {
        return ID;
    }
}
