package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;

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
  // This will be deprecated with the switch to the new DB layer post 0.3!
  //@Deprecated
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

  @Override
  public AssociationID<T> getAssociationID() {
    return assoc;
  }

  @Override
  public T getValueFor(Integer objID) {
    return getDatabase().getAssociation(assoc, objID);
  }

  @Override
  public String getName() {
    return "annotation";
  }
}
