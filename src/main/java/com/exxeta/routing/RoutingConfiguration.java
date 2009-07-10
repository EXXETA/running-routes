package com.exxeta.routing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisDataStoreFactory;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

public class RoutingConfiguration {

    /**
     * Retrieves a {@link DataStore} for the local PostGIS database.
     *
     * @return data store for accessing features.
     * @throws java.io.IOException If the data store cannot be retrieved properly.
     */
    public static DataStore getPostGISDataStore() throws IOException {
        Map<String, String> config = new HashMap<String, String>();
        config.put(PostgisDataStoreFactory.DBTYPE.key, "postgis");
        config.put(PostgisDataStoreFactory.HOST.key, "localhost");
        config.put(PostgisDataStoreFactory.PORT.key, "5432");
        config.put(PostgisDataStoreFactory.SCHEMA.key, "public");
        config.put(PostgisDataStoreFactory.DATABASE.key, "gis");
        config.put(PostgisDataStoreFactory.USER.key, "postgres");
        config.put(PostgisDataStoreFactory.PASSWD.key, "postgres");
        return DataStoreFinder.getDataStore(config);
    }

    /**
     * Retrieves a list of filters that match the highways that are used for routing.
     *
     * @param ff filter factory for constructing the filters.
     * @return the list of filters.
     */
    public static List<Filter> getHighwayFilters(FilterFactory2 ff) {
        List<Filter> highwayMatch = new ArrayList<Filter>();
        highwayMatch.add(ff.equal(ff.property("highway"), ff.literal("residential"), false));
        highwayMatch.add(ff.equal(ff.property("highway"), ff.literal("track"), false));
        highwayMatch.add(ff.equal(ff.property("highway"), ff.literal("cycleway"), false));
        highwayMatch.add(ff.equal(ff.property("highway"), ff.literal("footway"), false));
        highwayMatch.add(ff.equal(ff.property("highway"), ff.literal("service"), false));
        highwayMatch.add(ff.equal(ff.property("highway"), ff.literal("unclassified"), false));
        highwayMatch.add(ff.equal(ff.property("highway"), ff.literal("living_street"), false));
        highwayMatch.add(ff.equal(ff.property("highway"), ff.literal("pedestrian"), false));
        highwayMatch.add(ff.equal(ff.property("highway"), ff.literal("path"), false));
        highwayMatch.add(ff.equal(ff.property("highway"), ff.literal("bridleway"), false));
        highwayMatch.add(ff.equal(ff.property("highway"), ff.literal("byway"), false));
        highwayMatch.add(ff.equal(ff.property("highway"), ff.literal("steps"), false));
        return highwayMatch;
    }
}
