package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;

/**
 * A preprocessor for annotation of the ids of nearest neighbors to each database object.
 * <p/>
 * The k nearest neighbors are assigned based on an arbitrary distance function.
 * <p/>
 * The association is annotated using the association id {@link AssociationID#SHARED_NEAREST_NEIGHBORS_SET}.
 *
 * @author Arthur Zimek
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
public class SharedNearestNeighborsPreprocessor<O extends DatabaseObject, D extends Distance<D>> extends AbstractParameterizable implements Preprocessor<O> {
    /**
     * OptionID for {@link #NUMBER_OF_NEIGHBORS_PARAM}
     */
    public static final OptionID NUMBER_OF_NEIGHBORS_ID = OptionID.getOrCreateOptionID(
        "sharedNearestNeighbors", "number of nearest neighbors to consider (at least 1)");

    /**
     * Parameter to indicate the number of neighbors to be taken into account for the shared-nearest-neighbor similarity.
     * <p/>
     * <p>Default value: 1</p>
     * <p>Key: {@code sharedNearestNeighbors}</p>
     */
    private final IntParameter NUMBER_OF_NEIGHBORS_PARAM = new IntParameter(NUMBER_OF_NEIGHBORS_ID, new GreaterEqualConstraint(1), 1);

    /**
     * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
     */
    public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID(
        "SNNDistanceFunction",
        "the distance function to asses the nearest neighbors");

    /**
     * Parameter to indicate the distance function to be used to ascertain the nearest neighbors.
     * <p/>
     * <p>Default value: {@link EuclideanDistanceFunction}</p>
     * <p>Key: {@code SNNDistanceFunction}</p>
     */
    private final ClassParameter<DistanceFunction<O,D>> DISTANCE_FUNCTION_PARAM =
      new ClassParameter<DistanceFunction<O,D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class,
        EuclideanDistanceFunction.class.getName());

    /**
     * Holds the number of nearest neighbors to be used.
     */
    private int numberOfNeighbors;

    /**
     * Hold the distance function to be used.
     */
    private DistanceFunction<O, D> distanceFunction;

    /**
     * Provides a SharedNearestNeighborPreprocessor.
     */
    public SharedNearestNeighborsPreprocessor() {
        super();
        addOption(NUMBER_OF_NEIGHBORS_PARAM);
        addOption(DISTANCE_FUNCTION_PARAM);
    }

    /**
     * Annotates the nearest neighbors based on the values of {@link #numberOfNeighbors}
     * and {@link #distanceFunction} to each database object.
     */
    public void run(Database<O> database, boolean verbose, boolean time) {
        distanceFunction.setDatabase(database, verbose, time);
        if (logger.isVerbose()) {
          logger.verbose("Assigning nearest neighbor lists to database objects");
        }
        FiniteProgress preprocessing = new FiniteProgress("assigning nearest neighbor lists", database.size());
        int count = 0;
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            count++;
            Integer id = iter.next();
            List<Integer> neighbors = new ArrayList<Integer>(numberOfNeighbors);
            List<DistanceResultPair<D>> kNN = database.kNNQueryForID(id, numberOfNeighbors, distanceFunction);
            for (int i = 1; i < kNN.size(); i++) {
                neighbors.add(kNN.get(i).getID());
            }
            SortedSet<Integer> set = new TreeSet<Integer>(neighbors);
            database.associate(getAssociationID(), id, set);
            if (logger.isVerbose()) {
                preprocessing.setProcessed(count);
                logger.progress(preprocessing);
            }
        }
    }

    /**
     * Sets the parameter values of
     * {@link #NUMBER_OF_NEIGHBORS_PARAM}
     * and {@link #DISTANCE_FUNCTION_PARAM}
     * to {@link #numberOfNeighbors} and
     * {@link #distanceFunction}, respectively.
     */
    @Override
    public List<String> setParameters(List<String> args) throws ParameterException {
        List<String> remainingParameters = super.setParameters(args);
        // number of neighbors
        numberOfNeighbors = NUMBER_OF_NEIGHBORS_PARAM.getValue();

        // distance function
        distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass();
        addParameterizable(distanceFunction);
        remainingParameters = distanceFunction.setParameters(remainingParameters);
        
        rememberParametersExcept(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Provides the association id used for annotation of the nearest neighbors.
     *
     * @return the association id used for annotation of the nearest neighbors ({@link AssociationID#SHARED_NEAREST_NEIGHBORS_SET})
     */
    public AssociationID<SortedSet<Integer>> getAssociationID() {
        return AssociationID.SHARED_NEAREST_NEIGHBORS_SET;
    }

    /**
     * Provides a short description of the purpose of this class.
     */
    @Override
    public String shortDescription() {
        StringBuffer description = new StringBuffer();
        description.append(SharedNearestNeighborsPreprocessor.class.getName());
        description.append(" computes the k nearest neighbors of objects of a certain database.\n");
        return description.toString();
    }

    /**
     * Returns the number of nearest neighbors considered
     * 
     * @return number of neighbors considered
     */
    public int getNumberOfNeighbors() {
      return numberOfNeighbors;
    }

    /**
     * Returns the distance function used by the preprocessor.
     * 
     * @return distance function used.
     */
    public DistanceFunction<O, D> getDistanceFunction() {
      return distanceFunction;
    }
}
