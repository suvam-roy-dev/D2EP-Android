package com.pwc.d2ep;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class DocCharge implements Parcelable {

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

    protected DocCharge(Parcel in) {
        name = in.readString();
        id = in.readString();
        masterID = in.readString();
        isApplied = in.readByte() != 0;
        amount = in.readDouble();
        isFloating = in.readByte() != 0;
        isTax = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(id);
        dest.writeString(masterID);
        dest.writeByte((byte) (isApplied ? 1 : 0));
        dest.writeDouble(amount);
        dest.writeByte((byte) (isFloating ? 1 : 0));
        dest.writeByte((byte) (isTax ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DocCharge> CREATOR = new Creator<DocCharge>() {
        @Override
        public DocCharge createFromParcel(Parcel in) {
            return new DocCharge(in);
        }

        @Override
        public DocCharge[] newArray(int size) {
            return new DocCharge[size];
        }
    };
}
