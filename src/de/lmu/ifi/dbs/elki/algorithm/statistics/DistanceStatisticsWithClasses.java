package de.lmu.ifi.dbs.elki.algorithm.statistics;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.histograms.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.math.histograms.FlexiHistogram;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.OnlyOneIsAllowedToBeSetGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.FCPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Algorithm to gather statistics over the distance distribution in the data
 * set.
 * 
 * @author Erich Schubert
 * @param <O> Object type
 * @param <D> Distance type
 */
// TODO: optimize for double distances.
@Title("Distance Histogram")
@Description("Computes a histogram over the distances occurring in the data set.")
public class DistanceStatisticsWithClasses<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, CollectionResult<DoubleVector>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(DistanceStatisticsWithClasses.class);

  /**
   * Flag to compute exact value range for binning.
   */
  public static final OptionID EXACT_ID = OptionID.getOrCreateOptionID("diststat.exact", "In a first pass, compute the exact minimum and maximum, at the cost of O(2*n*n) instead of O(n*n). The number of resulting bins is guaranteed to be as requested.");

  /**
   * Flag to enable sampling
   */
  public static final OptionID SAMPLING_ID = OptionID.getOrCreateOptionID("diststat.sampling", "Enable sampling of O(n) size to determine the minimum and maximum distances approximately. The resulting number of bins can be larger than the given n.");

  /**
   * Option to configure the number of bins to use.
   */
  public static final OptionID HISTOGRAM_BINS_ID = OptionID.getOrCreateOptionID("diststat.bins", "Number of bins to use in the histogram. By default, it is only guaranteed to be within 1*n and 2*n of the given number.");

  /**
   * Number of bins to use in sampling.
   */
  private int numbin;

  /**
   * Sampling
   */
  private boolean sampling = false;

  /**
   * Sampling
   */
  private boolean exact = false;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function to use
   * @param numbins Number of bins
   * @param exact Exactness flag
   * @param sampling Sampling flag
   */
  public DistanceStatisticsWithClasses(DistanceFunction<? super O, D> distanceFunction, int numbins, boolean exact, boolean sampling) {
    super(distanceFunction);
    this.numbin = numbins;
    this.exact = exact;
    this.sampling = sampling;
  }

  /**
   * Iterates over all points in the database.
   */
  @Override
  public HistogramResult<DoubleVector> run(Database database) throws IllegalStateException {
    final Relation<O> relation = database.getRelation(getInputTypeRestriction()[0]);
    final DistanceQuery<O, D> distFunc = database.getDistanceQuery(relation, getDistanceFunction());

    final StepProgress stepprog = logger.isVerbose() ? new StepProgress("Distance statistics", 2) : null;

    // determine binning ranges.
    DoubleMinMax gminmax = new DoubleMinMax();

    // Cluster by labels
    Collection<Cluster<Model>> split = (new ByLabelClustering()).run(database).getAllClusters();

    // global in-cluster min/max
    DoubleMinMax giminmax = new DoubleMinMax();
    // global other-cluster min/max
    DoubleMinMax gominmax = new DoubleMinMax();
    // in-cluster distances
    MeanVariance mimin = new MeanVariance();
    MeanVariance mimax = new MeanVariance();
    MeanVariance midif = new MeanVariance();
    // other-cluster distances
    MeanVariance momin = new MeanVariance();
    MeanVariance momax = new MeanVariance();
    MeanVariance modif = new MeanVariance();
    // Histogram
    final AggregatingHistogram<Pair<Long, Long>, Pair<Long, Long>> histogram;
    if(stepprog != null) {
      stepprog.beginStep(1, "Prepare histogram.", logger);
    }
    if(exact) {
      gminmax = exactMinMax(relation, distFunc);
      histogram = AggregatingHistogram.LongSumLongSumHistogram(numbin, gminmax.getMin(), gminmax.getMax());
    }
    else if(sampling) {
      gminmax = sampleMinMax(relation, distFunc);
      histogram = AggregatingHistogram.LongSumLongSumHistogram(numbin, gminmax.getMin(), gminmax.getMax());
    }
    else {
      histogram = FlexiHistogram.LongSumLongSumHistogram(numbin);
    }

    if(stepprog != null) {
      stepprog.beginStep(2, "Build histogram.", logger);
    }
    final FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Distance computations", relation.size(), logger) : null;
    // iterate per cluster
    final Pair<Long, Long> incFirst = new Pair<Long, Long>(1L, 0L);
    final Pair<Long, Long> incSecond = new Pair<Long, Long>(0L, 1L);
    for(Cluster<?> c1 : split) {
      for(DBID id1 : c1.getIDs()) {
        // in-cluster distances
        DoubleMinMax iminmax = new DoubleMinMax();
        for(DBID id2 : c1.getIDs()) {
          // skip the point itself.
          if(id1 == id2) {
            continue;
          }
          double d = distFunc.distance(id1, id2).doubleValue();

          histogram.aggregate(d, incFirst);

          iminmax.put(d);
        }
        // aggregate
        mimin.put(iminmax.getMin());
        mimax.put(iminmax.getMax());
        midif.put(iminmax.getDiff());
        // min/max
        giminmax.put(iminmax.getMin());
        giminmax.put(iminmax.getMax());

        // other-cluster distances
        DoubleMinMax ominmax = new DoubleMinMax();
        for(Cluster<?> c2 : split) {
          if(c2 == c1) {
            continue;
          }
          for(DBID id2 : c2.getIDs()) {
            // skip the point itself (shouldn't happen though)
            if(id1 == id2) {
              continue;
            }
            double d = distFunc.distance(id1, id2).doubleValue();

            histogram.aggregate(d, incSecond);

            ominmax.put(d);
          }
        }
        // aggregate
        momin.put(ominmax.getMin());
        momax.put(ominmax.getMax());
        modif.put(ominmax.getDiff());
        // min/max
        gominmax.put(ominmax.getMin());
        gominmax.put(ominmax.getMax());
        if(progress != null) {
          progress.incrementProcessed(logger);
        }
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }
    // Update values (only needed for sampling case).
    gminmax.setFirst(Math.min(giminmax.getMin(), gominmax.getMin()));
    gminmax.setSecond(Math.max(giminmax.getMax(), gominmax.getMax()));

    if(stepprog != null) {
      stepprog.setCompleted(logger);
    }

    // count the number of samples we have in the data
    long inum = 0;
    long onum = 0;
    for(Pair<Double, Pair<Long, Long>> ppair : histogram) {
      inum += ppair.getSecond().getFirst();
      onum += ppair.getSecond().getSecond();
    }
    long bnum = inum + onum;
    // Note: when full sampling is added, this assertion won't hold anymore.
    assert (bnum == relation.size() * (relation.size() - 1));

    Collection<DoubleVector> binstat = new ArrayList<DoubleVector>(numbin);
    for(Pair<Double, Pair<Long, Long>> ppair : histogram) {
      final double icof = (inum == 0) ? 0 : ((double) ppair.getSecond().getFirst()) / inum / histogram.getBinsize();
      final double icaf = ((double) ppair.getSecond().getFirst()) / bnum / histogram.getBinsize();
      final double ocof = (onum == 0) ? 0 : ((double) ppair.getSecond().getSecond()) / onum / histogram.getBinsize();
      final double ocaf = ((double) ppair.getSecond().getSecond()) / bnum / histogram.getBinsize();
      DoubleVector row = new DoubleVector(new double[] { ppair.getFirst(), icof, icaf, ocof, ocaf });
      binstat.add(row);
    }
    HistogramResult<DoubleVector> result = new HistogramResult<DoubleVector>("Distance Histogram", "distance-histogram", binstat);

    result.addHeader("Absolute minimum distance (abs): " + gminmax.getMin());
    result.addHeader("Absolute maximum distance (abs): " + gminmax.getMax());
    result.addHeader("In-Cluster minimum distance (abs, avg, stddev): " + giminmax.getMin() + " " + mimin.getMean() + " " + mimin.getSampleStddev());
    result.addHeader("In-Cluster maximum distance (abs, avg, stddev): " + giminmax.getMax() + " " + mimax.getMean() + " " + mimax.getSampleStddev());
    result.addHeader("Other-Cluster minimum distance (abs, avg, stddev): " + gominmax.getMin() + " " + momin.getMean() + " " + momin.getSampleStddev());
    result.addHeader("Other-Cluster maximum distance (abs, avg, stddev): " + gominmax.getMax() + " " + momax.getMean() + " " + momax.getSampleStddev());
    result.addHeader("Column description: bin center, in-cluster only frequency, in-cluster all frequency, other-cluster only frequency, other cluster all frequency");
    result.addHeader("In-cluster value count: " + inum + " other cluster value count: " + onum);
    return result;
  }

  private DoubleMinMax sampleMinMax(Relation<O> database, DistanceQuery<O, D> distFunc) {
    int size = database.size();
    Random rnd = new Random();
    // estimate minimum and maximum.
    int k = (int) Math.max(25, Math.pow(database.size(), 0.2));
    TreeSet<FCPair<Double, DBID>> minhotset = new TreeSet<FCPair<Double, DBID>>();
    TreeSet<FCPair<Double, DBID>> maxhotset = new TreeSet<FCPair<Double, DBID>>(Collections.reverseOrder());

    int randomsize = (int) Math.max(25, Math.pow(database.size(), 0.2));
    double rprob = ((double) randomsize) / size;
    ArrayModifiableDBIDs randomset = DBIDUtil.newArray(randomsize);

    Iterator<DBID> iter = database.iterDBIDs();
    if(!iter.hasNext()) {
      throw new IllegalStateException(ExceptionMessages.DATABASE_EMPTY);
    }
    DBID firstid = iter.next();
    minhotset.add(new FCPair<Double, DBID>(Double.MAX_VALUE, firstid));
    maxhotset.add(new FCPair<Double, DBID>(Double.MIN_VALUE, firstid));
    while(iter.hasNext()) {
      DBID id1 = iter.next();
      // generate candidates for min distance.
      ArrayList<FCPair<Double, DBID>> np = new ArrayList<FCPair<Double, DBID>>(k * 2 + randomsize * 2);
      for(FCPair<Double, DBID> pair : minhotset) {
        DBID id2 = pair.getSecond();
        // skip the object itself
        if(id1.compareTo(id2) == 0) {
          continue;
        }
        double d = distFunc.distance(id1, id2).doubleValue();
        np.add(new FCPair<Double, DBID>(d, id1));
        np.add(new FCPair<Double, DBID>(d, id2));
      }
      for(DBID id2 : randomset) {
        double d = distFunc.distance(id1, id2).doubleValue();
        np.add(new FCPair<Double, DBID>(d, id1));
        np.add(new FCPair<Double, DBID>(d, id2));
      }
      minhotset.addAll(np);
      shrinkHeap(minhotset, k);

      // generate candidates for max distance.
      ArrayList<FCPair<Double, DBID>> np2 = new ArrayList<FCPair<Double, DBID>>(k * 2 + randomsize * 2);
      for(FCPair<Double, DBID> pair : minhotset) {
        DBID id2 = pair.getSecond();
        // skip the object itself
        if(id1.compareTo(id2) == 0) {
          continue;
        }
        double d = distFunc.distance(id1, id2).doubleValue();
        np2.add(new FCPair<Double, DBID>(d, id1));
        np2.add(new FCPair<Double, DBID>(d, id2));
      }
      for(DBID id2 : randomset) {
        double d = distFunc.distance(id1, id2).doubleValue();
        np.add(new FCPair<Double, DBID>(d, id1));
        np.add(new FCPair<Double, DBID>(d, id2));
      }
      maxhotset.addAll(np2);
      shrinkHeap(maxhotset, k);

      // update random set
      if(randomset.size() < randomsize) {
        randomset.add(id1);
      }
      else if(rnd.nextDouble() < rprob) {
        randomset.set((int) Math.floor(rnd.nextDouble() * randomsize), id1);
      }
    }
    return new DoubleMinMax(minhotset.first().getFirst(), maxhotset.first().getFirst());
  }

  private DoubleMinMax exactMinMax(Relation<O> database, DistanceQuery<O, D> distFunc) {
    DoubleMinMax minmax = new DoubleMinMax();
    // find exact minimum and maximum first.
    for(DBID id1 : database.iterDBIDs()) {
      for(DBID id2 : database.iterDBIDs()) {
        // skip the point itself.
        if(id1.compareTo(id2) == 0) {
          continue;
        }
        double d = distFunc.distance(id1, id2).doubleValue();
        minmax.put(d);
      }
    }
    return minmax;
  }

  private void shrinkHeap(TreeSet<FCPair<Double, DBID>> hotset, int k) {
    // drop duplicates
    ModifiableDBIDs seenids = DBIDUtil.newHashSet(2 * k);
    int cnt = 0;
    for(Iterator<FCPair<Double, DBID>> i = hotset.iterator(); i.hasNext();) {
      FCPair<Double, DBID> p = i.next();
      if(cnt > k || seenids.contains(p.getSecond())) {
        i.remove();
      }
      else {
        seenids.add(p.getSecond());
        cnt++;
      }
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    /**
     * Number of bins to use in sampling.
     */
    private int numbin = 20;

    /**
     * Sampling
     */
    private boolean sampling = false;

    /**
     * Sampling
     */
    private boolean exact = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter numbinP = new IntParameter(HISTOGRAM_BINS_ID, new GreaterEqualConstraint(2), 20);
      if(config.grab(numbinP)) {
        numbin = numbinP.getValue();
      }

      final Flag EXACT_FLAG = new Flag(EXACT_ID);
      if(config.grab(EXACT_FLAG)) {
        exact = EXACT_FLAG.getValue();
      }

      final Flag SAMPLING_FLAG = new Flag(SAMPLING_ID);
      if(config.grab(SAMPLING_FLAG)) {
        sampling = SAMPLING_FLAG.getValue();
      }

      ArrayList<Parameter<?, ?>> exclusive = new ArrayList<Parameter<?, ?>>();
      exclusive.add(EXACT_FLAG);
      exclusive.add(SAMPLING_FLAG);
      config.checkConstraint(new OnlyOneIsAllowedToBeSetGlobalConstraint(exclusive));
    }

    @Override
    protected DistanceStatisticsWithClasses<O, D> makeInstance() {
      return new DistanceStatisticsWithClasses<O, D>(distanceFunction, numbin, exact, sampling);
    }
  }
}