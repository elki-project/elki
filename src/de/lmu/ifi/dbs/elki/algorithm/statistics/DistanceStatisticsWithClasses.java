package de.lmu.ifi.dbs.elki.algorithm.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.utilities.Description;

/**
 * Algorithm to gather statistics over the distance distribution in the data set.
 * 
 * TODO: Add sampling.
 * TODO: Collect in-cluster and cross-cluster averages.
 * 
 * @author Erich Schubert
 */
public class DistanceStatisticsWithClasses<V extends RealVector<V, ?>> extends DistanceBasedAlgorithm<V, DoubleDistance, CollectionResult<DoubleVector>> {
  private CollectionResult<DoubleVector> result;

  private int numbin = 100;

  /**
   * Empty constructor. Nothing to do.
   */
  public DistanceStatisticsWithClasses() {
    super();
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
    
    for (Integer id1 : database.getIDs()) {
      for (Integer id2 : database.getIDs()) {
        // skip the point itself.
        if (id1 == id2) {
          continue;
        }
        double d = distFunc.distance(id1, id2).getValue();
        gmin = Math.min(d, gmin);
        gmax = Math.max(d, gmax);
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
    double step = (gmax - gmin) / numbin;
    int[] ihist = new int[numbin];
    int[] ohist = new int[numbin];
    // zero out for sure.
    for(int i = 0; i < numbin; i++) {
      ihist[i] = 0;
      ohist[i] = 0;
    }

    // iterate per cluster
    for(Cluster<Model> c1 : splitted) {
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
          
          int bin = (int) Math.floor((d - gmin) / step);
          bin = Math.min(bin, numbin - 1);
          ihist[bin]++;

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
        for(Cluster<Model> c2 : splitted) {
          if(c2 == c1) {
            continue;
          }
          for(Integer id2 : c2) {
            // skip the point itself (shouldn't happen though)
            if(id1 == id2) {
              continue;
            }
            double d = distFunc.distance(id1, id2).getValue();
            
            int bin = (int) Math.floor((d - gmin) / step);
            bin = Math.min(bin, numbin - 1);
            ohist[bin]++;

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
    for (int i = 0; i < numbin; i++) {
      inum += ihist[i];
      onum += ohist[i];
    }
    int bnum = inum + onum;
    // Note: when sampling is added, this assertion won't hold anymore.
    assert(bnum == size * (size - 1));

    Collection<DoubleVector> binstat = new ArrayList<DoubleVector>(numbin);
    for(int i = 0; i < numbin; i++) {
      DoubleVector row = new DoubleVector(new double[] { gmin + (i + .5) * step, ((double)ihist[i]) / inum, ((double)ihist[i]) / bnum, ((double)ohist[i]) / onum, ((double)ohist[i]) / bnum});
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
    result.addHeader("In-cluster value count: "+inum+" other cluster value count: "+onum);
    return result;
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
}
