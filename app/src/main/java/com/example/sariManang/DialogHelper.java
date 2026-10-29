package com.example.sariManang;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
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
        void onSale(List<CartItem> cartItems, double grandTotal);
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

    //Para magrepresent as a cart
    public static class CartItem {
      public String item;
      public int qty;
      public double price;

    public CartItem(String item, int qty, double price) {
      this.item = item;
      this.qty = qty;
      this.price = price;
    }
    }

    /** Displays "Log Sale" dialog popup. */
    public static void showNewSale(Context c, List<InventoryActivity.Item> inventoryItems, SaleListener l) {
      AlertDialog d = open(c, R.layout.dialognewsale);

      AutoCompleteTextView item = d.findViewById(R.id.etItem);
      EditText qty = d.findViewById(R.id.etQty), price = d.findViewById(R.id.etPrice);
      TextView total = d.findViewById(R.id.tvTotal);
      TextView btnAddItem = d.findViewById(R.id.btnAddItem);
      LinearLayout cartContainer = d.findViewById(R.id.cartContainer);

      List<CartItem> cartList = new ArrayList<>();

      // Populate dropdown with inventory item names
      List<String> names = new ArrayList<>();
      if (inventoryItems != null) {
        for (InventoryActivity.Item i : inventoryItems) {
          names.add(i.name);
        }
      }
      ArrayAdapter<String> adapter = new ArrayAdapter<>(c, android.R.layout.simple_dropdown_item_1line, names);
      if (item != null) item.setAdapter(adapter);

      // AUTO-FILL PRICE WHEN AN ITEM IS SELECTED FROM DROPDOWN
      if (item != null) {
        item.setOnItemClickListener((parent, view, position, id) -> {
          String selectedName = (String) parent.getItemAtPosition(position);
          if (inventoryItems != null) {
            for (InventoryActivity.Item i : inventoryItems) {
              if (i.name.equalsIgnoreCase(selectedName)) {
                if (price != null) price.setText(String.valueOf(i.price));
                break;
              }
            }
          }
        });
      }

      // REAL-TIME ROW TOTAL CALCULATION
      TextWatcher w = new TextWatcher() {
        public void beforeTextChanged(CharSequence a, int b, int c2, int d2) {}
        public void onTextChanged(CharSequence a, int b, int c2, int d2) {}
        public void afterTextChanged(Editable e) {
          // Live calculation preview for the item currently being edited
          double currentItemTotal = num(qty) * num(price);
        }
      };

      if (qty != null) qty.addTextChangedListener(w);
      if (price != null) price.addTextChangedListener(w);

      // ADD ITEM TO TRANSACTION CART LIST
      if (btnAddItem != null) {
        btnAddItem.setOnClickListener(v -> {
          if (!req(item, qty, price)) return;
          if (num(qty) <= 0) {
            if (qty != null) qty.setError("Must be at least 1");
            return;
          }

          String itemName = s(item);
          int itemQty = (int) num(qty);
          double itemPrice = num(price);

          // Add item to temporary transaction array
          cartList.add(new CartItem(itemName, itemQty, itemPrice));

          // Display row in cart UI
          TextView rowView = new TextView(c);
          rowView.setText(String.format(Locale.US, "• %s x%d - ₱%,.2f", itemName, itemQty, itemQty * itemPrice));
          rowView.setTextSize(13);
          rowView.setPadding(0, 4, 0, 4);
          if (cartContainer != null) cartContainer.addView(rowView);

          // Calculate and display cumulative grand total
          double grandTotal = 0;
          for (CartItem ci : cartList) {
            grandTotal += (ci.qty * ci.price);
          }
          if (total != null) {
            total.setText(String.format(Locale.US, "Total: ₱%,.2f", grandTotal));
          }

          // Clear inputs for the next entry
          item.setText("");
          qty.setText("");
          price.setText("");
        });
      }

      // SAVE COMPLETE MULTI-ITEM TRANSACTION
      TextView btnSave = d.findViewById(R.id.btnSave);
      if (btnSave != null) {
        btnSave.setOnClickListener(v -> {
          if (cartList.isEmpty()) {
            Toast.makeText(c, "Add at least 1 item to sale", Toast.LENGTH_SHORT).show();
            return;
          }

          double grandTotal = 0;
          for (CartItem ci : cartList) {
            grandTotal += (ci.qty * ci.price);
          }

          if (l != null) l.onSale(cartList, grandTotal);
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
    public static void showAddSupplier(Context c, SupplierListener l) {
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
    public static void showLogQuality(Context c, Supplier sup, Runnable done) {
        AlertDialog d = open(c, R.layout.dialoglogquality);
        TextView tvSupName = d.findViewById(R.id.tvSupName);
        if (tvSupName != null) tvSupName.setText(sup.name);

        EditText good = d.findViewById(R.id.etGood), bad = d.findViewById(R.id.etBad);

        TextView btnSave = d.findViewById(R.id.btnSave);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (!req(good, bad)) return;
                sup.good += (int) num(good);
                sup.bad += (int) num(bad);
                if (done != null) done.run();
                d.dismiss();
            });
        }
    }
}
