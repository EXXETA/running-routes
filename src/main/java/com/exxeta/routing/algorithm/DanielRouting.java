package com.exxeta.routing.algorithm;

import java.util.List;

import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.DijkstraIterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;

public class DanielRouting implements RoutingAlgorithm {

    private static Logger logger = Logger.getLogger(DanielRouting.class.getName());

    @SuppressWarnings("unchecked")
    @Override
    public double doRouting(Node startNode, Graph g, List<Point> collection, double distance) {
        GeometryFactory fac = new GeometryFactory();
        double currentDistance = 0;
        Node currentNode = startNode;
        collection.add(fac.createPoint((Coordinate) startNode.getObject()));

        // Random start-edge with direction
        Edge randomEdge = (Edge) currentNode.getEdges().get(0);
        double angle = calculateAngle(currentNode, randomEdge.getOtherNode(currentNode));
        logger.debug("Start angle: " + angle);
        currentDistance += calculateDistance(randomEdge);
        Node otherNode = randomEdge.getOtherNode(currentNode);
        currentNode = otherNode;
        collection.add(fac.createPoint((Coordinate) currentNode.getObject()));

        // calculate one quarter
        while (currentDistance < distance / 2) {
            randomEdge = getBestEdge(currentNode, angle);

            otherNode = randomEdge.getOtherNode(currentNode);
            currentDistance += ((Coordinate) currentNode.getObject()).distance((Coordinate) otherNode.getObject());
            currentNode = otherNode;
            randomEdge.setVisited(true);
            collection.add(fac.createPoint((Coordinate) currentNode.getObject()));
        }

        // Calculate last part
        DijkstraIterator.EdgeWeighter weighterWithoutVisited = new DijkstraIterator.EdgeWeighter() {

            @Override
            public double getWeight(org.geotools.graph.structure.Edge e) {
                if (e.isVisited()) {
                    return Double.MAX_VALUE;
                }
                return calculateDistance(e);
            }
        };

        DijkstraShortestPathFinder pf = new DijkstraShortestPathFinder(g, startNode, weighterWithoutVisited);
        pf.calculate();
        Path path = pf.getPath(currentNode);
        if (path == null) {
            DijkstraIterator.EdgeWeighter weighterWithVisited = new DijkstraIterator.EdgeWeighter() {

                @Override
                public double getWeight(org.geotools.graph.structure.Edge e) {
                    return calculateDistance(e);
                }
            };
            pf = new DijkstraShortestPathFinder(g, startNode, weighterWithVisited);
            pf.calculate();
            path = pf.getPath(currentNode);
        }
        logger.debug("Path: " + path);
        for (Edge e : (List<Edge>) path.getEdges()) {
            otherNode = e.getOtherNode(currentNode);
            currentDistance += ((Coordinate) currentNode.getObject()).distance((Coordinate) otherNode.getObject());
            currentNode = otherNode;
            collection.add(fac.createPoint((Coordinate) currentNode.getObject()));
        }
        return currentDistance;
    }

    private double calculateAngle(Node currentNode, Node otherNode) {
        Coordinate a = (Coordinate) currentNode.getObject();
        Coordinate b = (Coordinate) otherNode.getObject();
        double angle = Math.toDegrees(Math.atan2(b.y - a.y, b.x - a.x));
        logger.debug("x: " + (b.x - a.x) + ", y: " + (b.y - a.y) + ", angle: " + angle);
        return angle;
    }

    private double calculateDistance(Edge e) {
        return ((Coordinate) e.getNodeA().getObject()).distance((Coordinate) e.getNodeB().getObject());
    }

    @SuppressWarnings("unchecked")
    private Edge getBestEdge(Node node, double angle) {
        Edge bestEdge = null;
        double currentAngleDifference = Double.MAX_VALUE;
        for (Edge e : (List<Edge>) node.getEdges()) {
            double tmpAngleDifference = Math.abs(calculateAngle(node, e.getOtherNode(node)) - angle);
            if (tmpAngleDifference < currentAngleDifference && !e.isVisited()) {
                currentAngleDifference = tmpAngleDifference;
                bestEdge = e;
            }
        }
        if (bestEdge == null) {
            for (Edge e : (List<Edge>) node.getEdges()) {
                double tmpAngleDifference = Math.abs(calculateAngle(node, e.getOtherNode(node)) - angle);
                if (tmpAngleDifference < currentAngleDifference) {
                    currentAngleDifference = tmpAngleDifference;
                    bestEdge = e;
                }
            }
        }
        return bestEdge;
    }
}
