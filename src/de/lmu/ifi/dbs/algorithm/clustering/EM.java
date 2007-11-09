package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.algorithm.result.clustering.EMClusters;
import de.lmu.ifi.dbs.algorithm.result.clustering.EMModel;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.data.SimpleClassLabel;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Provides the EM algorithm (clustering by expectation maximization).
 * 
 * Initialization is implemented as
 * random initialization of means (uniformly distributed within the attribute ranges
 * of the given database)
 * and initial zero-covariance and variance=1 in covariance matrices.
 * 
 * @param <V> a type of {@link RealVector RealVector} as a suitable datatype for this algorithm
 * 
 *  
 * @author Arthur Zimek
 */
public class EM<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> implements Clustering<V>
{
    /**
     * Small value to increment diagonally of a matrix
     * in order to avoid singularity befor building the inverse.
     */
    private static final double SINGULARITY_CHEAT = 1E-9;
    
    /**
     * Parameter k.
     */
    public static final String K_P = "k";

    /**
     * Description for parameter k.
     */
    public static final String K_D = "k - the number of clusters to find (positive integer)";

    /**
     * Parameter for k.
     * Constaint greater 0.
     */
    private static final IntParameter K_PARAM = new IntParameter(K_P, K_D, new GreaterConstraint(0));
    
    /**
     * Keeps k - the number of clusters to find.
     */
    private int k;
    
    /**
     * Parameter delta.
     */
    public static final String DELTA_P = "delta";
    
    /**
     * Description for parameter delta.
     */
    public static final String DELTA_D = "delta - the termination criterion for maximization of E(M): E(M) - E(M') < delta";
    
    /**
     * Parameter for delta.
     * GreaterEqual 0.0.
     * (Default: 0.0).
     */
    private static final DoubleParameter DELTA_PARAM = new DoubleParameter(DELTA_P, DELTA_D, new GreaterEqualConstraint(0.0));
    static{
        DELTA_PARAM.setDefaultValue(0.0);
    }
    
    /**
     * Keeps delta - a small value as termination criterion in expectation maximization
     */
    private double delta;

    /**
     * Keeps the result.
     */
    private EMClusters<V> result;

    /**
     * Provides the EM algorithm (clustering by expectation maximization).
     */
    public EM()
    {
        super();
        optionHandler.put(K_P, K_PARAM);
        optionHandler.put(DELTA_P, DELTA_PARAM);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void runInTime(Database<V> database) throws IllegalStateException
    {
        if(database.size() == 0)
        {
            throw new IllegalArgumentException("database empty: must contain elements");
        }
        // initial models
        if(isVerbose())
        {
            verbose("initializing "+k+" models");
        }
        List<V> means = initialMeans(database);
        List<Matrix> covarianceMatrices = new ArrayList<Matrix>(k);
        List<Double> normDistrFactor = new ArrayList<Double>(k);
        List<Matrix> invCovMatr = new ArrayList<Matrix>(k);
        List<Double> clusterWeights = new ArrayList<Double>(k);
        int dimensionality = means.get(0).getDimensionality();
        for(int i = 0; i < k; i++)
        {
            Matrix m = Matrix.identity(dimensionality, dimensionality);
            covarianceMatrices.add(m);
            normDistrFactor.add(1.0 / Math.sqrt(Math.pow(2*Math.PI, dimensionality) * m.det()));
            invCovMatr.add(m.inverse());
            clusterWeights.add(1.0/k);
            if(this.debug && false)
            {
                StringBuffer msg = new StringBuffer();
                msg.append("\nmodel "+i+":\n");
                msg.append(" mean:    "+means.get(i)+"\n");
                msg.append(" m:\n"+m.toString("        ")+"\n");
                msg.append(" m.det(): "+m.det()+"\n");
                msg.append(" cluster weight: "+clusterWeights.get(i)+"\n");
                msg.append(" normDistFact:   "+normDistrFactor.get(i)+"\n");
                debugFine(msg.toString());
            }
        }
        assignProbabilitiesToInstances(database, normDistrFactor, means, invCovMatr, clusterWeights);
        double emNew = expectationOfMixture(database);
        
        // iteration unless no change
        if(isVerbose())
        {
            verbose("iterating EM");
        }
        double em;
        int it = 0;
        do{
            it++;
            if(isVerbose())
            {
                verbose("iteration "+it+" - expectation value: "+emNew);
            }
            em = emNew;
            
            // recompute models
            List<V> meanSums = new ArrayList<V>(k);
            double[] sumOfClusterProbabilities = new double[k];
            
            for(int i = 0; i < k; i++)
            {
                clusterWeights.set(i, 0.0);
                meanSums.add(means.get(i).nullVector());
                covarianceMatrices.set(i, Matrix.zeroMatrix(dimensionality));
            }
            
            // weights and means
            for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
            {
                Integer id = iter.next();
                List<Double> clusterProbabilities = (List<Double>) database.getAssociation(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id);
                
                for(int i = 0; i < k; i++)
                {
                    sumOfClusterProbabilities[i] += clusterProbabilities.get(i);
                    V summand = database.get(id).multiplicate(clusterProbabilities.get(i));
                    V currentMeanSum = meanSums.get(i).plus(summand);
                    meanSums.set(i, currentMeanSum);
                }
            }
            int n = database.size();
            for(int i = 0; i < k; i++)
            {
                clusterWeights.set(i, sumOfClusterProbabilities[i] / n);
                V newMean = meanSums.get(i).multiplicate(1 / sumOfClusterProbabilities[i]);
                means.set(i, newMean);
            }
            // covariance matrices
            for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
            {
                Integer id = iter.next();
                List<Double> clusterProbabilities = (List<Double>) database.getAssociation(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id);
                V instance = database.get(id);
                for(int i = 0; i < k; i++)
                {
                    V difference = instance.plus(means.get(i).negativeVector());
                    Matrix newCovMatr = covarianceMatrices.get(i).plus(difference.getColumnVector().times(difference.getRowVector()).times(clusterProbabilities.get(i)));
                    newCovMatr = newCovMatr.cheatToAvoidSingularity(SINGULARITY_CHEAT);
                    covarianceMatrices.set(i, newCovMatr);
                }
            }
            for(int i = 0; i < k; i++)
            {
                covarianceMatrices.set(i,covarianceMatrices.get(i).times(1 / sumOfClusterProbabilities[i]));
            }
            for(int i = 0; i < k; i++)
            {
                normDistrFactor.set(i,1.0 / Math.sqrt(Math.pow(2*Math.PI, dimensionality) * covarianceMatrices.get(i).det()));
                invCovMatr.set(i,covarianceMatrices.get(i).inverse());
            }            
            // reassign probabilities
            assignProbabilitiesToInstances(database, normDistrFactor, means, invCovMatr, clusterWeights);
            
            // new expectation
            emNew = expectationOfMixture(database);
            
        }while(Math.abs(em - emNew) > delta);
        if(isVerbose())
        {
            verbose("\nassigning clusters");
        }
        
        // fill result with clusters and models
        List<List<Integer>> hardClusters = new ArrayList<List<Integer>>(k);
        for(int i = 0; i < k; i++)
        {
            hardClusters.add(new LinkedList<Integer>());
        }
        
        // provide a hard clustering
        for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
        {
            Integer id = iter.next();
            List<Double> clusterProbabilities = (List<Double>) database.getAssociation(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id);
            int maxIndex = 0;
            double currentMax = 0.0;
            for(int i = 0; i < k; i++)
            {
                if(clusterProbabilities.get(i) > currentMax)
                {
                    maxIndex = i;
                    currentMax = clusterProbabilities.get(i);
                }
            }
            hardClusters.get(maxIndex).add(id);
        }
        Integer[][] resultClusters = new Integer[k][];
        for(int i = 0; i < k; i++)
        {
            resultClusters[i] = hardClusters.get(i).toArray(new Integer[hardClusters.get(i).size()]);
        }
        result = new EMClusters<V>(resultClusters, database);
        result.associate(SimpleClassLabel.class);
        // provide models within the result
        for(int i = 0; i < k; i++)
        {
            SimpleClassLabel label = new SimpleClassLabel();
            label.init(result.canonicalClusterLabel(i));
            result.appendModel(label, new EMModel<V>(database, means.get(i), covarianceMatrices.get(i)));
        }
    }
    
    /**
     * Assigns the current probability values to the instances in the database.
     *  
     * @param database the database used for assignment to instances
     * @param normDistrFactor normalization factor for density function, based on current covariance matrix
     * @param means the current means
     * @param invCovMatr the inverse covariance matrices
     * @param clusterWeights the weights of the current clusters
     */
    @SuppressWarnings("unchecked")
    protected void assignProbabilitiesToInstances(Database<V> database, List<Double> normDistrFactor, List<V> means, List<Matrix> invCovMatr, List<Double> clusterWeights)
    {
        Iterator<Integer> databaseIterator = database.iterator();
        while(databaseIterator.hasNext())
        {
            Integer id = databaseIterator.next();
            V x = database.get(id);
            List<Double> probabilities = new ArrayList<Double>(k);
            for(int i = 0; i < k; i++)
            {
                V difference = x.plus(means.get(i).negativeVector());
                Matrix differenceRow = difference.getRowVector();
                Matrix differenceCol = difference.getColumnVector();
                Matrix rowTimesCov = differenceRow.times(invCovMatr.get(i));
                Matrix rowTimesCovTimesCol = rowTimesCov.times(differenceCol); 
                double power = rowTimesCovTimesCol.get(0, 0) / 2.0;
                double prob = normDistrFactor.get(i) * Math.exp(-power);
                if(debug && false)
                {
                    debugFine("\n" +
                              " difference vector= ( "+difference.toString()+" )\n" +
                              " differenceRow:\n"+differenceRow.toString("    ")+"\n" +
                              " differenceCol:\n"+differenceCol.toString("    ")+"\n" +
                              " rowTimesCov:\n"+rowTimesCov.toString("    ")+"\n"+
                              " rowTimesCovTimesCol:\n"+rowTimesCovTimesCol.toString("    ")+"\n"+
                              " power= "+power+"\n" +
                              " prob="+prob+"\n"+
                              " inv cov matrix: \n"+invCovMatr.get(i).toString("     "));
                }
                
                probabilities.add(prob);
            }
            database.associate(AssociationID.PROBABILITY_X_GIVEN_CLUSTER_I, id, probabilities);
            double priorProbability = 0.0;
            for(int i = 0; i < k; i++)
            {
                priorProbability += probabilities.get(i) * clusterWeights.get(i);
            }
            database.associate(AssociationID.PROBABILITY_X, id, priorProbability);
            List<Double> clusterProbabilities = new ArrayList<Double>(k);
            for(int i = 0; i < k; i++)
            {
                clusterProbabilities.add(probabilities.get(i) / priorProbability * clusterWeights.get(i));
            }
            database.associate(AssociationID.PROBABILITY_CLUSTER_I_GIVEN_X, id, clusterProbabilities);
        }
    }
    
    /**
     * The expectation value of the current mixture of distributions.
     * 
     * Computed as the sum of the logarithms of the prior probability of each instance.
     * 
     * @param database the database where the prior probability of each instance is associated
     * @return the expectation value of the current mixture of distributions
     */
    protected double expectationOfMixture(Database<V> database)
    {
        double sum = 0.0;
        for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
        {
            Integer id = iter.next();
            double priorProbX = (Double) database.getAssociation(AssociationID.PROBABILITY_X, id);
            double logP = Math.log(priorProbX);
            sum += logP;
            if(debug && false)
            {
                debugFine("\nid="+id+"\nP(x)="+priorProbX+"\nlogP="+logP+"\nsum="+sum);
            }             
        }
        return sum;
    }

    /**
     * Creates {@link #k k} random points distributed uniformly within the
     * attribute ranges of the given database.
     * 
     * @param database the database must contain enough points in order to
     *        ascertain the range of attribute values. Less than two points
     *        would make no sense. The content of the database is not touched
     *        otherwise.
     * @return a list of {@link #k k} random points distributed uniformly within
     *         the attribute ranges of the given database
     */
    protected List<V> initialMeans(Database<V> database)
    {
        Random random = new Random();
        if(database.size() > 0)
        {
            // needs normalization to ensure the randomly generated means
            // are in the same range as the vectors in the database
            // XXX perhaps this can be done more conveniently?
            V randomBase = database.get(database.iterator().next());
            AttributeWiseRealVectorNormalization<V> normalization = new AttributeWiseRealVectorNormalization<V>();
            List<V> list = new ArrayList<V>(database.size());
            for(Iterator<Integer> dbIter = database.iterator(); dbIter.hasNext();)
            {
                list.add(database.get(dbIter.next()));
            }
            try
            {
                normalization.normalize(list);
            }
            catch(NonNumericFeaturesException e)
            {
                warning(e.getMessage());
            }
            List<V> means = new ArrayList<V>(k);
            if(isVerbose())
            {
                verbose("initializing random vectors");
            }
            for(int i = 0; i < k; i++)
            {
                V randomVector = randomBase.randomInstance(random);
                try
                {
                    means.add(normalization.restore(randomVector));
                }
                catch(NonNumericFeaturesException e)
                {
                    warning(e.getMessage());
                    means.add(randomVector);
                }
            }
            return means;
        }
        else
        {
            return new ArrayList<V>(0);
        }
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description("EM-Clustering", "Clustering by Expectation Maximization", "Provides k Gaussian mixtures maximizing the probability of the given data", "A. P. Dempster, N. M. Laird, D. B. Rubin: Maximum Likelihood from Incomplete Data via the EM algorithm. In Journal of the Royal Statistical Society, Series B, 39(1), pp. 1-31");
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public Clusters<V> getResult()
    {
        return this.result;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = super.setParameters(args);

        k = optionHandler.getParameterValue(K_PARAM);

        delta = optionHandler.getParameterValue(DELTA_PARAM);
        
        setParameters(args, remainingParameters);
        return remainingParameters;
    }
}
