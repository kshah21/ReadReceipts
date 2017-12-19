package com.kshah21.readreceipts.Bookkeeping;


import java.util.Date;

import io.realm.RealmObject;

public class Expense extends RealmObject{
    private String total;
    private Date date;
    private String category;
    private String store;
    private Date createdAt;

    public String getTotal(){
        return total;
    }
    public Date getDate(){
        return date;
    }
    public String getCategory(){
        return category;
    }
    public String getStore(){
        return store;
    }
    public Date getCreatedAt() {
        return createdAt;
    }
    public void setTotal(String total){
        this.total=total;
    }
    public void setDate(Date date){
        this.date=date;
    }
    public void setCategory(String category){
        this.category=category;
    }
    public void setStore(String store){
        this.store=store;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Expense(){

    }
}

