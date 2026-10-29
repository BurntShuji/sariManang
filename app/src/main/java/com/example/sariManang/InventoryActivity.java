package com.example.sariManang;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * InventoryActivity manages store stock items, filtering, low stock and expiration warnings,
 * and maintains items in alphabetical order.
 */
public class InventoryActivity extends AppCompatActivity {

    /** Model class representing an inventory item. */
    public static class Item {
        public String name, expiry, batch;
        int qty, alertAt;
        public double price;

        Item(String n, int q, double p, String e, String b, int a) {
            name = n;
            qty = q;
            price = p;
            expiry = e;
            batch = b;
            alertAt = a;
        }
    }

    private userIDManager user;
    private String userid;

    private dbManager db = new dbManager();


    public static final List<Item> items = new ArrayList<>();

    private LinearLayout list;
    private EditText search;

    /** Helper method to return list of existing inventory item names for autocomplete suggestions. */
    public static List<String> getExistingItemNames() {
        List<String> names = new ArrayList<>();
        for (Item item : items) {
            names.add(item.name);
        }
        return names;
    }

  public static void refreshItems(dbManager db, String userid, Runnable done) {
    db.getInv(userid, null).addOnSuccessListener(snapshot -> {
      items.clear();
      for (DocumentSnapshot doc : snapshot.getDocuments()) {
        Long q = doc.getLong("quantity");
        Double p = doc.getDouble("price");
        Long a = doc.getLong("alertAt");
        String expiry = doc.getString("expiry");
        String batch = doc.getString("batch");
        String name = doc.getString("item");
        if (name == null) continue;
        items.add(new Item(
            name,
            q != null ? q.intValue() : 0,
            p != null ? p : 0.0,
            expiry != null ? expiry : "",
            batch != null ? batch : "",
            a != null ? a.intValue() : 0));
      }
      if (done != null) done.run();
    }).addOnFailureListener(e -> Log.e("INV", "Failed to load inventory", e));
  }

  private void loadItems() {
    refreshItems(db, userid, this::render);
  }
    /** Sorts the inventory items list in alphabetical order by item name. */
    private static void sortItemsAlphabetically() {
        Collections.sort(items, (a, b) -> a.name.compareToIgnoreCase(b.name));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        user = new userIDManager(InventoryActivity.this);
        userid = user.getUserID();
        setContentView(R.layout.inventorypage);

        // Section 1: Toolbar & Navigation setup
        NavigationHelper.setupToolbar(this, "Inventory");
        NavigationHelper.setup(this, R.id.navInventory);

        list = findViewById(R.id.listItems);
        search = findViewById(R.id.etSearch);

        // Section 2: Search filter listener
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence a, int x, int y, int z) {}
            public void onTextChanged(CharSequence a, int x, int y, int z) {}
            public void afterTextChanged(Editable e) { render(); }
        });

        // Section 3: Add stock listener with autocomplete suggestions and alphabetical sorting
        findViewById(R.id.btnAddStock).setOnClickListener(v -> DialogHelper.showAddStock(
                this,
                getExistingItemNames(),
                (n, q, p, e, bt, a) -> {
                    addOrUpdateStockItem(this, n, q, p, e, bt, a);
                    render();
                }
        ));

        // Section 4: Initial render
        loadItems();
    }

  @Override
  protected void onResume() {
    super.onResume();
    if (list != null) render();
  }

    /**
     * Adds a new stock item or updates quantity/details if the item already exists in inventory.
     * Maintains alphabetical order.
     */
    private void addOrUpdateStockItem(Context context, String name, int qty, double price, String expiry, String batch, int alertAt) {

      boolean exists = false;
        for (Item item : items) {
            if (item.name.equalsIgnoreCase(name.trim())) {
                item.qty += qty;
                if (price > 0) item.price = price;
                if (!expiry.isEmpty()) item.expiry = expiry;
                if (!batch.isEmpty()) item.batch = batch;
                if (alertAt > 0) item.alertAt = alertAt;
                exists = true;
                break;
            }
        }
        if (!exists) {
            items.add(new Item(name.trim(), qty, price, expiry, batch, alertAt));
        }

      Map<String, Object> stock = new HashMap<>();
      stock.put("item", name.trim());
      stock.put("quantity", qty);
      stock.put("price", price);
      stock.put("expiry", expiry);
      stock.put("batch", batch);
      stock.put("alertAt", alertAt);

      db.addOrUpdateStock(userid, stock)
          .addOnFailureListener(e -> Log.e("STOCK", "add failed", e));        sortItemsAlphabetically();

        db.updateLastActive(userid);
    }

    /** Formats and creates table view cells. */
    private TextView cell(String t, float w, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, w));
        tv.setText(t);
        tv.setTextColor(color);
        tv.setTextSize(13);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    /** Renders the inventory table list in alphabetical order with search filtering. */
    private void render() {
        list.removeAllViews();
        sortItemsAlphabetically();

        String q = search.getText().toString().toLowerCase().trim();
        int shown = 0;

      int threshold = SettingsActivity.getLowStockDefault(this);

      for (Item it : items) {
            if (!it.name.toLowerCase().contains(q)) continue;
            shown++;

            boolean low = it.qty <= threshold;
            long days = Long.MAX_VALUE;
            try {
                days = TimeUnit.MILLISECONDS.toDays(
                        new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it.expiry).getTime() - System.currentTimeMillis()
                );
            } catch (Exception ignored) {}

            int expColor = days < 0 ? Color.parseColor("#D32F2F") : days <= 30 ? Color.parseColor("#F9A825") : Color.parseColor("#111111");

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(30, 24, 30, 24);

            LinearLayout nameCol = new LinearLayout(this);
            nameCol.setOrientation(LinearLayout.VERTICAL);
            nameCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 2f));

            TextView n = new TextView(this);
            n.setText(it.name);
            n.setTextColor(Color.BLACK);
            n.setTextSize(13);
            n.setTypeface(null, Typeface.BOLD);

            TextView p = new TextView(this);
            p.setText(String.format(Locale.US, "₱%.2f", it.price));
            p.setTextColor(Color.GRAY);
            p.setTextSize(11);

            nameCol.addView(n);
            nameCol.addView(p);

            row.addView(nameCol);
            row.addView(cell(low ? it.qty + " ⚠" : String.valueOf(it.qty), 1f, low ? Color.parseColor("#D32F2F") : Color.BLACK, low));
            row.addView(cell(it.expiry.isEmpty() ? "—" : it.expiry, 1.4f, expColor, days <= 30));
            row.addView(cell(it.batch.isEmpty() ? "—" : it.batch, 1.1f, Color.BLACK, false));

            list.addView(row);

            View div = new View(this);
            div.setLayoutParams(new LinearLayout.LayoutParams(-1, 2));
            div.setBackgroundColor(Color.parseColor("#E5E5E5"));
            list.addView(div);
        }

        if (shown == 0) {
            TextView e = new TextView(this);
            e.setText("No items found");
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, 80, 0, 80);
            e.setTextColor(Color.GRAY);
            list.addView(e);
        }
    }
}
