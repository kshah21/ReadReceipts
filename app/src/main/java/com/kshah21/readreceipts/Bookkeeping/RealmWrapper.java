package com.kshah21.readreceipts.Bookkeeping;

import android.content.Context;

import com.github.mikephil.charting.data.LineData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by kunal on 12/20/17.
 */

public class RealmWrapper {

    private Realm realm;

    /**
     * Init realmDB
     */
    public RealmWrapper(Context context){
        Realm.init(context);
        realm=Realm.getDefaultInstance();
        RealmConfiguration realmConfiguration = new RealmConfiguration
                .Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
    }

    public void close(){
        realm.close();
    }

    /**
     * Create expense object from total and store into Realm
     */
    public void createExpense(final String result){
        realm.executeTransaction(new Realm.Transaction(){
            @Override
            public void execute(Realm realm) {
                Expense expense = realm.createObject(Expense.class);
                expense.setTotal(result);
                expense.setCategory("Groceries");
                expense.setStore("Target");
                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                try {
                    date = sdf.parse("07/06/2017");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                expense.setDate(date);
                expense.setCreatedAt(new Date());

            }
        });
        queryExpense();
    }

    /**
     * Simple query with search parameters
     */
    public String queryExpense(){
        RealmResults<Expense> results = realm.where(Expense.class).equalTo("store", "Target").findAll();
        String target_expense="No Query Results";
        for(Expense expense:results) {
            target_expense = ("Total: " + expense.getTotal() + "\n");
            target_expense+=("Category: " + expense.getCategory() + "\n");
            target_expense+=("Store: " + expense.getStore() + "\n");
            target_expense+=("Date: " + expense.getDate().toString() + "\n");
            target_expense+=("Created At: " + expense.getCreatedAt() + "\n");
            target_expense+=("\n");

        }
        System.out.println("Query Size: " + results.size());
        return target_expense;
        //TODO Chart Data
    }
}
