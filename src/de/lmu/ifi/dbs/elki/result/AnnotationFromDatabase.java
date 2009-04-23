package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class representing Annotations backed by a database using {@link AssociationID}s.
 * 
 * @author Erich Schubert
 *
 * @param <O> Database object type
 * @param <T> Association type
 */
public class AnnotationFromDatabase<T, O extends DatabaseObject> implements AnnotationResult<T> {
  /**
   * database storage
   */
  private Database<O> database;

  /**
   * Storage of included associations.
   */
  private AssociationID<T> assoc;

  /**
   * Constructor
   * 
   * @param db Database
   * @param association Association to use
   */
  public AnnotationFromDatabase(Database<O> db, AssociationID<T> association) {
    this.database = db;
    this.assoc = association;
  }

  /**
   * Return database referenced by annotations
   * 
   * @return database referenced
   */
  public Database<O> getDatabase() {
    return this.database;
  }

  /**
   * Retrieve the given annotations for this object from the Database.
   * @deprecated
   */
  @Deprecated
  @Override
  @SuppressWarnings("deprecation")
  public Pair<String,T>[] getAnnotations(Integer objID) {
    Pair<String,T>[] result = Pair.newArray(1);
    T o = getDatabase().getAssociation(assoc, objID);
    result[0] = new Pair<String, T>(assoc.getLabel(), o);
    return result;
  }

  @Override
  public AssociationID<T> getAssociationID() {
    return assoc;
  }

  @Override
  public T getValueFor(Integer objID) {
    return getDatabase().getAssociation(assoc, objID);
  }
}
