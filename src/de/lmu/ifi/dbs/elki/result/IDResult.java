package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * The trivial 'result' of the object IDs used.
 * 
 * @author Erich Schubert
 */
public class IDResult extends TreeResult implements AnnotationResult<Integer> {
  /**
   * The association ID to use.
   */
  public final static AssociationID<Integer> OBJECT_ID = AssociationID.getOrCreateAssociationID("ID", Integer.class);

  /**
   * Constructor.
   */
  public IDResult() {
    super("ID", "id");
  }

  @Override
  public AssociationID<Integer> getAssociationID() {
    return OBJECT_ID;
  }

  @Override
  public Integer getValueFor(DBID objID) {
    return objID.getIntegerID();
  }
}
