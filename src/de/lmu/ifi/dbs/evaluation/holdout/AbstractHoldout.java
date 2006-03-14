package de.lmu.ifi.dbs.evaluation.holdout;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.*;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractHoldout<O extends DatabaseObject> implements Holdout<O> {

  /**
   * The association id for the class label.
   */
  public static final AssociationID CLASS = AssociationID.CLASS;

  protected Database<O> database;

  protected ClassLabel[] labels;

  /**
   * The parameterToDescription map.
   */
  protected Map<String, String> parameterToDescription = new HashMap<String, String>();

  protected OptionHandler optionHandler;

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    return optionHandler.grabOptions(args);
  }

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
