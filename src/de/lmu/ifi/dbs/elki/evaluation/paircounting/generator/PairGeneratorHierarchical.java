package de.lmu.ifi.dbs.elki.evaluation.paircounting.generator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.result.clustering.HierarchicalCluster;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * Generate sorted {@link IntIntPair}s for a {@link HierarchicalCluster}.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 *
 * @param <C> Cluster class
 */
public class PairGeneratorHierarchical<C extends HierarchicalCluster<C>> extends PairSortedGenerator {
  /**
   * Ids in parent clusters
   */
  private int[] parentids;
  /**
   * ids in this cluster
   */
  private int[] thisids;
  /**
   * Position in first set
   */
  private int pos1;
  /**
   * Position in second set
   */
  private int pos2;
  
  /**
   * Generate pairs for a hierarchical cluster.
   * 
   * @param cluster Cluster
   */
  public PairGeneratorHierarchical(C cluster) {
    // collect all parent clusters into a flat list.
    java.util.Vector<C> allparents = new java.util.Vector<C>();
    allparents.addAll(cluster.getParents());
    for (int i=0; i < allparents.size(); i++)
      for (C newc : allparents.get(i).getParents())
        if (! allparents.contains(newc))
          allparents.add(newc);

    // build int array for the cluster
    List<Integer> idslist = cluster.getIDs();
    thisids = new int[idslist.size()];
    for (int j = 0; j < thisids.length; j++)
      thisids[j] = idslist.get(j);
    Arrays.sort(thisids);
    // TODO: ensure there are no duplicate IDs?
    
    HashSet<Integer> idsset = new HashSet<Integer>(idslist);
    for (C parent : allparents)
      idsset.addAll(parent.getIDs());
    parentids = new int[idsset.size()];
    int j=0;
    for (Integer in : idsset) {
      parentids[j] = in;
      j++;
    }
    Arrays.sort(parentids);

    // initialize iterator.
    pos1 = 0; pos2 = 0;
    if (thisids.length > 0)
      setCurrent(new IntIntPair(parentids[pos1], thisids[pos2]));
  }
  
  /**
   * Advance iterator
   */
  @Override
  protected IntIntPair advance() {
    if (current() == null) return null;
    pos2++;
    if (pos2 >= thisids.length) {
      pos2 = 0; pos1++;
    }
    if (pos1 >= parentids.length)
      return null;
    else
      return new IntIntPair(parentids[pos1],thisids[pos2]);
  }
}