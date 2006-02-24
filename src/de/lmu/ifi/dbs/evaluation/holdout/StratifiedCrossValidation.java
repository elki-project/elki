package de.lmu.ifi.dbs.evaluation.holdout;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A stratified n-fold crossvalidation to distribute the data
 * to n buckets where each bucket exhibits approximately
 * the same distribution of classes as does the complete dataset.
 * The buckets are disjoint. The distribution is deterministic.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class StratifiedCrossValidation<O extends DatabaseObject> extends AbstractHoldout<O>
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
     * Provides a stratified crossvalidation.
     * Setting parameter N_P to the OptionHandler.
     *
     */
    public StratifiedCrossValidation()
    {
        super();
        parameterToDescription.put(N_P+OptionHandler.EXPECTS_VALUE,N_D);

        optionHandler = new OptionHandler(parameterToDescription,StratifiedCrossValidation.class.getName());
  
    }
    /**
     * 
     * @see de.lmu.ifi.dbs.evaluation.holdout.Holdout#partition(de.lmu.ifi.dbs.database.Database)
     */
    public TrainingAndTestSet<O>[] partition(Database<O> database)
    {
        this.database = database;
        setClassLabels(database);
        
        List<Integer>[] classBuckets = new ArrayList[this.labels.length];
        for(int i = 0; i < classBuckets.length; i++)
        {
            classBuckets[i] = new ArrayList<Integer>();
        }
        for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
        {
            Integer id = iter.next();
            int classIndex = Arrays.binarySearch(labels,database.getAssociation(CLASS,id));
            classBuckets[classIndex].add(id);
        }
        List<Integer>[] folds = new ArrayList[nfold];
        for(int i = 0; i < folds.length; i++)
        {
            folds[i] = new ArrayList<Integer>();
        }
        for(List<Integer> bucket : classBuckets)
        {
            for(int i = 0; i < bucket.size(); i++)
            {
                folds[i % nfold].add(bucket.get(i));
            }
        }
        TrainingAndTestSet<O>[] partitions = new TrainingAndTestSet[nfold];
        for(int i = 0; i < nfold; i++)
        {
            Map<Integer,List<Integer>> partition = new HashMap<Integer,List<Integer>>();
            List<Integer> training = new ArrayList<Integer>();
            for(int j = 0; j < nfold; j++)
            {
                if(j!=i)
                {
                    training.addAll(folds[j]);
                }
            }
            partition.put(0,training);
            partition.put(1,folds[i]);
            try
            {
                Map<Integer,Database<O>> part = database.partition(partition);
                partitions[i] = new TrainingAndTestSet<O>(part.get(0),part.get(1),this.labels);
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
        return "Provides a stratified n-fold cross-validation holdout.";
    }
    
    /**
     * Sets the parameter n additionally to the parameters set by
     * {@link AbstractHoldout#setParameters(String[]) AbstractHoldout.setParameters(args)}.
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
    
    /**
     * Adds attribute setting N_P.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> settings = super.getAttributeSettings();
        AttributeSettings attributeSettings = settings.get(0);
        attributeSettings.addSetting(N_P,Integer.toString(nfold));
        settings.add(attributeSettings);
        return settings;
    }
}
