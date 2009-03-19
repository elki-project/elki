package de.lmu.ifi.dbs.elki.utilities;

import java.text.NumberFormat;
import java.util.BitSet;
import java.util.Locale;

/**
 * Utility methods for output formatting of various number objects
 */
public final class FormatUtil {
  /**
   * Formats the double d with the specified fraction digits.
   * 
   * @param d the double array to be formatted
   * @param digits the number of fraction digits
   * @return a String representing the double d
   */
  public static String format(final double d, int digits) {
    final NumberFormat nf = NumberFormat.getInstance(Locale.US);
    nf.setMaximumFractionDigits(digits);
    nf.setMinimumFractionDigits(digits);
    nf.setGroupingUsed(false);
    return nf.format(d);
  }

  /**
   * Formats the double d with the specified number format.
   * 
   * @param d the double array to be formatted
   * @param nf the number format to be used for formatting
   * @return a String representing the double d
   */
  public static String format(final double d, NumberFormat nf) {
    return nf.format(d);
  }

  /**
   * Formats the double d with 2 fraction digits.
   * 
   * @param d the double to be formatted
   * @return a String representing the double d
   */
  public static String format(final double d) {
    return format(d, 2);
  }

  /**
   * Formats the double array d with the specified separator.
   * 
   * @param d the double array to be formatted
   * @param sep the separator between the single values of the double array,
   *        e.g. ','
   * @return a String representing the double array d
   */
  public static String format(double[] d, String sep) {
    StringBuffer buffer = new StringBuffer();
    for(int i = 0; i < d.length; i++) {
      if(i < d.length - 1) {
        buffer.append(d[i]).append(sep);
      }
      else {
        buffer.append(d[i]);
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the double array d with the specified separator and the specified
   * fraction digits.
   * 
   * @param d the double array to be formatted
   * @param sep the separator between the single values of the double array,
   *        e.g. ','
   * @param digits the number of fraction digits
   * @return a String representing the double array d
   */
  public static String format(double[] d, String sep, int digits) {
    StringBuffer buffer = new StringBuffer();
    for(int i = 0; i < d.length; i++) {
      if(i < d.length - 1) {
        buffer.append(format(d[i], digits)).append(sep);
      }
      else {
        buffer.append(format(d[i], digits));
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the double array d with the specified number format.
   * 
   * @param d the double array to be formatted
   * @param nf the number format to be used for formatting
   * @return a String representing the double array d
   */
  public static String format(double[] d, NumberFormat nf) {
    return format(d, " ", nf);
  }

  /**
   * Formats the double array d with the specified number format.
   * 
   * @param d the double array to be formatted
   * @param sep the separator between the single values of the double array,
   *        e.g. ','
   * @param nf the number format to be used for formatting
   * @return a String representing the double array d
   */
  public static String format(double[] d, String sep, NumberFormat nf) {
    StringBuffer buffer = new StringBuffer();
    for(int i = 0; i < d.length; i++) {
      if(i < d.length - 1) {
        buffer.append(format(d[i], nf)).append(sep);
      }
      else {
        buffer.append(format(d[i], nf));
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the double array d with ',' as separator and 2 fraction digits.
   * 
   * @param d the double array to be formatted
   * @return a String representing the double array d
   */
  public static String format(double[] d) {
    return format(d, ", ", 2);
  }

  /**
   * Formats the double array d with ',' as separator and 2 fraction digits.
   * 
   * @param d the double array to be formatted
   * @return a String representing the double array d
   */
  public static String format(double[][] d) {
    StringBuffer buffer = new StringBuffer();
    for(double[] array : d) {
      buffer.append(format(array, ", ", 2)).append("\n");
    }
    return buffer.toString();
  }

  /**
   * Formats the array of double arrays d with 'the specified separators and
   * fraction digits.
   * 
   * @param d the double array to be formatted
   * @param sep1 the first separator of the outer array
   * @param sep2 the second separator of the inner array
   * @param digits the number of fraction digits
   * @return a String representing the double array d
   */
  public static String format(double[][] d, String sep1, String sep2, int digits) {
    StringBuffer buffer = new StringBuffer();
  
    for(int i = 0; i < d.length; i++) {
      if(i < d.length - 1) {
        buffer.append(format(d[i], sep2, digits)).append(sep1);
      }
      else {
        buffer.append(format(d[i], sep2, digits));
      }
    }
  
    return buffer.toString();
  }

  /**
   * Formats the float array f with the specified separator and the specified
   * fraction digits.
   * 
   * @param f the float array to be formatted
   * @param sep the separator between the single values of the float array, e.g.
   *        ','
   * @param digits the number of fraction digits
   * @return a String representing the float array f
   */
  public static String format(float[] f, String sep, int digits) {
    StringBuffer buffer = new StringBuffer();
    for(int i = 0; i < f.length; i++) {
      if(i < f.length - 1) {
        buffer.append(format(f[i], digits)).append(sep);
      }
      else {
        buffer.append(format(f[i], digits));
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the float array f with ',' as separator and 2 fraction digits.
   * 
   * @param f the float array to be formatted
   * @return a String representing the float array f
   */
  public static String format(float[] f) {
    return format(f, ", ", 2);
  }

  /**
   * Formats the int array a for printing purposes.
   * 
   * @param a the int array to be formatted
   * @param sep the separator between the single values of the float array, e.g.
   *        ','
   * @return a String representing the int array a
   */
  public static String format(int[] a, String sep) {
    StringBuffer buffer = new StringBuffer();
    for(int i = 0; i < a.length; i++) {
      if(i < a.length - 1) {
        buffer.append(a[i]).append(sep);
      }
      else {
        buffer.append(a[i]);
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the int array a for printing purposes.
   * 
   * @param a the int array to be formatted
   * @return a String representing the int array a
   */
  public static String format(int[] a) {
    return format(a, ", ");
  }

  /**
   * Formats the Integer array a for printing purposes.
   * 
   * @param a the Integer array to be formatted
   * @param sep the separator between the single values of the float array, e.g.
   *        ','
   * @return a String representing the Integer array a
   */
  public static String format(Integer[] a, String sep) {
    StringBuffer buffer = new StringBuffer();
    for(int i = 0; i < a.length; i++) {
      if(i < a.length - 1) {
        buffer.append(a[i]).append(sep);
      }
      else {
        buffer.append(a[i]);
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the Integer array a for printing purposes.
   * 
   * @param a the Integer array to be formatted
   * @return a String representing the Integer array a
   */
  public static String format(Integer[] a) {
    return format(a, ", ");
  }

  /**
   * Formats the long array a for printing purposes.
   * 
   * @param a the long array to be formatted
   * @return a String representing the long array a
   */
  public static String format(long[] a) {
    StringBuffer buffer = new StringBuffer();
    for(int i = 0; i < a.length; i++) {
      if(i < a.length - 1) {
        buffer.append(a[i]).append(", ");
      }
      else {
        buffer.append(a[i]);
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the byte array a for printing purposes.
   * 
   * @param a the byte array to be formatted
   * @return a String representing the byte array a
   */
  public static String format(byte[] a) {
    StringBuffer buffer = new StringBuffer();
    for(int i = 0; i < a.length; i++) {
      if(i < a.length - 1) {
        buffer.append(a[i]).append(", ");
      }
      else {
        buffer.append(a[i]);
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the boolean array b with ',' as separator.
   * 
   * @param b the boolean array to be formatted
   * @param sep the separator between the single values of the double array,
   *        e.g. ','
   * @return a String representing the boolean array b
   */
  public static String format(boolean[] b, final String sep) {
    StringBuffer buffer = new StringBuffer();
    for(int i = 0; i < b.length; i++) {
      if(i < b.length - 1) {
        buffer.append(format(b[i])).append(sep);
      }
      else {
        buffer.append(format(b[i]));
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the boolean b.
   * 
   * @param b the boolean to be formatted
   * @return a String representing of the boolean b
   */
  public static String format(final boolean b) {
    if(b) {
      return "1";
    }
    return "0";
  }

  /**
   * Returns a string representation of the specified bit set.
   * 
   * @param bitSet the bitSet
   * @param dim the overall dimensionality of the bit set
   * @param sep the separator
   * @return a string representation of the specified bit set.
   */
  public static String format(BitSet bitSet, int dim, String sep) {
    StringBuffer msg = new StringBuffer();

    for(int d = 0; d < dim; d++) {
      if(d > 0) {
        msg.append(sep);
      }
      if(bitSet.get(d)) {
        msg.append("1");
      }
      else {
        msg.append("0");
      }
    }

    return msg.toString();
  }

  /**
   * Returns a string representation of the specified bit set.
   * 
   * @param dim the overall dimensionality of the bit set
   * @param bitSet the bitSet
   * @return a string representation of the specified bit set.
   */
  public static String format(int dim, BitSet bitSet) {
    // TODO: removed whitespace - hierarchy reading to be adapted!
    return format(bitSet, dim, ",");
  }
}
