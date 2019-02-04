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
package de.lmu.ifi.dbs.elki.algorithm.statistics;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.AlternativeTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.scores.AveragePrecisionEvaluation;
import de.lmu.ifi.dbs.elki.evaluation.scores.ROCEvaluation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
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
public class EvaluateRetrievalPerformance<O> extends AbstractDistanceBasedAlgorithm<O, EvaluateRetrievalPerformance.RetrievalPerformanceResult> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateRetrievalPerformance.class);

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
   * Prefix for statistics.
   */
  private final String PREFIX = this.getClass().getName();

  /**
   * K nearest neighbors to use for classification evaluation.
   */
  protected int maxk = 100;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param sampling Sampling rate
   * @param random Random sampling generator
   * @param includeSelf Include query object in evaluation
   * @param maxk Maximum k for kNN evaluation
   */
  public EvaluateRetrievalPerformance(DistanceFunction<? super O> distanceFunction, double sampling, RandomFactory random, boolean includeSelf, int maxk) {
    super(distanceFunction);
    this.sampling = sampling;
    this.random = random;
    this.includeSelf = includeSelf;
    this.maxk = maxk;
  }

  /**
   * Run the algorithm
   *
   * @param database Database to run on (for kNN queries)
   * @param relation Relation for distance computations
   * @param lrelation Relation for class label comparison
   * @return Vectors containing mean and standard deviation.
   */
  public RetrievalPerformanceResult run(Database database, Relation<O> relation, Relation<?> lrelation) {
    final DistanceQuery<O> distQuery = database.getDistanceQuery(relation, getDistanceFunction());
    final DBIDs ids = DBIDUtil.randomSample(relation.getDBIDs(), sampling, random);

    // For storing the positive neighbors.
    ModifiableDBIDs posn = DBIDUtil.newHashSet();
    // Distance storage.
    ModifiableDoubleDBIDList nlist = DBIDUtil.newDistanceDBIDList(relation.size());

    // For counting labels seen in kNN
    Object2IntOpenHashMap<Object> counters = new Object2IntOpenHashMap<>();

    // Statistics tracking
    double map = 0., mroc = 0.;
    double[] knnperf = new double[maxk];
    int samples = 0;

    FiniteProgress objloop = LOG.isVerbose() ? new FiniteProgress("Processing query objects", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      Object label = lrelation.get(iter);
      findMatches(posn, lrelation, label);
      if(posn.size() > 0) {
        computeDistances(nlist, iter, distQuery, relation);
        if(nlist.size() != relation.size() - (includeSelf ? 0 : 1)) {
          LOG.warning("Neighbor list does not have the desired size: " + nlist.size());
        }
        map += AveragePrecisionEvaluation.STATIC.evaluate(posn, nlist);
        mroc += ROCEvaluation.STATIC.evaluate(posn, nlist);
        KNNEvaluator.STATIC.evaluateKNN(knnperf, nlist, lrelation, counters, label);
        samples += 1;
      }
      LOG.incrementProcessed(objloop);
    }
    LOG.ensureCompleted(objloop);
    if(samples < 1) {
      throw new AbortException("No object matched - are labels parsed correctly?");
    }
    if(!(map >= 0) || !(mroc >= 0)) {
      throw new AbortException("NaN in MAP/ROC.");
    }

    map /= samples;
    mroc /= samples;
    LOG.statistics(new DoubleStatistic(PREFIX + ".map", map));
    LOG.statistics(new DoubleStatistic(PREFIX + ".rocauc", mroc));
    LOG.statistics(new DoubleStatistic(PREFIX + ".samples", samples));
    for(int k = 0; k < maxk; k++) {
      knnperf[k] = knnperf[k] / samples;
      LOG.statistics(new DoubleStatistic(PREFIX + ".knn-" + (k + 1), knnperf[k]));
    }

    return new RetrievalPerformanceResult(samples, map, mroc, knnperf);
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
    // Fallback to equality, e.g. on class labels
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
    posn.clear();
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

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    TypeInformation cls = new AlternativeTypeInformation(TypeUtil.CLASSLABEL, TypeUtil.LABELLIST);
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction(), cls);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
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
  public static class RetrievalPerformanceResult implements Result, TextWriteable {
    /**
     * Sample size
     */
    private int samplesize;

    /**
     * MAP value
     */
    private double map;

    /**
     * ROC AUC value
     */
    private double rocauc;

    /**
     * KNN performance
     */
    private double[] knnperf;

    /**
     * Constructor.
     *
     * @param samplesize Sample size
     * @param map MAP value
     * @param rocauc ROC AUC value
     * @param knnperf
     */
    public RetrievalPerformanceResult(int samplesize, double map, double rocauc, double[] knnperf) {
      super();
      this.map = map;
      this.rocauc = rocauc;
      this.samplesize = samplesize;
      this.knnperf = knnperf;
    }

    /**
     * @return the area under curve
     */
    public double getROCAUC() {
      return rocauc;
    }

    /**
     * @return the medium average precision
     */
    public double getMAP() {
      return map;
    }

    @Override
    public String getLongName() {
      return "Distance function retrieval evaluation.";
    }

    @Override
    public String getShortName() {
      return "distance-retrieval-evaluation";
    }

    @Override
    public void writeToText(TextWriterStream out, String label) {
      out.inlinePrintNoQuotes("MAP");
      out.inlinePrint(map);
      out.flush();
      out.inlinePrintNoQuotes("ROCAUC");
      out.inlinePrint(rocauc);
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
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
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
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter samplingP = new DoubleParameter(SAMPLING_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .setOptional(true);
      if(config.grab(samplingP)) {
        sampling = samplingP.getValue();
      }
      final RandomParameter rndP = new RandomParameter(SEED_ID);
      if(config.grab(rndP)) {
        seed = rndP.getValue();
      }
      final Flag includeP = new Flag(INCLUDESELF_ID);
      if(config.grab(includeP)) {
        includeSelf = includeP.isTrue();
      }
      IntParameter maxkP = new IntParameter(MAXK_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .setOptional(true);
      if(config.grab(maxkP)) {
        maxk = maxkP.intValue();
      }
    }

    @Override
    protected EvaluateRetrievalPerformance<O> makeInstance() {
      return new EvaluateRetrievalPerformance<>(distanceFunction, sampling, seed, includeSelf, maxk);
    }
  }
}
