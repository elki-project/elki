package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DataQuery;

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
  public static class ClassLabelAnnotation extends BasicResult implements AnnotationResult<ClassLabel> {
    /**
     * Database to wrap
     */
    private final DataQuery<ClassLabel> rep;

    /**
     * Constructor.
     * 
     * @param database Database to access
     */
    public ClassLabelAnnotation(Database<?> database) {
      super("Class Label", AssociationID.CLASS.getName());
      this.rep = database.getClassLabelQuery();
    }

    @Override
    public AssociationID<ClassLabel> getAssociationID() {
      return AssociationID.CLASS;
    }

    @Override
    public ClassLabel getValueFor(DBID objID) {
      return rep.get(objID);
    }
  }

  /**
   * Object label "result" view
   * 
   * @author Erich Schubert
   */
  public static class ObjectLabelAnnotation extends BasicResult implements AnnotationResult<String> {
    /**
     * Database to wrap
     */
    private final DataQuery<String> rep;

    /**
     * Constructor.
     * 
     * @param database Database to access
     */
    public ObjectLabelAnnotation(Database<?> database) {
      super("Object Label", AssociationID.LABEL.getName());
      this.rep = database.getObjectLabelQuery();
    }

    @Override
    public AssociationID<String> getAssociationID() {
      return AssociationID.LABEL;
    }

    @Override
    public String getValueFor(DBID objID) {
      return rep.get(objID);
    }
  }
}