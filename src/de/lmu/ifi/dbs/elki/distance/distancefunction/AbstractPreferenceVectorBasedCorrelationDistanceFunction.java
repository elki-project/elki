package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.distance.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.preprocessing.PreferenceVectorPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.BitSet;

/**
 * Abstract super class for all preference vector based correlation distance functions.
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector to compute the distances in between
 * @param <P> the type of Preprocessor used
 */
public abstract class AbstractPreferenceVectorBasedCorrelationDistanceFunction<V extends RealVector<V, ?>, P extends Preprocessor<V>>
    extends AbstractCorrelationDistanceFunction<V, P, PreferenceVectorBasedCorrelationDistance> {

    /**
     * OptionID for {@link #EPSILON_PARAM}
     */
    public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID(
        "distancefunction.epsilon",
        "The maximum distance between two vectors with equal preference vectors before considering them as parallel."
    );

    /**
     * Parameter to specify the maximum distance between two vectors with equal preference vectors before considering them as parallel,
     * must be a double equal to or greater than 0.
     * <p>Default value: {@code 0.001} </p>
     * <p>Key: {@code -pvbasedcorrelationdf.epsilon} </p>
     */
    private final DoubleParameter EPSILON_PARAM = new DoubleParameter(
        EPSILON_ID,
        new GreaterEqualConstraint(0),
        0.001);

    /**
     * Holds the value of {@link #EPSILON_PARAM}.
     */
    private double epsilon;


    /**
     * Provides a preference vector based CorrelationDistanceFunction,
     * adding parameter
     * {@link #EPSILON_PARAM}
     * to the option handler
     * additionally to parameters of super class.
     */
    public AbstractPreferenceVectorBasedCorrelationDistanceFunction() {
        super();

        // parameter epsilon
        addOption(EPSILON_PARAM);
    }

    public PreferenceVectorBasedCorrelationDistance valueOf(String pattern)
        throws IllegalArgumentException {
        if (pattern.equals(INFINITY_PATTERN)) {
            return infiniteDistance();
        }
        if (matches(pattern)) {
            String[] values = AbstractCorrelationDistanceFunction.SEPARATOR.split(pattern);
            return new PreferenceVectorBasedCorrelationDistance(
                getDatabase().dimensionality(),
                Integer.parseInt(values[0]),
                Double.parseDouble(values[1]), new BitSet());
        }
        else {
            throw new IllegalArgumentException("Given pattern \"" +
                pattern +
                "\" does not match required pattern \"" +
                requiredInputPattern() + "\"");
        }
    }

    public PreferenceVectorBasedCorrelationDistance infiniteDistance() {
        return new PreferenceVectorBasedCorrelationDistance(
            dimensionality(),
            Integer.MAX_VALUE,
            Double.POSITIVE_INFINITY,
            new BitSet());
    }

    public PreferenceVectorBasedCorrelationDistance nullDistance() {
        return new PreferenceVectorBasedCorrelationDistance(
            dimensionality(),
            0,
            0,
            new BitSet());
    }

    public PreferenceVectorBasedCorrelationDistance undefinedDistance() {
        return new PreferenceVectorBasedCorrelationDistance(
            dimensionality(),
            -1,
            Double.NaN,
            new BitSet());
    }

    protected PreferenceVectorBasedCorrelationDistance correlationDistance(V v1, V v2) {
        BitSet preferenceVector1 = getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, v1.getID());
        BitSet preferenceVector2 = getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, v2.getID());
        return correlationDistance(v1, v2, preferenceVector1, preferenceVector2);
    }

    /**
     * Computes the correlation distance between the two specified vectors
     * according to the specified preference vectors.
     *
     * @param v1  first RealVector
     * @param v2  second RealVector
     * @param pv1 the first preference vector
     * @param pv2 the second preference vector
     * @return the correlation distance between the two specified vectors
     */
    public abstract PreferenceVectorBasedCorrelationDistance correlationDistance(V v1, V v2, BitSet pv1, BitSet pv2);

    /**
     * Computes the weighted distance between the two specified vectors
     * according to the given preference vector.
     *
     * @param v1           the first vector
     * @param v2           the second vector
     * @param weightVector the preference vector
     * @return the weighted distance between the two specified vectors according to the given preference vector
     */
    public double weightedDistance(V v1, V v2, BitSet weightVector) {
        if (v1.getDimensionality() != v2.getDimensionality()) {
            throw new IllegalArgumentException("Different dimensionality of NumberVectors\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
        }

        double sqrDist = 0;
        for (int i = 1; i <= v1.getDimensionality(); i++) {
            if (weightVector.get(i - 1)) {
                double manhattanI = v1.getValue(i).doubleValue() - v2.getValue(i).doubleValue();
                sqrDist += manhattanI * manhattanI;
            }
        }
        return Math.sqrt(sqrDist);
    }

    /**
     * Computes the weighted distance between the two specified vectors
     * according to the given preference vector.
     *
     * @param id1          the id of the first vector
     * @param id2          the id of the second vector
     * @param weightVector the preference vector
     * @return the weighted distance between the two specified vectors according to the given preference vector
     */
    public double weightedDistance(Integer id1, Integer id2, BitSet weightVector) {
        return weightedDistance(getDatabase().get(id1), getDatabase().get(id2), weightVector);
    }

    /**
     * Computes the weighted distance between the two specified data vectors
     * according to their preference vectors.
     *
     * @param v1 the first vector
     * @param v2 the the second vector
     * @return the weighted distance between the two specified vectors
     *         according to the preference vector of the first data vector
     */
    public double weightedPrefereneceVectorDistance(V v1, V v2) {
        double d1 = weightedDistance(v1, v2, getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, v1.getID()));
        double d2 = weightedDistance(v2, v1, getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, v2.getID()));

        return Math.max(d1, d2);
    }

    /**
     * Computes the weighted distance between the two specified data vectors
     * according to their preference vectors.
     *
     * @param id1 the id of the first vector
     * @param id2 the id of the second vector
     * @return the weighted distance between the two specified vectors
     *         according to the preference vector of the first data vector
     */
    public double weightedPrefereneceVectorDistance(Integer id1, Integer id2) {
        return weightedPrefereneceVectorDistance(getDatabase().get(id1), getDatabase().get(id2));
    }

    /**
     * Calls the super method
     * and sets additionally the value of the parameter
     * {@link #EPSILON_PARAM}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // epsilon
        epsilon = EPSILON_PARAM.getValue();

        return remainingParameters;
    }

    /**
     * Returns epsilon.
     *
     * @return epsilon
     */
    public double getEpsilon() {
        return epsilon;
    }

    /**
     * Returns the association ID for the association to be set by the preprocessor.
     *
     * @return the association ID for the association to be set by the preprocessor,
     *         which is {@link AssociationID#PREFERENCE_VECTOR}
     */
    public final AssociationID<?> getAssociationID() {
        return AssociationID.PREFERENCE_VECTOR;
    }

    /**
     * @return the super class for the preprocessor parameter,
     *         which is {@link PreferenceVectorPreprocessor}
     */
    @SuppressWarnings("unchecked")
    public final Class<? extends Preprocessor> getPreprocessorSuperClass() {
        return PreferenceVectorPreprocessor.class;
    }

    public final String getPreprocessorDescription() {
        return "Classname of the preprocessor to determine the preference vector of each object "
            + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(getPreprocessorSuperClass()) +
            ".";
    }

    /**
     * Returns the dimensionality of the database.
     *
     * @return the dimensionality of the database, -1 if no database is assigned.
     */
    private int dimensionality() {
        if (getDatabase() != null) {
            return getDatabase().dimensionality();
        }
        else return -1;
    }
}
