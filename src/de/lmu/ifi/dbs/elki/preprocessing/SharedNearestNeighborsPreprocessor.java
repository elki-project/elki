package de.lmu.ifi.dbs.elki.preprocessing;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

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
     * Parameter to indicate the number of neighbors to be taken into account for the shared-nearest-neighbor similarity.
     * <p/>
     * <p>Default value: 1</p>
     * <p>Key: {@code sharedNearestNeighbors}</p>
     */
    public static final IntParameter NUMBER_OF_NEIGHBORS_PARAM = new IntParameter("sharedNearestNeighbors", "number of nearest neighbors to consider (at least 1)", new GreaterEqualConstraint(1));

    static {
        NUMBER_OF_NEIGHBORS_PARAM.setDefaultValue(1);
    }

    /**
     * Parameter to indicate the distance function to be used to ascertain the nearest neighbors.
     * <p/>
     * <p>Default value: {@link EuclideanDistanceFunction}</p>
     * <p>Key: {@code SNNDistanceFunction}</p>
     */
    public final ClassParameter<DistanceFunction<O,D>> DISTANCE_FUNCTION_PARAM =
      new ClassParameter<DistanceFunction<O,D>>("SNNDistanceFunction",
          "the distance function to asses the nearest neighbors", DistanceFunction.class,
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
        if (verbose) {
            verbose("Assigning nearest neighbor lists to database objects");
        }
        Progress preprocessing = new Progress("assigning nearest neighbor lists", database.size());
        int count = 0;
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            count++;
            Integer id = iter.next();
            List<Integer> neighbors = new ArrayList<Integer>(numberOfNeighbors);
            List<QueryResult<D>> kNN = database.kNNQueryForID(id, numberOfNeighbors, distanceFunction);
            for (int i = 1; i < kNN.size(); i++) {
                neighbors.add(kNN.get(i).getID());
            }
            SortedSet<Integer> set = new TreeSet<Integer>(neighbors);
            database.associate(getAssociationID(), id, set);
            if (verbose) {
                preprocessing.setProcessed(count);
                progress(preprocessing);
            }
        }
        if (verbose) {
            verbose();
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
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        // number of neighbors
        numberOfNeighbors = NUMBER_OF_NEIGHBORS_PARAM.getValue();

        // distance function
        distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass();
        remainingParameters = distanceFunction.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Provides the association id used for annotation of the nearest neighbors.
     *
     * @return the association id used for annotation of the nearest neighbors ({@link AssociationID#SHARED_NEAREST_NEIGHBORS_SET})
     */
    public AssociationID<SortedSet<?>> getAssociationID() {
        return AssociationID.SHARED_NEAREST_NEIGHBORS_SET;
    }

    /**
     * Provides a short description of the purpose of this class.
     */
    @Override
    public String parameterDescription() {
        StringBuffer description = new StringBuffer();
        description.append(SharedNearestNeighborsPreprocessor.class.getName());
        description.append(" computes the k nearest neighbors of objects of a certain database.\n");
        description.append(super.parameterDescription());
        return description.toString();
    }


}
