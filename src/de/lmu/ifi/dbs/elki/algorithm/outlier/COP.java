package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.model.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.ErrorFunctions;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDatabase;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromAssociation;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * Algorithm to compute local correlation outlier probability.
 * <p/>
 * Publication pending
 *
 * @author Erich Schubert
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class COP<V extends RealVector<V, ?>> extends DistanceBasedAlgorithm<V, DoubleDistance, MultiResult> {
    /**
     * OptionID for {@link #K_PARAM}
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID(
        "cop.k",
        "The number of nearest neighbors of an object to be considered for computing its COP_SCORE.");

    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its COP_SCORE, must be an integer greater than 0.
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
    MultiResult result;

    /**
     * The association id to associate the Correlation Outlier Probability of an object
     */
    public static final AssociationID<Double> COP_SCORE = AssociationID.getOrCreateAssociationID("cop", Double.class);

    /**
     * The association id to associate the COP_SCORE error vector of an object for the COP_SCORE
     * algorithm.
     */
    public static final AssociationID<Vector> COP_ERROR_VECTOR = AssociationID.getOrCreateAssociationID("cop error vector", Vector.class);

    /**
     * The association id to associate the COP_SCORE data vector of an object for the COP_SCORE
     * algorithm.
     */
    // TODO: use or remove.
    public static final AssociationID<Matrix> COP_DATA_VECTORS = AssociationID.getOrCreateAssociationID("cop data vectors", Matrix.class);

    /**
     * The association id to associate the COP_SCORE correlation dimensionality of an object for the COP_SCORE
     * algorithm.
     */
    public static final AssociationID<Integer> COP_DIM = AssociationID.getOrCreateAssociationID("cop dim", Integer.class);

    /**
     * The association id to associate the COP_SCORE correlation solution
     */
    public static final AssociationID<CorrelationAnalysisSolution<?>> COP_SOL = AssociationID.getOrCreateAssociationIDGenerics("cop sol", CorrelationAnalysisSolution.class);

    /**
     * Sets minimum points to the optionhandler additionally to the parameters
     * provided by super-classes.
     */
    public COP() {
        super();
        addOption(K_PARAM);
    }

    @Override
    protected MultiResult runInTime(Database<V> database) throws IllegalStateException {
        getDistanceFunction().setDatabase(database, isVerbose(), isTime());
        if (logger.isVerbose()) {
          logger.verbose("CorrelationOutlierProbability ");
        }

        {// compute neighbors of each db object
            if (logger.isVerbose()) {
              logger.verbose("Running dependency derivation");
            }
            FiniteProgress progressLocalPCA = new FiniteProgress("COP_SCORE", database.size());
            int counter = 1;
            double sqrt2 = Math.sqrt(2.0);
            for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
                Integer id = iter.next();
                List<DistanceResultPair<DoubleDistance>> neighbors = database.kNNQueryForID(id, k + 1, getDistanceFunction());
                neighbors.remove(0);

                List<Integer> ids = new ArrayList<Integer>(neighbors.size());
                for (DistanceResultPair<DoubleDistance> n : neighbors) {
                    ids.add(n.getID());
                }

                // TODO: do we want to use the query point as centroid?
                CorrelationAnalysisSolution<V> depsol = dependencyDerivator.generateModel(database, ids);

                // temp code, experimental.
                if (false) {
                    double traddistance = depsol.getCentroid().minus(database.get(id).getColumnVector()).euclideanNorm(0);
                    if (traddistance > 0.0) {
                        double distance = depsol.distance(database.get(id));
                        database.associate(COP_SCORE, id, distance / traddistance);
                    }
                    else {
                        database.associate(COP_SCORE, id, 0.0);
                    }
                }
                double stddev = depsol.getStandardDeviation();
                double distance = depsol.distance(database.get(id));
                double prob = ErrorFunctions.erf(distance / (stddev * sqrt2));

                database.associate(COP_SCORE, id, prob);

                Vector errv = depsol.errorVector(database.get(id));
                database.associate(COP_ERROR_VECTOR, id, errv);

                Matrix datav = depsol.dataProjections(database.get(id));
                database.associate(COP_DATA_VECTORS, id, datav);

                database.associate(COP_DIM, id, depsol.getCorrelationDimensionality());

                database.associate(COP_SOL, id, depsol);

                if (logger.isVerbose()) {
                    progressLocalPCA.setProcessed(counter);
                    logger.progress(progressLocalPCA);
                }
            }
        }
        // combine results.
        result = new MultiResult();
        result.addResult(new AnnotationFromDatabase<Double, V>(database,COP_SCORE));
        result.addResult(new AnnotationFromDatabase<Integer, V>(database,COP_DIM));
        result.addResult(new AnnotationFromDatabase<Vector, V>(database,COP_ERROR_VECTOR));
        result.addResult(new AnnotationFromDatabase<CorrelationAnalysisSolution<?>, V>(database,COP_SOL));
        result.addResult(new OrderingFromAssociation<Double, V>(database,COP_SCORE, true)); 
        return result;
    }

    public Description getDescription() {
        return new Description("COP_SCORE", "Correlation Outlier Probability", "Algorithm to compute correlation-based local outlier probabilitys in a database based on the parameter " + K_PARAM + " and different distance functions", "unpublished");
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
        addParameterizable(dependencyDerivator);

        rememberParametersExcept(args, remainingParameters);
        return remainingParameters;
    }

    public MultiResult getResult() {
        return result;
    }
}
