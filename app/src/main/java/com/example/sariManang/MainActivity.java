package com.example.sariManang;

import static android.content.ContentValues.TAG;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MainActivity handles the application's homepage dashboard.
 * Displays date, store greeting, quick action buttons, today's sales summary, overdue utang list, and low stock warnings.
 */
public class MainActivity extends AppCompatActivity {

  private userIDManager userid;
  private String id;
  private dbManager DBHelper;


  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);

    DBHelper = new dbManager();
    userid = new userIDManager(MainActivity.this);
    id = userid.getUserID();
    setContentView(R.layout.homepage);

    String user = userid.getUserID();

    android.content.SharedPreferences sp = getSharedPreferences("sari_settings", MODE_PRIVATE);
    String todayKey = SalesRepository.getTodayFormatted();
    if (id != null && !todayKey.equals(sp.getString("last_purge", ""))) {
      DBHelper.purgeOldData(id);
      sp.edit().putString("last_purge", todayKey).apply();
    }


    // Section 1: Set current date and user greeting
    ((TextView) findViewById(R.id.tvDate)).setText(new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date()));
    DBHelper.getFromUsers(user, "username")
        .addOnSuccessListener(result -> {
          ((TextView) findViewById(R.id.tvHello)).setText("Hello, " + result);
        })
        .addOnFailureListener(e -> {
          Log.e(TAG, "Failed to get username", e);
        });



    //dummy stocks lang 'to ha
    List<InventoryActivity.Item> testInventory = new ArrayList<>();
// Section 2: Set up quick action listeners
    View btnNewSale = findViewById(R.id.btnNewSale);
    if (btnNewSale != null) {
      btnNewSale.setOnClickListener(v ->
          InventoryActivity.refreshItems(DBHelper, id, () ->
              DialogHelper.showNewSale(this, InventoryActivity.items, (cartItems, grandTotal) -> {
                if (cartItems.isEmpty()) return;

                final int[] remaining = {cartItems.size()};
                final boolean[] failed = {false};

                for (DialogHelper.CartItem ci : cartItems) {
                  SalesRepository.addSale(this, ci.item, ci.qty, ci.price,
                      () -> {
                        // stock was already deducted by the dialog
                        remaining[0]--;
                        if (remaining[0] == 0 && !failed[0]) {
                          runOnUiThread(() -> {
                            renderTodaySales();
                            renderBestLeast();
                            Toast.makeText(this, "Sale saved successfully", Toast.LENGTH_SHORT).show();
                          });
                        }
                      },
                      error -> runOnUiThread(() -> {
                        failed[0] = true;
                        Toast.makeText(this, "Failed to save sale: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                      }));
                }
              })));
    }

    findViewById(R.id.btnRecordUtang).setOnClickListener(v ->
        DialogHelper.showRecordUtang(this, (customer, amount, date) ->
            UtangRepository.addUtang(this, customer, amount, date, this::renderUtang)));

    findViewById(R.id.btnAddStock).setOnClickListener(v ->
        InventoryActivity.refreshItems(DBHelper, id, () ->
            DialogHelper.showAddStock(this, InventoryActivity.getExistingItemNames(),
                (n, q, p, e, bt, a) -> {
                  Map<String, Object> stock = new HashMap<>();
                  stock.put("item", n.trim());
                  stock.put("quantity", q);
                  stock.put("price", p);
                  stock.put("expiry", e);
                  stock.put("batch", bt);
                  stock.put("alertAt", a);
                  DBHelper.addOrUpdateStock(id, stock)
                      .addOnFailureListener(err -> Log.e("STOCK", "add failed", err));
                })));
    // Section 3: Configure bottom navigation bar
    NavigationHelper.setup(this, R.id.navHome);

    // Section 4: Initial UI rendering
    renderTodaySales();
    renderUtang();
    renderBestLeast();
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Refresh dashboard data whenever activity returns to foreground
    renderTodaySales();
    renderUtang();
    renderBestLeast();
  }

  /**
   * Calculates and updates "Today's Total Sales" and transaction count on homepage.
   */
  private void renderTodaySales() {
    SalesRepository.loadRecords(id, DBHelper, records -> {
      TextView tvSales = findViewById(R.id.tvTodaySales);
      TextView tvTrans = findViewById(R.id.tvTodayTransactions);
      TextView est = findViewById(R.id.tvHomeEstimate);
      ProgressBar pb = findViewById(R.id.pbHomeEstimate);

      String today = SalesRepository.getTodayFormatted();
      double total = 0;
      int count = 0;
      for (SalesRepository.SaleRecord r : records) {
        if (today.equals(r.date)) {
          total += r.getTotal();
          count++;
        }
      }

      if (tvSales != null) tvSales.setText(String.format(Locale.US, "₱ %,.2f", total));
      if (tvTrans != null) tvTrans.setText(count + (count == 1 ? " transaction today" : " transactions today"));

      int n = records.size();
      if (pb != null) pb.setProgress(Math.min(n, 5));
      if (est != null) {
        if (n < 5) {
          est.setText("Log at least 5 sales to unlock estimates. " + n + " / 5 logged.");
        } else {
          double sum = 0;
          for (SalesRepository.SaleRecord r : records) sum += r.getTotal();
          est.setText(String.format(Locale.US,
              "Average per sale: ₱%,.2f (based on %d records)", sum / n, n));
        }
      }
    });
  }

  /**
   * Dynamically populates the unpaid and overdue utang list on homepage.
   */
  private void renderUtang() {
    LinearLayout container = findViewById(R.id.listOverdueUtang);
    if (container == null) return;

    container.removeAllViews();

    UtangRepository.getUnpaidUtangList(id, DBHelper)
        .addOnSuccessListener(list -> {

          // Clear again after the data finishes loading
          container.removeAllViews();

          double total = 0;
          int count = 0;

          for (Utang u : list) {
            total += u.amount;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            row.setPadding(0, 8, 0, 8);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvName = new TextView(this);
            tvName.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ));
            tvName.setText(u.customer);
            tvName.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            tvName.setTextSize(13);

            TextView tvAmount = new TextView(this);
            tvAmount.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            tvAmount.setText(String.format(Locale.US, "₱%,.2f", u.amount));
            tvAmount.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            tvAmount.setTextSize(13);
            tvAmount.setPadding(0, 0, 24, 0);

            TextView tvStatus = new TextView(this);
            tvStatus.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            tvStatus.setText(u.getStatusText());
            tvStatus.setTextSize(13);
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);

            if (u.isOverdue()) {
              tvStatus.setTextColor(
                  ContextCompat.getColor(this, R.color.red_dark)
              );
            } else {
              tvStatus.setTextColor(
                  ContextCompat.getColor(this, R.color.text_dark)
              );
            }

            row.addView(tvName);
            row.addView(tvAmount);
            row.addView(tvStatus);

            container.addView(row);
            count++;
          }

          if (count == 0) {
            TextView empty = new TextView(this);
            empty.setText("No pending utang");
            empty.setTextColor(ContextCompat.getColor(this, R.color.muted));
            empty.setTextSize(12);
            empty.setPadding(0, 8, 0, 8);
            container.addView(empty);
          }

          TextView tvSummary = findViewById(R.id.tvHomeUtangSummary);

          if (tvSummary != null) {
            tvSummary.setText(
                String.format(Locale.US, "Total: ₱%,.2f", total)
            );
          }
        })
        .addOnFailureListener(e ->
            Log.e("UTANG", "Failed to load unpaid debts", e)
        );
  }

  private void renderLowStock() {
    LinearLayout container = findViewById(R.id.listLowStock);
    if (container == null) return;

    InventoryActivity.refreshItems(DBHelper, id, () -> {
      container.removeAllViews();
      int defaultThreshold = SettingsActivity.getLowStockDefault(this);
      int count = 0;

      if (InventoryActivity.items != null) {
        for (InventoryActivity.Item item : InventoryActivity.items) {
          if (item.qty <= defaultThreshold) {
            LinearLayout row = new LinearLayout(this);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8, 0, 8);

            LinearLayout nameCol = new LinearLayout(this);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            colParams.setMargins((int) (10 * getResources().getDisplayMetrics().density), 0, 0, 0);
            nameCol.setLayoutParams(colParams);
            nameCol.setOrientation(LinearLayout.VERTICAL);

            TextView tvName = new TextView(this);
            tvName.setText(item.name);
            tvName.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            tvName.setTextSize(13);
            tvName.setTypeface(null, Typeface.BOLD);
            nameCol.addView(tvName);

            TextView tvQty = new TextView(this);
            tvQty.setText(item.qty + " left");
            tvQty.setTextColor(ContextCompat.getColor(this, R.color.red_dark));
            tvQty.setTextSize(13);
            tvQty.setTypeface(null, Typeface.BOLD);

            row.addView(nameCol);
            row.addView(tvQty);

            container.addView(row);
            count++;
          }
        }
      }

      if (count == 0) {
        TextView empty = new TextView(this);
        empty.setText("All stock levels normal");
        empty.setTextColor(ContextCompat.getColor(this, R.color.muted));
        empty.setTextSize(12);
        empty.setPadding(0, 8, 0, 8);
        container.addView(empty);
      }
    });
  }

  private void renderBestLeast() {
    TextView tvBest = findViewById(R.id.tvHomeBest);
    TextView tvLeast = findViewById(R.id.tvHomeLeast);

    if (tvBest == null || tvLeast == null) return;

    SalesRepository.loadRecords(id, DBHelper, records -> {
      Map<String, Integer> totals = new LinkedHashMap<>();

      for (SalesRepository.SaleRecord r : records) {
        totals.merge(r.item, r.qty, Integer::sum);
      }

      String best = null, least = null;

      for (Map.Entry<String, Integer> e : totals.entrySet()) {
        if (best == null ||
            e.getValue() > totals.get(best)) {
          best = e.getKey();
        }

        if (least == null ||
            e.getValue() < totals.get(least)) {
          least = e.getKey();
        }
      }

      tvBest.setText(best == null ? "—" :
          best + "\nSold this month: " + totals.get(best));

      tvLeast.setText(least == null ? "—" :
          least + "\nSold this month: " + totals.get(least));
    });
  }
}