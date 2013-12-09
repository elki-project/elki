package de.lmu.ifi.dbs.elki.datasource.parser;

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.Iter;

/**
 * String tokenizer.
 * 
 * @author Erich Schubert
 */
public class Tokenizer implements Iter {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(Tokenizer.class);

  /**
   * Separator pattern.
   */
  private Pattern colSep;

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
    this.colSep = colSep;
    this.quoteChars = quoteChars.toCharArray();
  }

  /**
   * Regular expression match helper.
   */
  private Matcher m = null;

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
   * Initialize parser with a new string.
   * 
   * @param input New string to parse.
   * @param begin Begin
   * @param end End
   */
  public void initialize(CharSequence input, int begin, int end) {
    this.input = input;
    this.send = end;
    this.m = colSep.matcher(input).region(begin, end);
    this.index = begin;
    advance();
  }

  @Override
  public boolean valid() {
    return start < send;
  }

  @Override
  public void advance() {
    char inquote = isQuote(index);
    while(m.find()) {
      // Quoted code path vs. regular code path
      if(inquote != 0) {
        // Matching closing quote found?
        if(m.start() > index + 1 && input.charAt(m.start() - 1) == inquote) {
          this.start = index + 1;
          this.end = m.start() - 1;
          this.index = m.end();
          return;
        }
        continue;
      }
      else {
        this.start = index;
        this.end = m.start();
        this.index = m.end();
        return;
      }
    }
    // Add tail after last separator.
    this.start = index;
    this.end = send;
    this.index = end + 1;
    if(inquote != 0) {
      final int last = send - 1;
      if(input.charAt(last) == inquote) {
        ++this.start;
        --this.end;
      }
      else {
        LOG.warning("Invalid quoted line in input: no closing quote found in: " + input);
      }
    }
  }

  /**
   * Get the current part as substring
   * 
   * @return Current value as substring.
   */
  public String getSubstring() {
    // TODO: detect Java <6 and make sure we only return the substring?
    // With java 7, String.substring will arraycopy the characters.
    return input.subSequence(start, end).toString();
  }

  /**
   * Get current value as double.
   * 
   * @return double value
   * @throws NumberFormatException when current value cannot be parsed as double
   *         value.
   */
  public double getDouble() throws NumberFormatException {
    return FormatUtil.parseDouble(input, start, end);
  }

  /**
   * Get current value as long.
   * 
   * @return double value
   * @throws NumberFormatException when current value cannot be parsed as long
   *         value.
   */
  public long getLongBase10() throws NumberFormatException {
    return FormatUtil.parseLongBase10(input, start, end);
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
   * 
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
}
