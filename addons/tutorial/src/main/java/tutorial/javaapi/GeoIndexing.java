/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package tutorial.javaapi;

import java.util.Arrays;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.geo.LatLngDistanceFunction;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.SortTileRecursiveBulkSplit;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.math.geodesy.WGS84SpheroidEarthModel;
import de.lmu.ifi.dbs.elki.persistent.AbstractPageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;

/**
 * Example code for using the R-tree index of ELKI, with Haversine distance.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class GeoIndexing {
  public static void main(String[] args) {
    // Set the logging level to statistics:
    LoggingConfiguration.setStatistics();

    // Generate a random data set.
    Random rand = new Random(0L);
    // Note: ELKI has a nice data generator class, use that instead.
    double[][] data = new double[100000][];
    for(int i = 0; i < data.length; i++) {
      data[i] = randomLatitudeLongitude(rand);
    }

    // Adapter to load data from an existing array.
    DatabaseConnection dbc = new ArrayAdapterDatabaseConnection(data);

    // Since the R-tree has so many options, it is a bit easier to configure it
    // using the parameterization API, which handles defaults, instantiation,
    // and additional constraint checks.
    RStarTreeFactory<?> indexfactory = new ELKIBuilder<>(RStarTreeFactory.class) //
        // If you have large query results, a larger page size can be better.
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 512) //
        // Use bulk loading, for better performance.
        .with(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SortTileRecursiveBulkSplit.class) //
        .build();
    // Create the database, and initialize it.
    Database db = new StaticArrayDatabase(dbc, Arrays.asList(indexfactory));
    // This will build the index of the database.
    db.initialize();
    // Relation containing the number vectors we put in above:
    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    // We can use this to identify rows of the input data below.
    DBIDRange ids = (DBIDRange) rel.getDBIDs();

    // For all indexes, dump their statistics.
    for(It<Index> it = db.getHierarchy().iterDescendants(db).filter(Index.class); it.valid(); it.advance()) {
      it.get().logStatistics();
    }

    // We use the WGS84 earth model, and "latitude, longitude" coordinates:
    // This distance function returns meters.
    LatLngDistanceFunction df = new LatLngDistanceFunction(WGS84SpheroidEarthModel.STATIC);
    // k nearest neighbor query:
    KNNQuery<NumberVector> knnq = QueryUtil.getKNNQuery(rel, df);

    // Let's find the closest points to New York:
    DoubleVector newYork = DoubleVector.wrap(new double[] { 40.730610, -73.935242 });
    KNNList knns = knnq.getKNNForObject(newYork, 10);
    // Iterate over all results.
    System.out.println("Close to New York:");
    for(DoubleDBIDListIter it = knns.iter(); it.valid(); it.advance()) {
      double km = it.doubleValue() / 1000; // To kilometers
      System.out.println(rel.get(it) + " distance: " + km + " km row: " + ids.getOffset(it));
    }

    // Many other indexes will fail if we search close to the date line.
    DoubleVector tuvalu = DoubleVector.wrap(new double[] { -7.4784205, 178.679924 });
    knns = knnq.getKNNForObject(tuvalu, 10);
    // Iterate over all results.
    System.out.println("Close to Tuvalu:");
    for(DoubleDBIDListIter it = knns.iter(); it.valid(); it.advance()) {
      double km = it.doubleValue() / 1000; // To kilometers
      System.out.println(rel.get(it) + " distance: " + km + " km row: " + ids.getOffset(it));
    }
    // Note that the ELKI index does find points both at longitude +179 and -179

    // For all indexes, dump their statistics, again.
    // You will see some have increased their values.
    // System.out.println(Arrays.toString(data[ids.getOffset(it)]));

    // But also that we only read a small part of the data, and only computed
    // the distances to a few points in the data set.
    for(It<Index> it = db.getHierarchy().iterDescendants(db).filter(Index.class); it.valid(); it.advance()) {
      it.get().logStatistics();
    }
  }

  /**
   * Generate random coordinates.
   * 
   * @param r Random generator
   * @return Latitude, Longitude array.
   */
  private static double[] randomLatitudeLongitude(Random r) {
    // Make marginally more realistic looking data by non-uniformly sampling
    // latitude, since Earth is a sphere, and there is not much at the poles
    double lat = Math.pow(1. - r.nextDouble() * 2., 2) / 2. * 180;
    double lng = (.5 - r.nextDouble()) * 360.;
    return new double[] { lat, lng };
  }
}
