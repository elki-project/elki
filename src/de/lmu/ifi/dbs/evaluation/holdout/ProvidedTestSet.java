package de.lmu.ifi.dbs.evaluation.holdout;


import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Puts all data into the training set and requests a testset via a database connection.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ProvidedTestSet<O extends DatabaseObject> extends AbstractHoldout<O>
{

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
    public static final String TESTSET_DATABASE_CONNECTION_D = "<class>connection to testset database - default: "+DEFAULT_DATABASE_CONNECTION;

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
     * @see de.lmu.ifi.dbs.evaluation.holdout.Holdout#partition(de.lmu.ifi.dbs.database.Database)
     */
    public TrainingAndTestSet<O>[] partition(Database<O> database)
    {
        this.database = database;
        setClassLabels(database);
        TrainingAndTestSet<O>[] split = new TrainingAndTestSet[1];
        Set<ClassLabel> joinedLabels = Util.getClassLabels(testset);
        for(ClassLabel label : this.labels)
        {
            joinedLabels.add(label);
        }
        this.labels = joinedLabels.toArray(new ClassLabel[joinedLabels.size()]);
        Arrays.sort(this.labels);
        split[0] = new TrainingAndTestSet<O>(this.database,testset,labels);
        return split;
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage("AllTraining puts the complete database in the training set. A testset is expected via a database connection.", false));
        // TODO: available dbcs?
        description.append(dbc.description());
        return description.toString();
    }

    /**
     * Returns the given parameter array unchanged, since no parameters are required by this class.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        if(optionHandler.isSet(TESTSET_DATABASE_CONNECTION_P))
        {
            try
            {
                dbc = (DatabaseConnection<O>) Class.forName(optionHandler.getOptionValue(TESTSET_DATABASE_CONNECTION_P)).newInstance();
            }
            catch(UnusedParameterException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(NoParameterValueException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(InstantiationException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(IllegalAccessException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(ClassNotFoundException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
        }
        else
        {
            try
            {
                dbc = (DatabaseConnection<O>) Class.forName(DEFAULT_DATABASE_CONNECTION).newInstance();
            }
            catch(InstantiationException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(IllegalAccessException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
            catch(ClassNotFoundException e)
            {
                IllegalArgumentException iae = new IllegalArgumentException(e);
                iae.fillInStackTrace();
                throw iae;
            }
        }
        remainingParameters = dbc.setParameters(remainingParameters);
        testset = dbc.getDatabase(null);
        return remainingParameters;
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.evaluation.holdout.AbstractHoldout#getAttributeSettings()
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> settings = super.getAttributeSettings();
        AttributeSettings setting = settings.get(0);
        setting.addSetting(TESTSET_DATABASE_CONNECTION_P, dbc.getClass().getName());
        settings.addAll(dbc.getAttributeSettings());
        return settings;
    }

}
