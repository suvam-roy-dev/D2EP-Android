package com.pwc.d2ep;

public class SalesOrder {
    String oderID, orderName, status, customerName, branchName, date, productCount, totalCost;

    public SalesOrder(String oderID, String orderName, String status, String customerName, String branchName, String date, String productCount, String totalCost) {
        this.oderID = oderID;
        this.orderName = orderName;
        this.status = status;
        this.customerName = customerName;
        this.branchName = branchName;
        this.date = date;
        this.productCount = productCount;
        this.totalCost = totalCost;
    }

    public String getOderID() {
        return oderID;
    }

    public String getOrderName() {
        return orderName;
    }

    public String getStatus() {
        return status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getDate() {
        return date;
    }

    public String getProductCount() {
        return productCount;
    }

    public String getTotalCost() {
        return totalCost;
    }
}
