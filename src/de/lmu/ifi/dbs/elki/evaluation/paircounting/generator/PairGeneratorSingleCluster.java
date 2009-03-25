package de.lmu.ifi.dbs.elki.evaluation.paircounting.generator;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * Generate sorted ID pairs for a {@link Cluster}.
 * 
 * @author Erich Schubert
 */
public class PairGeneratorSingleCluster extends PairSortedGenerator {
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
  public PairGeneratorSingleCluster(Cluster<?> cluster) {
    // collect all parent clusters into a flat list.
    java.util.Vector<Cluster<?>> allparents = new java.util.Vector<Cluster<?>>();
    if(cluster.isHierarchical()) {
      allparents.addAll(cluster.getParents());
      for(int i = 0; i < allparents.size(); i++) {
        for(Cluster<?> newc : allparents.get(i).getParents()) {
          if(!allparents.contains(newc)) {
            allparents.add(newc);
          }
        }
      }
    }

    // build int array for the cluster
    Collection<Integer> cids = cluster.getIDs();
    thisids = new int[cids.size()];
    {
      int j = 0;
      for (Integer id : cids) {
        thisids[j] = id;
        j++;
      }
    }
    Arrays.sort(thisids);
    // TODO: ensure there are no duplicate IDs?

    HashSet<Integer> idsset = new HashSet<Integer>(cids);
    for(Cluster<?> parent : allparents) {
      idsset.addAll(parent.getIDs());
    }
    parentids = new int[idsset.size()];
    int j = 0;
    for(Integer in : idsset) {
      parentids[j] = in;
      j++;
    }
    Arrays.sort(parentids);

    // initialize iterator.
    pos1 = 0;
    pos2 = 0;
    if(thisids.length > 0) {
      setCurrent(new IntIntPair(parentids[pos1], thisids[pos2]));
    }
  }

  /**
   * Advance iterator
   */
  @Override
  protected IntIntPair advance() {
    if(current() == null) {
      return null;
    }
    pos2++;
    if(pos2 >= thisids.length) {
      pos2 = 0;
      pos1++;
    }
    if(pos1 >= parentids.length) {
      return null;
    }
    else {
      return new IntIntPair(parentids[pos1], thisids[pos2]);
    }
  }
}