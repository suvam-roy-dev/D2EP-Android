package com.pwc.d2ep;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;

public class OrderObject implements Parcelable {
    String dealer,branch,customer,state,narration, orderName, orderID;
    String dealerID, branchID, customerID;
    ArrayList<CartProduct> products;
    ArrayList<DocCharge> docCharges;
    ArrayList<ProductCharges> combinedCharges;


    public OrderObject(String orderName, String orderID, String dealer, String dealerID, String branch, String branchID, String customer, String customerID, String state, String narration, ArrayList<CartProduct> products, ArrayList<DocCharge> docCharges) {
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

        this.orderName = orderName;
        this.orderID = orderID;
    }

    public OrderObject(String orderName, String orderID, String dealer, String dealerID, String branch, String branchID, String customer, String customerID, String state, String narration, ArrayList<CartProduct> products, ArrayList<DocCharge> docCharges, ArrayList<ProductCharges> productCharges) {
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

        this.orderName = orderName;
        this.orderID = orderID;
        this.combinedCharges = productCharges;
    }


    protected OrderObject(Parcel in) {
        dealer = in.readString();
        branch = in.readString();
        customer = in.readString();
        state = in.readString();
        narration = in.readString();
        orderName = in.readString();
        orderID = in.readString();
        dealerID = in.readString();
        branchID = in.readString();
        customerID = in.readString();
        products = in.createTypedArrayList(CartProduct.CREATOR);
        combinedCharges = in.createTypedArrayList(ProductCharges.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(dealer);
        dest.writeString(branch);
        dest.writeString(customer);
        dest.writeString(state);
        dest.writeString(narration);
        dest.writeString(orderName);
        dest.writeString(orderID);
        dest.writeString(dealerID);
        dest.writeString(branchID);
        dest.writeString(customerID);
        dest.writeTypedList(products);
        dest.writeTypedList(combinedCharges);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<OrderObject> CREATOR = new Creator<OrderObject>() {
        @Override
        public OrderObject createFromParcel(Parcel in) {
            return new OrderObject(in);
        }

        @Override
        public OrderObject[] newArray(int size) {
            return new OrderObject[size];
        }
    };
}
