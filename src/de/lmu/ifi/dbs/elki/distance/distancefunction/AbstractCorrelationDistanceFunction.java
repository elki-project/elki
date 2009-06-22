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
     * correlationDistance(v1, v2)}.
     *
     * @return the Correlation distance between the given two vectors as an
     *         instance of {@link CorrelationDistance CorrelationDistance}.
     */
    public final D distance(V v1, V v2) {
        return correlationDistance(v1, v2);
    }

    @Override
    public String shortDescription() {
        return "Correlation distance for real vectors.\n";
    }

    /**
     * Computes the correlation distance between the two specified vectors.
     *
     * @param v1 first RealVector
     * @param v2 second RealVector
     * @return the correlation distance between the two specified vectors
     */
    abstract D correlationDistance(V v1, V v2);
}
