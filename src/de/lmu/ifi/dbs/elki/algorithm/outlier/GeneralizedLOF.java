package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.result.AnnotationsFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromAssociation;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * Algorithm to compute density-based local outlier factors in a database based
 * on a specified parameter minpts.
 *
 * @author Peer Kr&ouml;ger
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public class GeneralizedLOF<O extends DatabaseObject> extends DistanceBasedAlgorithm<O, DoubleDistance, MultiResult> {
    /**
     * OptionID for {@link #REACHABILITY_DISTANCE_FUNCTION_PARAM}
     */
    public static final OptionID REACHABILITY_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID(
        "genlof.reachdistfunction",
        "Distance function to determine the reachability distance between database objects."
    );

    /**
     * The distance function to determine the reachability distance between database objects.
     * <p>Default value: {@link EuclideanDistanceFunction} </p>
     * <p>Key: {@code -genlof.reachdistfunction} </p>
     */
    private final ClassParameter<DistanceFunction<O, DoubleDistance>> REACHABILITY_DISTANCE_FUNCTION_PARAM =
        new ClassParameter<DistanceFunction<O, DoubleDistance>>(
            REACHABILITY_DISTANCE_FUNCTION_ID,
            DistanceFunction.class,
            EuclideanDistanceFunction.class.getName());

    /**
     * Holds the instance of the reachability distance function specified by
     * {@link #REACHABILITY_DISTANCE_FUNCTION_PARAM}.
     */
    private DistanceFunction<O, DoubleDistance> reachabilityDistanceFunction;

    /**
     * OptionID for {@link #K_PARAM}
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID(
        "genlof.k",
        "The number of nearest neighbors of an object to be considered for computing its LOF."
    );

    /**
     * Parameter to specify the number of nearest neighbors of an object to be considered for computing its LOF,
     * must be an integer greater than 1.
     * <p>Key: {@code -genlof.k} </p>
     */
    private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));

    /**
     * Holds the value of {@link #K_PARAM}.
     */
    int k;

    /**
     * Provides the result of the algorithm.
     */
    MultiResult result;

    /**
     * Provides the Generalized LOF algorithm,
     * adding parameters
     * {@link #K_PARAM} and {@link #REACHABILITY_DISTANCE_FUNCTION_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public GeneralizedLOF() {
        super();
        //parameter k
        addOption(K_PARAM);
        // parameter reachability distance function
        addOption(REACHABILITY_DISTANCE_FUNCTION_PARAM);
    }

    /**
     * Performs the Generalized LOF algorithm on the given database.
     */
    @Override
    protected MultiResult runInTime(Database<O> database) throws IllegalStateException {
        getDistanceFunction().setDatabase(database, isVerbose(), isTime());
        reachabilityDistanceFunction.setDatabase(database, isVerbose(), isTime());
        if (logger.isVerbose()) {
          logger.verbose("LOF ");
        }

        {// compute neighbors of each db object
            if (logger.isVerbose()) {
              logger.verbose("computing neighborhoods");
            }
            Progress progressNeighborhoods = new Progress("LOF", database.size());
            int counter = 1;
            for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
                Integer id = iter.next();
                List<DistanceResultPair<DoubleDistance>> neighbors = database.kNNQueryForID(id, k + 1, getDistanceFunction());
                neighbors.remove(0);
                database.associate(AssociationID.NEIGHBORS, id, neighbors);
                if (logger.isVerbose()) {
                    progressNeighborhoods.setProcessed(counter);
                    logger.progress(progressNeighborhoods);
                }
            }
        }

        {// computing reach dist function neighborhoods
            if (logger.isVerbose()) {
              logger.verbose("computing neighborhoods for reachability function");
            }
            Progress reachDistNeighborhoodsProgress = new Progress("Reachability DIstance Neighborhoods", database.size());
            int counter = 1;
            for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
                Integer id = iter.next();
                List<DistanceResultPair<DoubleDistance>> neighbors = database.kNNQueryForID(id, k + 1, reachabilityDistanceFunction);
                neighbors.remove(0);
                database.associate(AssociationID.NEIGHBORS_2, id, neighbors);
                if (logger.isVerbose()) {
                    reachDistNeighborhoodsProgress.setProcessed(counter);
                    logger.progress(reachDistNeighborhoodsProgress);
                }
            }
        }
        {// computing LRDs
            if (logger.isVerbose()) {
              logger.verbose("computing LRDs");
            }
            Progress lrdsProgress = new Progress("LRD", database.size());
            int counter = 1;
            for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
                Integer id = iter.next();
                double sum = 0;
                List<DistanceResultPair<DoubleDistance>> neighbors = database.getAssociation(AssociationID.NEIGHBORS_2, id);
                for (DistanceResultPair<DoubleDistance> neighbor : neighbors) {
                    List<DistanceResultPair<DoubleDistance>> neighborsNeighbors = database.getAssociation(AssociationID.NEIGHBORS_2, neighbor.getID());
                    sum += Math.max(neighbor.getDistance().getValue(),
                        neighborsNeighbors.get(neighborsNeighbors.size() - 1).getDistance().getValue());
                }
                Double lrd = neighbors.size() / sum;
                database.associate(AssociationID.LRD, id, lrd);
                if (logger.isVerbose()) {
                    lrdsProgress.setProcessed(counter);
                    logger.progress(lrdsProgress);
                }
            }
        }
        // XXX: everything here appears to be stupid
        {// compute LOF of each db object
            if (logger.isVerbose()) {
              logger.verbose("computing LOFs");
            }

            Progress progressLOFs = new Progress("LOF for objects", database.size());
            int counter = 1;
            for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
                Integer id = iter.next();
                //computeLOF(database, id);
                Double lrd = database.getAssociation(AssociationID.LRD, id);
                List<DistanceResultPair<DoubleDistance>> neighbors = database.getAssociation(AssociationID.NEIGHBORS, id);
                double sum = 0;
                for (DistanceResultPair<DoubleDistance> neighbor1 : neighbors) {
                    sum += database.getAssociation(AssociationID.LRD, neighbor1.getSecond()) / lrd;
                }
                Double lof = sum / neighbors.size();
                database.associate(AssociationID.LOF, id, lof);
                if (logger.isVerbose()) {
                    progressLOFs.setProcessed(counter);
                    logger.progress(progressLOFs);
                }
            }
        }
        AnnotationsFromDatabase<O, Double> res1 = new AnnotationsFromDatabase<O, Double>(database);
        res1.addAssociation("LOF", AssociationID.LOF);
        // Ordering
        OrderingFromAssociation<Double, O> res2 = new OrderingFromAssociation<Double, O>(database, AssociationID.LOF, true); 
        // combine results.
        result = new MultiResult();
        result.addResult(res1);
        result.addResult(res2);

        return result;
    }

    public Description getDescription() {
        return new Description("GeneralizedLOF",
            "Generalized Local Outlier Factor",
            "Algorithm to compute density-based local outlier factors in a database based on the parameter " +
                K_PARAM + " and different distance functions",
            "unpublished");
    }

    /**
     * Calls the super method
     * and sets additionally the value of the parameter
     * {@link #K_PARAM}
     * and instantiates {@link #reachabilityDistanceFunction} according to the value of parameter
     * {@link #REACHABILITY_DISTANCE_FUNCTION_PARAM}.
     * The remaining parameters are passed to the {@link #reachabilityDistanceFunction}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // k
        k = K_PARAM.getValue();

        // reachabilityDistanceFunction
        reachabilityDistanceFunction = REACHABILITY_DISTANCE_FUNCTION_PARAM.instantiateClass();
        remainingParameters = reachabilityDistanceFunction.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #reachabilityDistanceFunction}.
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(reachabilityDistanceFunction.getAttributeSettings());
        return attributeSettings;
    }

    /**
     * Calls the super method
     * and appends the parameter description of {@link #reachabilityDistanceFunction}
     * (if it is already initialized).
     */
    @Override
    public String parameterDescription() {
        StringBuilder description = new StringBuilder();
        description.append(super.parameterDescription());

        // reachabilityDistanceFunction
        if (reachabilityDistanceFunction != null) {
            description.append(Description.NEWLINE);
            description.append(reachabilityDistanceFunction.parameterDescription());
        }

        return description.toString();
    }

    public MultiResult getResult() {
        return result;
    }
}
