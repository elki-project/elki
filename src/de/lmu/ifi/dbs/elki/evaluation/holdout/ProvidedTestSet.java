package de.lmu.ifi.dbs.elki.evaluation.holdout;


import java.util.Arrays;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Puts all data into the training set and requests a test set via a database connection.
 *
 * @author Arthur Zimek
 */
public class ProvidedTestSet<O extends DatabaseObject, L extends ClassLabel> extends AbstractHoldout<O, L> {

    /**
     * Holds the test set.
     */
    private Database<O> testset;

    /**
     * The default database connection.
     */
    private static final String DEFAULT_DATABASE_CONNECTION = FileBasedDatabaseConnection.class.getName();

    /**
     * OptionID for {@link #TESTSET_DATABASE_CONNECTION_PARAM}
     */
    public static final OptionID TESTSET_DATABASE_CONNECTION_ID = OptionID.getOrCreateOptionID(
        "testdbc", "connection to testset database " +
        Properties.ELKI_PROPERTIES.restrictionString(DatabaseConnection.class) +
        ".");

    /**
     * Parameter for test set database connection
     */
    private final ClassParameter<DatabaseConnection<O>> TESTSET_DATABASE_CONNECTION_PARAM =
      new ClassParameter<DatabaseConnection<O>>(TESTSET_DATABASE_CONNECTION_ID, DatabaseConnection.class, DEFAULT_DATABASE_CONNECTION);
    
    /**
     * Holds the database connection to test set.
     */
    private DatabaseConnection<O> dbc;

    public ProvidedTestSet() {
        super();
        addOption(TESTSET_DATABASE_CONNECTION_PARAM);
    }

    /**
     * Provides a single pair of training and test data sets,
     * where the training set contains the complete data set
     * as comprised by the given database,
     * the test data set is given via a database connection.
     */
    @SuppressWarnings("unchecked")
    public TrainingAndTestSet<O, L>[] partition(Database<O> database) {
        this.database = database;
        setClassLabels(database);
        TrainingAndTestSet<O, L>[] split = new TrainingAndTestSet[1];
        Set<ClassLabel> joinedLabels = DatabaseUtil.getClassLabels(testset);
        for (L label : this.labels) {
            joinedLabels.add(label);
        }
        this.labels = joinedLabels.toArray((L[]) new ClassLabel[joinedLabels.size()]);
        Arrays.sort(this.labels);
        split[0] = new TrainingAndTestSet(this.database, testset, labels);
        return split;
    }

    @Override
    public String parameterDescription() {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage("AllTraining puts the complete database in the training set. A testset is expected via a database connection.", false));
        // TODO: available dbcs?
        description.append(dbc.parameterDescription());
        return description.toString();
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        dbc = TESTSET_DATABASE_CONNECTION_PARAM.instantiateClass();

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
