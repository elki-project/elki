package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.Bit;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.distance.BitDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

/**
 * Provides a distance function for building the hierarchiy in the ERiC algorithm.
 *
 * @author Elke Achtert
 * @param <V> the type of RealVector to compute the distances in between
 * @param <P> the type of Preprocessor used
 */
public class ERiCDistanceFunction<V extends RealVector<V, ?>, P extends Preprocessor<V>>
    extends AbstractPreprocessorBasedDistanceFunction<V, P, BitDistance> {

    /**
     * OptionID for {@link #DELTA_PARAM}
     */
    public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID(
        "ericdf.delta",
        "Threshold for approximate linear dependency: " +
            "the strong eigenvectors of q are approximately linear dependent " +
            "from the strong eigenvectors p if the following condition " +
            "holds for all stroneg eigenvectors q_i of q (lambda_q < lambda_p): " +
            "q_i' * M^check_p * q_i <= delta^2."
    );

    /**
     * Parameter to specify the threshold for approximate linear dependency:
     * the strong eigenvectors of q are approximately linear dependent
     * from the strong eigenvectors p if the following condition
     * holds for all stroneg eigenvectors q_i of q (lambda_q < lambda_p):
     * q_i' * M^check_p * q_i <= delta^2,
     * must be a double equal to or greater than 0.
     * <p>Default value: {@code 0.1} </p>
     * <p>Key: {@code -ericdf.delta} </p>
     */
    private final DoubleParameter DELTA_PARAM = new DoubleParameter(
        DELTA_ID,
        new GreaterEqualConstraint(0),
        0.1
    );

    /**
     * OptionID for {@link #TAU_PARAM}
     */
    public static final OptionID TAU_ID = OptionID.getOrCreateOptionID(
        "ericdf.tau",
        "Threshold for the maximum distance between two approximately linear " +
            "dependent subspaces of two objects p and q " +
            "(lambda_q < lambda_p) before considering them as parallel."
    );

    /**
     * Parameter to specify the threshold for the maximum distance between two approximately linear
     * dependent subspaces of two objects p and q
     * (lambda_q < lambda_p) before considering them as parallel,
     * must be a double equal to or greater than 0.
     * <p>Default value: {@code 0.1} </p>
     * <p>Key: {@code -ericdf.tau} </p>
     */
    private final DoubleParameter TAU_PARAM = new DoubleParameter(
        TAU_ID,
        new GreaterEqualConstraint(0),
        0.1
    );

    /**
     * Holds the value of {@link #DELTA_PARAM}.
     */
    private double delta;

    /**
     * Holds the value of {@link #TAU_PARAM}.
     */
    private double tau;

    /**
     * Provides a distance function for the ERiC algorithm,
     * adding parameters
     * {@link #DELTA_PARAM} and {#TAU_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public ERiCDistanceFunction() {
        super(Bit.BIT_PATTERN);

        // delta
        addOption(DELTA_PARAM);

        // tau
        addOption(TAU_PARAM);
    }

    /**
     * Calls the super method
     * AbstractPreprocessorBasedDistanceFunction#setParameters(args)}
     * and sets additionally the values of the parameters
     * {@link #DELTA_PARAM} and {#TAU_PARAM}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // delta, tau
        delta = DELTA_PARAM.getValue();
        tau = TAU_PARAM.getValue();

        return remainingParameters;
    }

    /**
     * Returns the name of the default preprocessor.
     *
     * @return the name of the default preprocessor,
     *         which is {@link de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor}
     */
    public String getDefaultPreprocessorClassName() {
        return KnnQueryBasedHiCOPreprocessor.class.getName();
    }

    public String getPreprocessorDescription() {
        return "Preprocessor class to determine the correlation dimension of each object.";
    }

    /**
     * @return the super class for the preprocessor parameter,
     *         which is {@link de.lmu.ifi.dbs.elki.preprocessing.Preprocessor}
     */
    public Class<P> getPreprocessorSuperClass() {
        return ClassGenericsUtil.uglyCastIntoSubclass(Preprocessor.class);
    }

    /**
     * @return the association ID for the association to be set by the preprocessor,
     *         which is {@link AssociationID#LOCAL_PCA}
     */
    public AssociationID<?> getAssociationID() {
        return AssociationID.LOCAL_PCA;
    }

    public BitDistance valueOf(String pattern) throws IllegalArgumentException {
        if (matches(pattern)) {
            return new BitDistance(Bit.valueOf(pattern).bitValue());
        }
        else {
            throw new IllegalArgumentException("Given pattern \"" + pattern
                + "\" does not match required pattern \""
                + requiredInputPattern() + "\"");
        }
    }

    public BitDistance infiniteDistance() {
        return new BitDistance(true);
    }

    public BitDistance nullDistance() {
        return new BitDistance(false);
    }

    public BitDistance undefinedDistance() {
        throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_UNDEFINED_DISTANCE);
    }

    /**
     * Note, that the pca of o1 must have equal ore more strong
     * eigenvectors than the pca of o2.
     *
     */
    public BitDistance distance(V v1, V v2) {
        PCAFilteredResult pca1 = getDatabase().getAssociation(AssociationID.LOCAL_PCA, v1.getID());
        PCAFilteredResult pca2 = getDatabase().getAssociation(AssociationID.LOCAL_PCA, v2.getID());
        return distance(v1, v2, pca1, pca2);
    }

    /**
     * Computes the distance between two given DatabaseObjects according to this
     * distance function. Note, that the first pca must have equal or more strong
     * eigenvectors than the second pca.
     *
     * @param v1   first DatabaseObject
     * @param v2   second DatabaseObject
     * @param pca1 first PCA
     * @param pca2 second PCA
     * @return the distance between two given DatabaseObjects according to this
     *         distance function
     */
    public BitDistance distance(V v1, V v2, PCAFilteredResult pca1, PCAFilteredResult pca2) {
        if (pca1.getCorrelationDimension() < pca2.getCorrelationDimension()) {
            throw new IllegalStateException("pca1.getCorrelationDimension() < pca2.getCorrelationDimension(): " +
                pca1.getCorrelationDimension() + " < " + pca2.getCorrelationDimension());
        }

        boolean approximatelyLinearDependent;
        if (pca1.getCorrelationDimension() == pca2.getCorrelationDimension()) {
            approximatelyLinearDependent = approximatelyLinearDependent(pca1, pca2) &&
                approximatelyLinearDependent(pca2, pca1);
        }
        else {
            approximatelyLinearDependent = approximatelyLinearDependent(pca1, pca2);
        }


        if (!approximatelyLinearDependent) {
            return new BitDistance(true);
        }

        else {
            double affineDistance;

            if (pca1.getCorrelationDimension() == pca2.getCorrelationDimension()) {
                WeightedDistanceFunction<V> df1 = new WeightedDistanceFunction<V>(pca1.similarityMatrix());
                WeightedDistanceFunction<V> df2 = new WeightedDistanceFunction<V>(pca2.similarityMatrix());
                affineDistance = Math.max(df1.distance(v1, v2).getValue(),
                    df2.distance(v1, v2).getValue());
            }
            else {
                WeightedDistanceFunction<V> df1 = new WeightedDistanceFunction<V>(pca1.similarityMatrix());
                affineDistance = df1.distance(v1, v2).getValue();
            }

            if (affineDistance > tau) {
                return new BitDistance(true);
            }

            return new BitDistance(false);
        }
    }

    /**
     * Returns true, if the strong eigenvectors of the two specified
     * pcas span up the same space. Note, that the first pca must have equal ore more strong
     * eigenvectors than the second pca.
     *
     * @param pca1 first PCA
     * @param pca2 second PCA
     * @return true, if the strong eigenvectors of the two specified
     *         pcas span up the same space
     */
    private boolean approximatelyLinearDependent(PCAFilteredResult pca1, PCAFilteredResult pca2) {
        Matrix m1_czech = pca1.dissimilarityMatrix();
        Matrix v2_strong = pca2.adapatedStrongEigenvectors();
        for (int i = 0; i < v2_strong.getColumnDimensionality(); i++) {
            Matrix v2_i = v2_strong.getColumn(i);
            // check, if distance of v2_i to the space of pca_1 > delta
            // (i.e., if v2_i spans up a new dimension)
            double dist = Math.sqrt(v2_i.transpose().times(v2_i).get(0, 0) - v2_i.transpose().times(m1_czech).times(v2_i).get(0, 0));

            // if so, return false
            if (dist > delta) {
                return false;
            }
        }

        return true;
    }
}
