package de.lmu.ifi.dbs.elki.utilities;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Utility methods for output formatting of various number objects
 */
public final class FormatUtil {

  /**
   * Number Formatter (2 digits) for output purposes.
   */
  public static final NumberFormat NF2 = NumberFormat.getInstance(Locale.US);

  /**
   * Number Formatter (4 digits) for output purposes.
   */
  public static final NumberFormat NF4 = NumberFormat.getInstance(Locale.US);

  /**
   * Number Formatter (8 digits) for output purposes.
   */
  public static final NumberFormat NF8 = NumberFormat.getInstance(Locale.US);

  static {
    NF2.setMinimumFractionDigits(2);
    NF2.setMaximumFractionDigits(2);
    NF4.setMinimumFractionDigits(4);
    NF4.setMaximumFractionDigits(4);
    NF8.setMinimumFractionDigits(8);
    NF8.setMaximumFractionDigits(8);
  }

  /**
   * Whitespace. The string should cover the commonly used length.
   */
  private static final String WHITESPACE_BUFFER = "                                                                                ";

  /**
   * The system newline setting.
   */
  public static final String NEWLINE = System.getProperty("line.separator");

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
   * Formats the double array d with ', ' as separator and with the specified
   * fraction digits.
   * 
   * @param d the double array to be formatted
   * @param digits the number of fraction digits
   * @return a String representing the double array d
   */
  public static String format(double[] d, int digits) {
    return format(d, ", ", digits);
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
   * Formats the Double array f with the specified separator and the specified
   * fraction digits.
   * 
   * @param f the Double array to be formatted
   * @param sep the separator between the single values of the Double array,
   *        e.g. ','
   * @param digits the number of fraction digits
   * @return a String representing the Double array f
   */
  public static String format(Double[] f, String sep, int digits) {
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
   * Formats the Double array f with ',' as separator and 2 fraction digits.
   * 
   * @param f the Double array to be formatted
   * @return a String representing the Double array f
   */
  public static String format(Double[] f) {
    return format(f, ", ", 2);
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

  /**
   * Formats the String collection with the specified separator.
   * 
   * @param d the String collection to format
   * @param sep the separator between the single values of the double array,
   *        e.g. ' '
   * @return a String representing the String Collection d
   */
  public static String format(Collection<String> d, String sep) {
    if(d.size() == 0) {
      return "";
    }
    if(d.size() == 1) {
      return d.iterator().next();
    }
    StringBuffer buffer = new StringBuffer();
    boolean first = true;
    for(String str : d) {
      if(!first) {
        buffer.append(sep);
      }
      buffer.append(str);
      first = false;
    }
    return buffer.toString();
  }

  /**
   * Find the first space before position w or if there is none after w.
   * 
   * @param s String
   * @param width Width
   * @return index of best whitespace or <code>-1</code> if no whitespace was
   *         found.
   */
  public static int findSplitpoint(String s, int width) {
    // the newline (or EOS) is the fallback split position.
    int in = s.indexOf(NEWLINE);
    if(in < 0) {
      in = s.length();
    }
    // Good enough?
    if(in < width) {
      return in;
    }
    // otherwise, search for whitespace
    int iw = s.lastIndexOf(' ', width);
    // good whitespace found?
    if(iw >= 0 && iw < width) {
      return iw;
    }
    // sub-optimal splitpoint - retry AFTER the given position
    int bp = nextPosition(s.indexOf(' ', width), s.indexOf(NEWLINE, width));
    if(bp >= 0) {
      return bp;
    }
    // even worse - can't split!
    return s.length();
  }

  /**
   * Helper that is similar to {@code Math.min(a,b)}, except that negative
   * values are considered "invalid".
   * 
   * @param a String position
   * @param b String position
   * @return {@code Math.min(a,b)} if {@code a >= 0} and {@code b >= 0},
   *         otherwise whichever is positive.
   */
  private static int nextPosition(int a, int b) {
    if(a < 0) {
      return b;
    }
    if(b < 0) {
      return a;
    }
    return Math.min(a, b);
  }

  /**
   * Splits the specified string at the last blank before width. If there is no
   * blank before the given width, it is split at the next.
   * 
   * @param s the string to be split
   * @param width int
   * @return string fragments
   */
  public static List<String> splitAtLastBlank(String s, int width) {
    List<String> chunks = new ArrayList<String>();

    String tmp = s;
    while(tmp.length() > 0) {
      int index = findSplitpoint(tmp, width);
      // store first part
      chunks.add(tmp.substring(0, index));
      // skip whitespace at beginning of line
      while(index < tmp.length() && tmp.charAt(index) == ' ') {
        index += 1;
      }
      // remove a newline
      if(index < tmp.length() && tmp.regionMatches(index, NEWLINE, 0, NEWLINE.length())) {
        index += NEWLINE.length();
      }
      if(index >= tmp.length()) {
        break;
      }
      tmp = tmp.substring(index);
    }

    return chunks;
  }

  /**
   * Returns a string with the specified number of whitespace.
   * 
   * @param n the number of whitespace characters
   * @return a string with the specified number of blanks
   */
  public static String whitespace(int n) {
    if(n < WHITESPACE_BUFFER.length()) {
      return WHITESPACE_BUFFER.substring(0, n);
    }
    char[] buf = new char[n];
    for(int i = 0; i < n; i++) {
      buf[i] = WHITESPACE_BUFFER.charAt(0);
    }
    return new String(buf);
  }

  /**
   * Pad a string to a given length by adding whitespace to the right.
   * 
   * @param o original string
   * @param len destination length
   * @return padded string of at least length len (and o otherwise)
   */
  public static String pad(String o, int len) {
    if(o.length() >= len) {
      return o;
    }
    return o + whitespace(len - o.length());
  }

  /**
   * Pad a string to a given length by adding whitespace to the left.
   * 
   * @param o original string
   * @param len destination length
   * @return padded string of at least length len (and o otherwise)
   */
  public static String padRightAligned(String o, int len) {
    if(o.length() >= len) {
      return o;
    }
    return whitespace(len - o.length()) + o;
  }

  /**
   * Get the width of the terminal window (on Unix xterms), with a default of 78
   * characters.
   * 
   * @return Terminal width
   */
  public static int getConsoleWidth() {
    int termwidth = 78;
    try {
      termwidth = Integer.parseInt(System.getenv("COLUMNS")) - 1;
    }
    catch(Exception e) {
      // Do nothing, stick with default of 78.
    }
    return termwidth;
  }
}