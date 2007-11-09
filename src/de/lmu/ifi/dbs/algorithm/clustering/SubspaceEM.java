package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.data.RealVector;
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
import java.util.List;
import java.util.Random;

/**
 * @author Arthur Zimek
 */
public class SubspaceEM<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> implements Clustering<V>
{
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
     * Stores the result.
     */
    private Clusters<V> result;
    
    /**
     * 
     */
    public SubspaceEM()
    {
        super();
        optionHandler.put(K_P, K_PARAM);
        optionHandler.put(DELTA_P, DELTA_PARAM);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.clustering.Clustering#getResult()
     */
    public ClusteringResult<V> getResult()
    {
        return result;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description("SubspaceEM","SubspaceEM","","");
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
     */
    @Override
    public void runInTime(Database<V> database) throws IllegalStateException
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
        int dimensionality = means.get(0).getDimensionality();
        Matrix[] eigensystems = initialEigensystems(dimensionality);
        Matrix selectionWeak = Matrix.zeroMatrix(dimensionality);
        selectionWeak.set(dimensionality-1, dimensionality-1, 1);
        Matrix selectionStrong = Matrix.unitMatrix(dimensionality);
        selectionStrong.set(dimensionality-1, dimensionality-1, 0);
        
        List<CorrelationAnalysisSolution<V>> solutions = new ArrayList<CorrelationAnalysisSolution<V>>(k);
        for(int i = 0; i < k; i++)
        {
            /*
            // equation system is not really required in solution unless output is requested
            Matrix transposedWeakEigenvectors = eigensystems[i].times(selectionWeak).transpose();
            Matrix vTimesMean = transposedWeakEigenvectors.times(means.get(i).getColumnVector());
            double[][] a = new double[transposedWeakEigenvectors.getRowDimensionality()][transposedWeakEigenvectors.getColumnDimensionality()];
            double[][] we = transposedWeakEigenvectors.getArray();
            double[] b = vTimesMean.getColumn(0).getRowPackedCopy();
            System.arraycopy(we, 0, a, 0, transposedWeakEigenvectors.getRowDimensionality());
            LinearEquationSystem lq = new LinearEquationSystem(a, b);
            lq.solveByTotalPivotSearch();
            */
            CorrelationAnalysisSolution<V> solution = new CorrelationAnalysisSolution<V>(null, database, eigensystems[i].times(selectionStrong),eigensystems[i].times(selectionWeak),selectionWeak,means.get(i).getColumnVector());
            
        }
        
        // TODO Auto-generated method stub

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
    
    protected Matrix[] initialEigensystems(int dimensionality)
    {
        Random random = new Random();
        Matrix[] eigensystems = new Matrix[k];
        for(int i = 0; i < k; i++)
        {
            double[][] vec = new double[dimensionality][1];
            for(int d = 0; d < dimensionality; d++)
            {
                vec[d][0] = random.nextDouble() * 2 - 1;
            }
            Matrix eig = new Matrix(vec);
            eig = eig.completeToOrthonormalBasis();
            eigensystems[i] = eig;
        }
        return eigensystems;
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
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
