package de.lmu.ifi.dbs.elki.math.spacefillingcurves;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
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
  private static final Logging logger = Logging.getLogger(ZCurve.class);

  /**
   * Fake Constructor - use the static methods instead!
   */
  private ZCurve() {
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

  /**
   * Class to transform a relation to its Z coordinates.
   * 
   * @author Erich Schubert
   */
  public static class Transformer {
    /**
     * Maximum values in each dimension
     */
    private final double[] maxValues;
  
    /**
     * Minimum values in each dimension
     */
    private final double[] minValues;
  
    /**
     * Dimensionality
     */
    private final int dimensionality;
  
    /**
     * Constructor.
     * 
     * @param relation Relation to transform
     * @param ids IDs subset to process
     */
    public Transformer(Relation<? extends NumberVector<?, ?>> relation, DBIDs ids) {
      this.dimensionality = DatabaseUtil.dimensionality(relation);
      this.minValues = new double[dimensionality];
      this.maxValues = new double[dimensionality];
  
      // Compute scaling of vector space
      Arrays.fill(minValues, Double.POSITIVE_INFINITY);
      Arrays.fill(maxValues, Double.NEGATIVE_INFINITY);
      for(DBID id : ids) {
        NumberVector<?, ?> vector = relation.get(id);
        for(int dim = 0; dim < dimensionality; ++dim) {
          double dimValue = vector.doubleValue(dim + 1);
          minValues[dim] = Math.min(minValues[dim], dimValue);
          maxValues[dim] = Math.max(maxValues[dim], dimValue);
        }
      }
    }
  
    /**
     * Transform a single vector.
     * 
     * @param vector Vector to transform
     * @return Z curve value as bigint
     */
    public BigInteger asBigInteger(NumberVector<?, ?> vector) {
      return new BigInteger(asByteArray(vector));
    }

    /**
     * Transform a single vector.
     * 
     * @param vector Vector to transform
     * @return Z curve value as byte array
     */
    public byte[] asByteArray(NumberVector<?, ?> vector) {
      final long[] longValueList = new long[dimensionality];
  
      for(int dim = 0; dim < dimensionality; ++dim) {
        final double minValue = minValues[dim];
        final double maxValue = maxValues[dim];
        double dimValue = vector.doubleValue(dim + 1);
  
        dimValue = (dimValue - minValue) / (maxValue - minValue);
        longValueList[dim] = (long) (dimValue * (Long.MAX_VALUE));
      }
  
      final byte[] bytes = new byte[Long.SIZE * dimensionality * (Long.SIZE / Byte.SIZE)];
      int shiftCounter = 0;
      for(int i = 0; i < Long.SIZE; ++i) {
        for(int dim = 0; dim < dimensionality; ++dim) {
          long byteValue = longValueList[dim];
  
          int localShift = shiftCounter % Byte.SIZE;
          bytes[(bytes.length - 1) - (shiftCounter / Byte.SIZE)] |= ((byteValue >> i) & 0x01) << localShift;
  
          shiftCounter++;
        }
      }
      return bytes;
    }
  }
}