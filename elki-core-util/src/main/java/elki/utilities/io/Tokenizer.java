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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import elki.logging.Logging;
import elki.utilities.datastructures.iterator.Iter;

/**
 * String tokenizer.
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
public class Tokenizer implements Iter {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(Tokenizer.class);

  /**
   * Quote characters
   */
  public static final String QUOTE_CHAR = "\"'";

  /**
   * Stores the quotation character
   */
  private char[] quoteChars = QUOTE_CHAR.toCharArray();

  /**
   * Constructor.
   *
   * @param colSep Column separator pattern.
   * @param quoteChars Quotation character.
   */
  public Tokenizer(Pattern colSep, String quoteChars) {
    super();
    assert (colSep != null) : "Column separator may not be null.";
    this.matcher = colSep.matcher("");
    this.quoteChars = quoteChars != null ? quoteChars.toCharArray() : new char[0];
  }

  /**
   * Regular expression match helper.
   */
  private Matcher matcher;

  /**
   * Data currently processed.
   */
  private CharSequence input;

  /**
   * Substring to process.
   */
  private int send;

  /**
   * Current positions of result and iterator.
   */
  private int start, end, index;

  /**
   * Whether the current token is a quoted string.
   */
  private boolean quoted;

  /**
   * Initialize parser with a new string.
   *
   * @param input New string to parse.
   * @param begin Begin
   * @param end End
   */
  public void initialize(CharSequence input, int begin, int end) {
    this.input = input;
    this.send = end;
    this.matcher.reset(input).region(begin, end);
    this.index = begin;
    advance();
  }

  @Override
  public boolean valid() {
    return start < send;
  }

  @Override
  public Tokenizer advance() {
    char inquote = isQuote(index);
    while(matcher.find()) {
      // Quoted code path vs. regular code path
      if(inquote != 0) {
        // Matching closing quote found?
        if(matcher.start() > index + 1 && input.charAt(matcher.start() - 1) == inquote) {
          this.start = index + 1;
          this.end = matcher.start() - 1;
          this.index = matcher.end();
          this.quoted = true;
          return this;
        }
      }
      else {
        this.start = index;
        this.end = matcher.start();
        this.index = matcher.end();
        this.quoted = false;
        return this;
      }
    }
    // Add tail after last separator.
    this.start = index;
    this.end = send;
    this.index = end + 1;
    this.quoted = false;
    if(inquote != 0) {
      final int last = send - 1;
      if(input.charAt(last) == inquote) {
        ++this.start;
        --this.end;
        this.quoted = true;
      }
      else {
        LOG.warning("Invalid quoted line in input: no closing quote found in: " + input);
      }
    }
    return this;
  }

  /**
   * Get the current part as substring
   *
   * @return Current value as substring.
   */
  public String getSubstring() {
    return input.subSequence(start, end).toString();
  }

  /**
   * Get the current part as substring
   *
   * @return Current value as substring.
   */
  public String getStrippedSubstring() {
    int sstart = start, ssend = end;
    while(sstart < ssend) {
      char c = input.charAt(sstart);
      if(c != ' ' && c != '\n' && c != '\r' && c != '\t') {
        break;
      }
      ++sstart;
    }
    while(--ssend >= sstart) {
      char c = input.charAt(ssend);
      if(c != ' ' && c != '\n' && c != '\r' && c != '\t') {
        break;
      }
    }
    ++ssend;
    return (sstart < ssend) ? input.subSequence(sstart, ssend).toString() : "";
  }

  /**
   * Get current value as double.
   *
   * @return double value
   * @throws NumberFormatException when current value cannot be parsed as double
   */
  public double getDouble() {
    return ParseUtil.parseDouble(input, start, end);
  }

  /**
   * Get current value as int.
   *
   * @return int value
   * @throws NumberFormatException when current value cannot be parsed as int.
   */
  public int getIntBase10() {
    return ParseUtil.parseIntBase10(input, start, end);
  }

  /**
   * Get current value as long.
   *
   * @return long value
   * @throws NumberFormatException when current value cannot be parsed as long.
   */
  public long getLongBase10() {
    return ParseUtil.parseLongBase10(input, start, end);
  }

  /**
   * Test for empty tokens; usually at end of line.
   *
   * @return Empty
   */
  public boolean isEmpty() {
    return end <= start;
  }

  /**
   * Detect quote characters.
   * <p>
   * TODO: support more than one quote character, make sure opening and closing
   * quotes match then.
   *
   * @param index Position
   * @return {@code 1} when a quote character, {@code 0} otherwise.
   */
  private char isQuote(int index) {
    if(index >= input.length()) {
      return 0;
    }
    char c = input.charAt(index);
    for(int i = 0; i < quoteChars.length; i++) {
      if(c == quoteChars[i]) {
        return c;
      }
    }
    return 0;
  }

  /**
   * Test if the current string was quoted.
   *
   * @return {@code true} when quoted.
   */
  public boolean isQuoted() {
    return quoted;
  }

  /**
   * Get start of token.
   *
   * @return Start
   */
  public int getStart() {
    return start;
  }

  /**
   * Get end of token.
   *
   * @return End
   */
  public int getEnd() {
    return end;
  }

  /**
   * Get length of token.
   *
   * @return Token length
   */
  public int getLength() {
    return end - start;
  }

  /**
   * Get a single character.
   *
   * @param off Offset
   * @return Character
   */
  public char getChar(int off) {
    return input.charAt(start + off);
  }

  /**
   * Perform cleanup.
   */
  public void cleanup() {
    input = null;
    matcher.reset("");
  }
}
