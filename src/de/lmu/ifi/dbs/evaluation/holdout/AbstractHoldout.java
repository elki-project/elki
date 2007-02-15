package de.lmu.ifi.dbs.evaluation.holdout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractHoldout<O extends DatabaseObject> extends AbstractParameterizable implements Holdout<O> {

  /**
   * The association id for the class label.
   */
  public static final AssociationID CLASS = AssociationID.CLASS;

  protected Database<O> database;

  protected ClassLabel[] labels;

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
    this.labels = Util.getClassLabels(database).toArray(new ClassLabel[0]);
    Arrays.sort(this.labels);
  }

  public Database<O> completeData() {
    return database;
  }


}
