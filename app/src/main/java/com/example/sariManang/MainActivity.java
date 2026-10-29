package com.example.sariManang;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    testInventory.add(new InventoryActivity.Item("Lucky Me Instant Noodle", 100, 15.00, "", "", 10));
    testInventory.add(new InventoryActivity.Item("Coca-Cola 1.5L", 50, 65.00, "", "", 5));
    testInventory.add(new InventoryActivity.Item("Bear Brand Powdered Milk 33g", 80, 18.50, "", "", 10));

    // Section 2: Set up quick action listeners
    View btnNewSale = findViewById(R.id.btnNewSale);
    if (btnNewSale != null) {
      btnNewSale.setOnClickListener(v ->
          DialogHelper.showNewSale(this, testInventory, (cartItems, grandTotal) -> {
            for (DialogHelper.CartItem ci : cartItems) {
              SalesRepository.addSale(this, ci.item, ci.qty, ci.price);
            }
            renderTodaySales();
          })
      );
    }

    findViewById(R.id.btnRecordUtang).setOnClickListener(v -> DialogHelper.showRecordUtang(this, (customer, amount, date) -> {
      UtangRepository.addUtang(this, customer, amount, date);
      renderUtang();
    }));

    findViewById(R.id.btnAddStock).setOnClickListener(v -> DialogHelper.showAddStock(this, InventoryActivity.getExistingItemNames(), null));

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

      String today = SalesRepository.getTodayFormatted();
      double total = 0;
      int count = 0;

      for (SalesRepository.SaleRecord r : records) {
        if (today.equals(r.date)) {
          total += r.getTotal();
          count++;
        }
      }

      if (tvSales != null) {
        tvSales.setText(String.format(
            Locale.US, "₱ %,.2f", total
        ));
      }

      if (tvTrans != null) {
        tvTrans.setText(count +
            (count == 1
                ? " transaction today"
                : " transactions today"));
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