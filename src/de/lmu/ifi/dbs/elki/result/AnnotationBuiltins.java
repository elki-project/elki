package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * This is a temporary solution to handling class labels, object labels and
 * external ids, for the migration process to the new database layer.
 * 
 * @author Erich Schubert
 */
final public class AnnotationBuiltins {
  /**
   * Database to wrap
   */
  protected Database<?> database;

  /**
   * Constructor for the main class.
   * 
   * @param database Database to represent
   */
  public AnnotationBuiltins(Database<?> database) {
    super();
    this.database = database;
  }

  /**
   * Add the builtins to the result.
   * 
   * @param r Result to add to.
   */
  public void prependToResult(MultiResult r) {
    r.prependResult(new ExternalIDAnnotation());
    r.prependResult(new ObjectLabelAnnotation());
    r.prependResult(new ClassLabelAnnotation());
  }

  /**
   * Add the builtins to the result.
   * 
   * @param r Result to add to.
   */
  public void addToResult(MultiResult r) {
    r.addResult(new ClassLabelAnnotation());
    r.addResult(new ObjectLabelAnnotation());
    r.addResult(new ExternalIDAnnotation());
  }

  /**
   * Class label "result" view
   * 
   * @author Erich Schubert
   */
  public class ClassLabelAnnotation implements AnnotationResult<ClassLabel> {
    @Override
    public AssociationID<ClassLabel> getAssociationID() {
      return AssociationID.CLASS;
    }

    @Override
    public ClassLabel getValueFor(DBID objID) {
      return database.getClassLabel(objID);
    }

    @Override
    public String getName() {
      return getAssociationID().getName();
    }
  }

  /**
   * Object label "result" view
   * 
   * @author Erich Schubert
   */
  public class ObjectLabelAnnotation implements AnnotationResult<String> {
    @Override
    public AssociationID<String> getAssociationID() {
      return AssociationID.LABEL;
    }

    @Override
    public String getValueFor(DBID objID) {
      return database.getObjectLabel(objID);
    }

    @Override
    public String getName() {
      return getAssociationID().getName();
    }
  }

  /**
   * External ID "result" view
   * 
   * @author Erich Schubert
   */
  public class ExternalIDAnnotation implements AnnotationResult<String> {
    @Override
    public AssociationID<String> getAssociationID() {
      return AssociationID.EXTERNAL_ID;
    }

    @Override
    public String getValueFor(DBID objID) {
      return database.getExternalID(objID);
    }

    @Override
    public String getName() {
      return getAssociationID().getName();
    }
  }
}