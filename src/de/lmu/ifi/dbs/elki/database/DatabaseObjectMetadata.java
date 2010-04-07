package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.data.ClassLabel;

/**
 * Temporary class, containing the remaining associations.
 * 
 * FIXME: remove this, use the new DB layer.
 * This is only used in a few convenience places.
 * 
 * @author Erich Schubert
 */
public class DatabaseObjectMetadata {
  public String objectlabel = null;

  public ClassLabel classlabel = null;

  public String externalid = null;

  public DatabaseObjectMetadata(String objectlabel, ClassLabel classlabel, String externalid) {
    super();
    this.objectlabel = objectlabel;
    this.classlabel = classlabel;
    this.externalid = externalid;
  }

  public DatabaseObjectMetadata() {
    super();
  }

  public DatabaseObjectMetadata(Database<?> db, Integer objid) {
    this(db.getObjectLabel(objid), db.getClassLabel(objid), db.getExternalID(objid));
  }
}
