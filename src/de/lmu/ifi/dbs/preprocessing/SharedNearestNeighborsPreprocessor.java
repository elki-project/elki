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
 * @author Arthur Zimek
 */
public class SharedNearestNeighborsPreprocessor<O extends DatabaseObject, D extends Distance<D>> extends AbstractParameterizable implements Preprocessor<O>
{
    public static final String NUMBER_OF_NEIGHBORS_P = "sharedNearestNeighbors";
    
    public static final String NUMBER_OF_NEIGHBORS_D = "number of nearest neighbors to consider (at least 1)";
    
    private static final IntParameter NUMBER_OF_NEIGHBORS_PARAM = new IntParameter(NUMBER_OF_NEIGHBORS_P,NUMBER_OF_NEIGHBORS_D,new GreaterEqualConstraint(1));
    static
    {
        NUMBER_OF_NEIGHBORS_PARAM.setDefaultValue(1);
    }
    
    public static final ClassParameter<DistanceFunction> DISTANCE_FUNCTION_PARAM = new ClassParameter("SNNDistanceFunction", "the distance function to asses the nearest neighbors", DistanceFunction.class);
    static
    {
        DISTANCE_FUNCTION_PARAM.setDefaultValue(EuklideanDistanceFunction.class.getName());
    }
    
    private int numberOfNeighbors;
    
    private DistanceFunction<O,D> distanceFunction;
    
    
    public SharedNearestNeighborsPreprocessor()
    {
        super();
        addOption(NUMBER_OF_NEIGHBORS_PARAM);
        addOption(DISTANCE_FUNCTION_PARAM);
    }
    
    /**
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
    
    public AssociationID<SortedSet> getAssociationID()
    {
        return AssociationID.SHARED_NEAREST_NEIGHBORS_SET;
    }

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
