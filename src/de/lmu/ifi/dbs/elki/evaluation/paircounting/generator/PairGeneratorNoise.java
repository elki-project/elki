package de.lmu.ifi.dbs.elki.evaluation.paircounting.generator;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * Generator for noise points.
 * 
 * This generator will generate pairs (a,a) for all elements a in the given
 * list.
 * 
 * @author Erich Schubert
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
   * Crate new generator for a base cluster object.
   * 
   * @param cluster object
   */
  public PairGeneratorNoise(Cluster<?> cluster) {
    // build int array for the cluster
    // TODO: copy less.
    DBIDs dbids = cluster.getIDs();
    ids = new int[dbids.size()];
    int j = 0;
    for (DBID id : dbids) {
      ids[j] = id.getIntegerID();
      j++;
    }
    Arrays.sort(ids);

    pos = 0;
    if(ids.length > 0) {
      setCurrent(new IntIntPair(ids[pos], ids[pos]));
    }
  }

  /**
   * Advance iterator and return new pair.
   */
  @Override
  protected IntIntPair advance() {
    if(current() == null) {
      return null;
    }
    pos++;
    if(pos >= ids.length) {
      return null;
    }
    else {
      return new IntIntPair(ids[pos], ids[pos]);
    }
  }
}