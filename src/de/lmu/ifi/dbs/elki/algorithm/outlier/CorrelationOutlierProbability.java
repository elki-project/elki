package de.lmu.ifi.dbs.elki.algorithm.outlier;

import de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.algorithm.result.outlier.COPVerboseResult;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.ErrorFunctions;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
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
 * Algorithm to compute local correlation outlier probability.
 * <p/>
 * Publication pending
 *
 * @author Erich Schubert
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class CorrelationOutlierProbability<V extends RealVector<V, ?>> extends DistanceBasedAlgorithm<V, DoubleDistance> {
    /**
     * OptionID for {@link #K_PARAM}
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID(
        "cop.k",
        "The number of nearest neighbors of an object to be considered for computing its COP.");

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
    private DependencyDerivator<V, DoubleDistance> dependencyDerivator;

    /**
     * Provides the result of the algorithm.
     */
    COPVerboseResult<V> result;

    /**
     * Sets minimum points to the optionhandler additionally to the parameters
     * provided by super-classes.
     */
    public CorrelationOutlierProbability() {
        super();
        addOption(K_PARAM);
    }

    protected void runInTime(Database<V> database) throws IllegalStateException {
        getDistanceFunction().setDatabase(database, isVerbose(), isTime());
        if (isVerbose()) {
            verbose("\nCorrelationOutlierDetection ");
        }

        {// compute neighbors of each db object
            if (isVerbose()) {
                verbose("\nRunning dependency derivation");
            }
            Progress progressLocalPCA = new Progress("COD", database.size());
            int counter = 1;
            double sqrt2 = Math.sqrt(2.0);
            for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
                Integer id = iter.next();
                List<QueryResult<DoubleDistance>> neighbors = database.kNNQueryForID(id, k + 1, getDistanceFunction());
                neighbors.remove(0);

                List<Integer> ids = new ArrayList<Integer>(neighbors.size());
                for (QueryResult<DoubleDistance> n : neighbors) {
                    ids.add(n.getID());
                }

                // TODO: do we want to use the query point as centroid?
                CorrelationAnalysisSolution<V> depsol = dependencyDerivator.generateModel(database, ids);

                // temp code, experimental.
                if (false) {
                    double traddistance = depsol.getCentroid().minus(database.get(id).getColumnVector()).euclideanNorm(0);
                    if (traddistance > 0.0) {
                        double distance = depsol.distance(database.get(id));
                        database.associate(AssociationID.COP, id, distance / traddistance);
                    }
                    else {
                        database.associate(AssociationID.COP, id, 0.0);
                    }
                }
                double stddev = depsol.getStandardDeviation();
                double distance = depsol.distance(database.get(id));
                double prob = ErrorFunctions.erf(distance / (stddev * sqrt2));

                database.associate(AssociationID.COP, id, prob);

                Vector errv = depsol.errorVector(database.get(id));
                database.associate(AssociationID.COP_ERROR_VECTOR, id, errv);

                Matrix datav = depsol.dataProjections(database.get(id));
                database.associate(AssociationID.COP_DATA_VECTORS, id, datav);

                database.associate(AssociationID.COP_DIM, id, depsol.getCorrelationDimensionality());

                database.associate(AssociationID.COP_SOL, id, depsol);

                if (isVerbose()) {
                    progressLocalPCA.setProcessed(counter);
                    progress(progressLocalPCA);
                }
            }
            if (isVerbose()) {
                verbose("");
            }
        }
        result = new COPVerboseResult<V>(database);
    }

    public Description getDescription() {
        return new Description("COP", "Correlation Outlier Probability", "Algorithm to compute correlation-based local outlier probabilitys in a database based on the parameter " + K_PARAM + " and different distance functions", "unpublished");
    }

    /**
     * Calls the super method
     * and sets additionally the value of the parameter
     * {@link #K_PARAM}.
     * The remaining parameters are passed to {@link #dependencyDerivator}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        k = K_PARAM.getValue();

        // dependency derivator (currently hardcoded)
        dependencyDerivator = new DependencyDerivator<V, DoubleDistance>();
        remainingParameters = dependencyDerivator.setParameters(remainingParameters);

        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    public Result<V> getResult() {
        return result;
    }

    /**
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #dependencyDerivator}.
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(dependencyDerivator.getAttributeSettings());
        return attributeSettings;
    }
}
