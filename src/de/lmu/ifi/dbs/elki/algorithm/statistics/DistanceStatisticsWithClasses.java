package de.lmu.ifi.dbs.elki.algorithm.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.FlexiHistogram;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.OnlyOneIsAllowedToBeSetGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.pairs.FCPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * <p>Algorithm to gather statistics over the distance distribution in the data
 * set.</p>
 * 
 * @author Erich Schubert
 * @param <V> Vector type
 */
public class DistanceStatisticsWithClasses<V extends RealVector<V, ?>, D extends NumberDistance<D,?>> extends DistanceBasedAlgorithm<V, D, CollectionResult<DoubleVector>> {
  private CollectionResult<DoubleVector> result;

  /**
   * OptionID for {@link #EXACT_FLAG}
   */
  public static final OptionID EXACT_ID = OptionID.getOrCreateOptionID("diststat.exact", "In a first pass, compute the exact minimum and maximum, at the cost of O(2*n*n) instead of O(n*n). The number of resulting bins is guaranteed to be as requested.");

  /**
   * Flag to enable sampling
   * <p>
   * Key: {@code -diststat.exact}
   * </p>
   */
  private final Flag EXACT_FLAG = new Flag(EXACT_ID);

  /**
   * OptionID for {@link #SAMPLING_FLAG}
   */
  public static final OptionID SAMPLING_ID = OptionID.getOrCreateOptionID("diststat.sampling", "Enable sampling of O(n) size to determine the minimum and maximum distances approximately. The resulting number of bins can be larger than the given n.");

  /**
   * Flag to enable sampling
   * <p>
   * Key: {@code -diststat.sampling}
   * </p>
   */
  private final Flag SAMPLING_FLAG = new Flag(SAMPLING_ID);

  /**
   * OptionID for {@link #HISTOGRAM_BINS_OPTION}
   */
  public static final OptionID HISTOGRAM_BINS_ID = OptionID.getOrCreateOptionID("diststat.bins", "Number of bins to use in the histogram. By default, it is only guaranteed to be within 1*n and 2*n of the given number.");

  /**
   * Option to configure the number of bins to use.
   * <p>
   * Key: {@code -diststat.bins}
   * </p>
   */
  private final IntParameter HISTOGRAM_BINS_OPTION = new IntParameter(HISTOGRAM_BINS_ID, new GreaterEqualConstraint(2), 20);
  
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
   * Empty constructor. Nothing to do.
   */
  public DistanceStatisticsWithClasses() {
    super();
    addOption(HISTOGRAM_BINS_OPTION);
    addOption(EXACT_FLAG);
    addOption(SAMPLING_FLAG);
    
    ArrayList<Option<?>> exclusive = new ArrayList<Option<?>>();
    exclusive.add(EXACT_FLAG);
    exclusive.add(SAMPLING_FLAG);
    optionHandler.setGlobalParameterConstraint(new OnlyOneIsAllowedToBeSetGlobalConstraint(exclusive));
  }

  /**
   * Iterates over all points in the database.
   */
  @Override
  protected CollectionResult<DoubleVector> runInTime(Database<V> database) throws IllegalStateException {
    DistanceFunction<V, D> distFunc = getDistanceFunction();
    distFunc.setDatabase(database, isVerbose(), isTime());
    int size = database.size();

    // determine binning ranges.
    DoubleMinMax gminmax = new DoubleMinMax();

    // Cluster by labels
    ByLabelClustering<V> split = new ByLabelClustering<V>();
    Set<Cluster<Model>> splitted = split.run(database).getAllClusters();

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
    if (exact) {
      gminmax = exactMinMax(database, distFunc);
      histogram = AggregatingHistogram.LongSumLongSumHistogram(numbin, gminmax.getMin(), gminmax.getMax());
    } else if (sampling) {
      gminmax = sampleMinMax(database, distFunc);
      histogram = AggregatingHistogram.LongSumLongSumHistogram(numbin, gminmax.getMin(), gminmax.getMax());
    } else {
      histogram = FlexiHistogram.LongSumLongSumHistogram(numbin);
    }

    // iterate per cluster
    final Pair<Long,Long> incFirst = new Pair<Long,Long>(1L, 0L);
    final Pair<Long,Long> incSecond = new Pair<Long,Long>(0L, 1L);
    for(Cluster<?> c1 : splitted) {
      for(Integer id1 : c1) {
        // in-cluster distances
        DoubleMinMax iminmax = new DoubleMinMax();
        for(Integer id2 : c1) {
          // skip the point itself.
          if(id1 == id2) {
            continue;
          }
          double d = distFunc.distance(id1, id2).getValue().doubleValue();

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
        for(Cluster<?> c2 : splitted) {
          if(c2 == c1) {
            continue;
          }
          for(Integer id2 : c2) {
            // skip the point itself (shouldn't happen though)
            if(id1 == id2) {
              continue;
            }
            double d = distFunc.distance(id1, id2).getValue().doubleValue();

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
      DoubleVector row = new DoubleVector(new double[] { ppair.getFirst(), ((double) ppair.getSecond().getFirst()) / inum / histogram.getBinsize(), ((double) ppair.getSecond().getFirst()) / bnum / histogram.getBinsize(), ((double) ppair.getSecond().getSecond()) / onum / histogram.getBinsize(), ((double) ppair.getSecond().getSecond()) / bnum / histogram.getBinsize() });
      binstat.add(row);
    }
    result = new CollectionResult<DoubleVector>(binstat);

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
  
  private DoubleMinMax sampleMinMax(Database<V> database, DistanceFunction<V, D> distFunc) {
    int size = database.size();
    Random rnd = new Random();
    // estimate minimum and maximum.
    int k = (int) Math.max(25,Math.pow(database.size(), 0.2));
    TreeSet<FCPair<Double, Integer>> minhotset = new TreeSet<FCPair<Double, Integer>>();
    TreeSet<FCPair<Double, Integer>> maxhotset = new TreeSet<FCPair<Double, Integer>>(Collections.reverseOrder());
    
    int randomsize = (int)Math.max(25,Math.pow(database.size(), 0.2));
    double rprob = ((double) randomsize) / size;
    ArrayList<Integer> randomset = new ArrayList<Integer>(randomsize);
    
    Iterator<Integer> iter = database.iterator();
    if(!iter.hasNext()) {
      throw new IllegalStateException(ExceptionMessages.DATABASE_EMPTY);
    }
    Integer firstid = iter.next();
    minhotset.add(new FCPair<Double, Integer>(Double.MAX_VALUE, firstid));
    maxhotset.add(new FCPair<Double, Integer>(Double.MIN_VALUE, firstid));
    while(iter.hasNext()) {
      Integer id1 = iter.next();
      // generate candidates for min distance.
      ArrayList<FCPair<Double,Integer>> np = new ArrayList<FCPair<Double,Integer>>(k*2+randomsize*2);
      for(FCPair<Double, Integer> pair : minhotset) {
        Integer id2 = pair.getSecond();
        // skip the object itself
        if(id1 == id2) {
          continue;
        }
        double d = distFunc.distance(id1, id2).getValue().doubleValue();
        np.add(new FCPair<Double, Integer>(d, id1));
        np.add(new FCPair<Double, Integer>(d, id2));
      }
      for(Integer id2 : randomset) {
        double d = distFunc.distance(id1, id2).getValue().doubleValue();
        np.add(new FCPair<Double, Integer>(d, id1));
        np.add(new FCPair<Double, Integer>(d, id2));
      }
      minhotset.addAll(np);
      shrinkHeap(minhotset, k);
      
      // generate candidates for max distance.
      ArrayList<FCPair<Double,Integer>> np2 = new ArrayList<FCPair<Double,Integer>>(k*2+randomsize*2);
      for(FCPair<Double, Integer> pair : minhotset) {
        Integer id2 = pair.getSecond();
        // skip the object itself
        if(id1 == id2) {
          continue;
        }
        double d = distFunc.distance(id1, id2).getValue().doubleValue();
        np2.add(new FCPair<Double, Integer>(d, id1));
        np2.add(new FCPair<Double, Integer>(d, id2));
      }
      for(Integer id2 : randomset) {
        double d = distFunc.distance(id1, id2).getValue().doubleValue();
        np.add(new FCPair<Double, Integer>(d, id1));
        np.add(new FCPair<Double, Integer>(d, id2));
      }
      maxhotset.addAll(np2);
      shrinkHeap(maxhotset, k);
      
      // update random set
      if (randomset.size() < randomsize) {
        randomset.add(id1);
      } else if (rnd.nextDouble() < rprob) {
        randomset.set((int)Math.floor(rnd.nextDouble() * randomsize), id1);
      }
    }
    return new DoubleMinMax(minhotset.first().getFirst(),maxhotset.first().getFirst());
  }
  
  private DoubleMinMax exactMinMax(Database<V> database, DistanceFunction<V, D> distFunc) {
    DoubleMinMax minmax = new DoubleMinMax();
    // find exact minimum and maximum first.
    for(Integer id1 : database.getIDs()) {
      for(Integer id2 : database.getIDs()) {
        // skip the point itself.
        if(id1 == id2) {
          continue;
        }
        double d = distFunc.distance(id1, id2).getValue().doubleValue();
        minmax.put(d);
      }
    }    
    return minmax;
  }

  private void shrinkHeap(TreeSet<FCPair<Double, Integer>> hotset, int k) {
    // drop duplicates
    HashSet<Integer> seenids = new HashSet<Integer>(2*k);
    int cnt = 0;
    for (Iterator<FCPair<Double, Integer>> i = hotset.iterator(); i.hasNext(); ) {
      FCPair<Double, Integer> p = i.next();
      if (cnt > k || seenids.contains(p.getSecond())) {
        i.remove();
      } else {
        seenids.add(p.getSecond());
        cnt++;
      }
    }
  }

  /**
   * Describe the algorithm and it's use.
   */
  public Description getDescription() {
    return new Description("DistanceStatistics", "DistanceStatistics", "Computes a statistics over the distances occurring in the data set.", "");
  }

  /**
   * Return a result object
   */
  public CollectionResult<DoubleVector> getResult() {
    return result;
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    // exact
    if(EXACT_FLAG.isSet()) {
      exact = true;
    }
    // sampling
    if(SAMPLING_FLAG.isSet()) {
      sampling = true;
    }
    
    numbin = HISTOGRAM_BINS_OPTION.getValue();

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }
}
