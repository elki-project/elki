package de.lmu.ifi.dbs.elki.algorithm.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelMatrix;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.Logging.Level;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparableMaxHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ComparableMinHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleMinHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.ObjectHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Angle-Based Outlier Detection / Angle-Based Outlier Factor.
 * 
 * LB-ABOD (lower-bound) version. Exact on the top k outliers, approximate on
 * the remaining.
 * 
 * Outlier detection using variance analysis on angles, especially for high
 * dimensional data sets.
 * 
 * H.-P. Kriegel, M. Schubert, and A. Zimek: Angle-Based Outlier Detection in
 * High-dimensional Data. In: Proc. 14th ACM SIGKDD Int. Conf. on Knowledge
 * Discovery and Data Mining (KDD '08), Las Vegas, NV, 2008.
 * 
 * @author Matthias Schubert (Original Code)
 * @author Erich Schubert (ELKIfication)
 * 
 * @param <V> Vector type
 */
@Title("LB-ABOD: Lower Bounded Angle-Based Outlier Detection")
@Description("Outlier detection using variance analysis on angles, especially for high dimensional data sets.")
@Reference(authors = "H.-P. Kriegel, M. Schubert, and A. Zimek", title = "Angle-Based Outlier Detection in High-dimensional Data", booktitle = "Proc. 14th ACM SIGKDD Int. Conf. on Knowledge Discovery and Data Mining (KDD '08), Las Vegas, NV, 2008", url = "http://dx.doi.org/10.1145/1401890.1401946")
public class LBABOD<V extends NumberVector<?>> extends FastABOD<V> {
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
  public LBABOD(SimilarityFunction<? super V, DoubleDistance> kernelFunction, int k, int l) {
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
  public OutlierResult run(Database db, Relation<V> relation) {
    DBIDs ids = relation.getDBIDs();
    SimilarityQuery<V, DoubleDistance> sq = relation.getDatabase().getSimilarityQuery(relation, kernelFunction);
    KernelMatrix kernelMatrix = new KernelMatrix(sq, relation, ids);

    // Output storage.
    WritableDoubleDataStore abodvalues = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmaxabod = new DoubleMinMax();
    double max = 0.;

    // Storage for squared distances (will be reused!)
    WritableDoubleDataStore sqDists = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
    // Nearest neighbor heap (will be reused!)
    ComparableMaxHeap<DoubleDBIDPair> nn = new ComparableMaxHeap<>(k);

    // Priority queue for candidates
    ComparableMinHeap<DoubleDBIDPair> candidates = new ComparableMinHeap<>(relation.size());
    // get Candidate Ranking
    for(DBIDIter pA = relation.iterDBIDs(); pA.valid(); pA.advance()) {
      // Compute nearest neighbors and distances.
      nn.clear();
      double simAA = kernelMatrix.getSimilarity(pA, pA);
      // Sum of 1./(|AB|) and 1./(|AB|^2); for computing R2.
      double sumid = 0., sumisqd = 0.;
      for(DBIDIter nB = relation.iterDBIDs(); nB.valid(); nB.advance()) {
        if(DBIDUtil.equal(nB, pA)) {
          continue;
        }
        double simBB = kernelMatrix.getSimilarity(nB, nB);
        double simAB = kernelMatrix.getSimilarity(pA, nB);
        double sqdAB = simAA + simBB - simAB - simAB;
        sqDists.putDouble(nB, sqdAB);
        if(!(sqdAB > 0.)) {
          continue;
        }
        sumid += 1. / Math.sqrt(sqdAB);
        sumisqd += 1. / sqdAB;
        // Update heap
        if(nn.size() < k) {
          nn.add(DBIDUtil.newPair(sqdAB, nB));
        }
        else if(sqdAB < nn.peek().doubleValue()) {
          nn.replaceTopElement(DBIDUtil.newPair(sqdAB, nB));
        }
      }

      // Compute FastABOD approximation, adjust for lower bound.
      // LB-ABOF is defined via a numerically unstable formula.
      // Variance as E(X^2)-E(X)^2 suffers from catastrophic cancellation!
      // TODO: ensure numerical precision!
      double nnsum = 0., nnsumsq = 0., nnsumisqd = 0.;
      for(ObjectHeap.UnsortedIter<DoubleDBIDPair> iB = nn.unsortedIter(); iB.valid(); iB.advance()) {
        DoubleDBIDPair nB = iB.get();
        double sqdAB = nB.doubleValue();
        double simAB = kernelMatrix.getSimilarity(pA, nB);
        if(!(sqdAB > 0.)) {
          continue;
        }
        for(ObjectHeap.UnsortedIter<DoubleDBIDPair> iC = nn.unsortedIter(); iC.valid(); iC.advance()) {
          DoubleDBIDPair nC = iC.get();
          if(DBIDUtil.compare(nC, nB) < 0) {
            continue;
          }
          double sqdAC = nC.doubleValue();
          double simAC = kernelMatrix.getSimilarity(pA, nC);
          if(!(sqdAC > 0.)) {
            continue;
          }
          // Exploit bilinearity of scalar product:
          // <B-A, C-A> = <B, C-A> - <A,C-A>
          // = <B,C> - <B,A> - <A,C> + <A,A>
          double simBC = kernelMatrix.getSimilarity(nB, nC);
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
      candidates.add(DBIDUtil.newPair(lbabof, pA));
    }
    minmaxabod.put(max); // Put maximum from approximate values.

    // refine Candidates
    int refinements = 0;
    DoubleMinHeap topscores = new DoubleMinHeap(l);
    MeanVariance s = new MeanVariance();
    while(!candidates.isEmpty()) {
      // Stop refining
      if(topscores.size() >= k && candidates.peek().doubleValue() > topscores.peek()) {
        break;
      }
      DoubleDBIDPair pA = candidates.poll();
      final double abof = computeABOF(relation, kernelMatrix, pA, s);
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
    Relation<Double> scoreResult = new MaterializedRelation<>("Angle-based Outlier Detection", "abod-outlier", TypeUtil.DOUBLE, abodvalues, ids);
    OutlierScoreMeta scoreMeta = new InvertedOutlierScoreMeta(minmaxabod.getMin(), minmaxabod.getMax(), 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
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
  public static class Parameterizer<V extends NumberVector<?>> extends FastABOD.Parameterizer<V> {
    /**
     * Parameter to specify the number of outliers to compute exactly.
     */
    public static final OptionID L_ID = new OptionID("abod.l", "Number of top outliers to compute.");

    /**
     * Number of outliers to find.
     */
    protected int l = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter lP = new IntParameter(L_ID);
      lP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(lP)) {
        l = lP.getValue();
      }
    }

    @Override
    protected LBABOD<V> makeInstance() {
      return new LBABOD<>(kernelFunction, k, l);
    }
  }
}
