package de.lmu.ifi.dbs.elki.evaluation.holdout;


import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Puts all data into the training set and requests a testset via a database connection.
 *
 * @author Arthur Zimek
 */
public class ProvidedTestSet<O extends DatabaseObject, L extends ClassLabel<L>> extends AbstractHoldout<O,L> {

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

  
  public ProvidedTestSet(){
	  super();
	  
	  ClassParameter<DatabaseConnection<O>> dbCon = new ClassParameter(TESTSET_DATABASE_CONNECTION_P,TESTSET_DATABASE_CONNECTION_D,DatabaseConnection.class);
	  dbCon.setDefaultValue(DEFAULT_DATABASE_CONNECTION);
	  optionHandler.put(dbCon);
  }
  /**
   * Provides a single pair of training and test data sets,
   * where the training set contains the complete data set
   * as comprised by the given database,
   * the test data set is given via a database connection.
   *
   * @see Holdout#partition(de.lmu.ifi.dbs.elki.database.Database)
   */
  public TrainingAndTestSet<O,L>[] partition(Database<O> database) {
    this.database = database;
    setClassLabels(database);
    //noinspection unchecked
    TrainingAndTestSet<O,L>[] split = new TrainingAndTestSet[1];
    Set<ClassLabel<?>> joinedLabels =  Util.getClassLabels(testset);
    for (L label : this.labels) {
      joinedLabels.add(label);
    }
    this.labels = joinedLabels.toArray((L[])new ClassLabel[joinedLabels.size()]);
    Arrays.sort(this.labels);
    split[0] = new TrainingAndTestSet(this.database, testset, labels);
    return split;
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#parameterDescription()
   */
  public String parameterDescription() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("AllTraining puts the complete database in the training set. A testset is expected via a database connection.", false));
    // TODO: available dbcs?
    description.append(dbc.parameterDescription());
    return description.toString();
  }

  /**
   * Returns the given parameter array unchanged, since no parameters are required by this class.
   *
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    String dbcClassName = (String) optionHandler.getOptionValue(TESTSET_DATABASE_CONNECTION_P);
    try {
      //noinspection unchecked
        // todo
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
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #dbc}.
     */
    @Override
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    attributeSettings.addAll(dbc.getAttributeSettings());
    return attributeSettings;
  }

}
