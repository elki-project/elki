package de.lmu.ifi.dbs.elki.algorithm.outlier;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.MultivariateModelDerivator;
import de.lmu.ifi.dbs.elki.algorithm.result.MultivariateModel;
import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.algorithm.result.outlier.MCOPResult;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Algorithm to compute local multivariate correlation outlier probability
 * <p/>
 * Publication pending
 *
 * @author Erich Schubert
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class MultivariateCorrelationOutlierProbability<V extends RealVector<V, ?>> extends DistanceBasedAlgorithm<V, DoubleDistance> {
    /**
     * OptionID for {@link #K_PARAM}
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID(
        "mcop.k",
        "The number of nearest neighbors of an object to be considered for computing its MCOP.");

    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its COP, must be an integer greater than 0.
     * <p/>
     * Key: {@code -cop.k}
     * </p>
     */
    private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0));

    /**
     * Number of neighbors to be considered.
     */
    int k;

    /**
     * Holds the object performing the dependency derivation
     */
    private MultivariateModelDerivator<V, DoubleDistance> modelDerivator = new MultivariateModelDerivator<V, DoubleDistance>();

    /**
     * Provides the result of the algorithm.
     */
    MCOPResult<V> result;

    /**
     * Sets minimum points to the optionhandler additionally to the parameters
     * provided by super-classes.
     */
    public MultivariateCorrelationOutlierProbability() {
        super();
        addOption(K_PARAM);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm#runInTime(Database)
     */
    protected void runInTime(Database<V> database) throws IllegalStateException {
        getDistanceFunction().setDatabase(database, isVerbose(), isTime());
        if (isVerbose()) {
            verbose("\nMultiavariateCorrelationOutlierProbability ");
        }

        {// compute neighbors of each db object
            if (isVerbose()) {
                verbose("\nRunning model generation");
            }
            Progress progressLocalPCA = new Progress("MCOP", database.size());
            int counter = 1;
            for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
                Integer id = iter.next();
                List<QueryResult<DoubleDistance>> neighbors = database.kNNQueryForID(id, k + 1, getDistanceFunction());
                neighbors.remove(0);

                List<Integer> ids = new ArrayList<Integer>(neighbors.size());
                for (QueryResult<DoubleDistance> n : neighbors) {
                    ids.add(n.getID());
                }

                // TODO: do we want to use the query point as centroid?
                MultivariateModel<V> model = modelDerivator.generateModel(database, ids);

                double prob = 1.0 - model.boostedProbability(database.get(id));
                database.associate(AssociationID.MCOP, id, prob);

                if (isVerbose()) {
                    progressLocalPCA.setProcessed(counter);
                    progress(progressLocalPCA);
                }
            }
            if (isVerbose()) {
                verbose("");
            }
        }
        result = new MCOPResult<V>(database);
    }

    /**
     * @see Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description("MCOP", "Multivariate Correlation Outlier Probability", "Algorithm to compute multivariate correlation-based outlier probability in a database based on the parameter " + K_PARAM + " and different distance functions", "unpublished");
    }

    /**
     * Sets the parameters minpts additionally to the parameters set by the
     * super-class method.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        k = getParameterValue(K_PARAM);

        remainingParameters = modelDerivator.setParameters(remainingParameters);

        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getResult()
     */
    public Result<V> getResult() {
        return result;
    }

    /**
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #modelDerivator}.
     */
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(modelDerivator.getAttributeSettings());
        return attributeSettings;
    }
}
