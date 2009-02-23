package de.lmu.ifi.dbs.elki.algorithm.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
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
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.math.Histogram;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.Histogram.Constructor;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.pairs.ComparablePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.SimplePair;

/**
 * <p>Algorithm to gather statistics over the distance distribution in the data
 * set.</p>
 * 
 * TODO: Add sampling.
 * 
 * TODO: Collect in-cluster and cross-cluster averages.
 * 
 * @author Erich Schubert
 */
public class DistanceStatisticsWithClasses<V extends RealVector<V, ?>> extends DistanceBasedAlgorithm<V, DoubleDistance, CollectionResult<DoubleVector>> {
  private CollectionResult<DoubleVector> result;

  /**
   * OptionID for {@link #SAMPLING_FLAG}
   */
  public static final OptionID SAMPLING_ID = OptionID.getOrCreateOptionID("diststat.sampling", "Enable sampling to reduce runtime from O(2*n*n) to O(n*n)+O(n) " + "at the cost of evenutally having more than the configured number of bins.");

  /**
   * Flag to enable sampling
   * <p>
   * Key: {@code -h}
   * </p>
   */
  private final Flag SAMPLING_FLAG = new Flag(SAMPLING_ID);

  /**
   * Number of bins to use in sampling.
   */
  private int numbin = 100;

  /**
   * Sampling
   */
  private boolean sampling = false;

  /**
   * Empty constructor. Nothing to do.
   */
  public DistanceStatisticsWithClasses() {
    super();
    addOption(SAMPLING_FLAG);
  }

  /**
   * Iterates over all points in the database.
   */
  @Override
  protected CollectionResult<DoubleVector> runInTime(Database<V> database) throws IllegalStateException {
    DistanceFunction<V, DoubleDistance> distFunc = getDistanceFunction();
    distFunc.setDatabase(database, isVerbose(), isTime());
    int size = database.size();

    // determine binning ranges.
    double gmin = Double.MAX_VALUE;
    double gmax = Double.MIN_VALUE;

    if(sampling) {
      Random rnd = new Random();
      // estimate minimum and maximum.
      int k = (int) Math.max(25,Math.pow(database.size(), 0.2));
      TreeSet<ComparablePair<Double, Integer>> minhotset = new TreeSet<ComparablePair<Double, Integer>>();
      TreeSet<ComparablePair<Double, Integer>> maxhotset = new TreeSet<ComparablePair<Double, Integer>>(Collections.reverseOrder());
      
      int randomsize = (int)Math.max(25,Math.pow(database.size(), 0.2));
      double rprob = ((double) randomsize) / size;
      ArrayList<Integer> randomset = new ArrayList<Integer>(randomsize);
      
      Iterator<Integer> iter = database.iterator();
      if(!iter.hasNext()) {
        throw new IllegalStateException("Database is empty.");
      }
      Integer firstid = iter.next();
      minhotset.add(new ComparablePair<Double, Integer>(Double.MAX_VALUE, firstid));
      maxhotset.add(new ComparablePair<Double, Integer>(Double.MIN_VALUE, firstid));
      while(iter.hasNext()) {
        Integer id1 = iter.next();
        // generate candidates for min distance.
        ArrayList<ComparablePair<Double,Integer>> np = new ArrayList<ComparablePair<Double,Integer>>(k*2+randomsize*2);
        for(ComparablePair<Double, Integer> pair : minhotset) {
          Integer id2 = pair.getSecond();
          // skip the object itself
          if(id1 == id2) {
            continue;
          }
          double d = distFunc.distance(id1, id2).getValue();
          np.add(new ComparablePair<Double, Integer>(d, id1));
          np.add(new ComparablePair<Double, Integer>(d, id2));
        }
        for(Integer id2 : randomset) {
          double d = distFunc.distance(id1, id2).getValue();
          np.add(new ComparablePair<Double, Integer>(d, id1));
          np.add(new ComparablePair<Double, Integer>(d, id2));
        }
        minhotset.addAll(np);
        shrinkHeap(minhotset, k);
        
        // generate candidates for max distance.
        ArrayList<ComparablePair<Double,Integer>> np2 = new ArrayList<ComparablePair<Double,Integer>>(k*2+randomsize*2);
        for(ComparablePair<Double, Integer> pair : minhotset) {
          Integer id2 = pair.getSecond();
          // skip the object itself
          if(id1 == id2) {
            continue;
          }
          double d = distFunc.distance(id1, id2).getValue();
          np2.add(new ComparablePair<Double, Integer>(d, id1));
          np2.add(new ComparablePair<Double, Integer>(d, id2));
        }
        for(Integer id2 : randomset) {
          double d = distFunc.distance(id1, id2).getValue();
          np.add(new ComparablePair<Double, Integer>(d, id1));
          np.add(new ComparablePair<Double, Integer>(d, id2));
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
      gmin = minhotset.first().getFirst();
      gmax = maxhotset.first().getFirst();
    }
    else {
      // find exact minimum and maximum first.
      for(Integer id1 : database.getIDs()) {
        for(Integer id2 : database.getIDs()) {
          // skip the point itself.
          if(id1 == id2) {
            continue;
          }
          double d = distFunc.distance(id1, id2).getValue();
          gmin = Math.min(d, gmin);
          gmax = Math.max(d, gmax);
        }
      }
    }

    // Cluster by labels
    ByLabelClustering<V> split = new ByLabelClustering<V>();
    Set<Cluster<Model>> splitted = split.run(database).getAllClusters();

    // global in-cluster min/max
    double gimin = Double.MAX_VALUE;
    double gimax = Double.MIN_VALUE;
    // global other-cluster min/max
    double gomin = Double.MAX_VALUE;
    double gomax = Double.MIN_VALUE;
    // in-cluster distances
    MeanVariance mimin = new MeanVariance();
    MeanVariance mimax = new MeanVariance();
    MeanVariance midel = new MeanVariance();
    // other-cluster distances
    MeanVariance momin = new MeanVariance();
    MeanVariance momax = new MeanVariance();
    MeanVariance model = new MeanVariance();
    // Histograms
    Histogram<SimplePair<Integer, Integer>> hist = new Histogram<SimplePair<Integer, Integer>>(numbin, gmin, gmax, new Constructor<SimplePair<Integer, Integer>>() {
      @Override
      public SimplePair<Integer, Integer> make() {
        return new SimplePair<Integer, Integer>(0, 0);
      }
    });

    // iterate per cluster
    for(Cluster<?> c1 : splitted) {
      for(Integer id1 : c1) {
        // in-cluster distances
        double imin = Double.MAX_VALUE;
        double imax = Double.MIN_VALUE;
        for(Integer id2 : c1) {
          // skip the point itself.
          if(id1 == id2) {
            continue;
          }
          double d = distFunc.distance(id1, id2).getValue();

          SimplePair<Integer, Integer> pair = hist.get(d);
          pair.first += 1;

          imin = Math.min(d, imin);
          imax = Math.max(d, imax);
        }
        double idelta = imax - imin;
        // aggregate
        mimin.addData(imin);
        mimax.addData(imax);
        midel.addData(idelta);
        // min/max
        gimin = Math.min(imin, gimin);
        gimax = Math.max(imax, gimax);

        // other-cluster distances
        double omin = Double.MAX_VALUE;
        double omax = Double.MIN_VALUE;
        for(Cluster<?> c2 : splitted) {
          if(c2 == c1) {
            continue;
          }
          for(Integer id2 : c2) {
            // skip the point itself (shouldn't happen though)
            if(id1 == id2) {
              continue;
            }
            double d = distFunc.distance(id1, id2).getValue();

            SimplePair<Integer, Integer> pair = hist.get(d);
            pair.second += 1;

            omin = Math.min(d, omin);
            omax = Math.max(d, omax);
          }
        }
        double odelta = omax - omin;
        // aggregate
        momin.addData(omin);
        momax.addData(omax);
        model.addData(odelta);
        // min/max
        gomin = Math.min(omin, gomin);
        gomax = Math.max(omax, gomax);
      }
    }

    // count the number of samples we have in the data
    int inum = 0;
    int onum = 0;
    for(SimplePair<Double, SimplePair<Integer, Integer>> ppair : hist) {
      inum += ppair.getSecond().getFirst();
      onum += ppair.getSecond().getSecond();
    }
    int bnum = inum + onum;
    // Note: when sampling is added, this assertion won't hold anymore.
    assert (bnum == size * (size - 1));

    Collection<DoubleVector> binstat = new ArrayList<DoubleVector>(numbin);
    for(SimplePair<Double, SimplePair<Integer, Integer>> ppair : hist) {
      DoubleVector row = new DoubleVector(new double[] { ppair.getFirst(), ((double) ppair.getSecond().getFirst()) / inum, ((double) ppair.getSecond().getFirst()) / bnum, ((double) ppair.getSecond().getSecond()) / onum, ((double) ppair.getSecond().getSecond()) / bnum });
      binstat.add(row);
    }
    result = new CollectionResult<DoubleVector>(binstat);

    result.addHeader("Absolute minimum distance (abs): " + gmin);
    result.addHeader("Absolute maximum distance (abs): " + gmax);
    result.addHeader("In-Cluster minimum distance (abs, avg, stddev): " + gimin + " " + mimin.getMean() + " " + mimin.getVariance());
    result.addHeader("In-Cluster maximum distance (abs, avg, stddev): " + gimax + " " + mimax.getMean() + " " + mimax.getVariance());
    result.addHeader("Other-Cluster minimum distance (abs, avg, stddev): " + gomin + " " + momin.getMean() + " " + momin.getVariance());
    result.addHeader("Other-Cluster maximum distance (abs, avg, stddev): " + gomax + " " + momax.getMean() + " " + momax.getVariance());
    result.addHeader("Column description: bin center, in-cluster only frequency, in-cluster all frequency, other-cluster only frequency, other cluster all frequency");
    result.addHeader("In-cluster value count: " + inum + " other cluster value count: " + onum);
    return result;
  }

  private void shrinkHeap(TreeSet<ComparablePair<Double, Integer>> hotset, int k) {
    // drop duplicates
    HashSet<Integer> seenids = new HashSet<Integer>(2*k);
    int cnt = 0;
    for (Iterator<ComparablePair<Double, Integer>> i = hotset.iterator(); i.hasNext(); ) {
      ComparablePair<Double, Integer> p = i.next();
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
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // sampling
    if(SAMPLING_FLAG.isSet()) {
      sampling = true;
    }

    setParameters(args, remainingParameters);
    return remainingParameters;
  }
}
