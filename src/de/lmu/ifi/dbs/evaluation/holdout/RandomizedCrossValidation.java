package de.lmu.ifi.dbs.evaluation.holdout;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RandomizedCrossValidationHoldout provides a set of partitions of a database
 * to perform cross-validation. The test sets are not guaranteed to be disjoint.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class RandomizedCrossValidation<O extends DatabaseObject> extends
        RandomizedHoldout<O>
{
    /**
     * Parameter n for the number of folds.
     */
    public static final String N_P = "nfold";

    /**
     * Default number of folds.
     */
    public static final int N_DEFAULT = 10;

    /**
     * Description of the parameter n.
     */
    public static final String N_D = "<int>number of folds for cross-validation";

    /**
     * Holds the number of folds.
     */
    protected int nfold = N_DEFAULT;

    /**
     * Provides a holdout for n-fold cross-validation. Additionally to the
     * parameter seed, the parameter n is set.
     */
    public RandomizedCrossValidation()
    {
        super();
        parameterToDescription.put(N_P + OptionHandler.EXPECTS_VALUE, N_D);

        optionHandler = new OptionHandler(parameterToDescription,
                RandomizedCrossValidation.class.getName());
    }

    /**
     * Provides a set of n partitions of a database to perform n-fold
     * cross-validation.
     * 
     * @see Holdout#partition(de.lmu.ifi.dbs.database.Database)
     */
    public TrainingAndTestSet<O>[] partition(Database<O> database)
    {
        this.database = database;
        setClassLabels(database);
        // noinspection unchecked
        TrainingAndTestSet<O>[] partitions = new TrainingAndTestSet[nfold];
        List<Integer> ids = database.getIDs();
        for (int i = 0; i < nfold; i++)
        {
            List<Integer> training = new ArrayList<Integer>();
            List<Integer> test = new ArrayList<Integer>();
            for (Integer id : ids)
            {
                if (random.nextInt(nfold) < nfold - 1)
                {
                    training.add(id);
                } else
                {
                    test.add(id);
                }
            }
            Map<Integer, List<Integer>> partition = new HashMap<Integer, List<Integer>>();
            partition.put(0, training);
            partition.put(1, test);
            try
            {
                Map<Integer, Database<O>> part = database.partition(partition);
                partitions[i] = new TrainingAndTestSet<O>(part.get(0), part
                        .get(1), this.labels);
            } catch (UnableToComplyException e)
            {
                throw new RuntimeException(e);
            }
        }
        return partitions;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        return "Provides an n-fold cross-validation holdout.";
    }

    /**
     * Sets the parameter n additionally to the parameters set by
     * {@link RandomizedHoldout#setParameters(String[]) RandomizedHoldout.setParameters(args)}.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = super.setParameters(args);

        if (optionHandler.isSet(N_P))
        {
            String nfoldString = optionHandler.getOptionValue(N_P);
            try
            {
                nfold = Integer.parseInt(nfoldString);
                if (nfold < 1)
                {
                    throw new WrongParameterValueException(N_P, nfoldString,
                            N_D);
                }
            } catch (NumberFormatException e)
            {
                throw new WrongParameterValueException(N_P, nfoldString, N_D, e);
            }
        }
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * @see RandomizedHoldout#getAttributeSettings()
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> attributeSettings = super
                .getAttributeSettings();

        AttributeSettings mySettings = attributeSettings.get(0);
        mySettings.addSetting(N_P, Integer.toString(nfold));

        return attributeSettings;
    }
}
