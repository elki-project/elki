package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.KNNList;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromAssociation;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;

/**
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
// todo arthur comment
public class SOD<V extends RealVector<V, Double>, D extends Distance<D>> extends AbstractAlgorithm<V, MultiResult> {

    /**
     * The association id to associate a subspace outlier degree.
     */
    public static final AssociationID<SODModel<?>> SOD_MODEL = AssociationID.getOrCreateAssociationIDGenerics("SOD", SODModel.class);

    /**
     * OptionID for {@link #KNN_PARAM}
     */
    public static final OptionID KNN_ID = OptionID.getOrCreateOptionID(
        "sod.knn",
        "The number of shared nearest neighbors to be considered for learning the subspace properties."
    );

    /**
     * Parameter to specify the number of shared nearest neighbors to be considered for learning the subspace properties.,
     * must be an integer greater than 0.
     * <p>Default value: {@code 1} </p>
     * <p>Key: {@code -sod.knn} </p>
     */
    private final IntParameter KNN_PARAM = new IntParameter(KNN_ID, new GreaterConstraint(0), 1);

    /**
     * Holds the value of {@link #KNN_PARAM}.
     */
    private int knn;

    /**
     * OptionID for {@link #ALPHA_PARAM}
     */
    public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID(
        "sod.alpha",
        "The multiplier for the discriminance value for discerning small from large variances."
    );

    /**
     * Parameter to indicate the multiplier for the discriminance value for discerning small from large variances.
     * <p/>
     * <p>Default value: 1.1</p>
     * <p/>
     * <p>Key: {@code -sod.alpha}</p>
     */
    public final DoubleParameter ALPHA_PARAM = new DoubleParameter(ALPHA_ID, new GreaterConstraint(0), 1.1);

    /**
     * Holds the value of {@link #ALPHA_PARAM}.
     */
    private double alpha;

    /**
     * The similarity function.
     */
    private SharedNearestNeighborSimilarityFunction<V, D> similarityFunction = new SharedNearestNeighborSimilarityFunction<V, D>();

    /**
     * Holds the result.
     */
    private MultiResult sodResult;

    /**
     * Provides the SOD algorithm,
     * adding parameters
     * {@link #KNN_PARAM} and {@link #ALPHA_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public SOD() {
        super();
        addOption(KNN_PARAM);
        addOption(ALPHA_PARAM);
        
        addParameterizable(similarityFunction);
    }

    /**
     * Calls the super method
     * and appends the parameter description of {@link #similarityFunction}.
     */
    @Override
    public String parameterDescription() {
        StringBuilder description = new StringBuilder();
        description.append(super.parameterDescription());

        // similarityFunction
        description.append(Description.NEWLINE);
        description.append(similarityFunction.parameterDescription());

        return description.toString();
    }

    /**
     * Performs the PROCLUS algorithm on the given database.
     */
    @Override
    protected MultiResult runInTime(Database<V> database) throws IllegalStateException {
        FiniteProgress progress = new FiniteProgress("assigning SOD", database.size());
        int processed = 0;
        similarityFunction.setDatabase(database, isVerbose(), isTime());
        if (logger.isVerbose()) {
          logger.verbose("assigning subspace outlier degree:");
        }
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer queryObject = iter.next();
            processed++;
            if (logger.isVerbose()) {
                progress.setProcessed(processed);
                logger.progress(progress);
            }
            List<Integer> knnList = getKNN(database, queryObject).idsToList();
            SODModel<V> model = new SODModel<V>(database, knnList, alpha, database.get(queryObject));
            database.associate(SOD_MODEL, queryObject, model);
        }
        // combine results.
        sodResult = new MultiResult();
        sodResult.addResult(new AnnotationFromDatabase<SODModel<?>, V>(database, SOD_MODEL));
        sodResult.addResult(new OrderingFromAssociation<SODModel<?>, V>(database, SOD_MODEL, true));
        return sodResult;
    }

    /**
     * Provides the k nearest neighbors in terms of the shared nearest neighbor distance.
     * <p/>
     * The query object is excluded from the knn list.
     *
     * @param database    the database holding the objects
     * @param queryObject the query object for which the kNNs should be determined
     * @return the k nearest neighbors in terms of the shared nearest neighbor distance without the query object
     */
    private KNNList<DoubleDistance> getKNN(Database<V> database, Integer queryObject) {
        similarityFunction.getPreprocessor().getParameters();
        KNNList<DoubleDistance> kNearestNeighbors = new KNNList<DoubleDistance>(knn, new DoubleDistance(Double.POSITIVE_INFINITY));
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer id = iter.next();
            if (!id.equals(queryObject)) {
                DoubleDistance distance = new DoubleDistance(1.0 / similarityFunction.similarity(queryObject, id).getValue());
                kNearestNeighbors.add(new DistanceResultPair<DoubleDistance>(distance, id));
            }
        }
        return kNearestNeighbors;
    }

    /**
     * Calls the super method
     * and sets additionally the values of the parameters
     * {@link #KNN_PARAM} and {@link #ALPHA_PARAM}.
     * The remaining parameters are passed to the {@link #similarityFunction}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        knn = KNN_PARAM.getValue();
        alpha = ALPHA_PARAM.getValue();

        remainingParameters = similarityFunction.setParameters(remainingParameters);
        
        rememberParametersExcept(args, remainingParameters);
        return remainingParameters;
    }

    public Description getDescription() {
        return new Description("SOD", "Subspace outlier degree", "", "");
    }

    public MultiResult getResult() {
        return sodResult;
    }
}
