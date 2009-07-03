package com.exxeta.routing.algorithm;

import com.vividsolutions.jts.geom.Point;
import java.util.List;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;

public interface RoutingAlgorithm {

    public double doRouting(Node startNode, Graph g, List<Point> collection, double d);

}
