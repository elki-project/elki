/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.utilities.io;

import elki.logging.Logging;
import elki.utilities.datastructures.BitsUtil;

/**
 * Helper functionality for parsing.
 * <p>
 * This class is fairly optimized. Because exceptions frequently occur in
 * parsing (e.g., when trying to parse a text as float), this implementation
 * avoids allocating exceptions and stack traces in these cases, and uses static
 * instances instead. By setting the logging level for the ParseUtil class to
 * FINE, the backtraces can still be obtained for development. But usually you
 * will want to catch this exception and instead throw your own with additional
 * context information of the value that failed to parse.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public final class ParseUtil {
  /**
   * Logging helper.
   */
  private static final Logging LOG = Logging.getLogger(ParseUtil.class);

  /**
   * Private constructor. Static methods only.
   */
  private ParseUtil() {
    // Do not use.
  }

  /**
   * Preallocated exception.
   *
   * @author Erich Schubert
   */
  private static class PreallocatedException extends NumberFormatException {
    /**
     * Serialization version ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param msg Message
     */
    public PreallocatedException(String msg) {
      super(msg);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }

    @Override
    public String toString() {
      return getClass().getSuperclass().getName() + ": " + getMessage() + " (increase the logging level of " + ParseUtil.class.getName() + " to get a backtrace)";
    }
  }

  /**
   * Preallocated exceptions.
   */
  public static final NumberFormatException EMPTY_STRING = new PreallocatedException("Parser called on an empty string");

  /**
   * Preallocated exceptions.
   */
  public static final NumberFormatException EXPONENT_OVERFLOW = new PreallocatedException("Precision overflow for double exponent");

  /**
   * Preallocated exceptions.
   */
  public static final NumberFormatException INVALID_EXPONENT = new PreallocatedException("Invalid exponent");

  /**
   * Preallocated exceptions.
   */
  public static final NumberFormatException TRAILING_CHARACTERS = new PreallocatedException("String sequence was not completely consumed");

  /**
   * Preallocated exceptions.
   */
  public static final NumberFormatException PRECISION_OVERFLOW = new PreallocatedException("Precision overflow when parsing a number");

  /**
   * Preallocated exceptions.
   */
  public static final NumberFormatException NOT_A_NUMBER = new PreallocatedException("Number must start with a digit or dot");

  /**
   * Infinity pattern, with any capitalization
   */
  private static final char[] INFINITY_PATTERN = { //
      'I', 'n', 'f', 'i', 'n', 'i', 't', 'y', //
      'i', 'N', 'F', 'I', 'N', 'I', 'T', 'Y' };

  /** Length of pattern */
  private static final int INFINITY_LENGTH = INFINITY_PATTERN.length >> 1;

  /**
   * Parse a double from a character sequence.
   * <p>
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
      throw LOG.isDebuggingFine() ? new NumberFormatException(EMPTY_STRING.getMessage()) : EMPTY_STRING;
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

    final int realstart = pos;
    // Begin parsing real numbers!
    if(((cur < '0') || (cur > '9')) && (cur != '.')) {
      throw LOG.isDebuggingFine() ? new NumberFormatException(NOT_A_NUMBER.getMessage()) : NOT_A_NUMBER;
    }

    // Parse digits into a long, remember offset of decimal point.
    long decimal = 0;
    int decimalPoint = -1;
    while(true) {
      final int digit = cur - '0';
      if((digit >= 0) && (digit <= 9)) {
        final long tmp = (decimal << 3) + (decimal << 1) + digit;
        if(tmp >= decimal) {
          decimal = tmp; // Otherwise, silently ignore the extra digits.
        }
        else if(++decimalPoint == 0) { // Because we ignored the digit
          throw LOG.isDebuggingFine() ? new NumberFormatException(PRECISION_OVERFLOW.getMessage()) : PRECISION_OVERFLOW;
        }
      }
      else if((cur == '.') && (decimalPoint < 0)) {
        decimalPoint = pos;
      }
      else { // No more digits, or a second dot.
        break;
      }
      if(++pos >= end) {
        break;
      }
      cur = str[pos];
    }
    // Not a single digit, only "-." etc.
    if(pos == realstart + (decimalPoint >= 0 ? 1 : 0)) {
      throw LOG.isDebuggingFine() ? new NumberFormatException(NOT_A_NUMBER.getMessage()) : NOT_A_NUMBER;
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
        throw LOG.isDebuggingFine() ? new NumberFormatException(INVALID_EXPONENT.getMessage()) : INVALID_EXPONENT;
      }
      while(true) {
        final int digit = cur - '0';
        if(digit < 0 || digit > 9) {
          break;
        }
        exp = (exp << 3) + (exp << 1) + digit;
        if(exp > Double.MAX_EXPONENT) {
          throw LOG.isDebuggingFine() ? new NumberFormatException(EXPONENT_OVERFLOW.getMessage()) : EXPONENT_OVERFLOW;
        }
        if(++pos >= end) {
          break;
        }
        cur = str[pos];
      }
      exp = isNegativeExp ? -exp : exp;
    }
    // Adjust exponent by the offset of the dot in our long.
    exp = decimalPoint > 0 ? (exp - decimalPoint) : exp;
    if(pos != end) {
      throw LOG.isDebuggingFine() ? new NumberFormatException(TRAILING_CHARACTERS.getMessage()) : TRAILING_CHARACTERS;
    }

    return BitsUtil.lpow10(isNegative ? -decimal : decimal, exp);
  }

  /**
   * Parse a double from a character sequence.
   * <p>
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
   * <p>
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
      throw LOG.isDebuggingFine() ? new NumberFormatException(EMPTY_STRING.getMessage()) : EMPTY_STRING;
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

    final int realstart = pos;
    // Begin parsing real numbers!
    if(((cur < '0') || (cur > '9')) && (cur != '.')) {
      throw LOG.isDebuggingFine() ? new NumberFormatException(NOT_A_NUMBER.getMessage()) : NOT_A_NUMBER;
    }

    // Parse digits into a long, remember offset of decimal point.
    long decimal = 0;
    int decimalPoint = -1;
    while(true) {
      final int digit = cur - '0';
      if((digit >= 0) && (digit <= 9)) {
        final long tmp = (decimal << 3) + (decimal << 1) + digit;
        if(tmp >= decimal) {
          decimal = tmp; // Otherwise, silently ignore the extra digits.
        }
        else if(++decimalPoint == 0) { // Because we ignored the digit
          throw LOG.isDebuggingFine() ? new NumberFormatException(PRECISION_OVERFLOW.getMessage()) : PRECISION_OVERFLOW;
        }
      }
      else if((cur == '.') && (decimalPoint < 0)) {
        decimalPoint = pos;
      }
      else { // No more digits, or a second dot.
        break;
      }
      if(++pos >= end) {
        break;
      }
      cur = str.charAt(pos);
    }
    // Not a single digit, only "-." etc.
    if(pos == realstart + (decimalPoint >= 0 ? 1 : 0)) {
      throw LOG.isDebuggingFine() ? new NumberFormatException(NOT_A_NUMBER.getMessage()) : NOT_A_NUMBER;
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
        throw LOG.isDebuggingFine() ? new NumberFormatException(INVALID_EXPONENT.getMessage()) : INVALID_EXPONENT;
      }
      while(true) {
        final int digit = cur - '0';
        if(digit < 0 || digit > 9) {
          break;
        }
        exp = (exp << 3) + (exp << 1) + digit;
        if(exp > Double.MAX_EXPONENT) {
          throw LOG.isDebuggingFine() ? new NumberFormatException(EXPONENT_OVERFLOW.getMessage()) : EXPONENT_OVERFLOW;
        }
        if(++pos >= end) {
          break;
        }
        cur = str.charAt(pos);
      }
      exp = isNegativeExp ? -exp : exp;
    }
    // Adjust exponent by the offset of the dot in our long.
    exp = decimalPoint > 0 ? (exp - decimalPoint) : exp;
    if(pos != end) {
      throw LOG.isDebuggingFine() ? new NumberFormatException(TRAILING_CHARACTERS.getMessage()) : TRAILING_CHARACTERS;
    }

    return BitsUtil.lpow10(isNegative ? -decimal : decimal, exp);
  }

  /**
   * Parse a long integer from a character sequence.
   *
   * @param str String
   * @return Long value
   */
  public static long parseLongBase10(final CharSequence str) {
    return parseLongBase10(str, 0, str.length());
  }

  /**
   * Parse a long integer from a character sequence.
   *
   * @param str String
   * @param start Begin
   * @param end End
   * @return Long value
   */
  public static long parseLongBase10(final CharSequence str, final int start, final int end) {
    if(start >= end) {
      throw LOG.isDebuggingFine() ? new NumberFormatException(EMPTY_STRING.getMessage()) : EMPTY_STRING;
    }
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
      throw LOG.isDebuggingFine() ? new NumberFormatException(NOT_A_NUMBER.getMessage()) : NOT_A_NUMBER;
    }

    // Parse digits into a long, remember offset of decimal point.
    long decimal = 0;
    while(true) {
      final int digit = cur - '0';
      if((digit >= 0) && (digit <= 9)) {
        final long tmp = (decimal << 3) + (decimal << 1) + digit;
        if(tmp < decimal) {
          // Special case, Long.MIN_VALUE only.
          if(isNegative && tmp == 0x8000000000000000L && pos + 1 == end) {
            return Long.MIN_VALUE;
          }
          throw LOG.isDebuggingFine() ? new NumberFormatException(PRECISION_OVERFLOW.getMessage()) : PRECISION_OVERFLOW;
        }
        decimal = tmp;
      }
      else { // No more digits
        break;
      }
      if(++pos >= end) {
        break;
      }
      cur = str.charAt(pos);
    }
    if(pos != end) {
      throw LOG.isDebuggingFine() ? new NumberFormatException(TRAILING_CHARACTERS.getMessage()) : TRAILING_CHARACTERS;
    }

    return isNegative ? -decimal : decimal;
  }

  /**
   * Parse an integer from a character sequence.
   *
   * @param str String
   * @return Int value
   */
  public static int parseIntBase10(final CharSequence str) {
    return parseIntBase10(str, 0, str.length());
  }

  /**
   * Parse an integer from a character sequence.
   *
   * @param str String
   * @param start Begin
   * @param end End
   * @return int value
   */
  public static int parseIntBase10(final CharSequence str, final int start, final int end) {
    if(start >= end) {
      throw LOG.isDebuggingFine() ? new NumberFormatException(EMPTY_STRING.getMessage()) : EMPTY_STRING;
    }
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
      throw LOG.isDebuggingFine() ? new NumberFormatException(NOT_A_NUMBER.getMessage()) : NOT_A_NUMBER;
    }

    // Parse digits into a int, remember offset of decimal point.
    int decimal = 0;
    while(true) {
      final int digit = cur - '0';
      if((digit >= 0) && (digit <= 9)) {
        final int tmp = (decimal << 3) + (decimal << 1) + digit;
        if(tmp < decimal) {
          // Special case, Integer.MIN_VALUE only.
          if(isNegative && tmp == 0x80000000 && pos + 1 == end) {
            return Integer.MIN_VALUE;
          }
          throw LOG.isDebuggingFine() ? new NumberFormatException(PRECISION_OVERFLOW.getMessage()) : PRECISION_OVERFLOW;
        }
        decimal = tmp;
      }
      else { // No more digits
        break;
      }
      if(++pos >= end) {
        break;
      }
      cur = str.charAt(pos);
    }
    if(pos != end) {
      throw LOG.isDebuggingFine() ? new NumberFormatException(TRAILING_CHARACTERS.getMessage()) : TRAILING_CHARACTERS;
    }

    return isNegative ? -decimal : decimal;
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
    // The wonders of unicode. The infinity symbol \u221E is three bytes:
    if(len == 3 && firstchar == -0x1E && str[start + 1] == -0x78 && str[start + 2] == -0x62) {
      return true;
    }
    if((len != 3 && len != INFINITY_LENGTH) //
        || (firstchar != 'I' && firstchar != 'i')) {
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
    // The wonders of unicode: infinity symbol
    if(len == 1 && firstchar == '\u221E') {
      return true;
    }
    if((len != 3 && len != INFINITY_LENGTH) //
        || (firstchar != 'I' && firstchar != 'i')) {
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
    if(len < 2 || len > 3 || (firstchar != 'N' && firstchar != 'n')) {
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
    return c2 == 'N' || c2 == 'n';
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
    if(len < 2 || len > 3 || (firstchar != 'N' && firstchar != 'n')) {
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
    return c2 == 'N' || c2 == 'n';
  }
}
