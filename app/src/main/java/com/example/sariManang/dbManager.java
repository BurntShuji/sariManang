package com.example.sariManang;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class dbManager {
  FirebaseFirestore db = FirebaseFirestore.getInstance();
  public Task<String> getFromUsers(String userid, String field){
    return db.collection("user").document(userid)
        .get()
        .continueWith(task -> {
          if (!task.isSuccessful()) {
            throw task.getException();
          }
        return task.getResult().getString(field);
        });
  }
  public Task<QuerySnapshot> getAllSales(String userid){
    return db.collection("user").document(userid).collection("sales").get();
  }

  public Task<QuerySnapshot> getAlldebt(String userid){
    return db.collection("user").document(userid).collection("debt").get();
  }

  public Task<QuerySnapshot> getAllSuppliers(String userid){
    return db.collection("user").document(userid).collection("suppliers").get();
  }

  public Task<String> getDebtID(String userid, String name, double amount, String due){
    return db.collection("user").document(userid).collection("debt").whereEqualTo("customer", name).whereEqualTo("amount",amount).whereEqualTo("dueDate", due)
        .get()
        .continueWith(task -> {
          if (!task.isSuccessful()) {
            throw task.getException();
          }

          QuerySnapshot snapshot = task.getResult();

          if (snapshot.isEmpty()) {
            return null; // No matching debt found
          }

          DocumentSnapshot doc = snapshot.getDocuments().get(0);
          return doc.getId();
        });
  }

  public Task<Void> updateDebt(String userid, String name, double amount, String due) {
    return getDebtID(userid, name, amount, due)
        .continueWithTask(task -> {
          if (!task.isSuccessful()) {
            throw task.getException();
          }

          String debtID = task.getResult();

          if (debtID == null) {
            throw new Exception("Debt not found");
          }

          return db.collection("user")
              .document(userid)
              .collection("debt")
              .document(debtID)
              .update("paid", true);
        });
  }


  public Task<QuerySnapshot> getInv(String userid, String field){
    if(field == null){
      return db.collection("user").document(userid).collection("stock").get();
    }
    return null;
  }

  public Task<String> getSupplierID(String userid, String name, String contact){
    return db.collection("user").document(userid).collection("suppliers").whereEqualTo("customer", name).whereEqualTo("contact", contact)
        .get()
        .continueWith(task -> {
          if (!task.isSuccessful()) {
            throw task.getException();
          }

          QuerySnapshot snapshot = task.getResult();

          if (snapshot.isEmpty()) {
            return null; // No matching debt found
          }

          DocumentSnapshot doc = snapshot.getDocuments().get(0);
          return doc.getId();
        });
  }


  public Task<Void> updateDelivery(String userid, String name, String contact, int good, int bad) {
    return getSupplierID(userid, name, contact)
        .continueWithTask(task -> {
          if (!task.isSuccessful()) {
            throw task.getException();
          }

          String supplierID = task.getResult();

          if (supplierID == null) {
            throw new Exception("Debt not found");
          }

          return db.collection("user")
              .document(userid)
              .collection("suppliers")
              .document(supplierID)
              .update(
                  "good", good,
                  "bad", bad
              );
        });
  }

}
