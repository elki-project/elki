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

import elki.clustering.kmeans.LloydKMeans;
import elki.clustering.kmeans.initialization.RandomUniformGenerated;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRange;
import elki.database.relation.Relation;
import elki.datasource.ArrayAdapterDatabaseConnection;
import elki.datasource.DatabaseConnection;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.LoggingConfiguration;
import elki.utilities.random.RandomFactory;

/**
 * Example program to generate a random data set, and run k-means on it.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class PassingDataToELKI {
  /**
   * Main method
   * 
   * @param args Command line parameters (not supported)
   */
  public static void main(String[] args) {
    // Set the logging level to statistics:
    LoggingConfiguration.setStatistics();

    // Generate a random data set.
    // Note: ELKI has a nice data generator class, use that instead.
    double[][] data = new double[1000][2];
    for(int i = 0; i < data.length; i++) {
      for(int j = 0; j < data[i].length; j++) {
        data[i][j] = Math.random();
      }
    }

    // Adapter to load data from an existing array.
    DatabaseConnection dbc = new ArrayAdapterDatabaseConnection(data);
    // Create a database (which may contain multiple relations!)
    Database db = new StaticArrayDatabase(dbc, null);
    // Load the data into the database (do NOT forget to initialize...)
    db.initialize();
    // Relation containing the number vectors:
    Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    // We know that the ids must be a continuous range:
    DBIDRange ids = (DBIDRange) rel.getDBIDs();

    // K-means should be used with squared Euclidean (least squares):
    SquaredEuclideanDistance dist = SquaredEuclideanDistance.STATIC;
    // Default initialization, using global random:
    // To fix the random seed, use: new RandomFactory(seed);
    RandomUniformGenerated init = new RandomUniformGenerated(RandomFactory.DEFAULT);

    // Textbook k-means clustering:
    LloydKMeans<NumberVector> km = new LloydKMeans<>(dist, //
    3 /* k - number of partitions */, //
    0 /* maximum number of iterations: no limit */, init);

    // K-means will automatically choose a numerical relation from the data set:
    // But we could make it explicit (if there were more than one numeric
    // relation!): km.run(db, rel);
    Clustering<KMeansModel> c = km.run(db);

    // Output all clusters:
    int i = 0;
    for(Cluster<KMeansModel> clu : c.getAllClusters()) {
      // K-means will name all clusters "Cluster" in lack of noise support:
      System.out.println("#" + i + ": " + clu.getNameAutomatic());
      System.out.println("Size: " + clu.size());
      System.out.println("Center: " + clu.getModel().getPrototype().toString());
      // Iterate over objects:
      System.out.print("Objects: ");
      for(DBIDIter it = clu.getIDs().iter(); it.valid(); it.advance()) {
        // To get the vector use:
        // NumberVector v = rel.get(it);

        // Offset within our DBID range: "line number"
        final int offset = ids.getOffset(it);
        System.out.print(" " + offset);
        // Do NOT rely on using "internalGetIndex()" directly!
      }
      System.out.println();
      ++i;
    }
  }
}
