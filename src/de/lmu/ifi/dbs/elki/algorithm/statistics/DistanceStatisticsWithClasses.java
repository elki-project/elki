package de.lmu.ifi.dbs.elki.algorithm.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.FlexiHistogram;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
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
@Title("Distance Histogram")
@Description("Computes a histogram over the distances occurring in the data set.")
public class DistanceStatisticsWithClasses<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, CollectionResult<DoubleVector>> {
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
  protected HistogramResult<DoubleVector> runInTime(Database<O> database) throws IllegalStateException {
    DistanceQuery<O, D> distFunc = getDistanceFunction().instantiate(database);
    int size = database.size();

    // determine binning ranges.
    DoubleMinMax gminmax = new DoubleMinMax();

    // Cluster by labels
    ByLabelClustering<O> splitter = new ByLabelClustering<O>();
    Collection<Cluster<Model>> split = splitter.run(database).getAllClusters();

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
    if(exact) {
      gminmax = exactMinMax(database, distFunc);
      histogram = AggregatingHistogram.LongSumLongSumHistogram(numbin, gminmax.getMin(), gminmax.getMax());
    }
    else if(sampling) {
      gminmax = sampleMinMax(database, distFunc);
      histogram = AggregatingHistogram.LongSumLongSumHistogram(numbin, gminmax.getMin(), gminmax.getMax());
    }
    else {
      histogram = FlexiHistogram.LongSumLongSumHistogram(numbin);
    }

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
      }
    }
    // Update values (only needed for sampling case).
    gminmax.setFirst(Math.min(giminmax.getMin(), gominmax.getMin()));
    gminmax.setSecond(Math.max(giminmax.getMax(), gominmax.getMax()));

    // count the number of samples we have in the data
    long inum = 0;
    long onum = 0;
    for(Pair<Double, Pair<Long, Long>> ppair : histogram) {
      inum += ppair.getSecond().getFirst();
      onum += ppair.getSecond().getSecond();
    }
    long bnum = inum + onum;
    // Note: when full sampling is added, this assertion won't hold anymore.
    assert (bnum == size * (size - 1));

    Collection<DoubleVector> binstat = new ArrayList<DoubleVector>(numbin);
    for(Pair<Double, Pair<Long, Long>> ppair : histogram) {
      final double icof = (inum == 0) ? 0 : ((double) ppair.getSecond().getFirst()) / inum / histogram.getBinsize();
      final double icaf = ((double) ppair.getSecond().getFirst()) / bnum / histogram.getBinsize();
      final double ocof = (onum == 0) ? 0 : ((double) ppair.getSecond().getSecond()) / onum / histogram.getBinsize();
      final double ocaf = ((double) ppair.getSecond().getSecond()) / bnum / histogram.getBinsize();
      DoubleVector row = new DoubleVector(new double[] { ppair.getFirst(), icof, icaf, ocof, ocaf });
      binstat.add(row);
    }
    HistogramResult<DoubleVector> result = new HistogramResult<DoubleVector>(binstat);

    result.addHeader("Absolute minimum distance (abs): " + gminmax.getMin());
    result.addHeader("Absolute maximum distance (abs): " + gminmax.getMax());
    result.addHeader("In-Cluster minimum distance (abs, avg, stddev): " + giminmax.getMin() + " " + mimin.getMean() + " " + mimin.getStddev());
    result.addHeader("In-Cluster maximum distance (abs, avg, stddev): " + giminmax.getMax() + " " + mimax.getMean() + " " + mimax.getStddev());
    result.addHeader("Other-Cluster minimum distance (abs, avg, stddev): " + gominmax.getMin() + " " + momin.getMean() + " " + momin.getStddev());
    result.addHeader("Other-Cluster maximum distance (abs, avg, stddev): " + gominmax.getMax() + " " + momax.getMean() + " " + momax.getStddev());
    result.addHeader("Column description: bin center, in-cluster only frequency, in-cluster all frequency, other-cluster only frequency, other cluster all frequency");
    result.addHeader("In-cluster value count: " + inum + " other cluster value count: " + onum);
    return result;
  }

  private DoubleMinMax sampleMinMax(Database<O> database, DistanceQuery<O, D> distFunc) {
    int size = database.size();
    Random rnd = new Random();
    // estimate minimum and maximum.
    int k = (int) Math.max(25, Math.pow(database.size(), 0.2));
    TreeSet<FCPair<Double, DBID>> minhotset = new TreeSet<FCPair<Double, DBID>>();
    TreeSet<FCPair<Double, DBID>> maxhotset = new TreeSet<FCPair<Double, DBID>>(Collections.reverseOrder());

    int randomsize = (int) Math.max(25, Math.pow(database.size(), 0.2));
    double rprob = ((double) randomsize) / size;
    ArrayModifiableDBIDs randomset = DBIDUtil.newArray(randomsize);

    Iterator<DBID> iter = database.iterator();
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

  private DoubleMinMax exactMinMax(Database<O> database, DistanceQuery<O, D> distFunc) {
    DoubleMinMax minmax = new DoubleMinMax();
    // find exact minimum and maximum first.
    for(DBID id1 : database.getIDs()) {
      for(DBID id2 : database.getIDs()) {
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

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return KNN outlier detection algorithm
   */
  public static <O extends DatabaseObject, D extends NumberDistance<D, ?>> DistanceStatisticsWithClasses<O, D> parameterize(Parameterization config) {
    int bins = getParameterBins(config);
    boolean exact = false;
    final Flag EXACT_FLAG = new Flag(EXACT_ID);
    if(config.grab(EXACT_FLAG)) {
      exact = EXACT_FLAG.getValue();
    }
    boolean sampling = true;
    final Flag SAMPLING_FLAG = new Flag(SAMPLING_ID);
    if(config.grab(SAMPLING_FLAG)) {
      sampling = SAMPLING_FLAG.getValue();
    }

    ArrayList<Parameter<?, ?>> exclusive = new ArrayList<Parameter<?, ?>>();
    exclusive.add(EXACT_FLAG);
    exclusive.add(SAMPLING_FLAG);
    config.checkConstraint(new OnlyOneIsAllowedToBeSetGlobalConstraint(exclusive));

    DistanceFunction<O, D> distanceFunction = getParameterDistanceFunction(config);
    if(config.hasErrors()) {
      return null;
    }
    return new DistanceStatisticsWithClasses<O, D>(distanceFunction, bins, exact, sampling);
  }

  /**
   * Get the number of bins parameter
   * 
   * @param config Parameterization
   * @return bins parameter
   */
  protected static int getParameterBins(Parameterization config) {
    final IntParameter param = new IntParameter(HISTOGRAM_BINS_ID, new GreaterEqualConstraint(2), 20);
    if(config.grab(param)) {
      return param.getValue();
    }
    return -1;
  }
}