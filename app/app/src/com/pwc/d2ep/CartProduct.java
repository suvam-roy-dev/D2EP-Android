package com.pwc.d2ep;

import java.io.Serializable;

public class CartProduct implements Serializable {

    public CartProduct(String name, String ID, String UOM, int qty, double salesPrice, double tax, double discount, double total) {
        this.name = name;
        this.UOM = UOM;
        this.qty = qty;
        this.salesPrice = salesPrice;
        this.tax = tax;
        this.discount = discount;
        this.total = total;
        this.ID = ID;
    }

    String name, UOM, ID;
    int qty;
    double salesPrice, tax,discount, total;
}
