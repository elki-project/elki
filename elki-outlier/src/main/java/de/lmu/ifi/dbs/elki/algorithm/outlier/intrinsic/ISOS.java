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
package de.lmu.ifi.dbs.elki.algorithm.outlier.intrinsic;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.distance.SOS;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.AggregatedHillEstimator;
import de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality.IntrinsicDimensionalityEstimator;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Intrinsic Stochastic Outlier Selection.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Michael Gertz<br>
 * Intrinsic t-Stochastic Neighbor Embedding for Visualization and Outlier
 * Detection: A Remedy Against the Curse of Dimensionality?<br>
 * Proc. Int. Conf. Similarity Search and Applications, SISAP'2017
 * 
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <O> Object type.
 */
@Title("ISOS: Intrinsic Stochastic Outlier Selection")
@Reference(authors = "Erich Schubert, Michael Gertz", //
    title = "Intrinsic t-Stochastic Neighbor Embedding for Visualization and Outlier Detection: A Remedy Against the Curse of Dimensionality?", //
    booktitle = "Proc. Int. Conf. Similarity Search and Applications, SISAP'2017", //
    url = "https://doi.org/10.1007/978-3-319-68474-1_13", //
    bibkey = "DBLP:conf/sisap/SchubertG17")
public class ISOS<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ISOS.class);

  /**
   * Number of neighbors (not including query point).
   */
  protected int k;

  /**
   * Estimator of intrinsic dimensionality.
   */
  IntrinsicDimensionalityEstimator estimator;

  /**
   * Expected outlier rate.
   */
  protected double phi = 0.01;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param k Number of neighbors to consider
   * @param estimator Estimator of intrinsic dimensionality.
   */
  public ISOS(DistanceFunction<? super O> distance, int k, IntrinsicDimensionalityEstimator estimator) {
    super(distance);
    this.k = k;
    this.estimator = estimator;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  /**
   * Run the algorithm.
   * 
   * @param relation data relation.
   * @return outlier detection result
   */
  public OutlierResult run(Relation<O> relation) {
    final int k1 = k + 1; // Query size
    final double perplexity = k / 3.;
    KNNQuery<O> knnq = relation.getKNNQuery(getDistanceFunction(), k1);
    final double logPerp = perplexity > 1. ? FastMath.log(perplexity) : .1;

    double[] p = new double[k + 10];
    ModifiableDoubleDBIDList dists = DBIDUtil.newDistanceDBIDList(k + 10);
    DoubleDBIDListIter di = dists.iter();
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("ISOS scores", relation.size(), LOG) : null;
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB, 1.);
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      KNNList knns = knnq.getKNNForDBID(it, k1);
      if(p.length < knns.size() + 1) {
        p = new double[knns.size() + 10];
      }
      final DoubleDBIDListIter ki = knns.iter();
      try {
        double id = estimateID(it, ki, p);
        adjustDistances(it, ki, knns.getKNNDistance(), id, dists);
        // We now continue with the modified distances below.
        // Compute affinities
        SOS.computePi(it, di, p, perplexity, logPerp);
        // Normalization factor:
        double s = SOS.sumOfProbabilities(it, di, p);
        if(s > 0.) {
          nominateNeighbors(it, di, p, 1. / s, scores);
        }
      }
      catch(ArithmeticException e) {
        // ID estimation failed, supposedly constant values because of too many
        // duplicate points, or too small k. Fall back to KNNSOS.
        // Note: this looks almost identical to the above, but uses ki instead
        // of the adjusted distances di!
        // Compute affinities
        SOS.computePi(it, ki, p, perplexity, logPerp);
        // Normalization factor:
        double s = SOS.sumOfProbabilities(it, ki, p);
        if(s > 0.) {
          nominateNeighbors(it, ki, p, 1. / s, scores);
        }
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    DoubleMinMax minmax = transformScores(scores, relation.getDBIDs(), logPerp, phi);
    DoubleRelation scoreres = new MaterializedDoubleRelation("Intrinsic Stoachastic Outlier Selection", "isos-outlier", scores, relation.getDBIDs());
    OutlierScoreMeta meta = new ProbabilisticOutlierScore(minmax.getMin(), minmax.getMax(), 0.);
    return new OutlierResult(meta, scoreres);
  }

  protected static void adjustDistances(DBIDRef ignore, DoubleDBIDListIter ki, double max, double id, ModifiableDoubleDBIDList dists) {
    dists.clear();
    double scaleexp = id * .5; // Generate squared distances.
    double scalelin = 1. / max; // Linear scaling
    for(ki.seek(0); ki.valid(); ki.advance()) {
      if(DBIDUtil.equal(ignore, ki)) {
        continue;
      }
      double d = FastMath.pow(ki.doubleValue() * scalelin, scaleexp);
      dists.add(d, ki);
    }
    return;
  }

  /**
   * Estimate the local intrinsic dimensionality.
   * 
   * @param ignore Object to ignore
   * @param it Iterator
   * @param p Scratch array
   * @return ID estimate
   */
  protected double estimateID(DBIDRef ignore, DoubleDBIDListIter it, double[] p) {
    int j = 0;
    for(it.seek(0); it.valid(); it.advance()) {
      if(it.doubleValue() == 0. || DBIDUtil.equal(ignore, it)) {
        continue;
      }
      p[j++] = it.doubleValue();
    }
    if(j < 2) {
      throw new ArithmeticException("Too little data to estimate ID.");
    }
    return estimator.estimate(p, j);
  }

  /**
   * Vote for neighbors not being outliers. The key method of SOS.
   * 
   * @param ignore Object to ignore
   * @param di Neighbor object IDs.
   * @param p Probabilities
   * @param norm Normalization factor (1/sum)
   * @param scores Output score storage
   */
  public static void nominateNeighbors(DBIDIter ignore, DBIDArrayIter di, double[] p, double norm, WritableDoubleDataStore scores) {
    for(di.seek(0); di.valid(); di.advance()) {
      if(DBIDUtil.equal(ignore, di)) {
        continue;
      }
      double v = p[di.getOffset()] * norm; // Normalize
      if(!(v > 0)) {
        break;
      }
      scores.increment(di, FastMath.log1p(-v));
    }
  }

  /**
   * Transform scores
   * 
   * @param scores Scores to transform
   * @param ids DBIDs to process
   * @param logPerp log perplexity
   * @param phi Expected outlier rate
   * @return Minimum and maximum scores
   */
  public static DoubleMinMax transformScores(WritableDoubleDataStore scores, DBIDs ids, double logPerp, double phi) {
    DoubleMinMax minmax = new DoubleMinMax();
    double adj = (1 - phi) / phi;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      double or = FastMath.exp(-scores.doubleValue(it) * logPerp) * adj;
      double s = 1. / (1 + or);
      scores.putDouble(it, s);
      minmax.put(s);
    }
    return minmax;
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
     * Parameter to specify the number of neighbors
     */
    public static final OptionID KNN_ID = new OptionID("isos.k", "Number of neighbors to use. Should be about 3x the desired perplexity.");

    /**
     * Parameter for ID estimation.
     */
    public static final OptionID ESTIMATOR_ID = new OptionID("isos.estimator", "Estimator for intrinsic dimensionality.");

    /**
     * Number of neighbors
     */
    int k = 15;

    /**
     * Estimator of intrinsic dimensionality.
     */
    IntrinsicDimensionalityEstimator estimator = AggregatedHillEstimator.STATIC;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(KNN_ID, 100) //
          .addConstraint(new GreaterEqualConstraint(5));
      if(config.grab(kP)) {
        k = kP.intValue();
      }
      ObjectParameter<IntrinsicDimensionalityEstimator> estimatorP = new ObjectParameter<>(ESTIMATOR_ID, IntrinsicDimensionalityEstimator.class, AggregatedHillEstimator.class);
      if(config.grab(estimatorP)) {
        estimator = estimatorP.instantiateClass(config);
      }
    }

    @Override
    protected ISOS<O> makeInstance() {
      return new ISOS<O>(distanceFunction, k, estimator);
    }
  }
}
