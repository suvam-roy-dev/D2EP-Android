package com.pwc.d2ep;

import java.io.Serializable;
import java.util.ArrayList;

public class OrderObject implements Serializable {
    String dealer,branch,customer,state,narration;
    String dealerID, branchID, customerID;
    ArrayList<CartProduct> products;
    ArrayList<DocCharge> docCharges;


    public OrderObject(String dealer, String dealerID, String branch, String branchID, String customer, String customerID, String state, String narration, ArrayList<CartProduct> products, ArrayList<DocCharge> docCharges) {
        this.dealer = dealer;
        this.branch = branch;
        this.customer = customer;
        this.state = state;
        this.narration = narration;
        this.products = products;

        this.dealerID = dealerID;
        this.branchID = branchID;
        this.customerID = customerID;
        this.docCharges = docCharges;
    }
}
