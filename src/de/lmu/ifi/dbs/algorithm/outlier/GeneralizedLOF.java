package de.lmu.ifi.dbs.algorithm.outlier;


import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.GeneralizedLOFResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.Iterator;
import java.util.List;

/**
 * Algorithm to compute density-based local outlier factors in a database based
 * on a specified parameter minpts.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class GeneralizedLOF<O extends DatabaseObject> extends DistanceBasedAlgorithm<O, DoubleDistance>
{

    /**
     * The default reachability distance function.
     */
    public static final String DEFAULT_REACHABILITY_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

    public static final String REACHABILITY_DISTANCE_FUNCTION_P = "reachdistfunction";

    public static final String REACHABILITY_DISTANCE_FUNCTION_D = "The distance function to determine the reachability distance between database objects " + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DistanceFunction.class) + ". Default: " + DEFAULT_REACHABILITY_DISTANCE_FUNCTION;

    /**
     * The reachability distance function.
     */
    private DistanceFunction<O, DoubleDistance> reachabilityDistanceFunction;

    /**
     * Parameter minimum points.
     */
    public static final String MINPTS_P = "minpts";

    /**
     * Description for parameter minimum points.
     */
    public static final String MINPTS_D = "positive number of nearest neighbors of an object to be considered for computing its LOF.";

    /**
     * Minimum points.
     */
    int minpts;

    /**
     * Provides the result of the algorithm.
     */
    GeneralizedLOFResult<O> result;

    /**
     * Sets minimum points to the optionhandler additionally to the parameters
     * provided by super-classes.
     */
    public GeneralizedLOF()
    {
        super();
        ClassParameter reachabilityDistance = new ClassParameter(REACHABILITY_DISTANCE_FUNCTION_P, REACHABILITY_DISTANCE_FUNCTION_D, DistanceFunction.class);
        reachabilityDistance.setDefaultValue(DEFAULT_REACHABILITY_DISTANCE_FUNCTION);
        optionHandler.put(DISTANCE_FUNCTION_P, reachabilityDistance);
        //parameter minpts
        optionHandler.put(MINPTS_P, new IntParameter(MINPTS_P, MINPTS_D, new GreaterConstraint(0)));
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(Database)
     */
    protected void runInTime(Database<O> database) throws IllegalStateException
    {
        getDistanceFunction().setDatabase(database, isVerbose(), isTime());
        reachabilityDistanceFunction.setDatabase(database, isVerbose(), isTime());
        if(isVerbose())
        {
            verbose("\n##### Computing LOFs:");
        }

        {// compute neighbors of each db object
            if(isVerbose())
            {
                verbose("\ncomputing neighborhoods");
            }
            Progress progressNeighborhoods = new Progress("LOF", database.size());
            int counter = 1;
            for(Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++)
            {
                Integer id = iter.next();
                List<QueryResult<DoubleDistance>> neighbors = database.kNNQueryForID(id, minpts + 1, getDistanceFunction());
                neighbors.remove(0);
                database.associate(AssociationID.NEIGHBORS, id, neighbors);
                if(isVerbose())
                {
                    progressNeighborhoods.setProcessed(counter);
                    progress(progressNeighborhoods);
                }
            }
            if(isVerbose())
            {
                verbose("");
            }
        }

        {// computing LRDs
            if(isVerbose())
            {
                verbose("\ncomputing LRDs");
            }
            Progress lrdsProgress = new Progress("LRD", database.size());
            int counter = 1;
            for(Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++)
            {
                Integer id = iter.next();
                double sum = 0;
                List<QueryResult<DoubleDistance>> neighbors = database.kNNQueryForID(id, minpts + 1, reachabilityDistanceFunction);
                for(Iterator<QueryResult<DoubleDistance>> neighbor = neighbors.iterator(); neighbor.hasNext();)
                {
                    sum += neighbor.next().getDistance().getDoubleValue();
                }
                Double lrd = neighbors.size() / sum;
                database.associate(AssociationID.LRD, id, lrd);
                if(isVerbose())
                {
                    lrdsProgress.setProcessed(counter);
                    progress(lrdsProgress);
                }
            }
            if(isVerbose())
            {
                verbose("");
            }
        }
        
        {// compute LOF of each db object
            if(isVerbose())
            {
                verbose("\ncomputing LOFs");
            }
            {
                Progress progressLOFs = new Progress("LOF for objects", database.size());
                int counter = 1;
                for(Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++)
                {
                    Integer id = iter.next();
                    //computeLOF(database, id);
                    Double lrd = (Double) database.getAssociation(AssociationID.LRD, id);
                    List<QueryResult<DoubleDistance>> neighbors = (List<QueryResult<DoubleDistance>>) database.getAssociation(AssociationID.NEIGHBORS, id);
                    double sum = 0;
                    for(Iterator<QueryResult<DoubleDistance>> neighbor = neighbors.iterator(); neighbor.hasNext();)
                    {
                        sum += (Double) database.getAssociation(AssociationID.LRD, neighbor.next().getID()) / lrd;
                    }
                    Double lof = neighbors.size() / sum;
                    database.associate(AssociationID.LOF, id, lof);
                    if(isVerbose())
                    {
                        progressLOFs.setProcessed(counter);
                        progress(progressLOFs);
                    }
                }
                if(isVerbose())
                {
                    verbose("");
                }
            }
            result = new GeneralizedLOFResult<O>(database);
        }
    }


    /**
     * @see Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description("GeneralizedLOF", "Generalized Local Outlier Factor", "Algorithm to compute density-based local outlier factors in a database based on the parameter " + MINPTS_P +  " and different distance functions", "unpublished");
    }

    /**
     * Sets the parameters minpts additionally to the parameters set by the
     * super-class method.
     *
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = super.setParameters(args);

        // minpts
        minpts = (Integer) optionHandler.getOptionValue(MINPTS_P);
        // reachabilityDistanceFunction
        reachabilityDistanceFunction = (DistanceFunction<O,DoubleDistance>) optionHandler.getOptionValue(REACHABILITY_DISTANCE_FUNCTION_P);        
        
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public Result<O> getResult()
    {
        return result;
    }

    /**
     * Returns the parameter setting of this algorithm.
     *
     * @return the parameter setting of this algorithm
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();

        AttributeSettings mySettings = attributeSettings.get(0);
        mySettings.addSetting(MINPTS_P, Integer.toString(minpts));
        mySettings.addSetting(REACHABILITY_DISTANCE_FUNCTION_P, reachabilityDistanceFunction.getClass().getName());
        
        return attributeSettings;
    }

}
