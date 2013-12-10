package de.lmu.ifi.dbs.elki.utilities;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Utility methods for output formatting of various number objects
 */
public final class FormatUtil {
  /**
   * Dynamic number formatter, but with language constraint.
   */
  public static final NumberFormat NF = NumberFormat.getInstance(Locale.US);

  /**
   * Number Formatter (0 digits) for output purposes.
   */
  public static final NumberFormat NF0 = NumberFormat.getInstance(Locale.US);

  /**
   * Number Formatter (2 digits) for output purposes.
   */
  public static final NumberFormat NF2 = NumberFormat.getInstance(Locale.US);

  /**
   * Number Formatter (4 digits) for output purposes.
   */
  public static final NumberFormat NF4 = NumberFormat.getInstance(Locale.US);

  /**
   * Number Formatter (6 digits) for output purposes.
   */
  public static final NumberFormat NF6 = NumberFormat.getInstance(Locale.US);

  /**
   * Number Formatter (8 digits) for output purposes.
   */
  public static final NumberFormat NF8 = NumberFormat.getInstance(Locale.US);

  static {
    NF.setMinimumFractionDigits(0);
    NF.setMaximumFractionDigits(8);
    NF.setGroupingUsed(false);
    NF0.setMinimumFractionDigits(0);
    NF0.setMaximumFractionDigits(0);
    NF0.setGroupingUsed(false);
    NF2.setMinimumFractionDigits(2);
    NF2.setMaximumFractionDigits(2);
    NF2.setGroupingUsed(false);
    NF4.setMinimumFractionDigits(4);
    NF4.setMaximumFractionDigits(4);
    NF4.setGroupingUsed(false);
    NF6.setMinimumFractionDigits(6);
    NF6.setMaximumFractionDigits(6);
    NF6.setGroupingUsed(false);
    NF8.setMinimumFractionDigits(8);
    NF8.setMaximumFractionDigits(8);
    NF8.setGroupingUsed(false);
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
   * Non-breaking unicode space character.
   */
  public static final String NONBREAKING_SPACE = "\u00a0";

  /**
   * The time unit sizes: ms, s, m, h, d; all in ms.
   */
  private static final long[] TIME_UNIT_SIZES = new long[] { 1L, 1000L, 60000L, 3600000L, 86400000L };

  /**
   * The strings used in serialization
   */
  private static final String[] TIME_UNIT_NAMES = new String[] { "ms", "s", "m", "h", "d" };

  /**
   * The number of digits used for formatting
   */
  private static final int[] TIME_UNIT_DIGITS = new int[] { 3, 2, 2, 2, 2 };

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
    StringBuilder buffer = new StringBuilder();
    for(int i = 0; i < d.length; i++) {
      if(i > 0) {
        buffer.append(sep).append(d[i]);
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
    StringBuilder buffer = new StringBuilder();
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
    StringBuilder buffer = new StringBuilder();
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
    StringBuilder buffer = new StringBuilder();
    for(double[] array : d) {
      buffer.append(format(array, ", ", 2)).append('\n');
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
    StringBuilder buffer = new StringBuilder();

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
    StringBuilder buffer = new StringBuilder();
    for(int i = 0; i < f.length; i++) {
      if(i < f.length - 1) {
        buffer.append(format(f[i].doubleValue(), digits)).append(sep);
      }
      else {
        buffer.append(format(f[i].doubleValue(), digits));
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
   * Formats the Double array f with the specified separator and the specified
   * fraction digits.
   * 
   * @param f the Double array to be formatted
   * @param sep the separator between the single values of the Double array,
   *        e.g. ','
   * @param nf the number format
   * @return a String representing the Double array f
   */
  public static String format(Double[] f, String sep, NumberFormat nf) {
    StringBuilder buffer = new StringBuilder();
    for(int i = 0; i < f.length; i++) {
      if(i < f.length - 1) {
        buffer.append(format(f[i].doubleValue(), nf)).append(sep);
      }
      else {
        buffer.append(format(f[i].doubleValue(), nf));
      }
    }
    return buffer.toString();
  }

  /**
   * Formats the Double array f with ',' as separator and 2 fraction digits.
   * 
   * @param f the Double array to be formatted
   * @param nf the Number format
   * @return a String representing the Double array f
   */
  public static String format(Double[] f, NumberFormat nf) {
    return format(f, ", ", nf);
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
    StringBuilder buffer = new StringBuilder();
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
    StringBuilder buffer = new StringBuilder();
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
    StringBuilder buffer = new StringBuilder();
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
    StringBuilder buffer = new StringBuilder();
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
    StringBuilder buffer = new StringBuilder();
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
    StringBuilder buffer = new StringBuilder();
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
    StringBuilder msg = new StringBuilder();

    for(int d = 0; d < dim; d++) {
      if(d > 0) {
        msg.append(sep);
      }
      if(bitSet.get(d)) {
        msg.append('1');
      }
      else {
        msg.append('0');
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
    StringBuilder buffer = new StringBuilder();
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
   * Returns a string representation of this matrix.
   * 
   * @param w column width
   * @param d number of digits after the decimal
   * @return a string representation of this matrix
   */
  // TODO: in use?
  public static String format(Matrix m, int w, int d) {
    DecimalFormat format = new DecimalFormat();
    format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
    format.setMinimumIntegerDigits(1);
    format.setMaximumFractionDigits(d);
    format.setMinimumFractionDigits(d);
    format.setGroupingUsed(false);

    int width = w + 1;
    StringBuilder msg = new StringBuilder();
    msg.append('\n'); // start on new line.
    for(int i = 0; i < m.getRowDimensionality(); i++) {
      for(int j = 0; j < m.getColumnDimensionality(); j++) {
        String s = format.format(m.get(i, j)); // format the number
        int padding = Math.max(1, width - s.length()); // At _least_ 1
        // space
        for(int k = 0; k < padding; k++) {
          msg.append(' ');
        }
        msg.append(s);
      }
      msg.append('\n');
    }
    // msg.append("\n");

    return msg.toString();
  }

  /**
   * Returns a string representation of this matrix.
   * 
   * @param w column width
   * @param d number of digits after the decimal
   * @return a string representation of this matrix
   */
  // TODO: in use?
  public static String format(Vector v, int w, int d) {
    DecimalFormat format = new DecimalFormat();
    format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
    format.setMinimumIntegerDigits(1);
    format.setMaximumFractionDigits(d);
    format.setMinimumFractionDigits(d);
    format.setGroupingUsed(false);

    int width = w + 1;
    StringBuilder msg = new StringBuilder();
    msg.append('\n'); // start on new line.
    for(int i = 0; i < v.getDimensionality(); i++) {
      String s = format.format(v.get(i)); // format the number
      int padding = Math.max(1, width - s.length()); // At _least_ 1
      // space
      for(int k = 0; k < padding; k++) {
        msg.append(' ');
      }
      msg.append(s);
    }
    // msg.append("\n");

    return msg.toString();
  }

  /**
   * Returns a string representation of this matrix. In each line the specified
   * String <code>pre</code> is prefixed.
   * 
   * @param pre the prefix of each line
   * @return a string representation of this matrix
   */
  public static String format(Matrix m, String pre) {
    StringBuilder output = new StringBuilder();
    output.append(pre).append("[\n").append(pre);
    for(int i = 0; i < m.getRowDimensionality(); i++) {
      output.append(" [");
      for(int j = 0; j < m.getColumnDimensionality(); j++) {
        output.append(' ').append(m.get(i, j));
        if(j < m.getColumnDimensionality() - 1) {
          output.append(',');
        }
      }
      output.append(" ]\n").append(pre);
    }
    output.append("]\n").append(pre);

    return (output.toString());
  }

  /**
   * returns String-representation of Matrix.
   * 
   * @param nf NumberFormat to specify output precision
   * @return String representation of this Matrix in precision as specified by
   *         given NumberFormat
   */
  public static String format(Matrix m, NumberFormat nf) {
    int[] colMax = new int[m.getColumnDimensionality()];
    String[][] entries = new String[m.getRowDimensionality()][m.getColumnDimensionality()];
    for(int i = 0; i < m.getRowDimensionality(); i++) {
      for(int j = 0; j < m.getColumnDimensionality(); j++) {
        entries[i][j] = nf.format(m.get(i, j));
        if(entries[i][j].length() > colMax[j]) {
          colMax[j] = entries[i][j].length();
        }
      }
    }
    StringBuilder output = new StringBuilder();
    output.append("[\n");
    for(int i = 0; i < m.getRowDimensionality(); i++) {
      output.append(" [");
      for(int j = 0; j < m.getColumnDimensionality(); j++) {
        output.append(' ');
        int space = colMax[j] - entries[i][j].length();
        for(int s = 0; s < space; s++) {
          output.append(' ');
        }
        output.append(entries[i][j]);
        if(j < m.getColumnDimensionality() - 1) {
          output.append(',');
        }
      }
      output.append(" ]\n");
    }
    output.append("]\n");

    return (output.toString());
  }

  /**
   * returns String-representation of Matrix.
   * 
   * @return String representation of this Matrix
   */
  public static String format(Matrix m) {
    return format(m, FormatUtil.NF);
  }

  /**
   * returns String-representation of Vector.
   * 
   * @param nf NumberFormat to specify output precision
   * @return String representation of this Matrix in precision as specified by
   *         given NumberFormat
   */
  public static String format(Vector m, NumberFormat nf) {
    return "[" + FormatUtil.format(m.getArrayRef(), nf) + "]";
  }

  /**
   * Returns String-representation of Vector.
   * 
   * @return String representation of this Vector
   */
  public static String format(Vector m) {
    return format(m, FormatUtil.NF);
  }

  /**
   * Returns a string representation of this matrix. In each line the specified
   * String <code>pre</code> is prefixed.
   * 
   * @param pre the prefix of each line
   * @return a string representation of this matrix
   */
  public static String format(Vector v, String pre) {
    StringBuilder output = new StringBuilder();
    output.append(pre).append("[\n").append(pre);
    for(int j = 0; j < v.getDimensionality(); j++) {
      output.append(' ').append(v.get(j));
      if(j < v.getDimensionality() - 1) {
        output.append(',');
      }
    }
    output.append("]\n").append(pre);

    return (output.toString());
  }

  /**
   * Returns a string representation of this matrix. In each line the specified
   * String <code>pre<\code> is prefixed.
   * 
   * @param nf number format for output accuracy
   * @param pre the prefix of each line
   * @return a string representation of this matrix
   */
  public static String format(Matrix m, String pre, NumberFormat nf) {
    if(nf == null) {
      return FormatUtil.format(m, pre);
    }

    int[] colMax = new int[m.getColumnDimensionality()];
    String[][] entries = new String[m.getRowDimensionality()][m.getColumnDimensionality()];
    for(int i = 0; i < m.getRowDimensionality(); i++) {
      for(int j = 0; j < m.getColumnDimensionality(); j++) {
        entries[i][j] = nf.format(m.get(i, j));
        if(entries[i][j].length() > colMax[j]) {
          colMax[j] = entries[i][j].length();
        }
      }
    }
    StringBuilder output = new StringBuilder();
    output.append(pre).append("[\n").append(pre);
    for(int i = 0; i < m.getRowDimensionality(); i++) {
      output.append(" [");
      for(int j = 0; j < m.getColumnDimensionality(); j++) {
        output.append(' ');
        int space = colMax[j] - entries[i][j].length();
        for(int s = 0; s < space; s++) {
          output.append(' ');
        }
        output.append(entries[i][j]);
        if(j < m.getColumnDimensionality() - 1) {
          output.append(',');
        }
      }
      output.append(" ]\n").append(pre);
    }
    output.append("]\n").append(pre);

    return (output.toString());
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
    List<String> chunks = new ArrayList<>();

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
    final int default_termwidth = 78;
    try {
      return Integer.parseInt(System.getenv("COLUMNS")) - 1;
    }
    catch(SecurityException e) {
      return default_termwidth;
    }
    catch(NumberFormatException e) {
      return default_termwidth;
    }
  }

  /**
   * Formats a time delta in human readable format.
   * 
   * @param time time delta in ms
   * @return Formatted string
   */
  public static String formatTimeDelta(long time, CharSequence sep) {
    final StringBuilder sb = new StringBuilder();
    final Formatter fmt = new Formatter(sb);

    for(int i = TIME_UNIT_SIZES.length - 1; i >= 0; --i) {
      // We do not include ms if we are in the order of minutes.
      if(i == 0 && sb.length() > 4) {
        continue;
      }
      // Separator
      if(sb.length() > 0) {
        sb.append(sep);
      }
      final long acValue = time / TIME_UNIT_SIZES[i];
      time = time % TIME_UNIT_SIZES[i];
      if(!(acValue == 0 && sb.length() == 0)) {
        fmt.format("%0" + TIME_UNIT_DIGITS[i] + "d%s", Long.valueOf(acValue), TIME_UNIT_NAMES[i]);
      }
    }
    fmt.close();
    return sb.toString();
  }

  /**
   * Formats the string array d with the specified separator.
   * 
   * @param d the string array to be formatted
   * @param sep the separator between the single values of the double array,
   *        e.g. ','
   * @return a String representing the string array d
   */
  public static String format(String[] d, String sep) {
    StringBuilder buffer = new StringBuilder();
    for(int i = 0; i < d.length; i++) {
      if(i > 0) {
        buffer.append(sep).append(d[i]);
      }
      else {
        buffer.append(d[i]);
      }
    }
    return buffer.toString();
  }

  /**
   * Preallocated exceptions.
   */
  private static final NumberFormatException EXPONENT_OVERFLOW = new NumberFormatException("Precision overflow for double exponent.") {
    private static final long serialVersionUID = 1L;

    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  };

  /**
   * Preallocated exceptions.
   */
  private static final NumberFormatException INVALID_EXPONENT = new NumberFormatException("Invalid exponent") {
    private static final long serialVersionUID = 1L;

    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  };

  /**
   * Preallocated exceptions.
   */
  private static final NumberFormatException TRAILING_CHARACTERS = new NumberFormatException("String sequence was not completely consumed.") {
    private static final long serialVersionUID = 1L;

    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  };

  /**
   * Preallocated exceptions.
   */
  private static final NumberFormatException PRECISION_OVERFLOW = new NumberFormatException("Precision overflow for long values.") {
    private static final long serialVersionUID = 1L;

    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  };

  /**
   * Preallocated exceptions.
   */
  private static final NumberFormatException NOT_A_NUMBER = new NumberFormatException("Number must start with a digit or dot.") {
    private static final long serialVersionUID = 1L;

    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  };

  /**
   * Parse a double from a character sequence.
   * 
   * In contrast to Javas {@link Double#parseDouble}, this will <em>not</em>
   * create an object and thus is expected to put less load on the garbage
   * collector. It will accept some more spellings of NaN and infinity, thus
   * removing the need for checking for these independently.
   * 
   * @param str String
   * @return Double value
   */
  public static double parseDouble(final CharSequence str) {
    return parseDouble(str, 0, str.length());
  }

  /**
   * Parse a double from a character sequence.
   * 
   * In contrast to Javas {@link Double#parseDouble}, this will <em>not</em>
   * create an object and thus is expected to put less load on the garbage
   * collector. It will accept some more spellings of NaN and infinity, thus
   * removing the need for checking for these independently.
   * 
   * @param str String
   * @param start Begin
   * @param end End
   * @return Double value
   */
  public static double parseDouble(final CharSequence str, final int start, final int end) {
    // Current position and character.
    int pos = start;
    char cur = str.charAt(pos);

    // Match for NaN spellings
    if(matchNaN(str, cur, pos, end)) {
      return Double.NaN;
    }
    // Match sign
    boolean isNegative = (cur == '-');
    // Carefully consume the - character, update c and i:
    if((isNegative || (cur == '+')) && (++pos < end)) {
      cur = str.charAt(pos);
    }
    if(matchInf(str, cur, pos, end)) {
      return isNegative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    }

    // Begin parsing real numbers!
    if(((cur < '0') || (cur > '9')) && (cur != '.')) {
      throw NOT_A_NUMBER;
    }

    // Parse digits into a long, remember offset of decimal point.
    long decimal = 0;
    int decimalPoint = -1;
    while(true) {
      final int digit = cur - '0';
      if((digit >= 0) && (digit <= 9)) {
        final long tmp = (decimal << 3) + (decimal << 1) + digit;
        if((decimal > MAX_LONG_OVERFLOW) || (tmp < decimal)) {
          throw PRECISION_OVERFLOW;
        }
        decimal = tmp;
      }
      else if((cur == '.') && (decimalPoint < 0)) {
        decimalPoint = pos;
      }
      else { // No more digits, or a second dot.
        break;
      }
      if(++pos < end) {
        cur = str.charAt(pos);
      }
      else {
        break;
      }
    }
    // We need the offset from the back for adjusting the exponent:
    // Note that we need the current value of i!
    decimalPoint = (decimalPoint >= 0) ? pos - decimalPoint - 1 : 0;

    // Reads exponent.
    int exp = 0;
    if((pos < end) && ((cur == 'E') || (cur == 'e'))) {
      cur = str.charAt(++pos);
      final boolean isNegativeExp = (cur == '-');
      if((isNegativeExp || (cur == '+')) && (++pos < end)) {
        cur = str.charAt(pos);
      }
      if((cur < '0') || (cur > '9')) { // At least one digit required.
        throw INVALID_EXPONENT;
      }
      while(true) {
        final int digit = cur - '0';
        if((digit >= 0) && (digit < 10)) {
          final int tmp = (exp << 3) + (exp << 1) + digit;
          // Actually, double can only handle Double.MAX_EXPONENT? How about
          // subnormal?
          if((exp > MAX_INT_OVERFLOW) || (tmp < exp)) {
            throw EXPONENT_OVERFLOW;
          }
          exp = tmp;
        }
        else {
          break;
        }
        if(++pos < end) {
          cur = str.charAt(pos);
        }
        else {
          break;
        }
      }
      if(isNegativeExp) {
        exp = -exp;
      }
    }
    // Adjust exponent by the offset of the dot in our long.
    if(decimalPoint >= 0) {
      exp = exp - decimalPoint;
    }
    if(pos != end) {
      throw TRAILING_CHARACTERS;
    }

    return BitsUtil.lpow10(isNegative ? -decimal : decimal, exp);
  }

  /**
   * Match "NaN" in a number of different capitalizations.
   * 
   * @param str String to match
   * @param firstchar First character
   * @param start Interval begin
   * @param end Interval end
   * @return {@code true} when NaN was recognized.
   */
  private static boolean matchNaN(CharSequence str, char firstchar, int start, int end) {
    final int len = end - start;
    if(len < 2 || len > 3) {
      return false;
    }
    if(firstchar != 'N' && firstchar != 'n') {
      return false;
    }
    final char c1 = str.charAt(start + 1);
    if(c1 != 'a' && c1 != 'A') {
      return false;
    }
    // Accept just "NA", too:
    if(len == 2) {
      return true;
    }
    final char c2 = str.charAt(start + 2);
    if(c2 != 'N' && c2 != 'n') {
      return false;
    }
    return true;
  }

  /**
   * Maximum long that we can process without overflowing.
   */
  private static final long MAX_LONG_OVERFLOW = Long.MAX_VALUE / 10;

  /**
   * Maximum integer that we can process without overflowing.
   */
  private static final int MAX_INT_OVERFLOW = Integer.MAX_VALUE / 10;

  /**
   * Infinity pattern, with any capitalization
   */
  private static final char[] INFINITY_PATTERN = { //
  'I', 'n', 'f', 'i', 'n', 'i', 't', 'y', //
  'i', 'N', 'F', 'I', 'N', 'I', 'T', 'Y' };

  /** Length of pattern */
  private static final int INFINITY_LENGTH = INFINITY_PATTERN.length >> 1;

  /**
   * Match "inf", "infinity" in a number of different capitalizations.
   * 
   * @param str String to match
   * @param firstchar First character
   * @param start Interval begin
   * @param end Interval end
   * @return {@code true} when infinity was recognized.
   */
  private static boolean matchInf(CharSequence str, char firstchar, int start, int end) {
    final int len = end - start;
    // The wonders of unicode. This is more than one byte on UTF-8
    if(len == 1 && firstchar == '∞') {
      return true;
    }
    if(len != 3 && len != INFINITY_LENGTH) {
      return false;
    }
    // Test beginning: "inf"
    if(firstchar != 'I' && firstchar != 'i') {
      return false;
    }
    for(int i = 1, j = INFINITY_LENGTH + 1; i < INFINITY_LENGTH; i++, j++) {
      final char c = str.charAt(start + i);
      if(c != INFINITY_PATTERN[i] && c != INFINITY_PATTERN[j]) {
        return false;
      }
      if(i == 2 && len == 3) {
        return true;
      }
    }
    return true;
  }

  /**
   * Parse a long integer from a character sequence.
   * 
   * @param str String
   * @param start Begin
   * @param end End
   * @return Double value
   */
  public static long parseLongBase10(final CharSequence str, final int start, final int end) {
    // Current position and character.
    int pos = start;
    char cur = str.charAt(pos);

    // Match sign
    boolean isNegative = (cur == '-');
    // Carefully consume the - character, update c and i:
    if((isNegative || (cur == '+')) && (++pos < end)) {
      cur = str.charAt(pos);
    }

    // Begin parsing real numbers!
    if((cur < '0') || (cur > '9')) {
      throw NOT_A_NUMBER;
    }

    // Parse digits into a long, remember offset of decimal point.
    long decimal = 0;
    while(true) {
      final int digit = cur - '0';
      if((digit >= 0) && (digit <= 9)) {
        final long tmp = (decimal << 3) + (decimal << 1) + digit;
        if(tmp < decimal) {
          throw PRECISION_OVERFLOW;
        }
        decimal = tmp;
      }
      else { // No more digits, or a second dot.
        break;
      }
      if(++pos < end) {
        cur = str.charAt(pos);
      }
      else {
        break;
      }
    }
    if(pos != end) {
      throw TRAILING_CHARACTERS;
    }

    return isNegative ? -decimal : decimal;
  }
}
