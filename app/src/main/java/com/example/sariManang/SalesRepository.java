package com.example.sariManang;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    static {
        // Initial sample sales records for today
        String today = getTodayFormatted();
        records.add(new SaleRecord(today, "Pocari Sweat", 2, 36.5));
        records.add(new SaleRecord(today, "Always Ultra Thin", 3, 62.0));
        records.add(new SaleRecord(today, "Nestle All-Purpose Cream", 1, 28.0));
    }

    /** Returns formatted date string for today. */
    public static String getTodayFormatted() {
        return new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date());
    }

    /** Returns all sales records. */
    public static List<SaleRecord> getRecords() {
        return records;
    }

    /** Adds a new sale transaction. */
    public static void addSale(Context context, String item, int qty, double price) {
        records.add(0, new SaleRecord(getTodayFormatted(), item, qty, price));
      String currentUserID = context.getSharedPreferences("MyUserPrefs", Context.MODE_PRIVATE)
          .getString("userid", null);
      Map<String, Object> sale = new HashMap<>();
        sale.put("date", getTodayFormatted());
        sale.put("item", item);
        sale.put("quantity", qty);
        sale.put("price", price);
        sale.put("userID", currentUserID);


      FirebaseFirestore db = FirebaseFirestore.getInstance();

      db.collection("user").document(currentUserID).collection("sales")
          .add(sale)
          .addOnSuccessListener(documentReference -> {
            Log.d(TAG, "Sale added with ID: " + documentReference.getId());
          })
          .addOnFailureListener(e -> {
            Log.w(TAG, "Error adding sale", e);
          });

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
}
