/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.algorithm.statistics;

import elki.Algorithm;
import elki.data.LabelList;
import elki.data.type.AlternativeTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.evaluation.scores.AveragePrecisionEvaluation;
import elki.evaluation.scores.ROCEvaluation;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.result.textwriter.TextWriteable;
import elki.result.textwriter.TextWriterStream;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.*;
import elki.utilities.random.RandomFactory;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Evaluate a distance functions performance by computing the mean average
 * precision, ROC, and NN classification performance when ranking the objects by
 * distance.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - RetrievalPerformanceResult
 * @composed - - - KNNEvaluator
 *
 * @param <O> Object type
 */
public class EvaluateRetrievalPerformance<O> implements Algorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateRetrievalPerformance.class);

  /**
   * Prefix for statistics.
   */
  private final String PREFIX = this.getClass().getName();

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Relative number of object to use in sampling.
   */
  protected double sampling = 1.0;

  /**
   * Random sampling seed.
   */
  protected RandomFactory random = null;

  /**
   * Include query object in evaluation.
   */
  protected boolean includeSelf;

  /**
   * K nearest neighbors to use for classification evaluation.
   */
  protected int maxk = 100;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param sampling Sampling rate
   * @param random Random sampling generator
   * @param includeSelf Include query object in evaluation
   * @param maxk Maximum k for kNN evaluation
   */
  public EvaluateRetrievalPerformance(Distance<? super O> distance, double sampling, RandomFactory random, boolean includeSelf, int maxk) {
    super();
    this.distance = distance;
    this.sampling = sampling;
    this.random = random;
    this.includeSelf = includeSelf;
    this.maxk = maxk;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction(), //
        new AlternativeTypeInformation(TypeUtil.CLASSLABEL, TypeUtil.LABELLIST));
  }

  /**
   * Run the algorithm
   *
   * @param relation Relation for distance computations
   * @param lrelation Relation for class label comparison
   * @return Vectors containing mean and standard deviation.
   */
  public RetrievalPerformanceResult run(Relation<O> relation, Relation<?> lrelation) {
    DBIDs ids = DBIDUtil.randomSample(relation.getDBIDs(), sampling, random);
    DistanceQuery<O> distQuery = new QueryBuilder<>(relation, distance).distanceQuery();

    // For storing the positive neighbors.
    ModifiableDBIDs posn = DBIDUtil.newHashSet();
    // Distance storage.
    ModifiableDoubleDBIDList nlist = DBIDUtil.newDistanceDBIDList(relation.size());

    // For counting labels seen in kNN
    Object2IntOpenHashMap<Object> counters = new Object2IntOpenHashMap<>();

    // Statistics tracking
    double map = 0., mauroc = 0.;
    double[] knnperf = new double[maxk];
    int samples = 0;

    FiniteProgress objloop = LOG.isVerbose() ? new FiniteProgress("Processing query objects", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      Object label = lrelation.get(iter);
      findMatches(posn.clear(), lrelation, label);
      if(posn.size() > 0) {
        computeDistances(nlist, iter, distQuery, relation);
        if(nlist.size() != relation.size() - (includeSelf ? 0 : 1)) {
          LOG.warning("Neighbor list does not have the desired size: " + nlist.size());
        }
        map += AveragePrecisionEvaluation.STATIC.evaluate(posn, nlist);
        mauroc += ROCEvaluation.STATIC.evaluate(posn, nlist);
        KNNEvaluator.STATIC.evaluateKNN(knnperf, nlist, lrelation, counters, label);
        samples += 1;
      }
      LOG.incrementProcessed(objloop);
    }
    LOG.ensureCompleted(objloop);
    if(samples < 1) {
      throw new AbortException("No object matched - are labels parsed correctly?");
    }
    if(!(map >= 0) || !(mauroc >= 0)) {
      throw new AbortException("NaN in MAP/ROC.");
    }

    map /= samples;
    mauroc /= samples;
    LOG.statistics(new DoubleStatistic(PREFIX + ".map", map));
    LOG.statistics(new DoubleStatistic(PREFIX + ".auroc", mauroc));
    LOG.statistics(new DoubleStatistic(PREFIX + ".samples", samples));
    for(int k = 0; k < maxk; k++) {
      knnperf[k] = knnperf[k] / samples;
      LOG.statistics(new DoubleStatistic(PREFIX + ".knn-" + (k + 1), knnperf[k]));
    }

    return new RetrievalPerformanceResult(samples, map, mauroc, knnperf);
  }

  /**
   * Test whether two relation agree.
   *
   * @param ref Reference object
   * @param test Test object
   * @return {@code true} if the objects match
   */
  protected static boolean match(Object ref, Object test) {
    if(ref == null) {
      return false;
    }
    // Cheap and fast, may hold for class labels!
    if(ref == test) {
      return true;
    }
    if(ref instanceof LabelList && test instanceof LabelList) {
      final LabelList lref = (LabelList) ref;
      final LabelList ltest = (LabelList) test;
      final int s1 = lref.size(), s2 = ltest.size();
      if(s1 == 0 || s2 == 0) {
        return false;
      }
      for(int i = 0; i < s1; i++) {
        String l1 = lref.get(i);
        if(l1 == null) {
          continue;
        }
        for(int j = 0; j < s2; j++) {
          if(l1.equals(ltest.get(j))) {
            return true;
          }
        }
      }
    }
    // Fallback to equality, e.g., on class labels
    return ref.equals(test);
  }

  /**
   * Find all matching objects.
   *
   * @param posn Output set.
   * @param lrelation Label relation
   * @param label Query object label
   */
  private void findMatches(ModifiableDBIDs posn, Relation<?> lrelation, Object label) {
    for(DBIDIter ri = lrelation.iterDBIDs(); ri.valid(); ri.advance()) {
      if(match(label, lrelation.get(ri))) {
        posn.add(ri);
      }
    }
  }

  /**
   * Compute the distances to the neighbor objects.
   *
   * @param nlist Neighbor list (output)
   * @param query Query object
   * @param distQuery Distance function
   * @param relation Data relation
   */
  private void computeDistances(ModifiableDoubleDBIDList nlist, DBIDIter query, final DistanceQuery<O> distQuery, Relation<O> relation) {
    nlist.clear();
    O qo = relation.get(query);
    for(DBIDIter ri = relation.iterDBIDs(); ri.valid(); ri.advance()) {
      if(!includeSelf && DBIDUtil.equal(ri, query)) {
        continue;
      }
      double dist = distQuery.distance(qo, ri);
      if(dist != dist) { /* NaN */
        dist = Double.POSITIVE_INFINITY;
      }
      nlist.add(dist, ri);
    }
    nlist.sort();
  }

  /**
   * Evaluate kNN retrieval performance.
   *
   * @author Erich Schubert
   */
  public static class KNNEvaluator {
    /**
     * Static instance.
     */
    public static final KNNEvaluator STATIC = new KNNEvaluator();

    /**
     * Evaluate by simulating kNN classification for k=1...maxk
     *
     * @param knnperf Output data storage
     * @param nlist Neighbor list
     * @param lrelation Label relation
     * @param counters (Reused) map for counting the class occurrences.
     * @param label Label(s) of query object
     */
    public void evaluateKNN(double[] knnperf, ModifiableDoubleDBIDList nlist, Relation<?> lrelation, Object2IntOpenHashMap<Object> counters, Object label) {
      final int maxk = knnperf.length;
      int k = 1, prevk = 0, max = 0;
      counters.clear();
      DoubleDBIDListIter iter = nlist.iter();
      while(iter.valid() && prevk < maxk) {
        // Note: we already skipped the query object in {@link
        // #computeDistances}
        double prev = iter.doubleValue();
        Object l = lrelation.get(iter);
        max = Math.max(max, countkNN(counters, l));
        iter.advance();
        ++k;
        // End of ties.
        if(!iter.valid() || iter.doubleValue() > prev) {
          int pties = 0, ties = 0;
          for(ObjectIterator<Object2IntMap.Entry<Object>> cit = counters.object2IntEntrySet().fastIterator(); cit.hasNext();) {
            Object2IntMap.Entry<Object> entry = cit.next();
            if(entry.getIntValue() < max) {
              continue;
            }
            ties++;
            final Object key = entry.getKey();
            if(key == null) {
              continue;
            }
            if(key.equals(label)) {
              pties++;
            }
            else if(label instanceof LabelList) {
              LabelList ll = (LabelList) label;
              for(int i = 0, e = ll.size(); i < e; i++) {
                if(key.equals(ll.get(i))) {
                  pties++;
                  break;
                }
              }
            }
          }
          while(prevk < k && prevk < maxk) {
            knnperf[prevk++] += pties / (double) ties;
          }
        }
      }
    }

    /**
     * Counting helper for kNN classification.
     *
     * @param counters Counter storage
     * @param l Object labels
     * @return Maximum count
     */
    public int countkNN(Object2IntOpenHashMap<Object> counters, Object l) {
      // Count each label, return maximum.
      if(l instanceof LabelList) {
        LabelList ll = (LabelList) l;
        int m = 0;
        for(int i = 0, e = ll.size(); i < e; i++) {
          m = Math.max(m, counters.addTo(ll.get(i), 1));
        }
        return m;
      }
      return counters.addTo(l, 1);
    }
  }

  /**
   * Result object for MAP scores.
   *
   * @author Erich Schubert
   */
  public static class RetrievalPerformanceResult implements TextWriteable {
    /**
     * Sample size
     */
    private int samplesize;

    /**
     * MAP value
     */
    private double map;

    /**
     * AUROC value
     */
    private double auroc;

    /**
     * KNN performance
     */
    private double[] knnperf;

    /**
     * Constructor.
     *
     * @param samplesize Sample size
     * @param map MAP value
     * @param auroc AUROC value
     * @param knnperf
     */
    public RetrievalPerformanceResult(int samplesize, double map, double auroc, double[] knnperf) {
      super();
      this.map = map;
      this.auroc = auroc;
      this.samplesize = samplesize;
      this.knnperf = knnperf;
    }

    /**
     * @return the area under curve
     */
    public double getAUROC() {
      return auroc;
    }

    /**
     * @return the medium average precision
     */
    public double getMAP() {
      return map;
    }

    // @Override
    public String getLongName() {
      return "Distance function retrieval evaluation.";
    }

    // @Override
    public String getShortName() {
      return "distance-retrieval-evaluation";
    }

    @Override
    public void writeToText(TextWriterStream out, String label) {
      out.inlinePrintNoQuotes("MAP");
      out.inlinePrint(map);
      out.flush();
      out.inlinePrintNoQuotes("AUROC");
      out.inlinePrint(auroc);
      out.flush();
      out.inlinePrintNoQuotes("Samplesize");
      out.inlinePrint(samplesize);
      out.flush();
      for(int i = 0; i < knnperf.length; i++) {
        out.inlinePrintNoQuotes("knn-" + (i + 1));
        out.inlinePrint(knnperf[i]);
        out.flush();
      }
    }
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
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter to enable sampling.
     */
    public static final OptionID SAMPLING_ID = new OptionID("map.sampling", "Relative amount of object to sample.");

    /**
     * Parameter to control the sampling random seed.
     */
    public static final OptionID SEED_ID = new OptionID("map.sampling-seed", "Random seed for deterministic sampling.");

    /**
     * Parameter to include the query object.
     */
    public static final OptionID INCLUDESELF_ID = new OptionID("map.includeself", "Include the query object in the evaluation.");

    /**
     * Parameter for maximum value of k.
     */
    public static final OptionID MAXK_ID = new OptionID("map.maxk", "Maximum value of k for kNN evaluation.");

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * Relative amount of data to sample.
     */
    protected double sampling = 1.0;

    /**
     * Random sampling seed.
     */
    protected RandomFactory seed = null;

    /**
     * Include query object in evaluation.
     */
    protected boolean includeSelf;

    /**
     * Maximum k for evaluation.
     */
    protected int maxk = 0;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new DoubleParameter(SAMPLING_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .setOptional(true) //
          .grab(config, x -> sampling = x);
      new RandomParameter(SEED_ID).grab(config, x -> seed = x);
      new Flag(INCLUDESELF_ID).grab(config, x -> includeSelf = x);
      new IntParameter(MAXK_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .setOptional(true) //
          .grab(config, x -> maxk = x);
    }

    @Override
    public EvaluateRetrievalPerformance<O> make() {
      return new EvaluateRetrievalPerformance<>(distance, sampling, seed, includeSelf, maxk);
    }
  }
}
