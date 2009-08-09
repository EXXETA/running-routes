package com.exxeta.routing;

import java.text.DecimalFormat;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;

import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;

public class CalculateButton extends AjaxButton {

    private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(CalculateButton.class.getName());
    private RoutingResult result;
    private RunnerDataBean bean;

    CalculateButton(String string, RunnerDataBean bean) {
        super(string);
        this.bean = bean;
    }

    @Override
    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
        StringBuffer js = new StringBuffer("polygonLayer.destroyFeatures();\n");

        Calculator calc = new Calculator();
        final double longitude = Double.parseDouble(bean.getLonlat().split(",")[1]);
        final double latitude = Double.parseDouble(bean.getLonlat().split(",")[0]);
        double millis = System.currentTimeMillis();
        result = calc.calculate(longitude, latitude, Double.parseDouble(bean.getDistance()) * 1000);
        double duration = System.currentTimeMillis() - millis;
        List<Point> points = result.getRoute();

        // Display popup with error
        if (result.getErrorMessage() != null) {
            js.append("if (typeof(popup) != \"undefined\") { popup.destroy(); }\n");
            js.append("popup = new OpenLayers.Popup.AnchoredBubble(\"Ergebnis\", new OpenLayers.LonLat(");
            js.append(points.get(0).getX()).append(",").append(points.get(0).getY()).append("));\n");
            js.append("popup.setOpacity(.8);\n");
            js.append("popup.setContentHTML('" + result.getErrorMessage() + "');\n");
            js.append("popup.autoSize = true;\n");
            js.append("map.addPopup(popup);\n");
            target.appendJavascript(js.toString());
            return;
        }

        DecimalFormat formatter = new DecimalFormat("#.##");
        js.append("startPoint = new OpenLayers.Geometry.Point(");
        js.append(points.get(0).getX()).append(",").append(points.get(0).getY()).append(");\n");
        js.append("distance = " + formatter.format(result.getDistance() / 1000) + ";\n");

        js.append("var pointFeature = new OpenLayers.Feature.Vector(startPoint, null, style_blue);\n");

        js.append("var pointList = [];\n");
        js.append("var newPoint;\n");

        for (Point p : points) {
            js.append("newPoint = new OpenLayers.Geometry.Point(");
            js.append(p.getX()).append(",").append(p.getY()).append(");\n");
            js.append("pointList.push(newPoint);\n");
        }
        js.append("polygonLayer.addFeatures([pointFeature, new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString(pointList), null, style_lightblue)]);\n");

        js.append(getCodeForKilometers(result.getMarkers()));
        js.append(getCodeForMarkers(result.getMarkers()));
        js.append("map.zoomToExtent(polygonLayer.getDataExtent());");

        // Result popup
        js.append("if (typeof(popup) != \"undefined\") { popup.destroy(); }\n");
        js.append("popup = new OpenLayers.Popup(\"Result\", new OpenLayers.LonLat(");
        js.append(points.get(0).getX()).append(",").append(points.get(0).getY()).append("), new OpenLayers.Size(340,160), 'null', true);\n");
        js.append("popup.setOpacity(.8);\n");
        js.append("popup.setContentHTML('<div id=\"result_popup\">Distance: " + formatter.format(result.getDistance() / 1000) + "km<br><span style=\"font-size: 7pt;\">" + points.size() + " points, calculated in " + duration + "ms</span><br><div id=\"ch_heights\" style=\"width:300px;height:100px;\"></div></div>');\n");
        js.append("popup.autoSize = true;\n");
        js.append("map.addPopup(popup);\n");

        js.append("printHeightGraph();\n");

        target.appendJavascript(js.toString());
    }

    private String getCodeForMarkers(List<Point> markers) {
        StringBuffer markerString = new StringBuffer();
        markerString.append("var size = new OpenLayers.Size(26,40); var offset = new OpenLayers.Pixel(-(size.w/2), -size.h+2);\n");
        int i = 1;
        for (Point p : markers) {
            markerString.append("var icon" + i + " = new OpenLayers.Icon('marker?no=" + i + "', size, offset);\n");
            markerString.append("var marker" + i + " = new OpenLayers.Marker(new OpenLayers.LonLat(");
            markerString.append(p.getX()).append(",").append(p.getY()).append("),icon" + i + ");\n");
            markerString.append("marker" + i + ".setOpacity(0.5);\n");
            markerString.append("markers.addMarker(marker" + i + ");\n");
            i++;
        }

        return markerString.toString();
    }

    private String getCodeForKilometers(List<Point> markers) {
        StringBuffer markerString = new StringBuffer("kilometers = new Array();");
        int i = 0;
        for (Point p : markers) {
            markerString.append("kilometers[" + i + "] = new OpenLayers.Geometry.Point(");
            markerString.append(p.getX()).append(",").append(p.getY()).append(");\n");
            i++;
        }

        return markerString.toString();
    }
}
