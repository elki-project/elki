package de.lmu.ifi.dbs.elki.data;

import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.output.Format;

/**
 * A parameterization function decribes all lines in a
 * d-dimensional feature space intersecting in one point p.
 * A single line in d-dimensional space is uniquely determined by a
 * translation vector p and (d-1) angles alpha_i belonging to the normal vector n.
 *
 * @author Elke Achtert
 */
public class ParameterizationFunction extends DoubleVector {
    /**
     * Available types for the global extremum.
     */
    public enum ExtremumType {
        MINIMUM,
        MAXIMUM,
        CONSTANT
    }

    /**
     * A small number to handle numbers near 0 as 0.
     */
    public static final double DELTA = 1E-10;

    /**
     * Holds the alpha values of the global extremum.
     */
    private double[] alphaExtremum;

    /**
     * Holds the type of the global extremum.
     */
    private ExtremumType extremumType;

    /**
     * Provides a new parameterization function decribing all lines in a
     * d-dimensional feature space intersecting in one point p.
     *
     * @param p the values of the point p
     */
    public ParameterizationFunction(double[] p) {
        super(p);
//    this.debug = true;
        determineGlobalExtremum();

        if (debug) {
            StringBuffer msg = new StringBuffer();
            msg.append("\np = ").append(Format.format(p));
            msg.append("\n").append(this.toString());
            msg.append("\nextremum ").append(Util.format(alphaExtremum)).append(", type ").append(extremumType);
            msg.append("\nvalue = ").append(function(alphaExtremum));
            msg.append("\n");
            this.debugFine(msg.toString());
        }
    }

    /**
     * @return a new DoubleVector with the specified values
     */
    @Override
    public DoubleVector newInstance(double[] values) {
        return new ParameterizationFunction(values);
    }

    /**
     * Computes the function value at <code>alpha</code>.
     *
     * @param alpha the values of the d-1 angles
     * @return the function value at alpha
     */
    public double function(double[] alpha) {
        int d = getDimensionality();
        if (alpha.length != d - 1) {
            throw new IllegalArgumentException("Parameter alpha must have a " +
                "dimensionality of " + (d - 1) +
                ", read: " + alpha.length);
        }

        double result = 0;
        for (int i = 0; i < d; i++) {
            double alpha_i = i == d - 1 ? 0 : alpha[i];
            result += getValue(i + 1) * sinusProduct(0, i, alpha) * Math.cos(alpha_i);
        }
        return result;
    }

    /**
     * Determines the alpha values where this function
     * has a minumum and maximum value in the given interval.
     *
     * @param interval the hyper bounding box defining the interval
     * @return he alpha values where this function
     *         has a minumum and maximum value in the given interval
     */
    public HyperBoundingBox determineAlphaMinMax(HyperBoundingBox interval) {
        int dim = getDimensionality();
        if (interval.getDimensionality() != dim - 1) {
            throw new IllegalArgumentException("Interval needs to have dimensionality d=" + (dim - 1) +
                ", read: " + interval.getDimensionality());
        }

        if (extremumType.equals(ExtremumType.CONSTANT)) {
            double[] centroid = interval.centroid();
            return new HyperBoundingBox(centroid, centroid);
        }

        double[] alpha_min = new double[dim - 1];
        double[] alpha_max = new double[dim - 1];

        if (interval.contains(alphaExtremum)) {
            if (extremumType.equals(ExtremumType.MINIMUM)) {
                alpha_min = alphaExtremum;
                for (int d = dim - 2; d >= 0; d--) {
                    alpha_max[d] = determineAlphaMax(d, alpha_max, interval);
                }
            }
            else {
                alpha_max = alphaExtremum;
                for (int d = dim - 2; d >= 0; d--) {
                    alpha_min[d] = determineAlphaMin(d, alpha_min, interval);
                }
            }
        }
        else {
            for (int d = dim - 2; d >= 0; d--) {
                alpha_min[d] = determineAlphaMin(d, alpha_min, interval);
                alpha_max[d] = determineAlphaMax(d, alpha_max, interval);
            }
        }

        return new HyperBoundingBox(alpha_min, alpha_max);
    }

    /**
     * Returns the type of the extremum at the specified alpha values.
     *
     * @param n             the index until the alpha values are computed
     * @param alpha_extreme the already computed alpha values
     * @param interval      the hyper bounding box defining the interval in which the extremum occurs
     * @return the type of the extremum at the specified alpha_values
     */
    private ExtremumType extremumType(int n, double[] alpha_extreme, HyperBoundingBox interval) {
        // return the type of the global extremum
        if (n == alpha_extreme.length - 1) {
            return extremumType;
        }

        // create random alpha values
        double[] alpha_extreme_l = new double[alpha_extreme.length];
        double[] alpha_extreme_r = new double[alpha_extreme.length];
        double[] alpha_extreme_c = new double[alpha_extreme.length];

        System.arraycopy(alpha_extreme, 0, alpha_extreme_l, 0, alpha_extreme.length);
        System.arraycopy(alpha_extreme, 0, alpha_extreme_r, 0, alpha_extreme.length);
        System.arraycopy(alpha_extreme, 0, alpha_extreme_c, 0, alpha_extreme.length);

        double[] centroid = interval.centroid();
        for (int i = 0; i < n; i++) {
            alpha_extreme_l[i] = centroid[i];
            alpha_extreme_r[i] = centroid[i];
            alpha_extreme_c[i] = centroid[i];
        }

        double intervalLength = interval.getMax(n + 1) - interval.getMin(n + 1);
        alpha_extreme_l[n] = Math.random() * intervalLength + interval.getMin(n + 1);
        alpha_extreme_r[n] = Math.random() * intervalLength + interval.getMin(n + 1);

        double f_c = function(alpha_extreme_c);
        double f_l = function(alpha_extreme_l);
        double f_r = function(alpha_extreme_r);

        if (f_l < f_c) {
            if (f_r < f_c || Math.abs(f_r - f_c) < DELTA) {
                return ExtremumType.MAXIMUM;
            }
        }
        if (f_r < f_c) {
            if (f_l < f_c || Math.abs(f_l - f_c) < DELTA) {
                return ExtremumType.MAXIMUM;
            }
        }

        if (f_l > f_c) {
            if (f_r > f_c || Math.abs(f_r - f_c) < DELTA) {
                return ExtremumType.MINIMUM;
            }
        }
        if (f_r > f_c) {
            if (f_l > f_c || Math.abs(f_l - f_c) < DELTA) {
                return ExtremumType.MINIMUM;
            }
        }

        if (Math.abs(f_l - f_c) < DELTA && Math.abs(f_r - f_c) < DELTA) {
            return ExtremumType.CONSTANT;
        }

        throw new IllegalArgumentException("Houston, we have a problem!\n" +
            getID() + "\n" +
            this + "\n" +
            "f_l " + f_l + "\n" +
            "f_c " + f_c + "\n" +
            "f_r " + f_r + "\n" +
            "p " + getColumnVector() + "\n" +
            "box min " + Format.format(interval.getMin()) + "\n" +
            "box max " + Format.format(interval.getMax()) + "\n" +
            "alpha   " + Format.format(alpha_extreme_c) + "\n" +
            "alpha_l " + Format.format(alpha_extreme_l) + "\n" +
            "alpha_r " + Format.format(alpha_extreme_r) + "\n" +
            "n " + n);

    }

    /**
     * Determines the n-th alpha value where this function has a minimum in
     * the specified interval.
     *
     * @param n         the index of the alpha value to be determined
     * @param alpha_min the already computed alpha values
     * @param interval  the hyper bounding box defining the interval
     * @return the n-th alpha value where this function has a minimum in
     *         the specified interval
     */
    private double determineAlphaMin(int n, double[] alpha_min, HyperBoundingBox interval) {
        double alpha_n = extremum_alpha_n(n, alpha_min);
        double lower = interval.getMin(n + 1);
        double upper = interval.getMax(n + 1);

        double[] alpha_extreme = new double[alpha_min.length];
        System.arraycopy(alpha_min, n, alpha_extreme, n, alpha_extreme.length - n);
        alpha_extreme[n] = alpha_n;

        ExtremumType type = extremumType(n, alpha_extreme, interval);
        if (type.equals(ExtremumType.MINIMUM) || type.equals(ExtremumType.CONSTANT)) {
            // A) lower <= alpha_n <= upper
            if (lower <= alpha_n && alpha_n <= upper) {
                return alpha_n;
            }
            // B) alpha_n < upper
            else if (alpha_n < lower) {
                return lower;
            }
            // C) alpha_n > max
            else {
                if (alpha_n <= upper) {
                    throw new IllegalStateException("Should never happen!");
                }
                return upper;
            }
        }
        // extremum is maximum
        else {
            if (lower <= alpha_n && alpha_n <= upper) {
                // A1) min <= alpha_n <= max  && alpha_n - min <= max - alpha_n
                if (alpha_n - lower <= upper - alpha_n) {
                    return upper;
                }
                // A2) min <= alpha_n <= max  && alpha_n - min > max - alpha_n
                else {
                    return lower;
                }
            }
            // B) alpha_n < min
            else if (alpha_n < lower) {
                return upper;
            }
            // C) alpha_n > max
            else {
                if (alpha_n <= upper) {
                    throw new IllegalStateException("Should never happen!");
                }
                return lower;
            }
        }
    }

    /**
     * Determines the n-th alpha value where this function has a maximum in
     * the specified interval.
     *
     * @param n         the index of the alpha value to be determined
     * @param alpha_max the already computed alpha values
     * @param interval  the hyper bounding box defining the interval
     * @return the n-th alpha value where this function has a minimum in
     *         the specified interval
     */
    private double determineAlphaMax(int n, double[] alpha_max, HyperBoundingBox interval) {
        double alpha_n = extremum_alpha_n(n, alpha_max);
        double lower = interval.getMin(n + 1);
        double upper = interval.getMax(n + 1);

        double[] alpha_extreme = new double[alpha_max.length];
        System.arraycopy(alpha_max, n, alpha_extreme, n, alpha_extreme.length - n);
        alpha_extreme[n] = alpha_n;

        ExtremumType type = extremumType(n, alpha_extreme, interval);
        if (type.equals(ExtremumType.MINIMUM) || type.equals(ExtremumType.CONSTANT)) {
            if (lower <= alpha_n && alpha_n <= upper) {
                // A1) min <= alpha_n <= max  && alpha_n - min <= max - alpha_n
                if (alpha_n - lower <= upper - alpha_n) {
                    return upper;
                }
                // A2) min <= alpha_n <= max  && alpha_n - min > max - alpha_n
                else {
                    return lower;
                }
            }
            // B) alpha_n < min
            else if (alpha_n < lower) {
                return upper;
            }
            // C) alpha_n > max
            else {
                if (alpha_n <= upper) {
                    throw new IllegalStateException("Should never happen!");
                }
                return lower;
            }
        }
        // extremum is maximum
        else {
            // A) min <= alpha_n <= max
            if (lower <= alpha_n && alpha_n <= upper) {
                return alpha_n;
            }
            // B) alpha_n < min
            else if (alpha_n < lower) {
                return lower;
            }
            // C) alpha_n > max
            else {
                if (alpha_n <= upper) {
                    throw new IllegalStateException("Should never happen!");
                }
                return upper;
            }
        }
    }

    /**
     * Returns the alpha values of the extremum point in interval [(0,...,0), (Pi,...,Pi)].
     *
     * @return the alpha values of the extremum
     */
    public double[] getGlobalAlphaExtremum() {
        return alphaExtremum;
    }

    /**
     * Returns the global extremum of this function in interval [0,...,Pi)^d-1.
     *
     * @return the global extremum
     */
    public double getGlobalExtremum() {
        return function(alphaExtremum);
    }

    /**
     * Returns the type of the global extremum in interval [0,...,Pi)^d-1.
     *
     * @return the type of the global extremum
     */
    public ExtremumType getGlobalExtremumType() {
        return extremumType;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return toString(0);
    }

    /**
     * Returns a string representation of the object
     * with the specified offset.
     *
     * @param offset the offset of the string representation
     * @return a string representation of the object.
     */
    public String toString(int offset) {
        StringBuffer result = new StringBuffer();
        for (int d = 0; d < getDimensionality(); d++) {
            if (d != 0) {
                result.append(" + \n").append(Format.blanks(offset));
            }
            result.append(Util.format(getValue(d + 1)));
            for (int j = 0; j < d; j++) {
                result.append(" * sin(a_").append(j + 1).append(")");
            }
            if (d != getDimensionality() - 1) {
                result.append(" * cos(a_").append(d + 1).append(")");
            }
        }
        return result.toString();
    }

    /**
     * Computes the product of all sinus values of the specified angles
     * from start to end index.
     *
     * @param start the index to start
     * @param end   the index to end
     * @param alpha the array of angles
     * @return the product of all sinus values of the specified angles
     *         from start to end index
     */
    private double sinusProduct(int start, int end, double[] alpha) {
        double result = 1;
        for (int j = start; j < end; j++) {
            result *= Math.sin(alpha[j]);
        }
        return result;
    }

    /**
     * Determines the global extremum of this parameterization function.
     */
    private void determineGlobalExtremum() {
        alphaExtremum = new double[getDimensionality() - 1];
        for (int n = alphaExtremum.length - 1; n >= 0; n--) {
            alphaExtremum[n] = extremum_alpha_n(n, alphaExtremum);
            if (Double.isNaN(alphaExtremum[n])) {
                throw new IllegalStateException("Houston, we have a problem!" +
                    "\n" + this +
                    "\n" + this.getColumnVector() +
                    "\n" + Format.format(alphaExtremum));
            }
        }

        determineGlobalExtremumType();
    }

    /**
     * Determines the type od the global extremum.
     */
    private void determineGlobalExtremumType() {
        double f = function(alphaExtremum);

        // create random alpha values
        double[] alpha_1 = new double[alphaExtremum.length];
        double[] alpha_2 = new double[alphaExtremum.length];
        for (int i = 0; i < alphaExtremum.length; i++) {
            alpha_1[i] = Math.random() * Math.PI;
            alpha_2[i] = Math.random() * Math.PI;
        }

        // look if f1 and f2 are less, greater or equal to f
        double f1 = function(alpha_1);
        double f2 = function(alpha_2);

        if (f1 < f && f2 < f) {
            extremumType = ExtremumType.MAXIMUM;
        }
        else if (f1 > f && f2 > f) {
            extremumType = ExtremumType.MINIMUM;
        }
        else if (Math.abs(f1 - f) < DELTA && Math.abs(f2 - f) < DELTA) {
            extremumType = ExtremumType.CONSTANT;
        }
        else {
            throw new IllegalStateException("Houston, we have a problem:" +
                "\n" + this +
                "\nextremum at " + Format.format(alphaExtremum) +
                "\nf  " + f +
                "\nf1 " + f1 +
                "\nf2 " + f2);
        }


    }

    /**
     * Determines the value for alpha_n where this function has a (local) extremum.
     *
     * @param n     the index of the angle
     * @param alpha the already determined alpha_values for the extremum
     * @return the value for alpha_n where this function has a (local) extremum
     */
    private double extremum_alpha_n(int n, double[] alpha) {
        // arctan(infinity) = PI/2
        if (getValue(n + 1) == 0) {
            return 0.5 * Math.PI;
        }

        double tan = 0;
        for (int j = n + 1; j < getDimensionality(); j++) {
            double alpha_j = j == getDimensionality() - 1 ? 0 : alpha[j];
            tan += getValue(j + 1) * sinusProduct(n + 1, j, alpha) * Math.cos(alpha_j);
        }
        tan /= getValue(n + 1);

        if (debug) {
            debugFiner("tan alpha_" + (n + 1) + " = " + tan);
        }
        double alpha_n = Math.atan(tan);
        if (alpha_n < 0) {
            alpha_n = Math.PI + alpha_n;
        }
        return alpha_n;
    }
}
