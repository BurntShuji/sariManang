package com.example.sariManang;

import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

/**
 * SupplierActivity manages the store's suppliers list, favorite suppliers, delivery quality ratings,
 * and search filtering.
 */
public class SupplierActivity extends AppCompatActivity {

    private final ArrayList<Supplier> suppliers = new ArrayList<>();
    private LinearLayout list, favs;
    private EditText search;

    @Override
    protected void onCreate(Bundle b) {

        super.onCreate(b);
        setContentView(R.layout.supplierpage);
      String currentUserID = getSharedPreferences("MyUserPrefs", MODE_PRIVATE)
          .getString("userid", null);

        // Section 1: Toolbar & Navigation setup
        NavigationHelper.setupToolbar(this, "Suppliers");
        NavigationHelper.setup(this, R.id.navSuppliers);

        list = findViewById(R.id.listSuppliers);
        favs = findViewById(R.id.listFavs);
        search = findViewById(R.id.etSearch);

        // Section 2: Initial sample supplier data
        suppliers.add(new Supplier("PureGold", "0917 000 0001", 45, 5, true));
        suppliers.add(new Supplier("Superman's Whole Foods", "0000 - 000 - 000", 70, 30, false));
        suppliers.add(new Supplier("Mang Tomas Trading", "0000 - 000 - 000", 0, 0, false));

        // Section 3: Search filter listener
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence a, int x, int y, int z) {}
            public void onTextChanged(CharSequence a, int x, int y, int z) {}
            public void afterTextChanged(Editable e) { render(); }
        });

        // Section 4: Add supplier listener
        findViewById(R.id.btnAddSupplier).setOnClickListener(v -> DialogHelper.showAddSupplier(this,
                (n, c) -> {
                    suppliers.add(0, new Supplier(n, c.isEmpty() ? "No contact" : c, 0, 0, false));
                    render();
                }));

        // Section 5: Initial render
        render();
    }

    /** Renders the list of suppliers and favorite supplier circle icons. */
    private void render() {
        String q = search.getText().toString().toLowerCase().trim();
        list.removeAllViews();
        favs.removeAllViews();

        for (Supplier s : suppliers) {
            // Render favorite supplier in top horizontal scroll section with circle avatar icon
            if (s.favorite) addFav(s);

            if (!s.name.toLowerCase().contains(q)) continue;

            View v = getLayoutInflater().inflate(R.layout.itemsuppliercard, list, false);
            ((TextView) v.findViewById(R.id.tvSupName)).setText(s.name);
            ((TextView) v.findViewById(R.id.tvSupContact)).setText(s.contact);

            ((TextView) v.findViewById(R.id.tvRatio)).setText(s.total() == 0 ? "No ratings yet"
                    : String.format(Locale.US, "%d%% Good / %d%% Bad", s.percentGood(), 100 - s.percentGood()));

            // Render star rating
            SpannableString stars = new SpannableString("★★★★★");
            stars.setSpan(new ForegroundColorSpan(0xFFF9A825), 0, s.stars(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            stars.setSpan(new ForegroundColorSpan(0xFFBDBDBD), s.stars(), 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ((TextView) v.findViewById(R.id.tvStars)).setText(stars);

            // Toggle favorite status
            CheckBox cb = v.findViewById(R.id.cbFav);
            cb.setChecked(s.favorite);
            cb.setOnCheckedChangeListener((btn, on) -> {
                s.favorite = on;
                render();
            });

            // Log delivery quality button
            v.findViewById(R.id.btnLogQuality).setOnClickListener(x -> DialogHelper.showLogQuality(this, s, this::render));

            list.addView(v);
        }

        if (favs.getChildCount() == 0) {
            TextView e = new TextView(this);
            e.setText("No favorites yet. Tap the heart on a supplier.");
            e.setTextColor(0xFF777777);
            e.setTextSize(12);
            favs.addView(e);
        }
    }

    /** Adds a favorite supplier item to the top horizontal list with circle avatar icon. */
    private void addFav(Supplier s) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(0, 12, 32, 0);

        TextView a = new TextView(this);
        a.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
        a.setBackgroundResource(R.drawable.bg_circle);
        a.setGravity(Gravity.CENTER);
        a.setText(s.name.substring(0, 1).toUpperCase());
        a.setTextColor(0xFFFFFFFF);
        a.setTextSize(18);

        TextView n = new TextView(this);
        n.setText(s.name.length() > 10 ? s.name.substring(0, 9) + "…" : s.name);
        n.setTextSize(11);
        n.setTextColor(0xFF111111);

        c.addView(a);
        c.addView(n);
        favs.addView(c);
    }
}
