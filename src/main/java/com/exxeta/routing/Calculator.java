package com.exxeta.routing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.graph.build.line.BasicLineGraphGenerator;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.exxeta.routing.algorithm.BoxRouting;
import com.exxeta.routing.algorithm.RoutingAlgorithm;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;

/**
 * 
 *
 * Possible test data:
 * Test "Schuh's Restaurant", Büchig, 1km
 *          latitude = 49.04772;
 *          longitude = 8.47037;
 *          distance = 1000;
 *
 * @author Daniel Weisser
 */
public class Calculator {

    private static String wkt = "PROJCS[\"Google Mercator\",  GEOGCS[\"WGS 84\",    DATUM[\"World Geodetic System 1984\",      SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]],      AUTHORITY[\"EPSG\",\"6326\"]],    PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]],    UNIT[\"degree\", 0.017453292519943295],    AXIS[\"Geodetic latitude\", NORTH],    AXIS[\"Geodetic longitude\", EAST],    AUTHORITY[\"EPSG\",\"4326\"]],  PROJECTION[\"Mercator_1SP\"],  PARAMETER[\"semi_minor\", 6378137.0],  PARAMETER[\"latitude_of_origin\", 0.0],  PARAMETER[\"central_meridian\", 0.0],  PARAMETER[\"scale_factor\", 1.0],  PARAMETER[\"false_easting\", 0.0],  PARAMETER[\"false_northing\", 0.0],  UNIT[\"m\", 1.0],  AXIS[\"Easting\", EAST],  AXIS[\"Northing\", NORTH],  AUTHORITY[\"EPSG\",\"900913\"]]";
    private static CoordinateReferenceSystem targetCRS;
    private static MathTransform transform;
    private static MathTransform transform2;
    private static DataStore data;
    private static Logger logger = Logger.getLogger(Calculator.class.getName());
    private double startRadius = 50.0;
    private static GeometryFactory fac = new GeometryFactory();


    static {
        try {
            targetCRS = ReferencingFactoryFinder.getCRSFactory(null).createFromWKT(wkt);
            transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, targetCRS);
            transform2 = CRS.findMathTransform(targetCRS, DefaultGeographicCRS.WGS84);
        } catch (FactoryException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
    }

    public static MathTransform getTransformation() {
        return transform2;
    }

    /**
     * Calculates the route.
     *
     * @param latitude Longitude of the selected starting point
     * @param longitude Latitude of the selected starting point
     * @param distance Distance in meters
     * @return The list of points defining the route
     */
    public RoutingResult calculate(double latitude, double longitude, double distance) {
        try {
            if (data == null) {
                data = RoutingConfiguration.getPostGISDataStore();
            }
            FeatureCollection<SimpleFeatureType, SimpleFeature> features = getRoutableFeatures(data, latitude, longitude, distance);
            Point clickPoint = (Point) JTS.transform(fac.createPoint(new Coordinate(longitude, latitude)), transform);
            logger.info("Click: " + clickPoint);
            Point startPoint = getStartPoint(data, latitude, longitude);
            if (startPoint == null) {
                RoutingResult result = new RoutingResult();
                List<Point> startPointList = new ArrayList<Point>();
                startPointList.add(clickPoint);
                result.setRoute(startPointList);
                result.setErrorMessage("No starting point in the radius of " + startRadius + "m found.");
                logger.info("No starting point found.");
                return result;
            }
            if (distance > 19000) {
                RoutingResult result = new RoutingResult();
                List<Point> startPointList = new ArrayList<Point>();
                startPointList.add(clickPoint);
                result.setRoute(startPointList);
                result.setErrorMessage("The entered distance is too long. Please use only values below 20km.");
                logger.info("Distance too long (" + distance + "m).");
                return result;
            }

            // Create graph
            BasicLineGraphGenerator graphGen = new BasicLineGraphGenerator();

            Iterator<SimpleFeature> iterator = features.iterator();
            try {
                while (iterator.hasNext()) {
                    Feature feature = (Feature) iterator.next();
                    GeometryAttribute o = feature.getDefaultGeometryProperty();
                    LineString l = (LineString) o.getValue();
                    Coordinate c1 = null;
                    for (Coordinate c2 : l.getCoordinates()) {
                        if (c1 == null) {
                            c1 = c2;
                            continue;
                        }
                        LineSegment line = new LineSegment(c1, c2);
                        graphGen.add(line);
                        c1 = c2;
                    }
                }

                Graph g = graphGen.getGraph();

                return route(g, distance, startPoint, clickPoint);
            } finally {
                features.close(iterator);
            }
        } catch (MismatchedDimensionException ex) {
            logger.fatal(ex.getMessage(), ex);
        } catch (TransformException ex) {
            logger.fatal(ex.getMessage(), ex);
        } catch (IOException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
        return null;
    }

    private FeatureCollection<SimpleFeatureType, SimpleFeature> getRoutableFeatures(DataStore data, double latitude, double longitude, double distance) throws IOException {
        FeatureSource<SimpleFeatureType, SimpleFeature> source = data.getFeatureSource("planet_osm_line"); // "planet_osm_roads"
        try {
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
            List<Filter> match = new ArrayList<Filter>();

            FeatureType schema = source.getSchema();
            String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
            // Get the bounding box for half the size of the distance
            ReferencedEnvelope bbox = getBoundingBox(latitude, longitude, distance / 2);
            Filter bboxFilter = ff.bbox(ff.property(geometryPropertyName), new ReferencedEnvelope(JTS.transform(bbox, transform), targetCRS));
            match.add(bboxFilter);

            List<Filter> highwayMatch = RoutingConfiguration.getHighwayFilters(ff);
            match.add(ff.or(highwayMatch));

            Filter allFilters = ff.and(match);
            return source.getFeatures(allFilters);
        } catch (TransformException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Calculates a boundig box.
     *
     * @param latitude Longitude of the selected starting point
     * @param longitude Latitude of the selected starting point
     * @param distance Distance in meters
     * @return Bounding box for the given parameters
     */
    private ReferencedEnvelope getBoundingBox(double latitude, double longitude, double distance) {
        // Aus der latitude, longitude und distance die Bounding Box berechnen
        // Addition der distance in alle Richtungen
        GeodeticCalculator cs = new GeodeticCalculator();
        cs.setStartingGeographicPoint(longitude, latitude);
        cs.setDirection(0, distance);
        double top = cs.getDestinationGeographicPoint().getY();
        cs.setDirection(90, distance);
        double right = cs.getDestinationGeographicPoint().getX();
        cs.setDirection(180, distance);
        double bottom = cs.getDestinationGeographicPoint().getY();
        cs.setDirection(-90, distance);
        double left = cs.getDestinationGeographicPoint().getX();

        Coordinate topLeft = new Coordinate(left, top);
        Coordinate bottomRight = new Coordinate(right, bottom);
        return new ReferencedEnvelope(new Envelope(topLeft, bottomRight), DefaultGeographicCRS.WGS84);
    }

    /**
     * Calculates the start point in the routable features. For a given point the shortest distance to a feature in the set of features is calculated.
     * @param data data store
     * @param latitude Longitude of the selected starting point
     * @param longitude Latitude of the selected starting point
     * @return start point
     * @throws java.io.IOException
     */
    private Point getStartPoint(DataStore data, double latitude, double longitude) throws IOException {
        Point near = null;
        FeatureSource<SimpleFeatureType, SimpleFeature> source = data.getFeatureSource("planet_osm_line"); // "planet_osm_roads"
        try {

            Point clickPoint = (Point) JTS.transform(fac.createPoint(new Coordinate(longitude, latitude)), transform);
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
            List<Filter> match = new ArrayList<Filter>();

            FeatureType schema = source.getSchema();
            String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
            ReferencedEnvelope bbox = getBoundingBox(latitude, longitude, startRadius);
            Filter bboxFilter = ff.bbox(ff.property(geometryPropertyName), new ReferencedEnvelope(JTS.transform(bbox, transform), targetCRS));
            match.add(bboxFilter);
            List<Filter> highwayMatch = RoutingConfiguration.getHighwayFilters(ff);
            match.add(ff.or(highwayMatch));

            Filter allFilters = ff.and(match);
            FeatureCollection<SimpleFeatureType, SimpleFeature> features = source.getFeatures(allFilters);

            Iterator<SimpleFeature> iterator = features.iterator();
            try {

                LineString shortest = null;
                double currentDistance = Double.MAX_VALUE;
                // Calculate nearest LineString
                while (iterator.hasNext()) {
                    Feature feature = (Feature) iterator.next();
                    LineString line = (LineString) feature.getDefaultGeometryProperty().getValue();
                    double dist = line.distance(clickPoint);
                    if (dist < currentDistance) {
                        currentDistance = dist;
                        shortest = line;
                    }
                }

                if (shortest != null) {
                    double currentDistancePoint = Double.MAX_VALUE;
                    // Calculate nearest Point of nearest LineString
                    for (Coordinate c : shortest.getCoordinates()) {
                        Point point = fac.createPoint(c);
                        double dist = point.distance(clickPoint);
                        if (dist < currentDistancePoint) {
                            currentDistancePoint = dist;
                            near = point;
                        }
                    }
                }
            } finally {
                features.close(iterator);
            }
            return near;
        } catch (TransformException ex) {
            logger.fatal(ex.getMessage(), ex);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private RoutingResult route(Graph g, double distance, Point startPoint, Point clickPoint) {
        RoutingResult result = new RoutingResult();
        List<Point> collection = new ArrayList<Point>();
        collection.add(clickPoint);
        double startDistance = 2 * clickPoint.distance(startPoint);

        Node startNode = null;
        for (Node n : (Collection<Node>) g.getNodes()) {
            Point p = fac.createPoint((Coordinate) n.getObject());
            if (p.equalsExact(startPoint)) {
                startNode = n;
            }
        }

        // RoutingAlgorithm algorithm = new RandomHalfwayRouting();
        RoutingAlgorithm algorithm = new BoxRouting();
        double calculatedDistance = algorithm.doRouting(startNode, g, collection, distance - startDistance);

        collection.add(clickPoint);
        result.setDistance(calculatedDistance + startDistance);
        result.setRoute(collection);

        // calculate markers
        result.setMarkers(getKilometerMarkers(collection));

        return result;
    }

    private List<Point> getKilometerMarkers(List<Point> collection) {
        return collection;
    }
}
