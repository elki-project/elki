package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.distance.CorrelationDistance;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;

import java.util.regex.Pattern;

/**
 * Abstract super class for correlation based distance functions. Provides the
 * correlation distance for real valued vectors.
 *
 * @author Elke Achtert
 * @param <V> the type of RealVector used
 * @param <P> the type of Preprocessor used
 * @param <D> the type of CorrelationDistance used
 */
public abstract class AbstractCorrelationDistanceFunction<V extends RealVector<V, ?>, P extends Preprocessor<V>, D extends CorrelationDistance<D>>
    extends AbstractPreprocessorBasedDistanceFunction<V, P, D> {

    /**
     * Indicates a separator.
     */
    public static final Pattern SEPARATOR = Pattern.compile("x");

    /**
     * Provides a CorrelationDistanceFunction with a pattern defined to accept
     * Strings that define an Integer followed by a separator followed by a
     * Double.
     */
    public AbstractCorrelationDistanceFunction() {
        super(Pattern.compile("\\d+" + AbstractCorrelationDistanceFunction.SEPARATOR.pattern() + "\\d+(\\.\\d+)?([eE][-]?\\d+)?"));
    }

    /**
     * Provides the Correlation distance between the given two vectors by
     * calling {@link #correlationDistance(de.lmu.ifi.dbs.elki.data.RealVector,de.lmu.ifi.dbs.elki.data.RealVector)
     * correlationDistance(rv1, rv2)}.
     *
     * @return the Correlation distance between the given two vectors as an
     *         instance of {@link CorrelationDistance CorrelationDistance}.
     * @see DistanceFunction#distance(de.lmu.ifi.dbs.elki.data.DatabaseObject,de.lmu.ifi.dbs.elki.data.DatabaseObject)
     */
    public final D distance(V rv1, V rv2) {
        return correlationDistance(rv1, rv2);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#parameterDescription()
     */
    @Override
    public String parameterDescription() {
        return "Correlation distance for real vectors. " + super.parameterDescription();
    }

    /**
     * Computes the correlation distance between the two specified vectors.
     *
     * @param dv1 first RealVector
     * @param dv2 second RealVector
     * @return the correlation distance between the two specified vectors
     */
    abstract D correlationDistance(V dv1, V dv2);
}
