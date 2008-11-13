package de.lmu.ifi.dbs.elki.evaluation.paircounting.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.cluster.BaseCluster;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * Generator for noise points.
 * 
 * This generator will generate pairs (a,a) for all elements a in the given list.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public class PairGeneratorNoise extends PairSortedGenerator {
  /**
   * Ids to use
   */
  private int[] ids;
  /**
   * Current position.
   */
  private int pos;
  
  /**
   * Create new generator for noise pairs.
   * 
   * Note: this will <em>not make a copy of param ids</em>, and ids will be sorted.
   * 
   * @param ids array to use as source.
   */
  @Deprecated
  public PairGeneratorNoise(int[] ids) {
    this.ids = ids;
    Arrays.sort(this.ids);
    
    pos = 0;
    if (ids.length > 0) {
      setCurrent(new IntIntPair(ids[pos], ids[pos]));
    }
  }

  /**
   * Crate new generator for a base cluster object.
   * 
   * @param <C> cluster type
   * @param cluster object
   */
  public <C extends BaseCluster<C, ?>> PairGeneratorNoise(C cluster) {
    // build int array for the cluster
    // TODO: copy less.
    List<Integer> idslist = new ArrayList<Integer>(cluster.getIDs());
    ids = new int[idslist.size()];
    for(int j = 0; j < ids.length; j++) {
      ids[j] = idslist.get(j);
    }
    Arrays.sort(ids);
    
    pos = 0;
    if (ids.length > 0) {
      setCurrent(new IntIntPair(ids[pos], ids[pos]));
    }
  }

  /**
   * Advance iterator and return new pair.
   */
  @Override
  protected IntIntPair advance() {
    if (current() == null) return null;
    pos++;
    if (pos >= ids.length) {
      return null;
    } else {
      return new IntIntPair(ids[pos],ids[pos]);
    }
  }
}