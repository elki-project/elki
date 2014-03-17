package de.lmu.ifi.dbs.elki.algorithm.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Outlier Detection based on the accumulated distances of a point to its k
 * nearest neighbors.
 * 
 * This implementation uses the k-nearest-neighbor definition of a database,
 * which inclues the query point. Using k=1 is therefore not sensible, as all
 * objects will have a score of 0 according to this definition.
 * 
 * Furthermore, we report the sum of the k distances, not the average (some
 * implementations will return the average).
 * 
 * Reference:
 * <p>
 * F. Angiulli, C. Pizzuti: Fast Outlier Detection in High Dimensional Spaces.
 * In: Proc. European Conference on Principles of Knowledge Discovery and Data
 * Mining (PKDD'02), Helsinki, Finland, 2002.
 * </p>
 * 
 * @author Lisa Reichert
 * 
 * @apiviz.has KNNQuery
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
@Title("KNNWeight outlier detection")
@Description("Outlier Detection based on the distances of an object to its k nearest neighbors.")
@Reference(authors = "F. Angiulli, C. Pizzuti", title = "Fast Outlier Detection in High Dimensional Spaces", booktitle = "Proc. European Conference on Principles of Knowledge Discovery and Data Mining (PKDD'02), Helsinki, Finland, 2002", url = "http://dx.doi.org/10.1007/3-540-45681-3_2")
public class KNNWeightOutlier<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KNNWeightOutlier.class);

  /**
   * Parameter to specify the k nearest neighbor.
   */
  public static final OptionID K_ID = new OptionID("knnwod.k", //
  "The k nearest neighbor, according to the database definition "//
      + "(where the 1NN is usually the query point, yielding a distance of 0)");

  /**
   * Holds the number of nearest neighbors to query (including query point!)
   */
  private int k;

  /**
   * Constructor with parameters.
   * 
   * @param distanceFunction Distance function
   * @param k k Parameter (including query point!)
   */
  public KNNWeightOutlier(DistanceFunction<? super O> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    final DistanceQuery<O> distanceQuery = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O> knnQuery = database.getKNNQuery(distanceQuery, k);

    if(LOG.isVerbose()) {
      LOG.verbose("computing outlier degree(sum of the distances to the k nearest neighbors");
    }
    FiniteProgress progressKNNWeight = LOG.isVerbose() ? new FiniteProgress("KNNWOD_KNNWEIGHT for objects", relation.size(), LOG) : null;

    DoubleMinMax minmax = new DoubleMinMax();

    // compute distance to the k nearest neighbor. n objects with the highest
    // distance are flagged as outliers
    WritableDoubleDataStore knnw_score = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      // compute sum of the distances to the k nearest neighbors

      final KNNList knn = knnQuery.getKNNForDBID(iditer, k);
      double skn = 0;
      int i = 0;
      for(DoubleDBIDListIter neighbor = knn.iter(); i < k && neighbor.valid(); neighbor.advance(), ++i) {
        skn += neighbor.doubleValue();
      }
      knnw_score.putDouble(iditer, skn);
      minmax.put(skn);

      if(progressKNNWeight != null) {
        progressKNNWeight.incrementProcessed(LOG);
      }
    }
    if(progressKNNWeight != null) {
      progressKNNWeight.ensureCompleted(LOG);
    }

    DoubleRelation res = new MaterializedDoubleRelation("Weighted kNN Outlier Score", "knnw-outlier", knnw_score, relation.getDBIDs());
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    return new OutlierResult(meta, res);
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
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    protected int k = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID) //
      .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected KNNWeightOutlier<O> makeInstance() {
      return new KNNWeightOutlier<>(distanceFunction, k);
    }
  }
}