package com.example.sariManang;

import static android.content.ContentValues.TAG;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  public Task<QuerySnapshot> getAllUsers(){
    return db.collection("user").get();
  }



  public Task<String> getDebtID(String userid, String name, double amount, String due){
    return db.collection("user").document(userid).collection("debt")
        .whereEqualTo("customer", name).whereEqualTo("amount",amount).whereEqualTo("dueDate", due)
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
    return db.collection("user").document(userid).collection("suppliers").whereEqualTo("supplier", name).whereEqualTo("contact", contact)
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

  public void updateLastActive(String userID) {
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    db.collection("user")
        .document(userID)
        .update("lastActive", FieldValue.serverTimestamp())
        .addOnFailureListener(e ->
            Log.e("LastActive", "Failed to update", e)
        );
  }


  public void addTofield(String collection, Map<String, Object> item, String userid){
    item.putIfAbsent("timestamp", System.currentTimeMillis());
    db.collection("user").document(userid).collection(collection)
        .add(item)
        .addOnSuccessListener(documentReference -> {
          Log.d(TAG, "added with ID: " + documentReference.getId());
        })
        .addOnFailureListener(e -> {
          Log.w(TAG, "Error adding", e);
        });
  }

  public Task<QuerySnapshot> LoginChecker(String u, String p){
      return FirebaseFirestore.getInstance()
          .collection("user")
          .whereEqualTo("username", u)
          .whereEqualTo("password", p)
          .get();
  }


  // Finds an existing stock doc by item name (case-insensitive)
  public Task<DocumentSnapshot> getStockDoc(String userid, String itemName) {
    return db.collection("user").document(userid).collection("stock").get()
        .continueWith(task -> {
          if (!task.isSuccessful()) throw task.getException();
          for (DocumentSnapshot doc : task.getResult().getDocuments()) {
            String n = doc.getString("item");
            if (n != null && n.equalsIgnoreCase(itemName.trim())) return doc;
          }
          return null;
        });
  }

  // Adds to the existing doc, or creates one if the item is new
  public Task<Void> addOrUpdateStock(String userid, Map<String, Object> stock) {
    String name = (String) stock.get("item");
    int qty = (Integer) stock.get("quantity");

    return getStockDoc(userid, name).continueWithTask(task -> {
      if (!task.isSuccessful()) throw task.getException();
      DocumentSnapshot existing = task.getResult();

      if (existing == null) {
        return db.collection("user").document(userid).collection("stock")
            .add(stock).continueWith(t -> (Void) null);
      }

      Map<String, Object> updates = new HashMap<>();
      updates.put("quantity", FieldValue.increment(qty));
      updates.put("timestamp", System.currentTimeMillis());
      if ((double) stock.get("price") > 0) updates.put("price", stock.get("price"));
      if (!((String) stock.get("expiry")).isEmpty()) updates.put("expiry", stock.get("expiry"));
      if (!((String) stock.get("batch")).isEmpty()) updates.put("batch", stock.get("batch"));
      if ((int) stock.get("alertAt") > 0) updates.put("alertAt", stock.get("alertAt"));

      return existing.getReference().update(updates);
    });
  }

  public Task<Void> subtractStock(String userid, String itemName, int soldQty) {
    return getStockDoc(userid, itemName).continueWithTask(task -> {
      if (!task.isSuccessful()) throw task.getException();
      DocumentSnapshot doc = task.getResult();
      if (doc == null) throw new IllegalStateException("Item not found: " + itemName);

      Long currentQty = doc.getLong("quantity");
      if (currentQty == null || soldQty <= 0 || soldQty > currentQty) {
        throw new IllegalStateException("Insufficient stock for " + itemName);
      }
      return doc.getReference().update("quantity", FieldValue.increment(-soldQty));
    });
  }

  private static final long RETENTION_MS = 30L * 24 * 60 * 60 * 1000;

  private void purge(String userid, String collection, long cutoff) {
    db.collection("user").document(userid).collection(collection)
        .whereLessThan("timestamp", cutoff)
        .get()
        .addOnSuccessListener(snap -> {
          List<DocumentSnapshot> docs = snap.getDocuments();
          for (int i = 0; i < docs.size(); i += 400) {
            WriteBatch batch = db.batch();
            for (DocumentSnapshot d : docs.subList(i, Math.min(i + 400, docs.size()))) {
              batch.delete(d.getReference());
            }
            batch.commit().addOnFailureListener(e -> Log.e("PURGE", collection + " failed", e));
          }
        })
        .addOnFailureListener(e -> Log.e("PURGE", collection + " query failed", e));
  }

  public void purgeOldData(String userid) {
    long cutoff = System.currentTimeMillis() - RETENTION_MS;
    purge(userid, "sales", cutoff);
    purge(userid, "debt", cutoff);
    purge(userid, "stock", cutoff);
    purge(userid, "suppliers", cutoff);
  }

  public Task<Void> updateUser(String userid, String name, String email) {
    return db.collection("user").document(userid)
        .update("username", name, "email", email);
  }

  // Deletes every doc in one of the user's subcollections
  private Task<Void> deleteCollection(String userid, String collection) {
    return db.collection("user").document(userid).collection(collection).get()
        .continueWithTask(task -> {
          if (!task.isSuccessful()) throw task.getException();
          List<DocumentSnapshot> docs = task.getResult().getDocuments();
          List<Task<Void>> commits = new ArrayList<>();
          for (int i = 0; i < docs.size(); i += 400) {
            WriteBatch batch = db.batch();
            for (DocumentSnapshot d : docs.subList(i, Math.min(i + 400, docs.size()))) {
              batch.delete(d.getReference());
            }
            commits.add(batch.commit());
          }
          return Tasks.whenAll(commits);
        });
  }

  public Task<Void> deleteUser(String userid) {
    List<Task<Void>> subs = new ArrayList<>();
    for (String c : new String[]{"sales", "debt", "stock", "suppliers"}) {
      subs.add(deleteCollection(userid, c));
    }
    return Tasks.whenAll(subs).continueWithTask(task -> {
      if (!task.isSuccessful()) throw task.getException();
      return db.collection("user").document(userid).delete();
    });
  }

  public Task<DocumentSnapshot> getUserDoc(String userid) {
    return db.collection("user").document(userid).get();
  }



}
