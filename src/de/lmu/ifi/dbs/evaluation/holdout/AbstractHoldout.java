package de.lmu.ifi.dbs.evaluation.holdout;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.*;

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

//  /**
//   * The parameterToDescription map.
//   */
//  protected Map<String, String> parameterToDescription = new HashMap<String, String>();
//
//  protected OptionHandler optionHandler;
//  
//
//  /**
//   * Holds the currently set parameter array.
//   */
//  private String[] currentParameterArray = new String[0];

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }
  
//  /**
//   * Sets the difference of the first array minus the second array
//   * as the currently set parameter array.
//   * 
//   * 
//   * @param complete the complete array
//   * @param part an array that contains only elements of the first array
//   */
//  protected void setParameters(String[] complete, String[] part)
//  {
//      currentParameterArray = Util.parameterDifference(complete, part);
//  }
  
//  /**
//   * 
//   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
//   */
//  public String[] getParameters()
//  {
//      String[] param = new String[currentParameterArray.length];
//      System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
//      return param;
//  }
  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = new ArrayList<AttributeSettings>();
    settings.add(new AttributeSettings(this));
    return settings;
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
