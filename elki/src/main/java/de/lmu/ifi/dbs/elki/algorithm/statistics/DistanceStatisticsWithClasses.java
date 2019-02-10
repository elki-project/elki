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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelOrAllInOneClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.histogram.AbstractObjDynamicHistogram;
import de.lmu.ifi.dbs.elki.utilities.datastructures.histogram.ObjHistogram;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.EmptyDataException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import net.jafama.FastMath;

/**
 * Algorithm to gather statistics over the distance distribution in the data
 * set.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @param <O> Object type
 */
@Title("Distance Histogram")
@Description("Computes a histogram over the distances occurring in the data set.")
public class DistanceStatisticsWithClasses<O> extends AbstractDistanceBasedAlgorithm<O, CollectionResult<double[]>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DistanceStatisticsWithClasses.class);

  /**
   * Number of bins to use in sampling.
   */
  protected int numbin;

  /**
   * Sampling flag.
   */
  protected boolean sampling = false;

  /**
   * Compute exactly (slower).
   */
  protected boolean exact = false;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use
   * @param numbins Number of bins
   * @param exact Exactness flag
   * @param sampling Sampling flag
   */
  public DistanceStatisticsWithClasses(DistanceFunction<? super O> distanceFunction, int numbins, boolean exact, boolean sampling) {
    super(distanceFunction);
    this.numbin = numbins;
    this.exact = exact;
    this.sampling = sampling;
  }

  @Override
  public HistogramResult run(Database database) {
    final Relation<O> relation = database.getRelation(getInputTypeRestriction()[0]);
    final DistanceQuery<O> distFunc = database.getDistanceQuery(relation, getDistanceFunction());

    final StepProgress stepprog = LOG.isVerbose() ? new StepProgress("Distance statistics", 2) : null;

    // determine binning ranges.
    DoubleMinMax gminmax = new DoubleMinMax();

    // Cluster by labels
    Collection<Cluster<Model>> split = (new ByLabelOrAllInOneClustering()).run(database).getAllClusters();

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
    final ObjHistogram<long[]> histogram;
    LOG.beginStep(stepprog, 1, "Prepare histogram.");
    if(exact) {
      gminmax = exactMinMax(relation, distFunc);
    }
    else if(sampling) {
      gminmax = sampleMinMax(relation, distFunc);
    }
    if(gminmax.isValid()) {
      histogram = new ObjHistogram<long[]>(numbin, gminmax.getMin(), gminmax.getMax(), () -> {
        return new long[2];
      });
    }
    else {
      histogram = new AbstractObjDynamicHistogram<long[]>(numbin) {
        @Override
        protected long[] downsample(Object[] data, int start, int end, int size) {
          long[] ret = new long[2];
          for(int i = start; i < end; i++) {
            long[] existing = (long[]) data[i];
            if(existing != null) {
              for(int c = 0; c < 2; c++) {
                ret[c] += existing[c];
              }
            }
          }
          return ret;
        }

        @Override
        protected long[] aggregate(long[] first, long[] second) {
          for(int c = 0; c < 2; c++) {
            first[c] += second[c];
          }
          return first;
        }

        @Override
        protected long[] cloneForCache(long[] data) {
          return data.clone();
        }

        @Override
        protected long[] makeObject() {
          return new long[2];
        }
      };
    }

    LOG.beginStep(stepprog, 2, "Build histogram.");
    final FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Distance computations", relation.size(), LOG) : null;
    for(Cluster<?> c1 : split) {
      for(DBIDIter id1 = c1.getIDs().iter(); id1.valid(); id1.advance()) {
        // in-cluster distances
        DoubleMinMax iminmax = new DoubleMinMax();
        for(DBIDIter iter2 = c1.getIDs().iter(); iter2.valid(); iter2.advance()) {
          // skip the point itself.
          if(DBIDUtil.equal(id1, iter2)) {
            continue;
          }
          double d = distFunc.distance(id1, iter2);
          histogram.get(d)[0]++;
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
          for(DBIDIter iter2 = c2.getIDs().iter(); iter2.valid(); iter2.advance()) {
            // skip the point itself (shouldn't happen though)
            if(DBIDUtil.equal(id1, iter2)) {
              continue;
            }
            double d = distFunc.distance(id1, iter2);
            histogram.get(d)[1]++;
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
        LOG.incrementProcessed(progress);
      }
    }
    LOG.ensureCompleted(progress);
    // Update values (only needed for sampling case).
    gminmax.put(gominmax);

    LOG.setCompleted(stepprog);

    // count the number of samples we have in the data
    long inum = 0;
    long onum = 0;
    for(ObjHistogram<long[]>.Iter iter = histogram.iter(); iter.valid(); iter.advance()) {
      inum += iter.getValue()[0];
      onum += iter.getValue()[1];
    }
    long bnum = inum + onum;

    Collection<double[]> binstat = new ArrayList<>(numbin);
    for(ObjHistogram<long[]>.Iter iter = histogram.iter(); iter.valid(); iter.advance()) {
      final long[] value = iter.getValue();
      final double icof = (inum == 0) ? 0 : ((double) value[0]) / inum / histogram.getBinsize();
      final double icaf = ((double) value[0]) / bnum / histogram.getBinsize();
      final double ocof = (onum == 0) ? 0 : ((double) value[1]) / onum / histogram.getBinsize();
      final double ocaf = ((double) value[1]) / bnum / histogram.getBinsize();
      binstat.add(new double[] { iter.getCenter(), icof, icaf, ocof, ocaf });
    }
    HistogramResult result = new HistogramResult("Distance Histogram", "distance-histogram", binstat);

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

  /**
   * Estimate minimum and maximum via sampling.
   *
   * @param relation Relation to process
   * @param distFunc Distance function to use
   * @return Minimum and maximum
   */
  private DoubleMinMax sampleMinMax(Relation<O> relation, DistanceQuery<O> distFunc) {
    int size = relation.size();
    Random rnd = new Random();
    // estimate minimum and maximum.
    int k = (int) Math.max(25, FastMath.pow(relation.size(), 0.2));
    TreeSet<DoubleDBIDPair> minhotset = new TreeSet<>();
    TreeSet<DoubleDBIDPair> maxhotset = new TreeSet<>(Collections.reverseOrder());

    int randomsize = (int) Math.max(25, FastMath.pow(relation.size(), 0.2));
    double rprob = ((double) randomsize) / size;
    ArrayModifiableDBIDs randomset = DBIDUtil.newArray(randomsize);

    DBIDIter iter = relation.iterDBIDs();
    if(!iter.valid()) {
      throw new EmptyDataException();
    }
    DBID firstid = DBIDUtil.deref(iter);
    iter.advance();
    minhotset.add(DBIDUtil.newPair(Double.MAX_VALUE, firstid));
    maxhotset.add(DBIDUtil.newPair(Double.MIN_VALUE, firstid));
    for(; iter.valid(); iter.advance()) {
      // generate candidates for min distance.
      ArrayList<DoubleDBIDPair> np = new ArrayList<>(k * 2 + randomsize * 2);
      for(DoubleDBIDPair pair : minhotset) {
        // skip the object itself
        if(DBIDUtil.equal(iter, pair)) {
          continue;
        }
        double d = distFunc.distance(iter, pair);
        np.add(DBIDUtil.newPair(d, iter));
        np.add(DBIDUtil.newPair(d, pair));
      }
      for(DBIDIter iter2 = randomset.iter(); iter2.valid(); iter2.advance()) {
        double d = distFunc.distance(iter, iter2);
        np.add(DBIDUtil.newPair(d, iter));
        np.add(DBIDUtil.newPair(d, iter2));
      }
      minhotset.addAll(np);
      shrinkHeap(minhotset, k);

      // generate candidates for max distance.
      ArrayList<DoubleDBIDPair> np2 = new ArrayList<>(k * 2 + randomsize * 2);
      for(DoubleDBIDPair pair : minhotset) {
        // skip the object itself
        if(DBIDUtil.equal(iter, pair)) {
          continue;
        }
        double d = distFunc.distance(iter, pair);
        np2.add(DBIDUtil.newPair(d, iter));
        np2.add(DBIDUtil.newPair(d, pair));
      }
      for(DBIDIter iter2 = randomset.iter(); iter2.valid(); iter2.advance()) {
        double d = distFunc.distance(iter, iter2);
        np.add(DBIDUtil.newPair(d, iter));
        np.add(DBIDUtil.newPair(d, iter2));
      }
      maxhotset.addAll(np2);
      shrinkHeap(maxhotset, k);

      // update random set
      if(randomset.size() < randomsize) {
        randomset.add(iter);
      }
      else if(rnd.nextDouble() < rprob) {
        randomset.set((int) Math.floor(rnd.nextDouble() * randomsize), iter);
      }
    }
    return new DoubleMinMax(minhotset.first().doubleValue(), maxhotset.first().doubleValue());
  }

  /**
   * Compute the exact maximum and minimum.
   *
   * @param relation Relation to process
   * @param distFunc Distance function
   * @return Exact maximum and minimum
   */
  private DoubleMinMax exactMinMax(Relation<O> relation, DistanceQuery<O> distFunc) {
    final FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Exact fitting distance computations", relation.size(), LOG) : null;
    DoubleMinMax minmax = new DoubleMinMax();
    // find exact minimum and maximum first.
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      for(DBIDIter iditer2 = relation.iterDBIDs(); iditer2.valid(); iditer2.advance()) {
        // skip the point itself.
        if(DBIDUtil.equal(iditer, iditer2)) {
          continue;
        }
        double d = distFunc.distance(iditer, iditer2);
        minmax.put(d);
      }
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
    return minmax;
  }

  /**
   * Shrink the heap of "hot" (extreme) items.
   *
   * @param hotset Set of hot items
   * @param k target size
   */
  private static void shrinkHeap(TreeSet<DoubleDBIDPair> hotset, int k) {
    // drop duplicates
    ModifiableDBIDs seenids = DBIDUtil.newHashSet(2 * k);
    int cnt = 0;
    for(Iterator<DoubleDBIDPair> i = hotset.iterator(); i.hasNext();) {
      DoubleDBIDPair p = i.next();
      if(cnt > k || seenids.contains(p)) {
        i.remove();
      }
      else {
        seenids.add(p);
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
     * Flag to compute exact value range for binning.
     */
    public static final OptionID EXACT_ID = new OptionID("diststat.exact", "In a first pass, compute the exact minimum and maximum, at the cost of O(2*n*n) instead of O(n*n). The number of resulting bins is guaranteed to be as requested.");

    /**
     * Flag to enable sampling.
     */
    public static final OptionID SAMPLING_ID = new OptionID("diststat.sampling", "Enable sampling of O(n) size to determine the minimum and maximum distances approximately. The resulting number of bins can be larger than the given n.");

    /**
     * Option to configure the number of bins to use.
     */
    public static final OptionID HISTOGRAM_BINS_ID = new OptionID("diststat.bins", "Number of bins to use in the histogram. By default, it is only guaranteed to be within 1*n and 2*n of the given number.");

    /**
     * Number of bins to use in sampling.
     */
    protected int numbin = 20;

    /**
     * Sampling.
     */
    protected boolean sampling = false;

    /**
     * Exactness flag.
     */
    protected boolean exact = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter numbinP = new IntParameter(HISTOGRAM_BINS_ID, 20) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(numbinP)) {
        numbin = numbinP.getValue();
      }

      final Flag exactF = new Flag(EXACT_ID);
      if(config.grab(exactF)) {
        exact = exactF.getValue();
      }

      if(!exact) {
        final Flag samplingF = new Flag(SAMPLING_ID);
        if(config.grab(samplingF)) {
          sampling = samplingF.getValue();
        }
      }
    }

    @Override
    protected DistanceStatisticsWithClasses<O> makeInstance() {
      return new DistanceStatisticsWithClasses<>(distanceFunction, numbin, exact, sampling);
    }
  }
}
