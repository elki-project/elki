/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.utilities.io;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Utility methods for output formatting of various number objects
 * <p>
 * FIXME: Handle formatting of infinity and NaN better.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.2
 */
public final class FormatUtil {
  /**
   * Private constructor. Static methods only.
   */
  private FormatUtil() {
    // Do not use.
  }

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

  /**
   * Number Formatter (16 digits) for output purposes.
   */
  public static final NumberFormat NF16 = NumberFormat.getInstance(Locale.US);

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
    NF16.setMinimumFractionDigits(0);
    NF16.setMaximumFractionDigits(16);
    NF16.setGroupingUsed(false);
  }

  /**
   * Whitespace. The string should cover the commonly used length.
   */
  private static final String WHITESPACE_BUFFER = "                                                                                ";

  /**
   * Length of the whitespace buffer.
   */
  private static final int WHITESPACE_BUFFER_LENGTH = WHITESPACE_BUFFER.length();

  /**
   * The system newline setting.
   */
  public static final String NEWLINE = System.getProperty("line.separator");

  /**
   * Non-breaking unicode space character.
   */
  public static final char NONBREAKING_SPACE = "UTF-8".equals(Charset.defaultCharset().name()) ? '\u00a0' : ' ';

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
   * Formats the double array d with ', ' as separator and default precision.
   *
   * @param d the double array to be formatted
   * @return a String representing the double array d
   */
  public static String format(double[] d) {
    return d == null ? "null" : (d.length == 0) ? "" : //
        formatTo(new StringBuilder(), d, ", ").toString();
  }

  /**
   * Formats the double array d with the specified separator.
   *
   * @param d the double array to be formatted
   * @param sep separator between the single values of the array, e.g. ','
   * @return a String representing the double array d
   */
  public static String format(double[] d, String sep) {
    return d == null ? "null" : (d.length == 0) ? "" : //
        formatTo(new StringBuilder(), d, sep).toString();
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
   * @param sep separator between the single values of the array, e.g. ','
   * @param nf the number format to be used for formatting
   * @return a String representing the double array d
   */
  public static String format(double[] d, String sep, NumberFormat nf) {
    return d == null ? "null" : (d.length == 0) ? "" : //
        formatTo(new StringBuilder(), d, sep, nf).toString();
  }

  /**
   * Returns a string representation of this vector.
   *
   * @param w column width
   * @param d number of digits after the decimal
   * @return a string representation of this matrix
   */
  public static String format(double[] v, int w, int d) {
    DecimalFormat format = new DecimalFormat();
    format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
    format.setMinimumIntegerDigits(1);
    format.setMaximumFractionDigits(d);
    format.setMinimumFractionDigits(d);
    format.setGroupingUsed(false);

    int width = w + 1;
    StringBuilder msg = new StringBuilder() //
        .append('\n'); // start on new line.
    for(int i = 0; i < v.length; i++) {
      String s = format.format(v[i]); // format the number
      // At _least_ 1 whitespace is added
      whitespace(msg, Math.max(1, width - s.length())).append(s);
    }
    return msg.toString();
  }

  /**
   * Formats the double array d with the default number format.
   *
   * @param buf String builder to append to
   * @param d the double array to be formatted
   * @param sep separator between the single values of the array, e.g. ','
   * @return Output buffer buf
   */
  public static StringBuilder formatTo(StringBuilder buf, double[] d, String sep) {
    if(d == null) {
      return buf.append("null");
    }
    if(d.length == 0) {
      return buf;
    }
    buf.append(d[0]);
    for(int i = 1; i < d.length; i++) {
      buf.append(sep).append(d[i]);
    }
    return buf;
  }

  /**
   * Formats the double array d with the specified number format.
   *
   * @param buf String builder to append to
   * @param d the double array to be formatted
   * @param sep separator between the single values of the array, e.g. ','
   * @param nf the number format to be used for formatting
   * @return Output buffer buf
   */
  public static StringBuilder formatTo(StringBuilder buf, double[] d, String sep, NumberFormat nf) {
    if(d == null) {
      return buf.append("null");
    }
    if(d.length == 0) {
      return buf;
    }
    buf.append(nf.format(d[0]));
    for(int i = 1; i < d.length; i++) {
      buf.append(sep).append(nf.format(d[i]));
    }
    return buf;
  }

  /**
   * Formats the float array d with the default number format.
   *
   * @param buf String builder to append to
   * @param d the float array to be formatted
   * @param sep separator between the single values of the array, e.g. ','
   * @return Output buffer buf
   */
  public static StringBuilder formatTo(StringBuilder buf, float[] d, String sep) {
    if(d == null) {
      return buf.append("null");
    }
    if(d.length == 0) {
      return buf;
    }
    buf.append(d[0]);
    for(int i = 1; i < d.length; i++) {
      buf.append(sep).append(d[i]);
    }
    return buf;
  }

  /**
   * Formats the float array d with the specified number format.
   *
   * @param buf String builder to append to
   * @param d the float array to be formatted
   * @param sep separator between the single values of the array, e.g. ','
   * @param nf the number format to be used for formatting
   * @return Output buffer buf
   */
  public static StringBuilder formatTo(StringBuilder buf, float[] d, String sep, NumberFormat nf) {
    if(d == null) {
      return buf.append("null");
    }
    if(d.length == 0) {
      return buf;
    }
    buf.append(nf.format(d[0]));
    for(int i = 1; i < d.length; i++) {
      buf.append(sep).append(nf.format(d[i]));
    }
    return buf;
  }

  /**
   * Formats the int array d.
   *
   * @param buf String builder to append to
   * @param d the int array to be formatted
   * @param sep separator between the single values of the array, e.g. ','
   * @return Output buffer buf
   */
  public static StringBuilder formatTo(StringBuilder buf, int[] d, String sep) {
    if(d == null) {
      return buf.append("null");
    }
    if(d.length == 0) {
      return buf;
    }
    buf.append(d[0]);
    for(int i = 1; i < d.length; i++) {
      buf.append(sep).append(d[i]);
    }
    return buf;
  }

  /**
   * Formats the long array d.
   *
   * @param buf String builder to append to
   * @param d the long array to be formatted
   * @param sep separator between the single values of the long array, e.g. ','
   * @return Output buffer buf
   */
  public static StringBuilder formatTo(StringBuilder buf, long[] d, String sep) {
    if(d == null) {
      return buf.append("null");
    }
    if(d.length == 0) {
      return buf;
    }
    buf.append(d[0]);
    for(int i = 1; i < d.length; i++) {
      buf.append(sep).append(d[i]);
    }
    return buf;
  }

  /**
   * Formats the short array d.
   *
   * @param buf String builder to append to
   * @param d the int array to be formatted
   * @param sep separator between the single values of the array, e.g. ','
   * @return Output buffer buf
   */
  public static StringBuilder formatTo(StringBuilder buf, short[] d, String sep) {
    if(d == null) {
      return buf.append("null");
    }
    if(d.length == 0) {
      return buf;
    }
    buf.append(d[0]);
    for(int i = 1; i < d.length; i++) {
      buf.append(sep).append(d[i]);
    }
    return buf;
  }

  /**
   * Formats the byte array d.
   *
   * @param buf String builder to append to
   * @param d the byte array to be formatted
   * @param sep separator between the single values of the array, e.g. ','
   * @return Output buffer buf
   */
  public static StringBuilder formatTo(StringBuilder buf, byte[] d, String sep) {
    if(d == null) {
      return buf.append("null");
    }
    if(d.length == 0) {
      return buf;
    }
    buf.append(d[0]);
    for(int i = 1; i < d.length; i++) {
      buf.append(sep).append(d[i]);
    }
    return buf;
  }

  /**
   * Formats the boolean array d.
   *
   * @param buf String builder to append to
   * @param d the boolean array to be formatted
   * @param sep separator between the single values of the array, e.g. ','
   * @return Output buffer buf
   */
  public static StringBuilder formatTo(StringBuilder buf, boolean[] d, String sep) {
    if(d == null) {
      return buf.append("null");
    }
    if(d.length == 0) {
      return buf;
    }
    buf.append(d[0]);
    for(int i = 1; i < d.length; i++) {
      buf.append(sep).append(format(d[i]));
    }
    return buf;
  }

  /**
   * Format a boolean value as string "true" or "false".
   *
   * @param buf Buffer to append to
   * @param b Boolean to Format
   * @return Same buffer
   */
  public static StringBuilder formatTo(StringBuilder buf, boolean b) {
    return buf.append(b ? "true" : "false");
  }

  /**
   * Format a boolean value as string "1" or "0".
   *
   * @param buf Buffer to append to
   * @param b Boolean to Format
   * @return Same buffer
   */
  public static StringBuilder formatBit(StringBuilder buf, boolean b) {
    return buf.append(b ? '1' : '0');
  }

  /**
   * Formats the float array d with the specified number format.
   *
   * @param d the float array to be formatted
   * @param sep separator between the single values of the array, e.g. ','
   * @param nf the number format to be used for formatting
   * @return a String representing the double array d
   */
  public static String format(float[] d, String sep, NumberFormat nf) {
    return (d == null) ? "null" : (d.length == 0) ? "" : //
        formatTo(new StringBuilder(), d, sep, nf).toString();
  }

  /**
   * Formats the float array d with the specified number format.
   *
   * @param d the float array to be formatted
   * @param sep separator between the single values of the array, e.g. ','
   * @return a String representing the double array d
   */
  public static String format(float[] d, String sep) {
    return (d == null) ? "null" : (d.length == 0) ? "" : //
        formatTo(new StringBuilder(), d, sep).toString();
  }

  /**
   * Formats the float array f with ',' as separator and default precision.
   *
   * @param f the float array to be formatted
   * @return a String representing the float array f
   */
  public static String format(float[] f) {
    return (f == null) ? "null" : (f.length == 0) ? "" : //
        formatTo(new StringBuilder(), f, ", ").toString();
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
    return (a == null) ? "null" : (a.length == 0) ? "" : //
        formatTo(new StringBuilder(), a, ", ").toString();
  }

  /**
   * Formats the long array a for printing purposes.
   *
   * @param a the long array to be formatted
   * @return a String representing the long array a
   */
  public static String format(long[] a) {
    return (a == null) ? "null" : (a.length == 0) ? "" : //
        formatTo(new StringBuilder(), a, ", ").toString();
  }

  /**
   * Formats the byte array a for printing purposes.
   *
   * @param a the byte array to be formatted
   * @return a String representing the byte array a
   */
  public static String format(byte[] a) {
    return (a == null) ? "null" : (a.length == 0) ? "" : //
        formatTo(new StringBuilder(), a, ", ").toString();
  }

  /**
   * Formats the boolean array b with ',' as separator.
   *
   * @param b the boolean array to be formatted
   * @param sep separator between the single values of the array, e.g. ','
   * @return a String representing the boolean array b
   */
  public static String format(boolean[] b, final String sep) {
    return (b == null) ? "null" : (b.length == 0) ? "" : //
        formatTo(new StringBuilder(), b, ", ").toString();
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
   * Formats the array of double arrays d with the specified separators and
   * fraction digits.
   *
   * @param buf Output buffer
   * @param d the double array to be formatted
   * @param pre Row prefix (e.g. " [")
   * @param pos Row postfix (e.g. "]\n")
   * @param csep Separator for columns (e.g. ", ")
   * @param nf the number format to use
   * @return Output buffer buf
   */
  public static StringBuilder formatTo(StringBuilder buf, double[][] d, String pre, String pos, String csep, NumberFormat nf) {
    if(d == null) {
      return buf.append("null");
    }
    if(d.length == 0) {
      return buf;
    }
    for(int i = 0; i < d.length; i++) {
      formatTo(buf.append(pre), d[i], csep, nf).append(pos);
    }
    return buf;
  }

  /**
   * Formats the double array d with ',' as separator and 2 fraction digits.
   *
   * @param d the double array to be formatted
   * @return a String representing the double array d
   */
  public static String format(double[][] d) {
    return d == null ? "null" : (d.length == 0) ? "[]" : //
        formatTo(new StringBuilder().append("[\n"), d, " [", "]\n", ", ", NF2).append(']').toString();
  }

  /**
   * Formats the array of double arrays d with 'the specified separators and
   * fraction digits.
   *
   * @param d the double matrix to be formatted
   * @param pre Row prefix (e.g. " [")
   * @param pos Row postfix (e.g. "]\n")
   * @param csep Separator for columns (e.g. ", ")
   * @param nf the number format to use
   * @return a String representing the double array d
   */
  public static String format(double[][] d, String pre, String pos, String csep, NumberFormat nf) {
    return d == null ? "null" : (d.length == 0) ? "" : //
        formatTo(new StringBuilder(), d, pre, pos, csep, nf).toString();
  }

  /**
   * Returns a string representation of this matrix.
   *
   * @param w column width
   * @param d number of digits after the decimal
   * @param pre Row prefix (e.g. " [")
   * @param pos Row postfix (e.g. "]\n")
   * @param csep Column separator (e.g. ", ")
   * @return a string representation of this matrix
   */
  public static String format(double[][] m, int w, int d, String pre, String pos, String csep) {
    DecimalFormat format = new DecimalFormat();
    format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
    format.setMinimumIntegerDigits(1);
    format.setMaximumFractionDigits(d);
    format.setMinimumFractionDigits(d);
    format.setGroupingUsed(false);

    StringBuilder msg = new StringBuilder();
    for(int i = 0; i < m.length; i++) {
      double[] row = m[i];
      msg.append(pre);
      for(int j = 0; j < row.length; j++) {
        if(j > 0) {
          msg.append(csep);
        }
        String s = format.format(row[j]); // format the number
        whitespace(msg, w - s.length()).append(s);
      }
      msg.append(pos);
    }
    return msg.toString();
  }

  /**
   * Returns a string representation of this matrix. In each line the specified
   * String <code>pre</code> is prefixed.
   *
   * @param pre the prefix of each line
   * @return a string representation of this matrix
   */
  public static String format(double[][] m, String pre) {
    StringBuilder output = new StringBuilder() //
        .append(pre).append("[\n").append(pre);
    for(int i = 0; i < m.length; i++) {
      formatTo(output.append(" ["), m[i], ", ").append("]\n").append(pre);
    }
    return output.append("]\n").toString();
  }

  /**
   * returns String-representation of Matrix.
   *
   * @param nf NumberFormat to specify output precision
   * @return String representation of this Matrix in precision as specified by
   *         given NumberFormat
   */
  public static String format(double[][] m, NumberFormat nf) {
    return formatTo(new StringBuilder().append("[\n"), m, " [", "]\n", ", ", nf).append("]").toString();
  }

  /**
   * Formats the String collection with the specified separator.
   *
   * @param d the String collection to format
   * @param sep separator between the single values of the array, e.g. ' '
   * @return a String representing the String Collection d
   */
  public static String format(Collection<String> d, String sep) {
    if(d == null) {
      return "null";
    }
    if(d.isEmpty()) {
      return "";
    }
    if(d.size() == 1) {
      return d.iterator().next();
    }
    int len = sep.length() * (d.size() - 1);
    for(String s : d) {
      len += s.length();
    }
    Iterator<String> it = d.iterator();
    StringBuilder buffer = new StringBuilder(len) //
        .append(it.next());
    while(it.hasNext()) {
      buffer.append(sep).append(it.next());
    }
    return buffer.toString();
  }

  /**
   * Formats the string array d with the specified separator.
   *
   * @param d the string array to be formatted
   * @param sep separator between the single values of the array, e.g. ','
   * @return a String representing the string array d
   */
  public static String format(String[] d, String sep) {
    if(d == null) {
      return "null";
    }
    if(d.length == 0) {
      return "";
    }
    if(d.length == 1) {
      return d[0];
    }
    int len = sep.length() * (d.length - 1);
    for(String s : d) {
      len += s.length();
    }
    StringBuilder buffer = new StringBuilder(len)//
        .append(d[0]);
    for(int i = 1; i < d.length; i++) {
      buffer.append(sep).append(d[i]);
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
    in = in < 0 ? s.length() : in;
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
   *         otherwise whichever is not negative.
   */
  private static int nextPosition(int a, int b) {
    return a < 0 ? b : b < 0 ? a : a < b ? a : b;
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
    if(n < WHITESPACE_BUFFER_LENGTH) {
      return WHITESPACE_BUFFER.substring(0, n);
    }
    char[] buf = new char[n];
    Arrays.fill(buf, WHITESPACE_BUFFER.charAt(0));
    return new String(buf);
  }

  /**
   * Returns a string with the specified number of whitespace.
   *
   * @param n the number of whitespace characters
   * @return a string with the specified number of blanks
   */
  public static StringBuilder whitespace(StringBuilder buf, int n) {
    while(n >= WHITESPACE_BUFFER_LENGTH) {
      buf.append(WHITESPACE_BUFFER);
      n -= WHITESPACE_BUFFER_LENGTH;
    }
    return n > 0 ? buf.append(WHITESPACE_BUFFER, 0, n) : buf;
  }

  /**
   * Pad a string to a given length by adding whitespace to the right.
   *
   * @param o original string
   * @param len destination length
   * @return padded string of at least length len (and o otherwise)
   */
  public static String pad(String o, int len) {
    return o.length() >= len ? o : (o + whitespace(len - o.length()));
  }

  /**
   * Pad a string to a given length by adding whitespace to the left.
   *
   * @param o original string
   * @param len destination length
   * @return padded string of at least length len (and o otherwise)
   */
  public static String padRightAligned(String o, int len) {
    return o.length() >= len ? o : (whitespace(len - o.length()) + o);
  }

  /**
   * Terminal width cache.
   */
  private static int width = -1;

  /**
   * Get the width of the terminal window (on Unix xterms), with a default of 78
   * characters.
   *
   * @return Terminal width
   */
  public static int getConsoleWidth() {
    if(width > 0) {
      return width;
    }
    final int default_termwidth = 78;
    try {
      final String env = System.getenv("COLUMNS");
      if(env != null) {
        int columns = ParseUtil.parseIntBase10(env);
        return width = (columns > 50 ? columns - 1 : default_termwidth);
      }
    }
    catch(SecurityException | NumberFormatException e) {
      // OK. Probably not exported.
    }
    try {
      Process p = Runtime.getRuntime().exec(new String[] { "sh", "-c", "tput cols 2> /dev/tty" });
      byte[] buf = new byte[16];
      p.getOutputStream().close(); // We do not intend to write.
      int l = p.getInputStream().read(buf);
      if(l >= 2 && l < buf.length) {
        int columns = ParseUtil.parseIntBase10(new String(buf, 0, buf[l - 1] == '\n' ? l - 1 : l));
        return width = (columns > 50 ? columns - 1 : default_termwidth);
      }
      p.destroy();
    }
    catch(IOException | SecurityException | NumberFormatException e) {
      // Ok. Probably not a unix system.
    }
    // We could use the jLine library, but that would introduce another
    // dependency. :-(
    return width = default_termwidth;
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
    (x < 10000) // 1-4 vs. 5-10
        ? (x < 100) // 1-2 vs. 3-4
            ? ((x < 10) ? 1 : 2) //
            : ((x < 1000) ? 3 : 4) //
        : (x < 1000000) // 5-6 vs. 7-10
            ? ((x < 100000) ? 5 : 6) // 5-6
            : (x < 100000000) // 7-8 vs. 9-10
                ? ((x < 10000000) ? 7 : 8) // 7-8
                : ((x < 1000000000) ? 9 : 10); // 9-10
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
    // This is almost a binary search - 10 cases is not a power of two, and we
    // assume that the smaller values are more frequent.
    return (x <= Integer.MAX_VALUE) ? stringSize((int) x) : //
        (x < 10000000000000L) // 10-13 vs. 14-19
            ? (x < 100000000000L) // 10-11 vs. 12-13
                ? ((x < 10000000000L) ? 10 : 11) //
                : ((x < 1000000000000L) ? 12 : 13) //
            : (x < 1000000000000000L) // 14-15 vs. 16-19
                ? ((x < 100000000000000L) ? 14 : 15) // 14-15
                : (x < 100000000000000000L) // 16-17 vs. 18-19
                    ? ((x < 10000000000000000L) ? 16 : 17) // 16-17
                    : ((x < 1000000000000000000L) ? 18 : 19); // 18-19
  }

  /**
   * Similar to {@link String#endsWith(String)} but for buffers.
   *
   * @param buf Buffer
   * @param end End
   * @return {@code true} if the buffer ends with the given sequence
   */
  public static boolean endsWith(CharSequence buf, CharSequence end) {
    int len = end.length(), start = buf.length() - len;
    if(start < 0) {
      return false;
    }
    for(int i = 0; i < len; i++, start++) {
      if(buf.charAt(start) != end.charAt(i)) {
        return false;
      }
    }
    return true;
  }
}
