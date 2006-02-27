package de.lmu.ifi.dbs.algorithm.classifier;

import de.lmu.ifi.dbs.algorithm.clustering.DependencyDerivator;
import de.lmu.ifi.dbs.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class CorrelationBasedClassifier <D extends Distance<D>> extends AbstractClassifier<DoubleVector>
{

    private DependencyDerivator<D> dependencyDerivator;
    
    private CorrelationAnalysisSolution[] model;
    
    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.classifier.Classifier#buildClassifier(de.lmu.ifi.dbs.database.Database, de.lmu.ifi.dbs.data.ClassLabel[])
     */
    public void buildClassifier(Database<DoubleVector> database, ClassLabel[] classLabels) throws IllegalStateException
    {
        this.setLabels(classLabels);
        model = new CorrelationAnalysisSolution[classLabels.length];
        Map<Integer,List<Integer>> partitions = new Hashtable<Integer,List<Integer>>();
        for(int i = 0; i < this.getLabels().length; i++)
        {
            partitions.put(i, new ArrayList<Integer>());
        }
        for(Iterator<Integer> dbIterator = database.iterator(); dbIterator.hasNext();)
        {
            Integer id = dbIterator.next();
            Integer classID = Arrays.binarySearch(this.getLabels(),database.getAssociation(CLASS, id));
            partitions.get(classID).add(id);
        }
        try
        {
            Map<Integer,Database<DoubleVector>> clusters = database.partition(partitions);
            for(Iterator<Integer> clusterIterator = clusters.keySet().iterator(); clusterIterator.hasNext();)
            {
                Integer classID = clusterIterator.next();
                Database<DoubleVector> cluster = clusters.get(classID);
                dependencyDerivator.run(cluster);
                model[classID] = dependencyDerivator.getResult();
                // TODO ...
            }
        }
        catch(UnableToComplyException e)
        {
            IllegalStateException ise = new IllegalStateException(e);
            ise.fillInStackTrace();
            throw ise;
        }
        
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.classifier.Classifier#classDistribution(de.lmu.ifi.dbs.data.DatabaseObject)
     */
    public double[] classDistribution(DoubleVector instance) throws IllegalStateException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.classifier.Classifier#model()
     */
    public String model()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<AttributeSettings> getAttributeSettings()
    {
        // TODO Auto-generated method stub
        return super.getAttributeSettings();
    }

    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        // TODO Auto-generated method stub
        // pass parameters to DependencyDerivator
        return super.setParameters(args);
    }

    @Override
    public String description()
    {
        // TODO Auto-generated method stub
        return super.description();
    }

}
