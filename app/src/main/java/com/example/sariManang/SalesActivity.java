package com.example.sariManang;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SalesActivity displays sales transaction history, sales estimates, best/least selling analytics,
 * and unpaid utang records with settlement actions.
 */
public class SalesActivity extends AppCompatActivity {

    private boolean expanded = false;
  private userIDManager user;
  private String id;
  private dbManager db = new dbManager();



    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

      user = new userIDManager(SalesActivity.this);
      id = user.getUserID();
        setContentView(R.layout.salespage);

        // Section 1: Set up toolbar and navigation
        NavigationHelper.setupToolbar(this, "Sales");
        NavigationHelper.setup(this, R.id.navSales);

      // Section 2: Quick action button listeners
      findViewById(R.id.btnLogSale).setOnClickListener(v ->
          DialogHelper.showNewSale(this, InventoryActivity.items, (cartItems, grandTotal) -> {
            for (DialogHelper.CartItem ci : cartItems) {
              SalesRepository.addSale(this, ci.item, ci.qty, ci.price);
            }
            renderSales();
          })
      );

        findViewById(R.id.btnRecordUtang).setOnClickListener(v -> DialogHelper.showRecordUtang(this, (cust, amt, date) -> {
            UtangRepository.addUtang(this, cust, amt, date);
            renderUtang();
        }));

        findViewById(R.id.tvViewFull).setOnClickListener(v -> {
            expanded = !expanded;
            ((TextView) v).setText(expanded ? "Show Less" : "View Full History");
            renderSales();
        });

        // Section 3: Initial rendering
        renderSales();
        renderUtang();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh sales history and utang lists when returning to foreground
        renderSales();
        renderUtang();
    }

    /** Helper function to create formatted TextView cells for rows. */
    private TextView tv(String t, float w, int gravity, boolean bold) {
        TextView x = new TextView(this);
        x.setLayoutParams(new LinearLayout.LayoutParams(0, -2, w));
        x.setText(t);
        x.setTextSize(13);
        x.setTextColor(0xFF111111);
        x.setGravity(gravity);
        if (bold) x.setTypeface(null, android.graphics.Typeface.BOLD);
        return x;
    }

    /** Renders sales history, average estimates, and best/least selling item analytics. */
    private void renderSales() {
        LinearLayout list = findViewById(R.id.listHistory);
        list.removeAllViews();

        List<SalesRepository.SaleRecord> records = SalesRepository.getRecords(id, db);
        int n = expanded ? records.size() : Math.min(2, records.size());

        if (records.isEmpty()) {
            TextView e = new TextView(this);
            e.setText("No sales logged yet. Tap + Log Sale.");
            e.setPadding(0, 30, 0, 30);
            list.addView(e);
        }

        for (int i = 0; i < n; i++) {
            SalesRepository.SaleRecord r = records.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setPadding(0, 18, 0, 18);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(tv(r.item + "\n₱" + String.format(Locale.US, "%.2f", r.price), 2f, Gravity.START, false));
            row.addView(tv("Qty: " + r.qty, 1f, Gravity.CENTER, false));
            row.addView(tv(String.format(Locale.US, "Total\n₱%,.2f", r.getTotal()), 1.2f, Gravity.END, true));
            list.addView(row);
        }

        // Calculate sales estimates
        TextView est = findViewById(R.id.tvEstimate);
        if (records.size() < 5) {
            est.setText("Log " + (5 - records.size()) + " more sale(s) to unlock estimates.");
        } else {
            double sum = 0;
            for (SalesRepository.SaleRecord r : records) {
                sum += r.getTotal();
            }
            est.setText(String.format(Locale.US, "Average per sale: ₱%,.2f (based on %d records)", sum / records.size(), records.size()));
        }

        // Calculate best-selling and least-selling products
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (SalesRepository.SaleRecord r : records) {
            totals.merge(r.item, r.qty, Integer::sum);
        }

        String best = null, least = null;
        for (Map.Entry<String, Integer> e : totals.entrySet()) {
            if (best == null || e.getValue() > totals.get(best)) best = e.getKey();
            if (least == null || e.getValue() < totals.get(least)) least = e.getKey();
        }

        ((TextView) findViewById(R.id.tvBest)).setText(best == null ? "—" : best + "\nSold this month: " + totals.get(best));
        ((TextView) findViewById(R.id.tvLeast)).setText(least == null ? "—" : least + "\nSold this month: " + totals.get(least));
    }

    /** Renders the list of customer utang records with a "Mark Paid" button for credit settlement. */
    private void renderUtang() {
      LinearLayout list = findViewById(R.id.listUtangSales);
      if (list == null) return;

      list.removeAllViews();

      UtangRepository.getUnpaidUtangList(id, db)
          .addOnSuccessListener(utangList -> {
            list.removeAllViews();

            double totalUtang = 0;

            if (utangList.isEmpty()) {
              TextView e = new TextView(this);
              e.setText("No unpaid utang recorded.");
              e.setPadding(0, 18, 0, 18);
              e.setTextColor(0xFF777777);
              list.addView(e);
            } else {
              for (Utang u : utangList) {
                totalUtang += u.amount;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 14, 0, 14);
                row.setGravity(Gravity.CENTER_VERTICAL);

                // Customer name and due date
                LinearLayout colInfo = new LinearLayout(this);
                colInfo.setOrientation(LinearLayout.VERTICAL);
                colInfo.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f
                ));

                TextView tvName = new TextView(this);
                tvName.setText(u.customer);
                tvName.setTextSize(13);
                tvName.setTextColor(0xFF111111);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);

                TextView tvDue = new TextView(this);
                tvDue.setText("Due: " + u.dueDate);
                tvDue.setTextSize(11);
                tvDue.setTextColor(0xFF777777);

                colInfo.addView(tvName);
                colInfo.addView(tvDue);

                // Status
                TextView tvStatus = new TextView(this);
                tvStatus.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f
                ));
                tvStatus.setText(u.getStatusText());
                tvStatus.setTextSize(12);
                tvStatus.setGravity(Gravity.CENTER);
                tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                tvStatus.setTextColor(
                    u.isOverdue() ? 0xFFD32F2F : 0xFF111111
                );

                // Amount
                TextView tvAmt = new TextView(this);
                tvAmt.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f
                ));
                tvAmt.setText(String.format(Locale.US, "₱%,.2f", u.amount));
                tvAmt.setTextSize(13);
                tvAmt.setTextColor(0xFF111111);
                tvAmt.setGravity(Gravity.END);
                tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);

                // Mark Paid button
                TextView btnPay = new TextView(this);
                LinearLayout.LayoutParams lpBtn =
                    new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                lpBtn.setMargins(16, 0, 0, 0);
                btnPay.setLayoutParams(lpBtn);
                btnPay.setText("Mark Paid");
                btnPay.setTextSize(11);
                btnPay.setTextColor(0xFFFFFFFF);
                btnPay.setBackgroundResource(R.drawable.bg_btn_red);
                btnPay.setPadding(16, 8, 16, 8);

                btnPay.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                        .setTitle("Settle Utang")
                        .setMessage(
                            "Mark " + u.customer +
                                "'s utang of ₱" +
                                String.format(Locale.US, "%.2f", u.amount) +
                                " as paid?"
                        )
                        .setPositiveButton("Yes, Paid", (dialog, which) -> {
                          btnPay.setEnabled(false);

                          UtangRepository.markPaid(
                              u,
                              id,
                              db,
                              () -> {
                                Toast.makeText(
                                    SalesActivity.this,
                                    "Utang marked as paid",
                                    Toast.LENGTH_SHORT
                                ).show();

                                renderUtang();
                              },
                              error -> {
                                btnPay.setEnabled(true);

                                Toast.makeText(
                                    SalesActivity.this,
                                    "Failed to update utang",
                                    Toast.LENGTH_SHORT
                                ).show();
                              }
                          );
                        })
                );

                row.addView(colInfo);
                row.addView(tvStatus);
                row.addView(tvAmt);
                row.addView(btnPay);
                list.addView(row);

                View div = new View(this);
                div.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ));
                div.setBackgroundColor(0xFFE5E5E5);
                list.addView(div);
              }
            }

            // Update total
            TextView tvSummary = findViewById(R.id.tvUtangSummary);
            if (tvSummary != null) {
              tvSummary.setText(
                  String.format(Locale.US, "Total: ₱%,.2f", totalUtang)
              );
            }
          })
          .addOnFailureListener(e ->
              android.util.Log.e("UTANG", "Failed to load unpaid debts", e)
          );
    }
}
