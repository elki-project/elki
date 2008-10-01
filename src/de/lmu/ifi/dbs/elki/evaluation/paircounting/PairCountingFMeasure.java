package de.lmu.ifi.dbs.elki.evaluation.paircounting;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.algorithm.result.clustering.Cluster;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.HierarchicalCluster;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.HierarchicalClusters;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairGeneratorFlat;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairGeneratorHierarchical;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairGeneratorMerge;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairGeneratorNoise;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairSortedGeneratorInterface;

/**
 * Compare two clustering results using a pair-counting F-Measure.
 * 
 * A pair are any two objects that belong to the same cluster.
 * 
 * Two clusterings are compared by comparing their pairs; if two clusterings completely agree,
 * they also agree on every pair; even when the clusters and points are ordered differently.
 * 
 * An empty clustering will of course have no pairs, the trivial all-in-one clustering
 * of course has n^2 pairs. Therefore neither recall nor precision itself are useful, however their
 * combination -- the F-Measure -- is useful.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 *
 * @param <O> Object type
 */
public class PairCountingFMeasure<O extends DatabaseObject> {
  public PairSortedGeneratorInterface getPairGenerator(ClusteringResult<O> clusters) {
    Cluster<O>[] cs = clusters.getClusters();
    PairSortedGeneratorInterface[] gens = new PairSortedGeneratorInterface[cs.length + 1];
    for (int i=0; i < cs.length; i++)
      gens[i] = new PairGeneratorFlat(cs[i].getClusterIDs());
    Database<O> noisedb = clusters.noise();
    int[] noise = new int[noisedb.size()];
    {
      int i=0;
      for (Integer id : noisedb.getIDs()) {
        noise[i] = id;
        i++;
      }
    }
    gens[cs.length] = new PairGeneratorNoise(noise);
    return new PairGeneratorMerge(gens);
  }

  public <C extends HierarchicalCluster<C>> PairSortedGeneratorInterface getPairGenerator(HierarchicalClusters<C,O> clusters) {
    // collect all clusters into a flat list.
    ArrayList<C> allclusters = new ArrayList<C>();
    allclusters.addAll(clusters.getRootClusters());
    for (int i=0; i < allclusters.size(); i++)
      allclusters.addAll(allclusters.get(i).getChildren());

    // Make generators for each cluster
    PairSortedGeneratorInterface[] gens = new PairSortedGeneratorInterface[allclusters.size()];
    for (int i=0; i < gens.length; i++)
      gens[i] = new PairGeneratorHierarchical<C>(allclusters.get(i));
    // TODO: noise?
    return new PairGeneratorMerge(gens);
  }

  /**
   * Compare two clustering results.
   * 
   * @param result1 first result
   * @param result2 second result
   * @param beta Beta value for the F-Measure
   * @return Pair counting F-Measure result.
   */
  public double compareDatabases(ClusteringResult<O> result1, ClusteringResult<O> result2, double beta) {
    PairSortedGeneratorInterface first = getPairGenerator(result1);
    PairSortedGeneratorInterface second = getPairGenerator(result2);
    return countPairs(first, second, beta);
  }

  /**
   * Compare two clustering results.
   * 
   * @param result1 first result
   * @param result2 second result
   * @param beta Beta value for the F-Measure
   * @return Pair counting F-Measure result.
   */
  public <C extends HierarchicalCluster<C>> double compareDatabases(HierarchicalClusters<C,O> result1, ClusteringResult<O> result2, double beta) {
    PairSortedGeneratorInterface first = getPairGenerator(result1);
    PairSortedGeneratorInterface second = getPairGenerator(result2);
    return countPairs(first, second, beta);
  }

  /**
   * Compare two clustering results.
   * 
   * @param result1 first result
   * @param result2 second result
   * @param beta Beta value for the F-Measure
   * @return Pair counting F-Measure result.
   */
  public <C extends HierarchicalCluster<C>, D extends HierarchicalCluster<D>> double compareDatabases(HierarchicalClusters<C,O> result1, HierarchicalClusters<D,O> result2, double beta) {
    PairSortedGeneratorInterface first = getPairGenerator(result1);
    PairSortedGeneratorInterface second = getPairGenerator(result2);
    return countPairs(first, second, beta);
  }

  /**
   * Compare two clustering results.
   * 
   * @param result1 first result
   * @param result2 second result
   * @return Pair counting F-1-Measure result.
   */
  public double compareDatabases(ClusteringResult<O> result1, ClusteringResult<O> result2) {
    return compareDatabases(result1, result2, 1.0);
  }

  /**
   * Compare two clustering results.
   * 
   * @param result1 first result
   * @param result2 second result
   * @return Pair counting F-1-Measure result.
   */
  public <C extends HierarchicalCluster<C>> double compareDatabases(HierarchicalClusters<C,O> result1, ClusteringResult<O> result2) {
    return compareDatabases(result1, result2, 1.0);
  }

  /**
   * Compare two clustering results.
   * 
   * @param result1 first result
   * @param result2 second result
   * @return Pair counting F-1-Measure result.
   */
  public <C extends HierarchicalCluster<C>, D extends HierarchicalCluster<D>> double compareDatabases(HierarchicalClusters<C,O> result1, HierarchicalClusters<D,O> result2) {
    return compareDatabases(result1, result2, 1.0);
  }

  /**
   * Compare two sets of generated pairs.
   * 
   * @param first first set
   * @param second second set
   * @param beta beta value for F-Measure
   * @return F-beta-Measure for pairs.
   */
  private double countPairs(PairSortedGeneratorInterface first, PairSortedGeneratorInterface second, double beta) {
    int inboth = 0;
    int infirst = 0;
    int insecond = 0;
    
    while (first.current() != null && second.current() != null) {
      int cmp = first.current().compareTo(second.current());
      if (cmp == 0) {
        inboth++;
        first.next();
        second.next();
      } else if (cmp < 0) {
        infirst++;
        first.next();
      } else {
        insecond++;
        second.next();
      }
    }
    while (first.current() != null) {
      infirst++;
      first.next();
    }
    while (second.current() != null) {
      insecond++;
      second.next();
    }
    
    //System.out.println("Both: "+inboth+" First: "+infirst+" Second: "+insecond);

    double fmeasure = ((1+beta*beta) * inboth) / ((1+beta*beta) * inboth + (beta*beta)*infirst + insecond);

    return fmeasure;
  }
}
