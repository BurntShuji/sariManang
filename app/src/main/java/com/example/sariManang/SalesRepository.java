package com.example.sariManang;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Repository class for managing sales transaction records.
 * Provides access to sales history and calculates daily sales metrics.
 */
public class SalesRepository {


    /** Model representing an individual sale record. */
    public static class SaleRecord {
        public String date;
        public String item;
        public int qty;
        public double price;
        public long timestamp;
        public SaleRecord(String date, String item, int qty, double price) {
            this.date = date;
            this.item = item;
            this.qty = qty;
            this.price = price;
        }

        public double getTotal() {
            return qty * price;
        }
    }

    private static final List<SaleRecord> records = new ArrayList<>();
    /** Returns formatted date string for today. */
    public static String getTodayFormatted() {
        return new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date());
    }

    /** Adds a new sale transaction. */


    public static void addSale(
        Context context,
        String item,
        int qty,
        double price,
        Runnable onSuccess,
        java.util.function.Consumer<Exception> onFailure
    ) {
      userIDManager userManager = new userIDManager(context);
      String currentUserID = userManager.getUserID();

      if (currentUserID == null || currentUserID.trim().isEmpty()) {
        onFailure.accept(new Exception("User ID is missing."));
        return;
      }

      Map<String, Object> sale = new HashMap<>();
      sale.put("date", getTodayFormatted());
      sale.put("item", item);
      sale.put("quantity", qty);
      sale.put("price", price);
      sale.put("timestamp", System.currentTimeMillis());
      FirebaseFirestore.getInstance()
          .collection("user")
          .document(currentUserID)
          .collection("sales")
          .add(sale)
          .addOnSuccessListener(documentReference -> onSuccess.run())
          .addOnFailureListener(onFailure::accept);
    }



  /** Calculates total sales amount for today. */
    public static double getTodayTotalSales() {
        String today = getTodayFormatted();
        double total = 0;
        for (SaleRecord r : records) {
            if (today.equals(r.date)) {
                total += r.getTotal();
            }
        }
        return total;
    }

    /** Counts total number of transactions recorded for today. */
    public static int getTodayTransactionCount() {
        String today = getTodayFormatted();
        int count = 0;
        for (SaleRecord r : records) {
            if (today.equals(r.date)) {
                count++;
            }
        }
        return count;
    }

  public interface SalesLoadedCallback {
    void onLoaded(List<SaleRecord> records);
  }

  public static void loadRecords(
      String userid,
      dbManager db,
      SalesLoadedCallback callback
  ) {
    db.getAllSales(userid)
        .addOnSuccessListener(querySnapshot -> {
          Log.d("SALES", "Loading sales for user ID: " + userid);
          Log.d("SALES", "Documents found: " + querySnapshot.size());

          records.clear();

          for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            Long quantity = doc.getLong("quantity");
            Double price = doc.getDouble("price");
            Long ts = doc.getLong("timestamp");

            SaleRecord r = new SaleRecord(
                doc.getString("date"),
                doc.getString("item"),
                quantity != null ? quantity.intValue() : 0,
                price != null ? price : 0.0
            );
            r.timestamp = ts != null ? ts : 0L;   // old sales without a timestamp go last
            records.add(r);
          }

          records.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));   // newest first

          callback.onLoaded(new ArrayList<>(records));

        })
        .addOnFailureListener(e ->
            Log.e("SALES", "Failed to load sales", e)
        );
  }
}
