package de.lmu.ifi.dbs.elki.math.spacefillingcurves;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * Computes the z-values for specified double values.
 *
 * @author Elke Achtert
 */
public class ZCurve {
    /**
     * The logger of this class.
     */
    private static Logging logger = Logging.getLogger(ZCurve.class);

    public ZCurve() {
        // nothing to do.
    }

    /**
     * Computes the z-values for the specified double values.
     *
     * @param valuesList the list of double values
     * @return the z-values for the specified double values
     */
    public static List<byte[]> zValues(List<double[]> valuesList) {
        if (valuesList.isEmpty()) return new ArrayList<byte[]>();

        // determine min and max value in each dimension and the scaling factor
        int dimensionality = valuesList.get(0).length;
        double[] minValues = new double[dimensionality];
        double[] maxValues = new double[dimensionality];
        Arrays.fill(minValues, Double.MAX_VALUE);
        Arrays.fill(maxValues, -Double.MAX_VALUE);
        for (double[] values : valuesList) {
            for (int d = 0; d < dimensionality; d++) {
                maxValues[d] = Math.max(values[d], maxValues[d]);
                minValues[d] = Math.min(values[d], minValues[d]);
            }
        }

        double[] scalingFactors = new double[dimensionality];
        for (int d = 0; d < dimensionality; d++) {
            // has to be > 0!!!
            scalingFactors[d] = (Long.MAX_VALUE) / (maxValues[d] - minValues[d]);
        }

        if (logger.isDebugging()) {
            StringBuffer msg = new StringBuffer();
            msg.append("\nmin   ").append(FormatUtil.format(minValues));
            msg.append("\nmax   ").append(FormatUtil.format(maxValues));
            msg.append("\nscale ").append(FormatUtil.format(scalingFactors));
            msg.append("\nLong.MAX_VALUE  " + Long.MAX_VALUE);
            msg.append("\nLong.MIN_VALUE  " + Long.MIN_VALUE);
            logger.debugFine(msg.toString());
        }

        // discretize the double value over the whole domain
        final List<byte[]> zValues = new ArrayList<byte[]>();
        for (double[] values : valuesList) {
            // convert the double values to long values
            long[] longValues = new long[values.length];
            for (int d = 0; d < values.length; d++) {
                longValues[d] = (long) ((values[d] - minValues[d]) * scalingFactors[d]);
            }

            if (logger.isDebugging()) {
                StringBuffer msg = new StringBuffer();
                msg.append("\ndouble values ").append(FormatUtil.format(values));
                msg.append("\nlong values   ").append(FormatUtil.format(longValues));
                logger.debugFine(msg.toString());
            }
            byte[] zValue = zValue(longValues);
            zValues.add(zValue);
        }

        return zValues;
    }

    /**
     * Computes the z-value for the specified long values
     *
     * @param longValues the array of the discretized double values
     * @return the z-value for the specified long values
     */
    private static byte[] zValue(long[] longValues) {
        byte[] zValues = new byte[longValues.length * 8];

        //convert longValues into zValues
        for (int shift = 0; shift < 64; shift++) {
            for (int dim = 0; dim < longValues.length; dim++) {
                int bitpos = shift * longValues.length + dim;    // bit position in zValues array
                zValues[bitpos >> 3] |= ((longValues[dim] >> shift) & 1) << (bitpos & 7);
            }
        }

        if (logger.isDebugging()) {
            //convert zValues to longValues
            long[] loutput = new long[longValues.length];
            for (int shift = 0; shift < 64; shift++) {
                for (int dim = 0; dim < longValues.length; dim++) {
                    int bitpos = shift * longValues.length + dim;    // bit position in zValues array
                    loutput[dim] |= ((long) (((zValues[bitpos >> 3] >> (bitpos & 7)) & 1))) << shift;
                }
            }
            StringBuffer msg = new StringBuffer();
            logger.debugFine(msg.toString());
        }

        return zValues;
    }
}
