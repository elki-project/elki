package de.lmu.ifi.dbs.algorithm.classifier;

import de.lmu.ifi.dbs.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * TODO comment
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class CorrelationBasedClassifier <D extends Distance<D>> extends AbstractClassifier<DoubleVector>
{

    /**
     * Generated serial version UID.
     */
    private static final long serialVersionUID = -6786297567169490313L;

    private DependencyDerivator<D> dependencyDerivator = new DependencyDerivator<D>();
    
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
            List<Integer> keys = new ArrayList<Integer>(clusters.keySet());
            Collections.sort(keys);
            for(Iterator<Integer> clusterIterator = keys.iterator(); clusterIterator.hasNext();)
            {
                Integer classID = clusterIterator.next();
                if(isVerbose())
                {
                    System.out.println("Deriving model for class "+this.getClassLabel(classID).toString());
                }                
                Database<DoubleVector> cluster = clusters.get(classID);
                dependencyDerivator.run(cluster);
                model[classID] = dependencyDerivator.getResult();
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
     * Provides the Normally distributed probability density value for
     * a given value distance and a given &sigma;.
     * &mu; is assumed as 0.
     * 
     * 
     * @param distance the distance to assess the probability of
     * @param sigma the standard deviation of the underlying distribution
     * @return the density for the given distance and sigma
     */
    protected double density(double distance, double sigma)
    {
        double distanceDivSigma = distance/sigma; 
        double density = Math.pow(Math.E,(distanceDivSigma*distanceDivSigma*-0.5)) / (sigma * Math.sqrt(2 * Math.PI));
        return density;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.classifier.Classifier#classDistribution(de.lmu.ifi.dbs.data.DatabaseObject)
     */
    public double[] classDistribution(DoubleVector instance) throws IllegalStateException
    {
        double[] distribution = new double[this.model.length];
        double sumOfDensities = 0.0;
        for(int i = 0; i < distribution.length; i++)
        {
            double distance = model[i].distance(instance);
            distribution[i] = density(distance, model[i].getStandardDeviation());
            sumOfDensities += distribution[i];
        }
        for(int i = 0; i < distribution.length; i++)
        {
            distribution[i] /= sumOfDensities;
        }
        return distribution;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.classifier.Classifier#model()
     */
    public String model()
    {
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(stream);
        for(int classID = 0; classID < model.length; classID++)
        {
            CorrelationAnalysisSolution model_i = model[classID];        
            try
            {
                printStream.print("Model for class ");
                printStream.println(getClassLabel(classID).toString());
                model_i.output(printStream, null, dependencyDerivator.getAttributeSettings());
            }
            catch(UnableToComplyException e)
            {
                e.printStackTrace();
            }
        }
        return stream.toString();
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        // TODO
        return new Description("CorrelationBasedClassifier","CorrelationBasedClassifier","...","unpublished");
    }

    /**
     * Returns the AttributeSettings of CorrelationBasedClassifier
     * and of the inherent DependencyDerivator.
     * 
     * @see de.lmu.ifi.dbs.algorithm.classifier.AbstractClassifier#getAttributeSettings()
     */
    @Override
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> settings = super.getAttributeSettings();
        settings.addAll(dependencyDerivator.getAttributeSettings());
        return settings;
    }

    /**
     * Sets the parameters required by {@link CorrelationBasedClassifier CorrelationBasedClassifier}
     * and passes the remaining parameters to the inherent
     * {@link DependencyDerivator DependencyDerivator}.
     * 
     * @see de.lmu.ifi.dbs.algorithm.classifier.AbstractClassifier#setParameters(java.lang.String[])
     */
    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        
        remainingParameters = dependencyDerivator.setParameters(remainingParameters);
        dependencyDerivator.setTime(this.isTime());
        dependencyDerivator.setVerbose(this.isVerbose());
        return remainingParameters;
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#description()
     */
    @Override
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(super.description());
        description.append(dependencyDerivator.description());
        return description.toString();
    }

}
