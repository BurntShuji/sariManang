package com.example.sariManang;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Repository class for managing sales transaction records.
 * Provides central access to sales history and calculates daily sales metrics.
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
    public static void addSale(String item, int qty, double price) {
        records.add(0, new SaleRecord(getTodayFormatted(), item, qty, price));
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
