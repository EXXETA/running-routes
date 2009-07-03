package com.exxeta.routing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.geotools.geometry.jts.JTS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;

public class CalculateButton extends AjaxButton {

    private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(CalculateButton.class.getName());
    private RoutingResult result;
    private RunnerDataBean bean;
    private List<Integer> heights = new ArrayList<Integer>();

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

        js.append("var startPoint = new OpenLayers.Geometry.Point(");
        js.append(points.get(0).getX()).append(",").append(points.get(0).getY()).append(");\n");
        js.append("var pointFeature = new OpenLayers.Feature.Vector(startPoint, null, style_blue);\n");

        js.append("var pointList = [];\n");
        js.append("var newPoint;\n");

        for (Point p : points) {
            js.append("newPoint = new OpenLayers.Geometry.Point(");
            js.append(p.getX()).append(",").append(p.getY()).append(");\n");
            js.append("pointList.push(newPoint);\n");
        }
        js.append("polygonLayer.addFeatures([pointFeature, new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString(pointList), null, style_lightblue)]);\n");

        js.append(getCodeForMarkers(result.getMarkers()));
        js.append("map.zoomToExtent(polygonLayer.getDataExtent());");

        // Result popup
        js.append("if (typeof(popup) != \"undefined\") { popup.destroy(); }\n");
        js.append("popup = new OpenLayers.Popup(\"Result\", new OpenLayers.LonLat(");
        js.append(points.get(0).getX()).append(",").append(points.get(0).getY()).append("), new OpenLayers.Size(340,160), 'null', true);\n");
        js.append("popup.setOpacity(.8);\n");
        DecimalFormat formatter = new DecimalFormat("#.##");
        js.append("popup.setContentHTML('<div>Distance: " + formatter.format(result.getDistance() / 1000) + "km<br><span style=\"font-size: 8pt;\">" + points.size() + " points, calculated in " + duration + "ms</span><br><div id=\"ch_heights\" style=\"width:300px;height:100px;\"></div></div>');\n");
        js.append("popup.autoSize = true;\n");
        js.append("map.addPopup(popup);\n");
        if (heights.size() > 0) {
            js.append(getCodeForHeights(result.getDistance() / 1000));
        }

        target.appendJavascript(js.toString());
    }

    private String getCodeForHeights(double d) {
        StringBuffer h = new StringBuffer();
        h.append("$.plot($(\"#ch_heights\"), [[");
        for (int i = 0; i < heights.size(); i++) {
            h.append("[" + i + "," + heights.get(i) + "], ");
        }
        h.append("[" + d + "," + heights.get(0) + "]");
        return h.append("]]);\n").toString();
    }

    private String getCodeForMarkers(List<Point> markers) {
        StringBuffer markerString = new StringBuffer();
        StringBuffer lats = new StringBuffer("lats=");
        StringBuffer lngs = new StringBuffer("lngs=");
        markerString.append("var size = new OpenLayers.Size(26,40); var offset = new OpenLayers.Pixel(-(size.w/2), -size.h+2);\n");
        int i = 1;
        double nextMeter = 1000;
        double distance = 0;
        Point lastPoint = null;
        for (Point p : markers) {
            if (lastPoint != null) {
                distance += p.distance(lastPoint);
                if (distance > nextMeter) {
                    double lengthToWalk = nextMeter - (distance - p.distance(lastPoint));
                    double percentToWalk = lengthToWalk / p.distance(lastPoint);
                    double x = lastPoint.getX() + (p.getX() - lastPoint.getX()) * percentToWalk;
                    double y = lastPoint.getY() + (p.getY() - lastPoint.getY()) * percentToWalk;

                    markerString.append("var icon" + i + " = new OpenLayers.Icon('marker?no=" + i + "', size, offset);\n");
                    markerString.append("var marker" + i + " = new OpenLayers.Marker(new OpenLayers.LonLat(");
                    markerString.append(x).append(",").append(y).append("),icon" + i + ");\n");
                    markerString.append("marker" + i + ".setOpacity(0.5);\n");
                    markerString.append("markers.addMarker(marker" + i + ");\n");
                    i++;
                    nextMeter += 1000;

                    // Get height profile
                    MathTransform transform = Calculator.getTransformation();
                    try {
                        Point heightPoint = (Point) JTS.transform(new GeometryFactory().createPoint(new Coordinate(x, y)), transform);
                        lats.append(heightPoint.getY() + ",");
                        lngs.append(heightPoint.getX() + ",");
                    } catch (MismatchedDimensionException e) {
                        logger.warn(e.getMessage(), e);
                    } catch (TransformException e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            } else {
                // Get height profile
                MathTransform transform = Calculator.getTransformation();
                try {
                    Point heightPoint = (Point) JTS.transform(new GeometryFactory().createPoint(new Coordinate(
                            p.getX(), p.getY())), transform);
                    lats.append(heightPoint.getY() + ",");
                    lngs.append(heightPoint.getX() + ",");
                } catch (MismatchedDimensionException e) {
                    logger.warn(e.getMessage(), e);
                } catch (TransformException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
            lastPoint = p;
        }
        // TODO Handle more than 20 points
        callRestService("http://ws.geonames.org/srtm3?" + lats.toString() + "&" + lngs.toString(), markerString);

        return markerString.toString();
    }

    public void callRestService(String request, StringBuffer markerString) {
        heights.clear();
        markerString.append("// REQUEST: " + request + "\n");
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod(request);
        String line = null;

        int statusCode;
        try {
            statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                markerString.append("// Method failed: " + method.getStatusLine() + "\n");
            }
            InputStream rstream = method.getResponseBodyAsStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(rstream));
            while ((line = br.readLine()) != null) {
                if (line != null) {
                    heights.add(new Integer(line));
                }
            }
            br.close();
        } catch (HttpException e) {
            logger.warn(e.getMessage(), e);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
    }
}
