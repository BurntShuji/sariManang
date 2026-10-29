package com.example.sariManang;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * helper class for building and displaying input dialog pop-ups.
 * Handles validation and delegates callback execution to listeners.
 */
public class DialogHelper {

    /** Callback interface for logging a new sale. */
    public interface SaleListener {
        void onSale(String item, int qty, double price);
    }

    /** Callback interface for adding or updating inventory stock. */
    public interface StockListener {
        void onStock(String item, int qty, double price, String expiry, String batch, int alertAt);
    }

    /** Callback interface for adding a new supplier. */
    public interface SupplierListener {
        void onSupplier(String name, String contact);
    }

    /** Callback interface for recording a customer utang. */
    public interface UtangListener {
        void onUtang(String customer, double amount, String dueDate);
    }

    /** Helper to inflate and create a transparent rounded dialog. */
    private static AlertDialog open(Context c, int layout) {
        AlertDialog d = new AlertDialog.Builder(c).setView(layout).create();
        d.show();
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        TextView btnCancel = d.findViewById(R.id.btnCancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> d.dismiss());
        }
        return d;
    }

    /** Validates required text fields and sets error popups. */
    private static boolean req(EditText... fields) {
        boolean ok = true;
        for (EditText e : fields) {
            if (e != null && s(e).isEmpty()) {
                e.setError("Required");
                ok = false;
            }
        }
        return ok;
    }

    /** Returns trimmed string from EditText field. */
    private static String s(EditText e) {
        return e == null ? "" : e.getText().toString().trim();
    }

    /** Parses numerical value from EditText field safely. */
    private static double num(EditText e) {
        try {
            return Double.parseDouble(s(e));
        } catch (Exception x) {
            return 0;
        }
    }

    /** Attaches a DatePickerDialog to an EditText field. */
    private static void datePicker(Context c, EditText e) {
        if (e == null) return;
        e.setFocusable(false);
        e.setOnClickListener(v -> {
            Calendar k = Calendar.getInstance();
            new DatePickerDialog(c, (p, y, m, d) -> e.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)),
                    k.get(Calendar.YEAR), k.get(Calendar.MONTH), k.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    /** Displays "Log Sale" dialog popup. */
    public static void showNewSale(Context c, SaleListener l) {
        AlertDialog d = open(c, R.layout.dialognewsale);
        EditText item = d.findViewById(R.id.etItem), qty = d.findViewById(R.id.etQty), price = d.findViewById(R.id.etPrice);
        TextView total = d.findViewById(R.id.tvTotal);

        TextWatcher w = new TextWatcher() {
            public void beforeTextChanged(CharSequence a, int b, int c2, int d2) {}
            public void onTextChanged(CharSequence a, int b, int c2, int d2) {}
            public void afterTextChanged(Editable e) {
                if (total != null) {
                    total.setText(String.format(Locale.US, "Total: ₱%,.2f", num(qty) * num(price)));
                }
            }
        };

        if (qty != null) qty.addTextChangedListener(w);
        if (price != null) price.addTextChangedListener(w);

        TextView btnSave = d.findViewById(R.id.btnSave);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (!req(item, qty, price)) return;
                if (num(qty) <= 0) {
                    if (qty != null) qty.setError("Must be at least 1");
                    return;
                }
                if (l != null) l.onSale(s(item), (int) num(qty), num(price));
                Toast.makeText(c, "Sale logged", Toast.LENGTH_SHORT).show();
                d.dismiss();
            });
        }
    }

    /** Displays "Record Utang" dialog popup. */
    public static void showRecordUtang(Context c, UtangListener l) {
        AlertDialog d = open(c, R.layout.dialogrecordutang);
        EditText name = d.findViewById(R.id.etCustomer), amt = d.findViewById(R.id.etAmount), date = d.findViewById(R.id.etDate);
        datePicker(c, date);

        TextView btnSave = d.findViewById(R.id.btnSave);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (!req(name, amt, date)) return;
                if (l != null) l.onUtang(s(name), num(amt), s(date));
                Toast.makeText(c, "Utang recorded", Toast.LENGTH_SHORT).show();
                d.dismiss();
            });
        }
    }

    public static void showRecordUtang(Context c) {
        showRecordUtang(c, null);
    }

    /** Displays "Add Stock" dialog popup with autocomplete suggestions for existing items. */
    public static void showAddStock(Context c, List<String> suggestions, StockListener l) {
        AlertDialog d = open(c, R.layout.dialogaddstock);
        AutoCompleteTextView item = d.findViewById(R.id.etItem);
        EditText qty = d.findViewById(R.id.etQty),
                 exp = d.findViewById(R.id.etExpiry),
                 batch = d.findViewById(R.id.etBatch),
                 alert = d.findViewById(R.id.etAlert),
                 markup = d.findViewById(R.id.etMarkup),
                 cost = d.findViewById(R.id.etCost);
        TextView price = d.findViewById(R.id.tvPrice);

        if (item != null && suggestions != null && !suggestions.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(c, android.R.layout.simple_dropdown_item_1line, suggestions);
            item.setAdapter(adapter);
        }

      //Real-time price calculation
      // FORMULA: price = cost + (cost * markup)
      TextWatcher priceWatcher = new TextWatcher() { //Para sa real-time edits
          public void beforeTextChanged(CharSequence a, int start, int b4, int count) {} //Walang laman kasi wala naman need gawin dyan
          public void onTextChanged(CharSequence a, int start, int b4, int count) {} //Same din dine
          public void afterTextChanged(Editable e) { //Here ang live modifications
              if (cost != null && markup != null && price != null) {
                  double costVal = num(cost);
                  double markupVal = num(markup);
                  double selPrice = costVal + (costVal * markupVal); //Simplified formula 'to ng markup
                  price.setText(String.format(Locale.US, "Price: ₱%,.2f", selPrice));
              }
          }
      };
        //Pang check ng inputs para sa live modification
        if(cost != null) cost.addTextChangedListener(priceWatcher);
        if (markup != null) markup.addTextChangedListener(priceWatcher);

      datePicker(c, exp);

        TextView btnSave = d.findViewById(R.id.btnSave);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (!req(item, qty, cost, alert)) return;
                //Calculation of price (nilagay ko d2 para mare-eval & masave siya)
                double costVal = num(cost);
                double markupVal = num(markup);
                double selPrice = costVal + (costVal * markupVal);
                if (l != null) l.onStock(s(item), (int) num(qty), selPrice, s(exp), s(batch), (int) num(alert));
                Toast.makeText(c, "Stock added", Toast.LENGTH_SHORT).show();
                d.dismiss();
            });
        }
    }

    public static void showAddStock(Context c, StockListener l) {
        showAddStock(c, null, l);
    }

    /** Displays "Add Supplier" dialog popup. */
    public static void showAddSupplier(Context c, SupplierListener l, dbManager db) {
        AlertDialog d = open(c, R.layout.dialogaddsupplier);
        EditText name = d.findViewById(R.id.etName), contact = d.findViewById(R.id.etContact);

        TextView btnSave = d.findViewById(R.id.btnSave);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (!req(name)) return;
                if (l != null) l.onSupplier(s(name), s(contact));
                d.dismiss();
            });
        }
    }

    /** Displays "Log Delivery Quality" rating dialog popup. */
    public static void showLogQuality(
        Context c,
        Supplier sup,
        String userID,
        dbManager db,
        Runnable done) {

      AlertDialog d = open(c, R.layout.dialoglogquality);

      TextView tvSupName = d.findViewById(R.id.tvSupName);
      if (tvSupName != null) {
        tvSupName.setText(sup.name);
      }

      EditText good = d.findViewById(R.id.etGood);
      EditText bad = d.findViewById(R.id.etBad);

      TextView btnSave = d.findViewById(R.id.btnSave);

      if (btnSave != null) {
        btnSave.setOnClickListener(v -> {
          if (!req(good, bad)) return;

          int goodCount = (int) num(good);
          int badCount = (int) num(bad);

          db.updateDelivery(userID, sup.name, sup.contact, goodCount, badCount).addOnSuccessListener(unused -> {
            sup.good += goodCount;
            sup.bad += badCount;

            if (done != null) done.run();

            d.dismiss();
          }).addOnFailureListener(e -> {
            Toast.makeText(
                c,
                "Failed to save delivery quality",
                Toast.LENGTH_SHORT
            ).show();
          });
        });
      }
    }
}
