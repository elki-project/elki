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
 * 
 * @apiviz.exclude
 */
public class AnnotationBuiltins {
  /**
   * NON-constructor. Packaging only
   */
  private AnnotationBuiltins() {
    super();
  }

  /**
   * Class label "result" view
   * 
   * @author Erich Schubert
   */
  public static class ClassLabelAnnotation extends TreeResult implements AnnotationResult<ClassLabel> {
    /**
     * Database to wrap
     */
    private final Database<?> database;

    /**
     * Constructor.
     * 
     * @param database Database to access
     */
    public ClassLabelAnnotation(Database<?> database) {
      super("Class Label", AssociationID.CLASS.getName());
      this.database = database;
    }

    @Override
    public AssociationID<ClassLabel> getAssociationID() {
      return AssociationID.CLASS;
    }

    @Override
    public ClassLabel getValueFor(DBID objID) {
      return database.getClassLabel(objID);
    }
  }

  /**
   * Object label "result" view
   * 
   * @author Erich Schubert
   */
  public static class ObjectLabelAnnotation extends TreeResult implements AnnotationResult<String> {
    /**
     * Database to wrap
     */
    private final Database<?> database;

    /**
     * Constructor.
     * 
     * @param database Database to access
     */
    public ObjectLabelAnnotation(Database<?> database) {
      super("Object Label", AssociationID.LABEL.getName());
      this.database = database;
    }

    @Override
    public AssociationID<String> getAssociationID() {
      return AssociationID.LABEL;
    }

    @Override
    public String getValueFor(DBID objID) {
      return database.getObjectLabel(objID);
    }
  }

  /**
   * External ID "result" view
   * 
   * @author Erich Schubert
   */
  public static class ExternalIDAnnotation extends TreeResult implements AnnotationResult<String> {
    /**
     * Database to wrap
     */
    private final Database<?> database;
    
    /**
     * Constructor.
     * 
     * @param database Database to access
     */
    public ExternalIDAnnotation(Database<?> database) {
      super("ExternalID", AssociationID.EXTERNAL_ID.getName());
      this.database = database;
    }

    @Override
    public AssociationID<String> getAssociationID() {
      return AssociationID.EXTERNAL_ID;
    }

    @Override
    public String getValueFor(DBID objID) {
      return database.getExternalID(objID);
    }
  }
}