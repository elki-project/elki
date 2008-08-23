package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;

import java.util.regex.Pattern;

/**
 * Provides an abstract superclass for KernelFunctions that are based on DoubleDistance.
 *
 * @author Elke Achtert
 */
public abstract class AbstractDoubleKernelFunction<O extends DatabaseObject> extends AbstractKernelFunction<O, DoubleDistance> {
    /**
     * Provides a AbstractDoubleKernelFunction with a pattern defined to accept
     * Strings that define a non-negative Double.
     */
    protected AbstractDoubleKernelFunction() {
        super(Pattern.compile("\\d+(\\.\\d+)?([eE][-]?\\d+)?"));
    }

    /**
     * An infinite DoubleDistance is based on
     * {@link Double#POSITIVE_INFINITY Double.POSITIVE_INFINITY}.
     */
    public DoubleDistance infiniteDistance() {
        return new DoubleDistance(1.0 / 0.0);
    }

    /**
     * A null DoubleDistance is based on 0.
     */
    public DoubleDistance nullDistance() {
        return new DoubleDistance(0);
    }

    /**
     * An undefined DoubleDistance is based on {@link Double#NaN Double.NaN}.
     */
    public DoubleDistance undefinedDistance() {
        return new DoubleDistance(0.0 / 0.0);
    }

    /**
     * As pattern is required a String defining a Double.
     */
    public DoubleDistance valueOf(String pattern)
        throws IllegalArgumentException {
        if (pattern.equals(INFINITY_PATTERN))
            return infiniteDistance();

        if (matches(pattern)) {
            return new DoubleDistance(Double.parseDouble(pattern));
        }
        else {
            throw new IllegalArgumentException("Given pattern \"" + pattern
                + "\" does not match required pattern \""
                + requiredInputPattern() + "\"");
        }
    }

    public DoubleDistance distance(final O fv1, final O fv2) {
        return new DoubleDistance(Math.sqrt(similarity(fv1, fv1).getValue()
            + similarity(fv2, fv2).getValue()
            - 2 * similarity(fv1, fv2).getValue()));
    }


}
