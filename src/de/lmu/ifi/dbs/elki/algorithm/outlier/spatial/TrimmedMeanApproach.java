package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2012
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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * A Trimmed Mean Approach to Finding Spatial Outliers.
 * 
 * Outliers are defined by their value deviation from a trimmed mean of the neighbors.
 * 
 * <p>
 * Reference: <br>
 * Tianming Hu and Sam Yuan Sung<br>
 * A Trimmed Mean Approach to finding Spatial Outliers<br>
 * in Intelligent Data Analysis, Volume 8, 2004.
 * </p>
 * 
 * <p>
 * the contiguity Matrix is definit as <br>
 * wij = 1/k if j is neighbor of i, k is the neighbors size of i.
 * </p>
 * 
 * @author Ahmed Hettab
 * @param <N> Neighborhood object type
 */
@Title("A Trimmed Mean Approach to Finding Spatial Outliers")
@Description("A local trimmed mean approach to evaluating the spatial outlier factor which is the degree that a site is outlying compared to its neighbors")
@Reference(authors = "Tianming Hu and Sam Yuan Sung", title = "A trimmed mean approach to finding spatial outliers", booktitle = "Intelligent Data Analysis, Volume 8, 2004", url = "http://iospress.metapress.com/content/PLVLT6431DVNJXNK")
public class TrimmedMeanApproach<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(TrimmedMeanApproach.class);

  /**
   * the parameter p
   */
  private double p;

  /**
   * Constructor
   * 
   * @param p Parameter p
   * @param npredf Neighborhood factory.
   */
  protected TrimmedMeanApproach(NeighborSetPredicate.Factory<N> npredf, double p) {
    super(npredf);
    this.p = p;
  }

  /**
   * Run the algorithm
   * 
   * @param database Database
   * @param nrel Neighborhood relation
   * @param relation Data Relation (1 dimensional!)
   * @return Outlier detection result
   */
  public OutlierResult run(Database database, Relation<N> nrel, Relation<? extends NumberVector<?, ?>> relation) {
    assert (DatabaseUtil.dimensionality(relation) == 1) : "TrimmedMean can only process one-dimensional data sets.";
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(nrel);

    WritableDoubleDataStore errors = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP);
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);

    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Computing trimmed means", relation.size(), logger) : null;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DBID id  = iditer.getDBID();
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      int num = 0;
      double[] values = new double[neighbors.size()];
      // calculate trimmedMean
      for(DBID n : neighbors) {
        values[num] = relation.get(n).doubleValue(1);
        num++;
      }

      // calculate local trimmed Mean and error term
      final double tm;
      if(num > 0) {
        int left = (int) Math.floor(p * (num - 1));
        int right = (int) Math.floor((1 - p) * (num - 1));
        Arrays.sort(values, 0, num);
        Mean mean = new Mean();
        for(int i = left; i <= right; i++) {
          mean.put(values[i]);
        }
        tm = mean.getMean();
      }
      else {
        tm = relation.get(id).doubleValue(1);
      }
      // Error: deviation from trimmed mean
      errors.putDouble(id, relation.get(id).doubleValue(1) - tm);

      if(progress != null) {
        progress.incrementProcessed(logger);
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }

    if(logger.isVerbose()) {
      logger.verbose("Computing median error.");
    }
    double median_dev_from_median;
    {
      // calculate the median error
      double[] ei = new double[relation.size()];
      {
        int i = 0;
        for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
          DBID id  = iditer.getDBID();
          ei[i] = errors.doubleValue(id);
          i++;
        }
      }
      double median_i = QuickSelect.median(ei);
      // Update to deviation from median
      for(int i = 0; i < ei.length; i++) {
        ei[i] = Math.abs(ei[i] - median_i);
      }
      // Again, extract median
      median_dev_from_median = QuickSelect.median(ei);
    }

    if(logger.isVerbose()) {
      logger.verbose("Normalizing scores.");
    }
    // calculate score
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DBID id  = iditer.getDBID();
      double score = Math.abs(errors.doubleValue(id)) * 0.6745 / median_dev_from_median;
      scores.putDouble(id, score);
      minmax.put(score);
    }
    //
    Relation<Double> scoreResult = new MaterializedRelation<Double>("TrimmedMean", "Trimmed Mean Score", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    OutlierResult or = new OutlierResult(scoreMeta, scoreResult);
    or.addChildResult(npred);
    return or;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // Get one dimensional attribute for analysis.
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), VectorFieldTypeInformation.get(NumberVector.class, 1));
  }

  /**
   * Parameterizer
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.exclude
   * 
   * @param <N> Neighborhood object type
   */
  public static class Parameterizer<N> extends AbstractNeighborhoodOutlier.Parameterizer<N> {
    /**
     * Parameter for the percentile value p
     */
    public static final OptionID P_ID = OptionID.getOrCreateOptionID("tma.p", "the percentile parameter");

    /**
     * Percentile parameter p
     */
    protected double p = 0.2;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter pP = new DoubleParameter(P_ID, new IntervalConstraint(0.0, IntervalBoundary.OPEN, 0.5, IntervalBoundary.OPEN));
      if(config.grab(pP)) {
        p = pP.getValue();
      }
    }

    @Override
    protected TrimmedMeanApproach<N> makeInstance() {
      return new TrimmedMeanApproach<N>(npredf, p);
    }
  }
}