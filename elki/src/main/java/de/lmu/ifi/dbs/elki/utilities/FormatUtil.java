package de.lmu.ifi.dbs.elki.utilities;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import java.util.Collection;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Utility methods for output formatting of various number objects
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.2
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
   * Number Formatter (3 digits) for output purposes.
   */
  public static final NumberFormat NF3 = NumberFormat.getInstance(Locale.US);

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
    NF3.setMinimumFractionDigits(3);
    NF3.setMaximumFractionDigits(3);
    NF3.setGroupingUsed(false);
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
   * Initialize a number format with ELKI standard options (US locale, no
   * grouping).
   * 
   * @param digits Number of digits to use
   * @return Number format
   */
  public static NumberFormat makeNumberFormat(int digits) {
    // Prefer predefined number formats where applicable.
    // TODO: cache others, too?
    switch(digits){
    case 0:
      return NF0;
    case 2:
      return NF2;
    case 3:
      return NF3;
    case 4:
      return NF4;
    case 6:
      return NF6;
    case 8:
      return NF8;
    }
    final NumberFormat nf = NumberFormat.getInstance(Locale.US);
    nf.setMaximumFractionDigits(digits);
    nf.setMinimumFractionDigits(digits);
    nf.setGroupingUsed(false);
    return nf;
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
    if(d.length == 0) {
      return "";
    }
    return formatTo(new StringBuilder(), d, sep).toString();
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
    if(d.length == 0) {
      return "";
    }
    return formatTo(new StringBuilder(), d, sep, nf).toString();
  }

  /**
   * Formats the double array d with the specified number format.
   * 
   * @param d the double array to be formatted
   * @param sep the separator between the single values of the double array,
   *        e.g. ','
   * @return a String representing the double array d
   */
  public static StringBuilder formatTo(StringBuilder a, double[] d, String sep) {
    if(d.length == 0) {
      return a;
    }
    a.append(d[0]);
    for(int i = 1; i < d.length; i++) {
      a.append(sep);
      a.append(d[i]);
    }
    return a;
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
  public static StringBuilder formatTo(StringBuilder a, double[] d, String sep, NumberFormat nf) {
    if(d.length == 0) {
      return a;
    }
    a.append(nf.format(d[0]));
    for(int i = 1; i < d.length; i++) {
      a.append(sep);
      a.append(nf.format(d[i]));
    }
    return a;
  }

  /**
   * Formats the double array d with ', ' as separator and 2 fraction digits.
   * 
   * @param d the double array to be formatted
   * @return a String representing the double array d
   */
  public static String format(double[] d) {
    return formatTo(new StringBuilder(), d, ", ").toString();
  }

  /**
   * Formats the double array d with ',' as separator and 2 fraction digits.
   * 
   * @param d the double array to be formatted
   * @return a String representing the double array d
   */
  public static String format(double[][] d) {
    return format(d, "\n", ", ", NF2);
  }

  /**
   * Formats the array of double arrays d with 'the specified separators and
   * fraction digits.
   * 
   * @param d the double array to be formatted
   * @param sep1 the first separator of the outer array
   * @param sep2 the second separator of the inner array
   * @param nf the number format to use
   * @return a String representing the double array d
   */
  public static String format(double[][] d, String sep1, String sep2, NumberFormat nf) {
    if(d.length == 0) {
      return "";
    }
    StringBuilder buffer = new StringBuilder();
    formatTo(buffer, d[0], sep2, nf);
    for(int i = 1; i < d.length; i++) {
      buffer.append(sep1);
      formatTo(buffer, d[i], sep2, nf);
    }
    return buffer.toString();
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
  public static String format(float[] d, String sep, NumberFormat nf) {
    if(d.length == 0) {
      return "";
    }
    StringBuilder buffer = new StringBuilder();
    buffer.append(nf.format((double) d[0]));
    for(int i = 1; i < d.length; i++) {
      buffer.append(sep);
      buffer.append(nf.format((double) d[i]));
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
    return format(f, ", ", NF2);
  }

  /**
   * Formats the int array a for printing purposes.
   * 
   * @param buf The buffer to serialize to
   * @param a the int array to be formatted
   * @param sep the separator between the single values of the array, e.g. ','
   * @return The output buffer {@code buf}
   */
  public static StringBuilder formatTo(StringBuilder buf, int[] a, String sep) {
    if(a.length == 0) {
      return buf;
    }
    buf.append(a[0]);
    for(int i = 1; i < a.length; i++) {
      buf.append(sep);
      buf.append(a[i]);
    }
    return buf;
  }

  /**
   * Formats the int array a for printing purposes.
   * 
   * @param a the int array to be formatted
   * @param sep the separator between the single values of the array, e.g. ','
   * @return a String representing the int array a
   */
  public static String format(int[] a, String sep) {
    return (a == null) ? "null" : (a.length == 0) ? "" : //
    formatTo(new StringBuilder(), a, sep).toString();
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
   * Formats the long array a for printing purposes.
   * 
   * @param a the long array to be formatted
   * @return a String representing the long array a
   */
  public static String format(long[] a) {
    if(a.length == 0) {
      return "";
    }
    StringBuilder buffer = new StringBuilder();
    buffer.append(a[0]);
    for(int i = 1; i < a.length; i++) {
      buffer.append(", ");
      buffer.append(a[i]);
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
    if(a.length == 0) {
      return "";
    }
    StringBuilder buffer = new StringBuilder();
    buffer.append(a[0]);
    for(int i = 1; i < a.length; i++) {
      buffer.append(", ");
      buffer.append(a[i]);
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
    if(b.length == 0) {
      return "";
    }
    StringBuilder buffer = new StringBuilder();
    buffer.append(format(b[0]));
    for(int i = 1; i < b.length; i++) {
      buffer.append(sep);
      buffer.append(format(b[i]));
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
    return b ? "1" : "0";
  }

  /**
   * Returns a string representation of the specified bit set.
   * 
   * @param bitSet the bitSet
   * @param dim the overall dimensionality of the bit set
   * @param sep the separator
   * @return a string representation of the specified bit set.
   */
  public static String format(long[] bitSet, int dim, String sep) {
    StringBuilder msg = new StringBuilder();
    msg.append(BitsUtil.get(bitSet, 0) ? '1' : '0');
    for(int d = 1; d < dim; d++) {
      msg.append(sep);
      msg.append(BitsUtil.get(bitSet, d) ? '1' : '0');
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
  public static String format(int dim, long[] bitSet) {
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
    Iterator<String> it = d.iterator();
    buffer.append(it.next());
    while(it.hasNext()) {
      buffer.append(sep);
      buffer.append(it.next());
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
    StringBuilder buf = new StringBuilder();
    buf.append('[');
    formatTo(buf, m.getArrayRef(), ", ", nf);
    buf.append(']');
    return buf.toString();
  }

  /**
   * Returns String-representation of Vector.
   * 
   * @return String representation of this Vector
   */
  public static String format(Vector m) {
    return format(m.getArrayRef());
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
  private static final NumberFormatException EMPTY_STRING = new NumberFormatException("Parser called on an empty string.") {
    private static final long serialVersionUID = 1L;

    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  };

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
    if(start >= end) {
      throw EMPTY_STRING;
    }
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
    if((pos + 1 < end) && ((cur == 'E') || (cur == 'e'))) {
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
  public static double parseDouble(final byte[] str, final int start, final int end) {
    if(start >= end) {
      throw EMPTY_STRING;
    }
    // Current position and character.
    int pos = start;
    byte cur = str[pos];

    // Match for NaN spellings
    if(matchNaN(str, cur, pos, end)) {
      return Double.NaN;
    }
    // Match sign
    boolean isNegative = (cur == '-');
    // Carefully consume the - character, update c and i:
    if((isNegative || (cur == '+')) && (++pos < end)) {
      cur = str[pos];
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
        cur = str[pos];
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
    if((pos + 1 < end) && ((cur == 'E') || (cur == 'e'))) {
      cur = str[++pos];
      final boolean isNegativeExp = (cur == '-');
      if((isNegativeExp || (cur == '+')) && (++pos < end)) {
        cur = str[pos];
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
          cur = str[pos];
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
   * Match "NaN" in a number of different capitalizations.
   * 
   * @param str String to match
   * @param firstchar First character
   * @param start Interval begin
   * @param end Interval end
   * @return {@code true} when NaN was recognized.
   */
  private static boolean matchNaN(byte[] str, byte firstchar, int start, int end) {
    final int len = end - start;
    if(len < 2 || len > 3) {
      return false;
    }
    if(firstchar != 'N' && firstchar != 'n') {
      return false;
    }
    final byte c1 = str[start + 1];
    if(c1 != 'a' && c1 != 'A') {
      return false;
    }
    // Accept just "NA", too:
    if(len == 2) {
      return true;
    }
    final byte c2 = str[start + 2];
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
   * Match "inf", "infinity" in a number of different capitalizations.
   * 
   * @param str String to match
   * @param firstchar First character
   * @param start Interval begin
   * @param end Interval end
   * @return {@code true} when infinity was recognized.
   */
  private static boolean matchInf(byte[] str, byte firstchar, int start, int end) {
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
      final byte c = str[start + i];
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

  /**
   * Format a boolean value as string "true" or "false".
   * 
   * @param b Boolean to Format
   * @param buf Buffer to append to
   * @return Same buffer
   */
  public static StringBuilder format(boolean b, StringBuilder buf) {
    return buf.append(b ? "true" : "false");
  }

  /**
   * Format a boolean value as string "1" or "0".
   * 
   * @param b Boolean to Format
   * @param buf Buffer to append to
   * @return Same buffer
   */
  public static StringBuilder formatBit(boolean b, StringBuilder buf) {
    return buf.append(b ? '1' : '0');
  }

  /**
   * Format an integer value as decimal.
   * 
   * @param i Integer value to format.
   * @param buf Buffer to append to
   * @return Same buffer
   */
  public static StringBuilder format(int i, StringBuilder buf) {
    // Int seems to be well optimized
    return buf.append(i);
  }

  /**
   * Format a long value as decimal.
   * 
   * @param i Long value to format.
   * @param buf Buffer to append to
   * @return Same buffer
   */
  public static StringBuilder format(long i, StringBuilder buf) {
    // Long seems to be well optimized
    return buf.append(i);
  }

  /**
   * Buffer for zero padding.
   */
  private static final char[] ZEROPADDING = new char[] { '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0' };

  /**
   * Buffer for whitespace padding.
   */
  private static final char[] SPACEPADDING = new char[] { ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ' };

  /**
   * Append zeros to a buffer.
   * 
   * @param buf Buffer to append to
   * @param zeros Number of zeros to append.
   * @return Buffer
   */
  public static StringBuilder appendZeros(StringBuilder buf, int zeros) {
    for(int i = zeros; i > 0; i -= ZEROPADDING.length) {
      buf.append(ZEROPADDING, 0, i < ZEROPADDING.length ? i : ZEROPADDING.length);
    }
    return buf;
  }

  /**
   * Append whitespace to a buffer.
   * 
   * @param buf Buffer to append to
   * @param spaces Number of spaces to append.
   * @return Buffer
   */
  public static StringBuilder appendSpace(StringBuilder buf, int spaces) {
    for(int i = spaces; i > 0; i -= SPACEPADDING.length) {
      buf.append(SPACEPADDING, 0, i < SPACEPADDING.length ? i : SPACEPADDING.length);
    }
    return buf;
  }

  /**
   * Compute the number of characters needed to represent the integer x.
   * 
   * Reimplementation of {@link Long#stringSize}, but public and without loop.
   * 
   * @param x Integer value
   * @return Number of digits needed
   */
  public static int stringSize(int x) {
    if(x < 0) {
      // Avoid overflow on extreme negative
      return (x == Integer.MIN_VALUE) ? 11 : stringSize(-x) + 1;
    }
    // This is almost a binary search - 10 cases is not a power of two, and we
    // assume that the smaller values are more frequent.
    return //
    (x < 10000) ? // 1-4 vs. 5-10
    /**/(x < 100) ? // 1-2 vs. 3-4
    /*   */((x < 10) ? 1 : 2) : //
    /*   */((x < 1000) ? 3 : 4) : //
    /**/(x < 1000000) ? // 5-6 vs. 7-10
    /*  */((x < 100000) ? 5 : 6) : // 5-6
    /*  */(x < 100000000) ? // 7-8 vs. 9-10
    /*    */((x < 10000000) ? 7 : 8) : // 7-8
    /*    */((x < 1000000000) ? 9 : 10) // 9-10
    ;
  }

  /**
   * Compute the number of characters needed to represent the integer x.
   * 
   * Reimplementation of {@link Long#stringSize}, but public and without loop.
   * 
   * @param x Integer value
   * @return Number of digits needed
   */
  public static int stringSize(long x) {
    if(x < 0) {
      // Avoid overflow on extreme negative
      return (x == Long.MIN_VALUE) ? 20 : stringSize(-x) + 1;
    }
    // This is almost a binary search.
    return (x <= Integer.MAX_VALUE) ? stringSize((int) x) : //
    (x < 10000000000000L) ? // 10-13 vs. 14-19
    /**/(x < 100000000000L) ? // 10-11 vs. 12-13
    /*   */((x < 10000000000L) ? 10 : 11) : //
    /*   */((x < 1000000000000L) ? 12 : 13) : //
    /**/(x < 1000000000000000L) ? // 14-15 vs. 16-19
    /*  */((x < 100000000000000L) ? 14 : 15) : // 14-15
    /*  */(x < 100000000000000000L) ? // 16-17 vs. 18-19
    /*    */((x < 10000000000000000L) ? 16 : 17) : // 16-17
    /*    */((x < 1000000000000000000L) ? 18 : 19) // 18-19
    ;
  }
}
