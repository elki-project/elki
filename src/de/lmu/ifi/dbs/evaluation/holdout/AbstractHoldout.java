package de.lmu.ifi.dbs.evaluation.holdout;

import java.util.Arrays;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * @author Arthur Zimek
 */
public abstract class AbstractHoldout<O extends DatabaseObject, L extends ClassLabel<L>> extends AbstractParameterizable implements Holdout<O,L> {

  /**
   * The association id for the class label.
   */
  public static final AssociationID CLASS = AssociationID.CLASS;

  protected Database<O> database;

  protected L[] labels;

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }
  
  /**
   * Checks whether the database has classes annotated and collects the available classes.
   *
   * @param database the database to collect classes from
   */
  public void setClassLabels(Database<O> database) {
    this.labels = Util.getClassLabels(database).toArray((L[])new Object[]{});
    Arrays.sort(this.labels);
  }

  public Database<O> completeData() {
    return database;
  }


}
