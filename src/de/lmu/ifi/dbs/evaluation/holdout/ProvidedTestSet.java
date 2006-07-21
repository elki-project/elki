package de.lmu.ifi.dbs.evaluation.holdout;


import java.util.Arrays;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Puts all data into the training set and requests a testset via a database connection.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ProvidedTestSet<O extends DatabaseObject> extends AbstractHoldout<O> {

  /**
   * Holds the testset.
   */
  private Database<O> testset;

  /**
   * The default database connection.
   */
  private static final String DEFAULT_DATABASE_CONNECTION = FileBasedDatabaseConnection.class.getName();

  /**
   * The parameter for the database connection to the testset.
   */
  public static final String TESTSET_DATABASE_CONNECTION_P = "testdbc";

  /**
   * The description for parameter testdbc.
   */
  public static final String TESTSET_DATABASE_CONNECTION_D = "<class>connection to testset database " +
                                                             Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DatabaseConnection.class) +
                                                             ". Default: " + DEFAULT_DATABASE_CONNECTION;

  /**
   * Holds the database connection to testset.
   */
  private DatabaseConnection<O> dbc;

  /**
   * Provides a single pair of training and test data sets,
   * where the training set contains the complete data set
   * as comprised by the given database,
   * the test data set is given via a database connection.
   *
   * @see Holdout#partition(de.lmu.ifi.dbs.database.Database)
   */
  public TrainingAndTestSet<O>[] partition(Database<O> database) {
    this.database = database;
    setClassLabels(database);
    //noinspection unchecked
    TrainingAndTestSet<O>[] split = new TrainingAndTestSet[1];
    Set<ClassLabel> joinedLabels = Util.getClassLabels(testset);
    for (ClassLabel label : this.labels) {
      joinedLabels.add(label);
    }
    this.labels = joinedLabels.toArray(new ClassLabel[joinedLabels.size()]);
    Arrays.sort(this.labels);
    split[0] = new TrainingAndTestSet<O>(this.database, testset, labels);
    return split;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("AllTraining puts the complete database in the training set. A testset is expected via a database connection.", false));
    // TODO: available dbcs?
    description.append(dbc.description());
    return description.toString();
  }

  /**
   * Returns the given parameter array unchanged, since no parameters are required by this class.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    String dbcClassName = optionHandler.isSet(TESTSET_DATABASE_CONNECTION_P) ?
                          optionHandler.getOptionValue(TESTSET_DATABASE_CONNECTION_P) :
                          DEFAULT_DATABASE_CONNECTION;
    try {
      //noinspection unchecked
      dbc = Util.instantiate(DatabaseConnection.class, dbcClassName);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(TESTSET_DATABASE_CONNECTION_P, dbcClassName, TESTSET_DATABASE_CONNECTION_D);
    }

    remainingParameters = dbc.setParameters(remainingParameters);
    testset = dbc.getDatabase(null);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * @see AbstractHoldout#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(TESTSET_DATABASE_CONNECTION_P, dbc.getClass().getName());

    attributeSettings.addAll(dbc.getAttributeSettings());
    return attributeSettings;
  }

}
