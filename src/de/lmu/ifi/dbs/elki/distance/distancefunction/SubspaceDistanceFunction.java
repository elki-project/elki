package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.distance.SubspaceDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.varianceanalysis.PCAFilteredResult;

import java.util.regex.Pattern;

/**
 * Provides a distance function to determine a kind of correlation distance
 * between two points, which is a pair consisting of the distance between the two subspaces
 * spanned by the strong eigenvectors of the two points and the affine distance
 * between the two subspaces.
 *
 * @author Elke Achtert
 * @param <V> the type of RealVector to compute the distances in between
 * @param <P> the type of Preprocessor used
 */
public class SubspaceDistanceFunction<V extends RealVector<V, ?>, P extends Preprocessor<V>>
    extends AbstractPreprocessorBasedDistanceFunction<V, P, SubspaceDistance> {

    /**
     * Provides a distance function to determine distances
     * between subspaces of equal dimensionality.
     */
    public SubspaceDistanceFunction() {
        super(Pattern.compile("\\d+(\\.\\d+)?([eE][-]?\\d+)?" +
            AbstractCorrelationDistanceFunction.SEPARATOR.pattern() +
            "\\d+(\\.\\d+)?([eE][-]?\\d+)?"));
    }

    /**
     * @return the name of the default preprocessor,
     *         which is {@link de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedHiCOPreprocessor}
     */
    public String getDefaultPreprocessorClassName() {
        return KnnQueryBasedHiCOPreprocessor.class.getName();
    }

    public final String getPreprocessorDescription() {
        return "Classname of the preprocessor to determine the correlation dimension of each object "
            + Properties.ELKI_PROPERTIES.restrictionString(getPreprocessorSuperClass()) +
            ".";
    }

    /**
     * @return the super class for the preprocessor,
     *         which is {@link de.lmu.ifi.dbs.elki.preprocessing.Preprocessor}
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Preprocessor> getPreprocessorSuperClass() {
        return Preprocessor.class;
    }

    /**
     * @return the association ID for the association to be set by the preprocessor,
     *         which is {@link AssociationID#LOCAL_PCA}
     */
    public AssociationID<?> getAssociationID() {
        return AssociationID.LOCAL_PCA;
    }

    public SubspaceDistance valueOf(String pattern) throws IllegalArgumentException {
        if (pattern.equals(INFINITY_PATTERN)) {
            return infiniteDistance();
        }
        if (matches(pattern)) {
            String[] values = AbstractCorrelationDistanceFunction.SEPARATOR.split(pattern);
            return new SubspaceDistance(Double.parseDouble(values[0]), Double.parseDouble(values[1]));
        }
        else {
            throw new IllegalArgumentException("Given pattern \"" +
                pattern +
                "\" does not match required pattern \"" +
                requiredInputPattern() + "\"");
        }
    }

    public SubspaceDistance infiniteDistance() {
        return new SubspaceDistance(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public SubspaceDistance nullDistance() {
        return new SubspaceDistance(0, 0);
    }

    public SubspaceDistance undefinedDistance() {
        return new SubspaceDistance(Double.NaN, Double.NaN);
    }

    /**
     * Note, that the pca of o1 must have equal ore more strong
     * eigenvectors than the pca of o2.
     *
     */
    public SubspaceDistance distance(V o1, V o2) {
        // noinspection unchecked
        PCAFilteredResult pca1 = getDatabase().getAssociation(AssociationID.LOCAL_PCA, o1.getID());
        // noinspection unchecked
        PCAFilteredResult pca2 = getDatabase().getAssociation(AssociationID.LOCAL_PCA, o2.getID());
        return distance(o1, o2, pca1, pca2);
    }

    /**
     * Computes the distance between two given DatabaseObjects according to this
     * distance function. Note, that the first pca must have an equal number of strong
     * eigenvectors than the second pca.
     *
     * @param o1   first DatabaseObject
     * @param o2   second DatabaseObject
     * @param pca1 first PCA
     * @param pca2 second PCA
     * @return the distance between two given DatabaseObjects according to this
     *         distance function
     */
    public SubspaceDistance distance(V o1, V o2, PCAFilteredResult pca1, PCAFilteredResult pca2) {
        if (pca1.getCorrelationDimension() != pca2.getCorrelationDimension()) {
            throw new IllegalStateException("pca1.getCorrelationDimension() != pca2.getCorrelationDimension()");
        }

        Matrix strong_ev1 = pca1.getStrongEigenvectors();
        Matrix weak_ev2 = pca2.getWeakEigenvectors();
        Matrix m1 = weak_ev2.getColumnDimensionality() == 0 ? strong_ev1.transpose() : strong_ev1.transpose().times(weak_ev2);
        double d1 = m1.norm2();

        WeightedDistanceFunction<V> df1 = new WeightedDistanceFunction<V>(pca1.similarityMatrix());
        WeightedDistanceFunction<V> df2 = new WeightedDistanceFunction<V>(pca2.similarityMatrix());

        double affineDistance = Math.max(df1.distance(o1, o2).getValue(),
            df2.distance(o1, o2).getValue());

        return new SubspaceDistance(d1, affineDistance);
    }

    @Override
    public String parameterDescription() {
        return "Subspace distance for real vectors. " + super.parameterDescription();
    }
}
