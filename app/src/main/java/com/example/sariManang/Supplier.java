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

    public String id;

    public Supplier(String name, String contact, int good, int bad, boolean favorite) {
        this.name = name;
        this.contact = contact;
        this.good = good;
        this.bad = bad;
        this.favorite = favorite;
    }

    public int total() { //total deliveries
      return good + bad;
    }

    public int percentGood() { //% of good quality
      return total() == 0 ? 0 : Math.round(good * 100f / total());
    }

    //FORMULA: double avgRate = (totGood/(totGood + totBad) * 5);
    public double stars() { //para possible half-star ratings
      if(total() ==0){
        return 0;
      }
      double avgRate = ((double) good / total()) * 5.0;
      return Math.round(avgRate * 2.0) / 2.0; //round avgRate sa pinakamalapit na half-star increment
    }
}
