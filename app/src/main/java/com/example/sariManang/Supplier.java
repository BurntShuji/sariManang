package com.example.sariManang;

/**
 * Data model class representing a store supplier.
 * Stores contact details, favorite status, and delivery quality statistics (good vs bad deliveries).
 */
public class Supplier {
    public String name;
    public String contact;
    public int good;
    public int bad;
    public boolean favorite;

    public Supplier(String name, String contact, int good, int bad, boolean favorite) {
        this.name = name;
        this.contact = contact;
        this.good = good;
        this.bad = bad;
        this.favorite = favorite;
    }

    /** Returns total number of delivery quality logs. */
    public int total() {
        return good + bad;
    }

    /** Returns percentage of good quality deliveries. */
    public int percentGood() {
        return total() == 0 ? 0 : Math.round(good * 100f / total());
    }

    /** Calculates 0-5 star rating based on good vs bad delivery ratio. */
    public int stars() {
        return total() == 0 ? 0 : Math.round(good * 5f / total());
    }
}
