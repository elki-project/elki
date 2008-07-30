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
 */
public abstract class AbstractPreferenceVectorBasedCorrelationDistanceFunction<O extends RealVector<O, ?>, P extends Preprocessor<O>>
    extends AbstractCorrelationDistanceFunction<O, P, PreferenceVectorBasedCorrelationDistance> {

    /**
     * OptionID for {@link #EPSILON_PARAM}
     */
    public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID(
        "pvbasedcorrelationdf.epsilon",
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

    /**
     * Provides an infinite distance.
     *
     * @return an infinite distance
     */
    public PreferenceVectorBasedCorrelationDistance infiniteDistance() {
        return new PreferenceVectorBasedCorrelationDistance(
            getDatabase().dimensionality(),
            Integer.MAX_VALUE,
            Double.POSITIVE_INFINITY,
            new BitSet());
    }

    /**
     * Provides a null distance.
     *
     * @return a null distance
     */
    public PreferenceVectorBasedCorrelationDistance nullDistance() {
        return new PreferenceVectorBasedCorrelationDistance(
            getDatabase().dimensionality(),
            0,
            0,
            new BitSet());
    }

    /**
     * Provides an undefined distance.
     *
     * @return an undefined distance
     */
    public PreferenceVectorBasedCorrelationDistance undefinedDistance() {
        return new PreferenceVectorBasedCorrelationDistance(
            getDatabase().dimensionality(),
            -1,
            Double.NaN,
            new BitSet());
    }

    /**
     * @see de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractCorrelationDistanceFunction#correlationDistance(de.lmu.ifi.dbs.elki.data.RealVector,de.lmu.ifi.dbs.elki.data.RealVector)
     */
    protected PreferenceVectorBasedCorrelationDistance correlationDistance(O v1, O v2) {
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
    public abstract PreferenceVectorBasedCorrelationDistance correlationDistance(O v1, O v2, BitSet pv1, BitSet pv2);

    /**
     * Computes the weighted distance between the two specified vectors
     * according to the given preference vector.
     *
     * @param dv1          the first vector
     * @param dv2          the second vector
     * @param weightVector the preference vector
     * @return the weighted distance between the two specified vectors according to the given preference vector
     */
    public double weightedDistance(O dv1, O dv2, BitSet weightVector) {
        if (dv1.getDimensionality() != dv2.getDimensionality()) {
            throw new IllegalArgumentException("Different dimensionality of NumberVectors\n  first argument: " + dv1.toString() + "\n  second argument: " + dv2.toString());
        }

        double sqrDist = 0;
        for (int i = 1; i <= dv1.getDimensionality(); i++) {
            if (weightVector.get(i - 1)) {
                double manhattanI = dv1.getValue(i).doubleValue() - dv2.getValue(i).doubleValue();
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
     * @param rv1 the first vector
     * @param rv2 the the second vector
     * @return the weighted distance between the two specified vectors
     *         according to the preference vector of the first data vector
     */
    public double weightedPrefereneceVectorDistance(O rv1, O rv2) {
        double d1 = weightedDistance(rv1, rv2, getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, rv1.getID()));
        double d2 = weightedDistance(rv2, rv1, getDatabase().getAssociation(AssociationID.PREFERENCE_VECTOR, rv2.getID()));

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
     * Calls {@link de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPreprocessorBasedDistanceFunction#setParameters(String[])
     * AbstractPreprocessorBasedDistanceFunction#setParameters(args)}
     * and sets additionally the value of the parameter
     * {@link #EPSILON_PARAM}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // epsilon
        epsilon = getParameterValue(EPSILON_PARAM);

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
     * Returns the assocoiation ID for the association to be set by the preprocessor.
     *
     * @return the assocoiation ID for the association to be set by the preprocessor,
     *         which is {@link AssociationID#PREFERENCE_VECTOR}
     * @see AbstractPreprocessorBasedDistanceFunction#getAssociationID()
     */
    final AssociationID getAssociationID() {
        return AssociationID.PREFERENCE_VECTOR;
    }

    /**
     * Returns the super class for the preprocessor.
     *
     * @return the super class for the preprocessor,
     *         which is {@link PreferenceVectorPreprocessor}
     * @see AbstractPreprocessorBasedDistanceFunction#getPreprocessorSuperClassName()
     */
    final Class<PreferenceVectorPreprocessor> getPreprocessorSuperClassName() {
        return PreferenceVectorPreprocessor.class;
    }


    /**
     * @see AbstractPreprocessorBasedDistanceFunction#getPreprocessorClassDescription()
     */
    final String getPreprocessorClassDescription() {
        return "Classname of the preprocessor to determine the preference vectors of the objects "
            + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(getPreprocessorSuperClassName());
    }
}
