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

  /**
   * Constructor
   */
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
    if(valuesList.isEmpty()) {
      return new ArrayList<byte[]>();
    }

    // determine min and max value in each dimension and the scaling factor
    int dimensionality = valuesList.get(0).length;
    double[] minValues = new double[dimensionality];
    double[] maxValues = new double[dimensionality];
    Arrays.fill(minValues, Double.POSITIVE_INFINITY);
    Arrays.fill(maxValues, Double.NEGATIVE_INFINITY);
    for(double[] values : valuesList) {
      for(int d = 0; d < dimensionality; d++) {
        maxValues[d] = Math.max(values[d], maxValues[d]);
        minValues[d] = Math.min(values[d], minValues[d]);
      }
    }

    double[] normalizationFactors = new double[dimensionality];
    for(int d = 0; d < dimensionality; d++) {
      // has to be > 0!!!
      normalizationFactors[d] = (maxValues[d] - minValues[d]);
    }

    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append("min   ").append(FormatUtil.format(minValues));
      msg.append("\nmax   ").append(FormatUtil.format(maxValues));
      msg.append("\nscale ").append(FormatUtil.format(normalizationFactors));
      msg.append("\nLong.MAX_VALUE  " + Long.MAX_VALUE);
      msg.append("\nLong.MIN_VALUE  " + Long.MIN_VALUE);
      logger.debugFine(msg.toString());
    }

    // discretize the double value over the whole domain
    final List<byte[]> zValues = new ArrayList<byte[]>();
    for(double[] values : valuesList) {
      // convert the double values to long values
      long[] longValues = new long[values.length];
      for(int d = 0; d < values.length; d++) {
        // normalize to 0:1
        final double normval = ((values[d] - minValues[d]) / normalizationFactors[d]);
        longValues[d] = (long) (normval * Long.MAX_VALUE);
      }

      if(logger.isDebugging()) {
        StringBuffer msg = new StringBuffer();
        msg.append("double values ").append(FormatUtil.format(values));
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
    final int numdim = longValues.length;
    final int numbits = numdim * 64;
    byte[] zValues = new byte[numbits / 8];

    // convert longValues into zValues
    for(int bitnum = 0; bitnum < numbits; bitnum ++) {
      // split into in-byte and in-array indexes
      final int lowpos = bitnum & 7;
      final int higpos = bitnum >> 3;
    
      final int dim = bitnum % numdim;
      final int shift = 63 - (bitnum / numdim);
      final byte val = (byte) ((longValues[dim] >> shift) & 1);
      
      zValues[higpos] |= val << (7 - lowpos);
    }
    /*for(int shift = 0; shift < 64; shift++) {
      for(int dim = 0; dim < longValues.length; dim++) {
        // destination bit position
        final int bitpos = shift  * longValues.length + dim;
        // split into in-byte and in-array indexes
        final int lowpos = bitpos & 7;
        final int higpos = bitpos >> 3;
        zValues[higpos] |= ((longValues[dim] >> (shift)) & 1) << lowpos;
      }
    }*/

    if(logger.isDebugging()) {
      // convert zValues to longValues
      long[] loutput = new long[longValues.length];
      for(int shift = 0; shift < 64; shift++) {
        for(int dim = 0; dim < longValues.length; dim++) {
          // destination bit position
          final int bitpos = shift * longValues.length + dim;
          // split into in-byte and in-array indexes
          final int lowpos = bitpos & 7;
          final int higpos = bitpos >> 3;
          loutput[dim] |= ((long) (((zValues[higpos] >> (lowpos)) & 1))) << (shift);
        }
      }
      StringBuffer msg = new StringBuffer();
      msg.append("reconstructed values:   ").append(FormatUtil.format(loutput));
      logger.debugFine(msg.toString());
    }

    return zValues;
  }
}