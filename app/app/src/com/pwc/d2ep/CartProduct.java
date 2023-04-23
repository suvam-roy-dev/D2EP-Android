package com.pwc.d2ep;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;

public class CartProduct implements Parcelable {

    public CartProduct(String name, String ID, String pID, String UOM, int qty, double salesPrice, double tax, double discount, double total) {
        this.name = name;
        this.UOM = UOM;
        this.qty = qty;
        this.salesPrice = salesPrice;
        this.tax = tax;
        this.discount = discount;
        this.total = total;
        this.ID = ID;
        this.pID = pID;
    }

    public CartProduct(String name, String ID, String pID, String UOM, int qty, double salesPrice, double tax, double discount, double total, ArrayList<ProductCharges> charges) {
        this.name = name;
        this.UOM = UOM;
        this.qty = qty;
        this.salesPrice = salesPrice;
        this.tax = tax;
        this.discount = discount;
        this.total = total;
        this.ID = ID;
        this.pID = pID;
        this.charges = charges;
    }
    String name, UOM, ID,pID;
    int qty;
    double salesPrice, tax,discount, total;
    ArrayList<ProductCharges> charges;

    protected CartProduct(Parcel in) {
        name = in.readString();
        UOM = in.readString();
        ID = in.readString();
        pID = in.readString();
        qty = in.readInt();
        salesPrice = in.readDouble();
        tax = in.readDouble();
        discount = in.readDouble();
        total = in.readDouble();
        charges = in.createTypedArrayList(ProductCharges.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(UOM);
        dest.writeString(ID);
        dest.writeString(pID);
        dest.writeInt(qty);
        dest.writeDouble(salesPrice);
        dest.writeDouble(tax);
        dest.writeDouble(discount);
        dest.writeDouble(total);
        dest.writeTypedList(charges);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CartProduct> CREATOR = new Creator<CartProduct>() {
        @Override
        public CartProduct createFromParcel(Parcel in) {
            return new CartProduct(in);
        }

        @Override
        public CartProduct[] newArray(int size) {
            return new CartProduct[size];
        }
    };
}
