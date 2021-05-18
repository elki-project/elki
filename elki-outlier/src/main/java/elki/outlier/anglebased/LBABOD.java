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
package elki.outlier.anglebased;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.similarity.SimilarityQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.Logging.Level;
import elki.logging.LoggingConfiguration;
import elki.logging.statistics.LongStatistic;
import elki.math.DoubleMinMax;
import elki.math.MeanVariance;
import elki.result.outlier.InvertedOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.similarity.Similarity;
import elki.similarity.kernel.KernelMatrix;
import elki.utilities.Alias;
import elki.utilities.datastructures.heap.DoubleMinHeap;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * LB-ABOD (lower-bound) version of
 * Angle-Based Outlier Detection / Angle-Based Outlier Factor.
 * <p>
 * Exact on the top k outliers, approximate on the remaining.
 * <p>
 * Outlier detection using variance analysis on angles, especially for high
 * dimensional data sets.
 * <p>
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Matthias Schubert, Arthur Zimek<br>
 * Angle-Based Outlier Detection in High-dimensional Data<br>
 * Proc. 14th ACM SIGKDD Int. Conf. Knowledge Discovery and Data Mining (KDD'08)
 *
 * @author Matthias Schubert (Original Code)
 * @author Erich Schubert (ELKIfication)
 * @since 0.6.0
 *
 * @param <V> Vector type
 */
@Title("LB-ABOD: Lower Bounded Angle-Based Outlier Detection")
@Description("Outlier detection using variance analysis on angles, especially for high dimensional data sets.")
@Reference(authors = "Hans-Peter Kriegel, Matthias Schubert, Arthur Zimek", //
    title = "Angle-Based Outlier Detection in High-dimensional Data", //
    booktitle = "Proc. 14th ACM SIGKDD Int. Conf. Knowledge Discovery and Data Mining (KDD'08)", //
    url = "https://doi.org/10.1145/1401890.1401946", //
    bibkey = "DBLP:conf/kdd/KriegelSZ08")
@Alias({ "lb-abod" })
public class LBABOD<V extends NumberVector> extends FastABOD<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(LBABOD.class);

  /**
   * Number of outliers to refine.
   */
  protected int l;

  /**
   * Actual constructor, with parameters. Fast mode (sampling).
   *
   * @param kernelFunction Kernel function to use
   * @param k k parameter
   * @param l Number of outliers to find exact
   */
  public LBABOD(Similarity<? super V> kernelFunction, int k, int l) {
    super(kernelFunction, k);
    this.l = l;
  }

  /**
   * Run LB-ABOD on the data set.
   *
   * @param relation Relation to process
   * @return Outlier detection result
   */
  @Override
  public OutlierResult run(Relation<V> relation) {
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    DBIDArrayIter pB = ids.iter(), pC = ids.iter();
    SimilarityQuery<V> sq = new QueryBuilder<>(relation, kernelFunction).similarityQuery();
    KernelMatrix kernelMatrix = new KernelMatrix(sq, relation, ids);

    // Output storage.
    WritableDoubleDataStore abodvalues = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmaxabod = new DoubleMinMax();
    double max = 0.;

    // Storage for squared distances (will be reused!)
    WritableDoubleDataStore sqDists = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
    // Nearest neighbor heap (will be reused!)
    KNNHeap nn = DBIDUtil.newHeap(k);

    // Priority queue for candidates
    ModifiableDoubleDBIDList candidates = DBIDUtil.newDistanceDBIDList(relation.size());
    // get Candidate Ranking
    for(DBIDIter pA = relation.iterDBIDs(); pA.valid(); pA.advance()) {
      // Compute nearest neighbors and distances.
      nn.clear();
      double simAA = kernelMatrix.getSimilarity(pA, pA);
      // Sum of 1./(|AB|) and 1./(|AB|^2); for computing R2.
      double sumid = 0., sumisqd = 0.;
      for(pB.seek(0); pB.valid(); pB.advance()) {
        if(DBIDUtil.equal(pB, pA)) {
          continue;
        }
        double simBB = kernelMatrix.getSimilarity(pB, pB);
        double simAB = kernelMatrix.getSimilarity(pA, pB);
        double sqdAB = simAA + simBB - simAB - simAB;
        sqDists.putDouble(pB, sqdAB);
        final double isqdAB = 1. / sqdAB;
        sumid += Math.sqrt(isqdAB);
        sumisqd += isqdAB;
        // Update heap
        nn.insert(sqdAB, pB);
      }

      // Compute FastABOD approximation, adjust for lower bound.
      // LB-ABOF is defined via a numerically unstable formula.
      // Variance as E(X^2)-E(X)^2 suffers from catastrophic cancellation!
      // TODO: ensure numerical precision!
      double nnsum = 0., nnsumsq = 0., nnsumisqd = 0.;
      KNNList nl = nn.toKNNList();
      DoubleDBIDListIter iB = nl.iter(), iC = nl.iter();
      for(; iB.valid(); iB.advance()) {
        double sqdAB = iB.doubleValue();
        double simAB = kernelMatrix.getSimilarity(pA, iB);
        if(!(sqdAB > 0.)) {
          continue;
        }
        for(iC.seek(iB.getOffset() + 1); iC.valid(); iC.advance()) {
          double sqdAC = iC.doubleValue();
          double simAC = kernelMatrix.getSimilarity(pA, iC);
          if(!(sqdAC > 0.)) {
            continue;
          }
          // Exploit bilinearity of scalar product:
          // <B-A, C-A> = <B, C-A> - <A,C-A>
          // = <B,C> - <B,A> - <A,C> + <A,A>
          double simBC = kernelMatrix.getSimilarity(iB, iC);
          double numerator = simBC - simAB - simAC + simAA;
          double sqweight = 1. / (sqdAB * sqdAC);
          double weight = Math.sqrt(sqweight);
          double val = numerator * sqweight;
          nnsum += val * weight;
          nnsumsq += val * val * weight;
          nnsumisqd += sqweight;
        }
      }
      // Remaining weight, term R2:
      double r2 = sumisqd * sumisqd - 2. * nnsumisqd;
      double tmp = (2. * nnsum + r2) / (sumid * sumid);
      double lbabof = 2. * nnsumsq / (sumid * sumid) - tmp * tmp;

      // Track maximum?
      if(lbabof > max) {
        max = lbabof;
      }
      abodvalues.putDouble(pA, lbabof);
      candidates.add(lbabof, pA);
    }
    minmaxabod.put(max); // Put maximum from approximate values.
    candidates.sort();

    // refine Candidates
    int refinements = 0;
    DoubleMinHeap topscores = new DoubleMinHeap(l);
    MeanVariance s = new MeanVariance();
    for(DoubleDBIDListIter pA = candidates.iter(); pA.valid(); pA.advance()) {
      // Stop refining
      if(topscores.size() >= k && pA.doubleValue() > topscores.peek()) {
        break;
      }
      final double abof = computeABOF(kernelMatrix, pA, pB, pC, s);
      // Store refined score:
      abodvalues.putDouble(pA, abof);
      minmaxabod.put(abof);
      // Update the heap tracking the top scores.
      if(topscores.size() < k) {
        topscores.add(abof);
      }
      else {
        if(topscores.peek() > abof) {
          topscores.replaceTopElement(abof);
        }
      }
      refinements += 1;
    }
    if(LOG.isStatistics()) {
      LoggingConfiguration.setVerbose(Level.VERYVERBOSE);
      LOG.statistics(new LongStatistic("lb-abod.refinements", refinements));
    }
    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Angle-based Outlier Detection", ids, abodvalues);
    OutlierScoreMeta scoreMeta = new InvertedOutlierScoreMeta(minmaxabod.getMin(), minmaxabod.getMax(), 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> extends FastABOD.Par<V> {
    /**
     * Parameter to specify the number of outliers to compute exactly.
     */
    public static final OptionID L_ID = new OptionID("abod.l", "Number of top outliers to compute.");

    /**
     * Number of outliers to find.
     */
    protected int l = 0;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(L_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> l = x);
    }

    @Override
    public LBABOD<V> make() {
      return new LBABOD<>(kernelFunction, k, l);
    }
  }
}
