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

  public DatabaseObjectMetadata(String objectlabel, ClassLabel classlabel) {
    super();
    this.objectlabel = objectlabel;
    this.classlabel = classlabel;
  }

  public DatabaseObjectMetadata() {
    super();
  }
}
