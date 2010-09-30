package de.lmu.ifi.dbs.elki.result.outlier;

import java.util.Collections;

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
  private AnnotationResult<Double> scores;

  /**
   * Factor for ascending (+1) and descending (-1) ordering.
   */
  boolean ascending;
  
  /**
   * Constructor for outlier orderings
   * 
   * @param scores outlier score result
   * @param ascending Ascending when {@code true}, descending otherwise
   */
  public OutlierOrderingResult(AnnotationResult<Double> scores, boolean ascending) {
    super();
    this.scores = scores;
    this.ascending = ascending;
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

  @Override
  public IterableIterator<DBID> iter(DBIDs ids) {
    ArrayModifiableDBIDs sorted = DBIDUtil.newArray(ids);
    if (ascending) {
      Collections.sort(sorted);
    } else {
      Collections.sort(sorted, Collections.reverseOrder());
    }
    return new IterableIteratorAdapter<DBID>(sorted);
  }
}
