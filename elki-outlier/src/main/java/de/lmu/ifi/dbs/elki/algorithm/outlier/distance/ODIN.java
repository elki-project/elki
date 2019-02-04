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
package de.lmu.ifi.dbs.elki.algorithm.outlier.distance;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Outlier detection based on the in-degree of the kNN graph.
 * <p>
 * This is a curried version: instead of using a threshold T to obtain a binary
 * decision, we use the computed value as outlier score; normalized by k to make
 * the numbers more comparable across different parameterizations.
 * <p>
 * Reference:
 * <p>
 * V. Hautamäki and I. Kärkkäinen and P. Fränti<br>
 * Outlier detection using k-nearest neighbour graph<br>
 * Proc. 17th Int. Conf. Pattern Recognition (ICPR 2004)
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @param <O> Object type
 */
@Title("ODIN: Outlier Detection Using k-Nearest Neighbour Graph")
@Reference(authors = "V. Hautamäki, I. Kärkkäinen, P. Fränti", //
    title = "Outlier detection using k-nearest neighbour graph", //
    booktitle = "Proc. 17th Int. Conf. Pattern Recognition (ICPR 2004)", //
    url = "https://doi.org/10.1109/ICPR.2004.1334558", //
    bibkey = "DBLP:conf/icpr/HautamakiKF04")
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.outlier.ODIN" })
public class ODIN<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ODIN.class);

  /**
   * Number of neighbors for kNN graph.
   */
  int k;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param k k parameter
   */
  public ODIN(DistanceFunction<? super O> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k + 1; // + query point
  }

  /**
   * Run the ODIN algorithm
   *
   * @param database Database to run on.
   * @param relation Relation to process.
   * @return ODIN outlier result.
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    // Get the query functions:
    DistanceQuery<O> dq = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O> knnq = database.getKNNQuery(dq, k);

    // Get the objects to process, and a data storage for counting and output:
    DBIDs ids = relation.getDBIDs();
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB, 0.);

    double inc = 1. / (k - 1);
    double min = Double.POSITIVE_INFINITY, max = 0.0;
    // Process all objects
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      // Find the nearest neighbors (using an index, if available!)
      DBIDs neighbors = knnq.getKNNForDBID(iter, k);
      // For each neighbor, except ourselves, increase the in-degree:
      for(DBIDIter nei = neighbors.iter(); nei.valid(); nei.advance()) {
        if(DBIDUtil.equal(iter, nei)) {
          continue;
        }
        final double value = scores.doubleValue(nei) + inc;
        if(value < min) {
          min = value;
        }
        if(value > max) {
          max = value;
        }
        scores.put(nei, value);
      }
    }

    // Wrap the result and add metadata.
    OutlierScoreMeta meta = new InvertedOutlierScoreMeta(min, max, 0., inc * (ids.size() - 1), 1);
    DoubleRelation rel = new MaterializedDoubleRelation("ODIN In-Degree", "odin", scores, ids);
    return new OutlierResult(meta, rel);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter for the number of nearest neighbors:
     *
     * <pre>
     * -odin.k &lt;int&gt;
     * </pre>
     */
    public static final OptionID K_ID = new OptionID("odin.k", "Number of neighbors to use for kNN graph.");

    /**
     * Number of nearest neighbors to use.
     */
    int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter param = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(param)) {
        k = param.intValue();
      }
    }

    @Override
    protected ODIN<O> makeInstance() {
      return new ODIN<>(distanceFunction, k);
    }
  }
}
