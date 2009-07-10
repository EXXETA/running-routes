package com.exxeta.routing;

import org.apache.wicket.IClusterable;

public class RunnerDataBean implements IClusterable {

    private String lonlat,  distance;

    public String getLonlat() {
        return lonlat;
    }

    public void setLonlat(String lonlat) {
        this.lonlat = lonlat;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }
}
