package com.example.sariManang;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.ComponentActivity;

/**
 * Utility helper class for setting up bottom navigation items, active tab highlights,
 * and top activity toolbar title/back button listeners across all activities.
 */
public class NavigationHelper {

    /** Bottom navigation view IDs. */
    private static final int[] IDS = {
            R.id.navHome, R.id.navInventory, R.id.navSales, R.id.navSuppliers, R.id.navSettings
    };

    /** Target Activity classes corresponding to navigation view IDs. */
    private static final Class<?>[] TARGETS = {
            MainActivity.class, InventoryActivity.class, SalesActivity.class, SupplierActivity.class, SettingsActivity.class
    };

    /** Binds click listeners and active tab color highlighting to bottom navigation bar items. */
    public static void setup(Activity a, int selectedId) {
        for (int i = 0; i < IDS.length; i++) {
            LinearLayout item = a.findViewById(IDS[i]);
            if (item == null) continue;

            boolean sel = (IDS[i] == selectedId);
            int color = Color.parseColor(sel ? "#EF4444" : "#111111");

            ((ImageView) item.getChildAt(0)).setColorFilter(color);
            ((TextView) item.getChildAt(1)).setTextColor(color);

            Class<?> target = TARGETS[i];
            item.setOnClickListener(v -> {
                if (sel) return;
                Intent in = new Intent(a, target);
                in.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                if (target == MainActivity.class) {
                    in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                }
                a.startActivity(in);
                if (!(a instanceof MainActivity) && target != MainActivity.class) {
                    a.finish();
                }
            });
        }
    }

    /** Configures top action bar title and back button navigation listener. */
    public static void setupToolbar(Activity a, String title) {
        TextView tvTitle = a.findViewById(R.id.tbTitle);
        if (tvTitle != null) {
            tvTitle.setText(title);
        }

        if (a.findViewById(R.id.tbBack) != null) {
            a.findViewById(R.id.tbBack).setOnClickListener(v -> {
                if (a instanceof ComponentActivity) {
                    ((ComponentActivity) a).getOnBackPressedDispatcher().onBackPressed();
                } else {
                    a.finish();
                }
            });
        }
    }
}
