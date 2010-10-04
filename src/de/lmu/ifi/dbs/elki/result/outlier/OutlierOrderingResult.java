package de.lmu.ifi.dbs.elki.result.outlier;

import java.util.Collections;
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.IterableIteratorAdapter;

/**
 * Ordering obtained from an outlier score.
 * 
 * @author Erich Schubert
 */
public class OutlierOrderingResult implements OrderingResult {
  /**
   * Outlier scores.
   */
  protected AnnotationResult<Double> scores;

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
  public OutlierOrderingResult(AnnotationResult<Double> scores, boolean ascending) {
    super();
    this.scores = scores;
    this.ascending = ascending ? +1 : -1;
  }
  
  /**
   * Ascending constructor.
   * 
   * @param scores
   */
  public OutlierOrderingResult(AnnotationResult<Double> scores) {
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
   */
  protected final class ImpliedComparator implements Comparator<DBID> {
    @Override
    public int compare(DBID id1, DBID id2) {
      Double k1 = scores.getValueFor(id1);
      Double k2 = scores.getValueFor(id2);
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
