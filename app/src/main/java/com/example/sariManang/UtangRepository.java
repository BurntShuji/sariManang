package com.example.sariManang;

import static android.content.ContentValues.TAG;

import static com.example.sariManang.SalesRepository.getTodayFormatted;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared repository for managing customer utang (credit) records across activities.
 */
public class UtangRepository {
    private static final List<Utang> utangList = new ArrayList<>();

    static {
        // Initial sample utang records
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -8);
        String date8DaysAgo = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -3);
        String date3DaysAgo = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());

        utangList.add(new Utang("Olivia Rodrigo", 1.00, date8DaysAgo));
        utangList.add(new Utang("Juan Dela Cruz", 150.00, date3DaysAgo));
    }

    /** Returns all pending (unpaid) utang records. */
    public static List<Utang> getUnpaidUtangList() {
        List<Utang> unpaid = new ArrayList<>();
        for (Utang u : utangList) {
            if (!u.paid) unpaid.add(u);
        }
        return unpaid;
    }

    /** Adds a new utang entry. */
    public static void addUtang(Context context, String customer, double amount, String dueDate) {
        utangList.add(0, new Utang(customer, amount, dueDate));
      String currentUserID = context.getSharedPreferences("MyUserPrefs", Context.MODE_PRIVATE)
          .getString("userid", null);
      Map<String, Object> utang = new HashMap<>();
      utang.put("customer", customer);
      utang.put("amount", amount);
      utang.put("dueDate", dueDate);

      FirebaseFirestore db = FirebaseFirestore.getInstance();

      db.collection("user").document(currentUserID).collection("debt")
          .add(utang)
          .addOnSuccessListener(documentReference -> {
            Log.d(TAG, "utang added with ID: " + documentReference.getId());
          })
          .addOnFailureListener(e -> {
            Log.w(TAG, "Error adding sale", e);
          });


    }

    /** Marks a customer utang record as paid/settled. */
    public static void markPaid(Utang utang) {
        utang.paid = true;
    }
}
