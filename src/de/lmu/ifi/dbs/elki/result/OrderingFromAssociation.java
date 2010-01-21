package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.IterableIteratorAdapter;

/**
 * Return an ordering result backed by a database. Note that the implementation
 * will sort with O(n lg n) database accesses on average, O(n*n) in worst case
 * to avoid having to store all objects in memory.
 * 
 * @author Erich Schubert
 * 
 * @param <T> data type of annotation.
 * @param <O> database object type
 */
public class OrderingFromAssociation<T extends Comparable<T>, O extends DatabaseObject> implements OrderingResult {
  /**
   * Database
   */
  protected Database<O> db;

  /**
   * AssocationID to use
   */
  protected AssociationID<T> association;

  /**
   * Internal comparator to use for sorting.
   */
  protected Comparator<T> comparator;

  /**
   * Factor to signal ascending (+1) / descending (-1) order
   */
  int ascending = 1;

  /**
   * Internally used comparator retrieving data from the database.
   * 
   * @author Erich Schubert
   */
  protected class ImpliedComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer id1, Integer id2) {
      T k1 = db.getAssociation(association, id1);
      T k2 = db.getAssociation(association, id2);
      return ascending * k1.compareTo(k2);
    }
  }

  /**
   * Internally used comparator using a preexisting comparator for the given
   * datatype
   * 
   * @author Erich Schubert
   * 
   */
  protected class DerivedComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer id1, Integer id2) {
      T k1 = db.getAssociation(association, id1);
      T k2 = db.getAssociation(association, id2);
      return ascending * comparator.compare(k1, k2);
    }
  }

  /**
   * Full Constructor (with comparator)
   * 
   * @param db Database
   * @param association AssociationID to use
   * @param comparator Comparator for data type used by association
   * @param descending boolean to mark descending ordering (true)
   */
  @Deprecated
  public OrderingFromAssociation(Database<O> db, AssociationID<T> association, Comparator<T> comparator, boolean descending) {
    this.db = db;
    this.association = association;
    this.comparator = comparator;
    this.ascending = descending ? -1 : 1;
  }

  /**
   * Constructor without comparator
   * 
   * @param db Database
   * @param association AssociationID to use
   * @param descending boolean to mark descending ordering (true)
   */
  @Deprecated
  public OrderingFromAssociation(Database<O> db, AssociationID<T> association, boolean descending) {
    this.db = db;
    this.association = association;
    this.comparator = null;
    this.ascending = descending ? -1 : 1;
  }

  /**
   * Minimal constructor, using implied comparator and ascending order.
   * 
   * @param db Database
   * @param association AssociationID to use
   */
  @Deprecated
  public OrderingFromAssociation(Database<O> db, AssociationID<T> association) {
    this.db = db;
    this.association = association;
    this.comparator = null;
    this.ascending = 1;
  }

  /**
   * Sort the given collection of IDs and return an iterator for the result.
   */
  @Override
  public IterableIterator<Integer> iter(Collection<Integer> ids) {
    ArrayList<Integer> sorted = new ArrayList<Integer>(ids);
    if(comparator != null) {
      Collections.sort(sorted, new DerivedComparator());
    }
    else {
      Collections.sort(sorted, new ImpliedComparator());
    }
    return new IterableIteratorAdapter<Integer>(sorted);
  }

  @Override
  public String getName() {
    return "order";
  }
}
