package com.exxeta.routing;

import com.vividsolutions.jts.geom.Point;
import java.util.List;
import org.apache.wicket.IClusterable;

public class RoutingResult implements IClusterable {
    private List<Point> route;
    private double distance;
    private String errorMessage;
    private List<Point> markers;

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public List<Point> getRoute() {
        return route;
    }

    public void setRoute(List<Point> route) {
        this.route = route;
    }

	public void setMarkers(List<Point> markers) {
		this.markers = markers;
	}
	
	public List<Point> getMarkers() {
		return markers;
	}

}
