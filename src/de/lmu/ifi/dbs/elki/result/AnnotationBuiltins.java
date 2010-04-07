package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;

/**
 * This is a temporary solution to handling class labels, object labels and
 * external ids, for the migration process to the new database layer.
 * 
 * @author Erich Schubert
 */
// TODO: document - or better: remove again!
public class AnnotationBuiltins {
  Database<?> database;

  public AnnotationBuiltins(Database<?> database) {
    super();
    this.database = database;
  }

  public void prependToResult(MultiResult r) {
    r.prependResult(new ClassLabelAnnotation());
    r.prependResult(new ObjectLabelAnnotation());
    r.prependResult(new ExternalIDAnnotation());
  }

  public class ClassLabelAnnotation implements AnnotationResult<ClassLabel> {
    @Override
    public AssociationID<ClassLabel> getAssociationID() {
      return AssociationID.CLASS;
    }

    @Override
    public ClassLabel getValueFor(Integer objID) {
      return database.getClassLabel(objID);
    }

    @Override
    public String getName() {
      return getAssociationID().getName();
    }
  }

  public class ObjectLabelAnnotation implements AnnotationResult<String> {
    @Override
    public AssociationID<String> getAssociationID() {
      return AssociationID.LABEL;
    }

    @Override
    public String getValueFor(Integer objID) {
      return database.getObjectLabel(objID);
    }

    @Override
    public String getName() {
      return getAssociationID().getName();
    }
  }

  public class ExternalIDAnnotation implements AnnotationResult<String> {
    @Override
    public AssociationID<String> getAssociationID() {
      return AssociationID.EXTERNAL_ID;
    }

    @Override
    public String getValueFor(Integer objID) {
      return database.getExternalID(objID);
    }

    @Override
    public String getName() {
      return getAssociationID().getName();
    }
  }
}