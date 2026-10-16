package com.example.sariManang;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Data model class representing a customer utang (unpaid credit) record.
 * Stores customer details, outstanding balance amount, promised payment due date, and payment status.
 */
public class Utang {
    public String customer;
    public double amount;
    public String dueDate;
    public boolean paid;

    public Utang(String customer, double amount, String dueDate) {
        this.customer = customer;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paid = false;
    }

    /** Calculates the number of days this utang is overdue relative to today's date. */
    public long getDaysOverdue() {
        if (dueDate == null || dueDate.trim().isEmpty()) return 0;
        try {
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date due = dayFormat.parse(dueDate);
            Date today = dayFormat.parse(dayFormat.format(new Date()));
            if (due == null || today == null) return 0;
            long diff = today.getTime() - due.getTime();
            return TimeUnit.MILLISECONDS.toDays(diff);
        } catch (Exception e) {
            return 0;
        }
    }

    /** Returns true if the promised due date has passed. */
    public boolean isOverdue() {
        return getDaysOverdue() > 0;
    }

    /** Returns human-readable payment status string (e.g., "8 Days Late", "Due Today", "Due in 3 Days"). */
    public String getStatusText() {
        long days = getDaysOverdue();
        if (days > 0) {
            return days + (days == 1 ? " Day Late" : " Days Late");
        } else if (days == 0) {
            return "Due Today";
        } else {
            long remaining = -days;
            return "Due in " + remaining + (remaining == 1 ? " Day" : " Days");
        }
    }
}
