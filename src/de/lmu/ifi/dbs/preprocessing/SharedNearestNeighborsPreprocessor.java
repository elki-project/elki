package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A preprocessor for annotation of the ids of nearest neighbors to each database object.
 * 
 * The k nearest neighbors are assigned based on an arbitrary distance function.
 * 
 * The association is annotated using the association id {@link AssociationID#SHARED_NEAREST_NEIGHBORS_SET}. 
 * 
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 * 
 * @author Arthur Zimek
 */
public class SharedNearestNeighborsPreprocessor<O extends DatabaseObject, D extends Distance<D>> extends AbstractParameterizable implements Preprocessor<O>
{

    /**
     * Parameter to indicate the number of neighbors to be taken into account for the shared-nearest-neighbor similarity.
     * 
     * <p>Default value: 1</p>
     * <p>Key: {@code sharedNearestNeighbors}</p>
     */
    public static final IntParameter NUMBER_OF_NEIGHBORS_PARAM = new IntParameter("sharedNearestNeighbors","number of nearest neighbors to consider (at least 1)",new GreaterEqualConstraint(1));
    static
    {
        NUMBER_OF_NEIGHBORS_PARAM.setDefaultValue(1);
    }
    
    /**
     * Parameter to indicate the distance function to be used to ascertain the nearest neighbors.
     * 
     * <p>Default value: {@link EuklideanDistanceFunction}</p>
     * <p>Key: {@code SNNDistanceFunction}</p>
     */
    @SuppressWarnings("unchecked")
    public static final ClassParameter<DistanceFunction> DISTANCE_FUNCTION_PARAM = new ClassParameter("SNNDistanceFunction", "the distance function to asses the nearest neighbors", DistanceFunction.class);
    static
    {
        DISTANCE_FUNCTION_PARAM.setDefaultValue(EuklideanDistanceFunction.class.getName());
    }
    
    /**
     * Holds the number of nearest neighbors to be used.
     */
    private int numberOfNeighbors;
    
    /**
     * Hold the distance funciton to be used.
     */
    private DistanceFunction<O,D> distanceFunction;
    
    /**
     * Provides a SharedNearestNeighborPreprocessor.
     *
     */
    public SharedNearestNeighborsPreprocessor()
    {
        super();
        addOption(NUMBER_OF_NEIGHBORS_PARAM);
        addOption(DISTANCE_FUNCTION_PARAM);
    }
    
    /**
     * Annotates the nearest neighbors based on the values of {@link #numberOfNeighbors}
     * and {@link #distanceFunction} to each database object.
     * 
     * @see de.lmu.ifi.dbs.preprocessing.Preprocessor#run(de.lmu.ifi.dbs.database.Database, boolean, boolean)
     */
    public void run(Database<O> database, boolean verbose, boolean time)
    {
        distanceFunction.setDatabase(database, verbose, time);
        if(verbose){
            verbose("Assigning nearest neighbor lists to database objects");
        }
        Progress preprocessing = new Progress("assigning nearest neighbor lists", database.size());
        int count = 0;
        for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
        {
            count++;
            Integer id = iter.next();
            List<Integer> neighbors = new ArrayList<Integer>(numberOfNeighbors);
            List<QueryResult<D>> kNN = database.kNNQueryForID(id, numberOfNeighbors, distanceFunction);
            for(int i = 1; i < kNN.size(); i++)
            {
                neighbors.add(kNN.get(i).getID());
            }
            SortedSet<Integer> set = new TreeSet<Integer>(neighbors);
            database.associate(getAssociationID(), id, set);
            if(verbose)
            {
                preprocessing.setProcessed(count);
                progress(preprocessing);
            }            
        }
        if(verbose)
        {
            verbose();
        }
    }

    /**
     * Sets the parameter values of
     * {@link #NUMBER_OF_NEIGHBORS_PARAM}
     * and {@link #DISTANCE_FUNCTION_PARAM}
     * to {@link #numberOfNeighbors} and
     * {@link #distanceFunction}, respectively.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable#setParameters(java.lang.String[])
     */
    @SuppressWarnings("unchecked")
    @Override
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = super.setParameters(args);
        numberOfNeighbors = getParameterValue(NUMBER_OF_NEIGHBORS_PARAM);
        String distanceFunctionClassName = getParameterValue(DISTANCE_FUNCTION_PARAM);
        try
        {
            distanceFunction = Util.instantiate(DistanceFunction.class, distanceFunctionClassName);
        }
        catch(UnableToComplyException e)
        {
            throw new WrongParameterValueException(DISTANCE_FUNCTION_PARAM.getName(),distanceFunctionClassName,DISTANCE_FUNCTION_PARAM.getDescription(),e);
        }
        remainingParameters = distanceFunction.setParameters(remainingParameters);
        
        return remainingParameters;
    }
    
    /**
     * Provides the association id used for annotation of the nearest neighbors.
     * 
     * 
     * @return the association id used for annotation of the nearest neighbors ({@link AssociationID#SHARED_NEAREST_NEIGHBORS_SET})
     */
    @SuppressWarnings("unchecked")
    public AssociationID<SortedSet> getAssociationID()
    {
        return AssociationID.SHARED_NEAREST_NEIGHBORS_SET;
    }

    /**
     * Provides a short description of the purpose of this class.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable#description()
     */
    @Override
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(SharedNearestNeighborsPreprocessor.class.getName());
        description.append(" computes the k nearest neighbors of objects of a certain database.\n");
        description.append(super.description("", false));
        return description.toString();
    }

    
    
}
