package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.distance.CorrelationDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.preprocessing.HiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.varianceanalysis.PCAFilteredResult;

/**
 * Provides the correlation distance for real valued vectors.
 *
 * @author Elke Achtert
 * @param <V> the type of RealVector to compute the distances in between
 * @param <P> the type of Preprocessor used
 * @param <D> the type of CorrelationDistance used
 */
// TODO: can we spec D differently so we don't get the unchecked warnings below?
public class PCABasedCorrelationDistanceFunction<V extends RealVector<V, ?>, P extends Preprocessor<V>, D extends CorrelationDistance<D>>
    extends AbstractCorrelationDistanceFunction<V, P, D> {

    /**
     * OptionID for {@link #DELTA_PARAM}
     */
    public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID(
        "pcabasedcorrelationdf.delta",
        "Threshold of a distance between a vector q and a given space that indicates that " +
            "q adds a new dimension to the space."
    );

    /**
     * Parameter to specify the threshold of a distance between a vector q and a given space
     * that indicates that q adds a new dimension to the space,
     * must be a double equal to or greater than 0.
     * <p>Default value: {@code 0.25} </p>
     * <p>Key: {@code -pcabasedcorrelationdf.delta} </p>
     */
    private static DoubleParameter DELTA_PARAM = new DoubleParameter(
        DELTA_ID,
        new GreaterEqualConstraint(0),
        0.25);

    /**
     * Holds the value of {@link #DELTA_PARAM}.
     */
    private double delta;

    /**
     * Provides a PCABasedCorrelationDistanceFunction,
     * adding parameter
     * {@link #DELTA_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public PCABasedCorrelationDistanceFunction() {
        super();
        addOption(DELTA_PARAM);
    }

    /**
     * Calls the super method
     * AbstractPreprocessorBasedDistanceFunction#setParameters(args)}
     * and sets additionally the value of the parameter
     * {@link #DELTA_PARAM}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // delta
        delta = DELTA_PARAM.getValue();

        return remainingParameters;
    }

    /**
     * Provides a distance suitable to this DistanceFunction based on the given
     * pattern.
     *
     * @param pattern A pattern defining a distance suitable to this
     *                DistanceFunction
     * @return a distance suitable to this DistanceFunction based on the given
     *         pattern
     * @throws IllegalArgumentException if the given pattern is not compatible with the requirements
     *                                  of this DistanceFunction
     */
    @SuppressWarnings("unchecked")
    public D valueOf(String pattern)
        throws IllegalArgumentException {
        if (pattern.equals(INFINITY_PATTERN)) {
            return infiniteDistance();
        }
        if (matches(pattern)) {
            String[] values = AbstractCorrelationDistanceFunction.SEPARATOR.split(pattern);
            return (D) new CorrelationDistance<D>(Integer.parseInt(values[0]), Double.parseDouble(values[1]));
        }
        else {
            throw new IllegalArgumentException("Given pattern \"" +
                pattern +
                "\" does not match required pattern \"" +
                requiredInputPattern() + "\"");
        }
    }

    /**
     * Provides an infinite distance.
     *
     * @return an infinite distance
     */
    @SuppressWarnings("unchecked")
    public D infiniteDistance() {
      return (D) new CorrelationDistance<D>(Integer.MAX_VALUE, Double.POSITIVE_INFINITY);
    }

    /**
     * Provides a null distance.
     *
     * @return a null distance
     */
    @SuppressWarnings("unchecked")
    public D nullDistance() {
      return (D) new CorrelationDistance<D>(0, 0);
    }

    /**
     * Provides an undefined distance.
     *
     * @return an undefined distance
     */
    @SuppressWarnings("unchecked")
    public D undefinedDistance() {
      return (D) new CorrelationDistance<D>(-1, Double.NaN);
    }

    @Override
    @SuppressWarnings("unchecked")
    D correlationDistance(V dv1, V dv2) {
        PCAFilteredResult pca1 = getDatabase().getAssociation(AssociationID.LOCAL_PCA, dv1.getID());
        PCAFilteredResult pca2 = getDatabase().getAssociation(AssociationID.LOCAL_PCA, dv2.getID());

        int correlationDistance = correlationDistance(pca1, pca2, dv1.getDimensionality());
        double euclideanDistance = euclideanDistance(dv1, dv2);

        return (D) new CorrelationDistance<D>(correlationDistance, euclideanDistance);
    }

    /**
     * Computes the correlation distance between the two subspaces
     * defined by the specified PCAs.
     *
     * @param pca1           first PCA
     * @param pca2           second PCA
     * @param dimensionality the dimensionality of the data space
     * @return the correlation distance between the two subspaces
     *         defined by the specified PCAs
     */
    public int correlationDistance(PCAFilteredResult pca1, PCAFilteredResult pca2, int dimensionality) {
        // TODO nur in eine Richtung?
        // pca of rv1
        Matrix v1 = pca1.getEigenvectors();
        Matrix v1_strong = pca1.adapatedStrongEigenvectors();
        Matrix e1_czech = pca1.selectionMatrixOfStrongEigenvectors();
        int lambda1 = pca1.getCorrelationDimension();

        // pca of rv2
        Matrix v2 = pca2.getEigenvectors();
        Matrix v2_strong = pca2.adapatedStrongEigenvectors();
        Matrix e2_czech = pca2.selectionMatrixOfStrongEigenvectors();
        int lambda2 = pca2.getCorrelationDimension();

        // for all strong eigenvectors of rv2
        Matrix m1_czech = pca1.dissimilarityMatrix();
        for (int i = 0; i < v2_strong.getColumnDimensionality(); i++) {
            Matrix v2_i = v2_strong.getColumn(i);
            // check, if distance of v2_i to the space of rv1 > delta
            // (i.e., if v2_i spans up a new dimension)
            double dist = Math.sqrt(v2_i.transpose().times(v2_i).get(0, 0) - v2_i.transpose().times(m1_czech).times(v2_i).get(0, 0));

            // if so, insert v2_i into v1 and adjust v1
            // and compute m1_czech new, increase lambda1
            if (lambda1 < dimensionality && dist > delta) {
                adjust(v1, e1_czech, v2_i, lambda1++);
                m1_czech = v1.times(e1_czech).times(v1.transpose());
            }
        }

        // for all strong eigenvectors of rv1
        Matrix m2_czech = pca2.dissimilarityMatrix();
        for (int i = 0; i < v1_strong.getColumnDimensionality(); i++) {
            Matrix v1_i = v1_strong.getColumn(i);
            // check, if distance of v1_i to the space of rv2 > delta
            // (i.e., if v1_i spans up a new dimension)
            double dist = Math.sqrt(v1_i.transpose().times(v1_i).get(0, 0) - v1_i.transpose().times(m2_czech).times(v1_i).get(0, 0));

            // if so, insert v1_i into v2 and adjust v2
            // and compute m2_czech new , increase lambda2
            if (lambda2 < dimensionality && dist > delta) {
                adjust(v2, e2_czech, v1_i, lambda2++);
                m2_czech = v2.times(e2_czech).times(v2.transpose());
            }
        }

        int correlationDistance = Math.max(lambda1, lambda2);

        // TODO delta einbauen
//     Matrix m_1_czech = pca1.dissimilarityMatrix();
//     double dist_1 = normalizedDistance(dv1, dv2, m1_czech);
//     Matrix m_2_czech = pca2.dissimilarityMatrix();
//     double dist_2 = normalizedDistance(dv1, dv2, m2_czech);
//     if (dist_1 > delta || dist_2 > delta) {
//     correlationDistance++;
//     }

        return correlationDistance;
    }

    /**
     * Inserts the specified vector into the given orthonormal matrix
     * <code>v</code> at column <code>corrDim</code>. After insertion the
     * matrix <code>v</code> is orthonormalized and column
     * <code>corrDim</code> of matrix <code>e_czech</code> is set to the
     * <code>corrDim</code>-th unit vector..
     *
     * @param v       the orthonormal matrix of the eigenvectors
     * @param e_czech the selection matrix of the strong eigenvectors
     * @param vector  the vector to be inserted
     * @param corrDim the column at which the vector should be inserted
     */
    private void adjust(Matrix v, Matrix e_czech, Matrix vector, int corrDim) {
        int dim = v.getRowDimensionality();

        // set e_czech[corrDim][corrDim] := 1
        e_czech.set(corrDim, corrDim, 1);

        // normalize v
        Matrix v_i = vector.copy();
        Matrix sum = new Matrix(dim, 1);
        for (int k = 0; k < corrDim; k++) {
            Matrix v_k = v.getColumn(k);
            sum = sum.plus(v_k.times(v_i.scalarProduct(0, v_k, 0)));
        }
        v_i = v_i.minus(sum);
        v_i = v_i.times(1.0 / v_i.euclideanNorm(0));
        v.setColumn(corrDim, v_i);
    }

    /**
     * Computes the Euclidean distance between the given two vectors.
     *
     * @param dv1 first NumberVector
     * @param dv2 second NumberVector
     * @return the Euclidean distance between the given two vectors
     */
    private double euclideanDistance(V dv1, V dv2) {
        if (dv1.getDimensionality() != dv2.getDimensionality()) {
            throw new IllegalArgumentException(
                "Different dimensionality of NumberVectors\n  first argument: "
                    + dv1.toString() + "\n  second argument: "
                    + dv2.toString());
        }

        double sqrDist = 0;
        for (int i = 1; i <= dv1.getDimensionality(); i++) {
            double manhattanI = dv1.getValue(i).doubleValue() - dv2.getValue(i).doubleValue();
            sqrDist += manhattanI * manhattanI;
        }
        return Math.sqrt(sqrDist);
    }

    /**
     * @return the name of the default preprocessor,
     *         which is {@link de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor}
     */
    public String getDefaultPreprocessorClassName() {
        return KnnQueryBasedHiCOPreprocessor.class.getName();
    }

    public String getPreprocessorDescription() {
        return "Classname of the preprocessor to determine the correlation dimension of each object "
            + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(getPreprocessorSuperClass())
            + ".";
    }

    /**
     * @return the super class for the preprocessor parameter,
     *         which is {@link HiCOPreprocessor}
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Preprocessor> getPreprocessorSuperClass() {
        return HiCOPreprocessor.class;
    }

    /**
     * @return the association ID for the association to be set by the preprocessor,
     *         which is {@link de.lmu.ifi.dbs.elki.database.AssociationID#LOCAL_PCA}
     */
    public AssociationID<?> getAssociationID() {
        return AssociationID.LOCAL_PCA;
    }
}