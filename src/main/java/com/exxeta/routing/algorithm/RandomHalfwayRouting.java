package com.exxeta.routing.algorithm;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;


public class RandomHalfwayRouting implements RoutingAlgorithm {

    @SuppressWarnings("unchecked")
	@Override
    public double doRouting(Node startNode, Graph g, List<Point> collection, double distance) {
        GeometryFactory fac = new GeometryFactory();
        // Calculate way
        // Choose next point randomly until half the distance is reached
        double currentDistance = 0;
        Random rand = new Random();
        Node currentNode = startNode;
        collection.add(fac.createPoint((Coordinate) startNode.getObject()));
        while (currentDistance < distance / 2) {
            Edge randomEdge = (Edge) currentNode.getEdges().get(rand.nextInt(currentNode.getDegree()));
            if (randomEdge.isVisited() && currentNode.getDegree() > 1) {
                for (Edge e : (Collection<Edge>) currentNode.getEdges()) {
                    if (!e.isVisited()) {
                        randomEdge = e;
                    }
                }
            }
            Node otherNode = randomEdge.getOtherNode(currentNode);
            currentDistance += ((Coordinate) currentNode.getObject()).distance((Coordinate) otherNode.getObject());
//            logger.info(currentNode.getObject() + " -> " + otherNode.getObject() + " : " + ((Coordinate) currentNode.getObject()).distance((Coordinate) otherNode.getObject()));
//            logger.info("currentDistance: " + currentDistance);
            currentNode = otherNode;
            randomEdge.setVisited(true);
            collection.add(fac.createPoint((Coordinate) currentNode.getObject()));
        }
        return currentDistance;
    }
}
