package com.pwc.d2ep;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class ProductCharges implements Parcelable {

    String id, productId, chargeName, chargeMaster, chargeValueType, chargeType, transactionAccount;
    double chargeValue, amount;

    public ProductCharges(String id, String productId, String chargeName, String chargeMaster, String chargeValueType, String chargeType, String transactionAccount, double chargeValue, double amount) {
        this.id = id;
        this.productId = productId;
        this.chargeName = chargeName;
        this.chargeMaster = chargeMaster;
        this.chargeValueType = chargeValueType;
        this.chargeType = chargeType;
        this.transactionAccount = transactionAccount;
        this.chargeValue = chargeValue;
        this.amount = amount;
    }

    protected ProductCharges(Parcel in) {
        id = in.readString();
        productId = in.readString();
        chargeName = in.readString();
        chargeMaster = in.readString();
        chargeValueType = in.readString();
        chargeType = in.readString();
        transactionAccount = in.readString();
        chargeValue = in.readDouble();
        amount = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(productId);
        dest.writeString(chargeName);
        dest.writeString(chargeMaster);
        dest.writeString(chargeValueType);
        dest.writeString(chargeType);
        dest.writeString(transactionAccount);
        dest.writeDouble(chargeValue);
        dest.writeDouble(amount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ProductCharges> CREATOR = new Creator<ProductCharges>() {
        @Override
        public ProductCharges createFromParcel(Parcel in) {
            return new ProductCharges(in);
        }

        @Override
        public ProductCharges[] newArray(int size) {
            return new ProductCharges[size];
        }
    };
}
