package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CrossValidationHoldout provides a set of partitions of a database to
 * perform cross-validation.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class CrossValidationHoldout<M extends MetricalObject> extends RandomizedHoldout<M>
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
     * Provides a holdout for cross-validation.
     * Additionally to the parameter seed, the parameter n is set.
     */
    public CrossValidationHoldout()
    {
        super();
        parameterToDescription.put(N_P+OptionHandler.EXPECTS_VALUE,N_D);

        optionHandler = new OptionHandler(parameterToDescription,CrossValidationHoldout.class.getName());
    }

    /**
     * Provides a set of partitions of a database to
     * perform cross-validation.
     * 
     * @see de.lmu.ifi.dbs.evaluation.Holdout#partition(de.lmu.ifi.dbs.database.Database)
     */
    public TrainingAndTestSet<M>[] partition(Database<M> database)
    {
        TrainingAndTestSet<M>[] partitions = new TrainingAndTestSet[nfold];
        List<Integer> ids = database.getIDs();
        for(int i = 0; i < nfold; i++)
        {
            List<Integer> training = new ArrayList<Integer>();
            List<Integer> test = new ArrayList<Integer>();
            for(Integer id : ids)
            {
                if(random.nextInt(nfold) < nfold-1)
                {
                    training.add(id);
                }
                else
                {
                    test.add(id);
                }
            }
            Map<Integer,List<Integer>> partition = new HashMap<Integer,List<Integer>>();
            partition.put(0,database.getIDs());
            partition.put(1,new ArrayList<Integer>(0));
            try
            {
                Map<Integer,Database<M>> part = database.partition(partition);
                partitions[i] = new TrainingAndTestSet<M>(part.get(0),part.get(1));
            }
            catch(UnableToComplyException e)
            {
                throw new RuntimeException(e);
            }
        }
        return partitions;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        return "Performs an n-fold cross-validation holdout.";
    }

    /**
     * Sets the parameter n additionally to the parameters set by
     * {@link RandomizedHoldout#setParameters(String[]) RandomizedHoldout.setParameters(args)}.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters =  super.setParameters(args);
        if(optionHandler.isSet(N_P))
        {
            try
            {
                int nfold = Integer.parseInt(optionHandler.getOptionValue(N_P));
                if(nfold<1)
                {
                    throw new NumberFormatException("Parameter "+N_P+" is supposed to be a positiv integer. Found: "+optionHandler.getOptionValue(N_P));
                }
                this.nfold = nfold;
            }
            catch(NumberFormatException e)
            {
                throw new IllegalArgumentException("Parameter "+N_P+" is supposed to be a positiv integer. Found: "+optionHandler.getOptionValue(N_P),e);
            }
        }
        return remainingParameters;
    }
}
