package de.lmu.ifi.dbs.elki.result.outlier;

import java.util.Collections;
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIteratorAdapter;

/**
 * Ordering obtained from an outlier score.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Relation
 */
public class OrderingFromRelation implements OrderingResult {
  /**
   * Outlier scores.
   */
  protected Relation<Double> scores;

  /**
   * Factor for ascending (+1) and descending (-1) ordering.
   */
  protected int ascending = +1;
  
  /**
   * Constructor for outlier orderings
   * 
   * @param scores outlier score result
   * @param ascending Ascending when {@code true}, descending otherwise
   */
  public OrderingFromRelation(Relation<Double> scores, boolean ascending) {
    super();
    this.scores = scores;
    this.ascending = ascending ? +1 : -1;
  }
  
  /**
   * Ascending constructor.
   * 
   * @param scores
   */
  public OrderingFromRelation(Relation<Double> scores) {
    this(scores, true);
  }

  @Override
  public String getLongName() {
    return scores.getLongName()+" Order";
  }

  @Override
  public String getShortName() {
    return scores.getShortName()+"_order";
  }

  /**
   * Internal comparator, accessing the map to sort objects
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected final class ImpliedComparator implements Comparator<DBID> {
    @Override
    public int compare(DBID id1, DBID id2) {
      Double k1 = scores.get(id1);
      Double k2 = scores.get(id2);
      assert (k1 != null);
      assert (k2 != null);
      return ascending * k2.compareTo(k1);
    }
  }

  @Override
  public IterableIterator<DBID> iter(DBIDs ids) {
    ArrayModifiableDBIDs sorted = DBIDUtil.newArray(ids);
    Collections.sort(sorted, new ImpliedComparator());
    return new IterableIteratorAdapter<DBID>(sorted);
  }
}
