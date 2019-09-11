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
package elki.outlier.spatial;

import java.util.Arrays;

import elki.outlier.spatial.neighborhood.NeighborSetPredicate;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.DoubleMinMax;
import elki.math.Mean;
import elki.result.Metadata;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.datastructures.QuickSelect;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * A Trimmed Mean Approach to Finding Spatial Outliers.
 * <p>
 * Outliers are defined by their value deviation from a trimmed mean of the
 * neighbors.
 * <p>
 * Reference:
 * <p>
 * T. Hu, S. Y. Sung<br>
 * A Trimmed Mean Approach to finding Spatial Outliers<br>
 * Intelligent Data Analysis 8
 * <p>
 * the contiguity Matrix is definit as<br>
 * wij = 1/k if j is neighbor of i, k is the neighbors size of i.
 * 
 * @author Ahmed Hettab
 * @since 0.4.0
 * @param <N> Neighborhood object type
 */
@Title("A Trimmed Mean Approach to Finding Spatial Outliers")
@Description("A local trimmed mean approach to evaluating the spatial outlier factor which is the degree that a site is outlying compared to its neighbors")
@Reference(authors = "T. Hu, S. Y. Sung", //
    title = "A trimmed mean approach to finding spatial outliers", //
    booktitle = "Intelligent Data Analysis 8", //
    url = "http://content.iospress.com/articles/intelligent-data-analysis/ida00153", //
    bibkey = "DBLP:journals/ida/HuS04")
public class TrimmedMeanApproach<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(TrimmedMeanApproach.class);

  /**
   * the parameter p.
   */
  private double p;

  /**
   * Constructor.
   * 
   * @param p Parameter p
   * @param npredf Neighborhood factory.
   */
  protected TrimmedMeanApproach(NeighborSetPredicate.Factory<N> npredf, double p) {
    super(npredf);
    this.p = p;
  }

  /**
   * Run the algorithm.
   * 
   * @param database Database
   * @param nrel Neighborhood relation
   * @param relation Data Relation (1 dimensional!)
   * @return Outlier detection result
   */
  public OutlierResult run(Database database, Relation<N> nrel, Relation<? extends NumberVector> relation) {
    assert (RelationUtil.dimensionality(relation) == 1) : "TrimmedMean can only process one-dimensional data sets.";
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(database, nrel);

    WritableDoubleDataStore errors = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP);
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Computing trimmed means", relation.size(), LOG) : null;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DBIDs neighbors = npred.getNeighborDBIDs(iditer);
      int num = 0;
      double[] values = new double[neighbors.size()];
      // calculate trimmedMean
      for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
        values[num] = relation.get(iter).doubleValue(0);
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
        tm = relation.get(iditer).doubleValue(0);
      }
      // Error: deviation from trimmed mean
      errors.putDouble(iditer, relation.get(iditer).doubleValue(0) - tm);

      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);

    if(LOG.isVerbose()) {
      LOG.verbose("Computing median error.");
    }
    double median_dev_from_median;
    {
      // calculate the median error
      double[] ei = new double[relation.size()];
      {
        int i = 0;
        for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
          ei[i] = errors.doubleValue(iditer);
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

    if(LOG.isVerbose()) {
      LOG.verbose("Normalizing scores.");
    }
    // calculate score
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double score = Math.abs(errors.doubleValue(iditer)) * 0.6745 / median_dev_from_median;
      scores.putDouble(iditer, score);
      minmax.put(score);
    }
    //
    DoubleRelation scoreResult = new MaterializedDoubleRelation("TrimmedMean", relation.getDBIDs(), scores);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    OutlierResult or = new OutlierResult(scoreMeta, scoreResult);
    Metadata.hierarchyOf(or).addChild(npred);
    return or;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // Get one dimensional attribute for analysis.
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD_1D);
  }

  /**
   * Par.
   * 
   * @author Ahmed Hettab
   * 
   * @hidden
   * 
   * @param <N> Neighborhood object type
   */
  public static class Par<N> extends AbstractNeighborhoodOutlier.Par<N> {
    /**
     * Parameter for the percentile value p.
     */
    public static final OptionID P_ID = new OptionID("tma.p", "the percentile parameter");

    /**
     * Percentile parameter p.
     */
    protected double p = 0.2;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new DoubleParameter(P_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_HALF_DOUBLE) //
          .grab(config, x -> p = x);
    }

    @Override
    public TrimmedMeanApproach<N> make() {
      return new TrimmedMeanApproach<>(npredf, p);
    }
  }
}
