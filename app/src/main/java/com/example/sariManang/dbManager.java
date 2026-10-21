package com.example.sariManang;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
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
  public Task<QuerySnapshot> getFromSuppliers(String userid, String field){
    return null;
  }

  public Task<QuerySnapshot> getInv(String userid, String field){
    if(field == null){
      return db.collection("user").document(userid).collection("stock").get();
    }
    return null;
  }
  public void getFromSales(){}
  public void getFromDebt(){}

}
