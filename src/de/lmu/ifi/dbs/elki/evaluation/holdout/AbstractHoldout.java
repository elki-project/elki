package de.lmu.ifi.dbs.elki.evaluation.holdout;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;

import java.util.Arrays;

/**
 * @author Arthur Zimek
 */
public abstract class AbstractHoldout<O extends DatabaseObject, L extends ClassLabel> extends AbstractParameterizable implements Holdout<O, L> {

  /**
   * The association id for the class label.
   */
  public static final AssociationID<ClassLabel> CLASS = AssociationID.CLASS;

  protected Database<O> database;

  protected L[] labels;

  /**
   * Checks whether the database has classes annotated and collects the available classes.
   *
   * @param database the database to collect classes from
   */
  @SuppressWarnings("unchecked")
  public void setClassLabels(Database<O> database) {
    // TODO: ugly hack?
    this.labels = Util.getClassLabels(database).toArray((L[]) new Object[]{});
    Arrays.sort(this.labels);
  }

  public Database<O> completeData() {
    return database;
  }


}
