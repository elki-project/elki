package de.lmu.ifi.dbs.elki.datasource.parser;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.StringLengthConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Abstract superclass for all parsers providing the option handler for handling
 * options.
 * 
 * @author Arthur Zimek
 */
public abstract class AbstractParser {
  /**
   * A pattern defining whitespace.
   */
  public static final String DEFAULT_SEPARATOR = "(\\s+|\\s*[,;]\\s*)";

  /**
   * A quote pattern
   */
  public static final char QUOTE_CHAR = '\"';

  /**
   * A pattern catching most numbers that can be parsed using Double.parseDouble:
   * 
   * Some examples: <code>1</code> <code>1.</code> <code>1.2</code>
   * <code>.2</code> <code>-.2e-03</code>
   */
  public static final String NUMBER_PATTERN = "[+-]?(?:\\d+\\.?|\\d*\\.\\d+)?(?:[eE][-]?\\d+)?";

  /**
   * OptionID for the column separator parameter (defaults to whitespace as in
   * {@link #DEFAULT_SEPARATOR}.
   */
  public static final OptionID COLUMN_SEPARATOR_ID = OptionID.getOrCreateOptionID("parser.colsep", "Column separator pattern. The default assumes whitespace separated data.");

  /**
   * OptionID for the quote character parameter (defaults to a double quotation
   * mark as in {@link #QUOTE_CHAR}.
   */
  public static final OptionID QUOTE_ID = OptionID.getOrCreateOptionID("parser.quote", "Quotation character. The default is to use a double quote.");

  /**
   * Stores the column separator pattern
   */
  private Pattern colSep = null;

  /**
   * Stores the quotation character
   */
  protected char quoteChar = QUOTE_CHAR;

  /**
   * The comment character.
   */
  public static final String COMMENT = "#";

  /**
   * A sign to separate attributes.
   */
  public static final String ATTRIBUTE_CONCATENATION = " ";

  /**
   * Constructor.
   * 
   * @param colSep Column separator
   * @param quoteChar Quote character
   */
  public AbstractParser(Pattern colSep, char quoteChar) {
    super();
    this.colSep = colSep;
    this.quoteChar = quoteChar;
  }

  /**
   * Tokenize a string. Works much like colSep.split() except it honors
   * quotation characters.
   * 
   * @param input Input string
   * @return Tokenized string
   */
  protected List<String> tokenize(String input) {
    ArrayList<String> matchList = new ArrayList<String>();
    Matcher m = colSep.matcher(input);

    int index = 0;
    boolean inquote = (input.length() > 0) && (input.charAt(0) == quoteChar);
    while(m.find()) {
      // Quoted code path vs. regular code path
      if(inquote && m.start() > 0) {
        // Closing quote found?
        if(m.start() > index + 1 && input.charAt(m.start() - 1) == quoteChar) {
          // Strip quote characters
          if (index + 1 < m.start() - 1) {
            matchList.add(input.substring(index + 1, m.start() - 1));
          }
          // Seek past
          index = m.end();
          // new quote?
          inquote = (index < input.length()) && (input.charAt(index) == quoteChar);
        }
      }
      else {
        // Add match before separator
        if (index < m.start()) {
          matchList.add(input.substring(index, m.start()));
        }
        // Seek past separator
        index = m.end();
        // new quote?
        inquote = (index < input.length()) && (input.charAt(index) == quoteChar);
      }
    }
    // Nothing found - return original string.
    if(index == 0) {
      matchList.add(input);
      return matchList;
    }
    // Add tail after last separator.
    if(inquote) {
      if(input.charAt(input.length() - 1) == quoteChar) {
        if (index + 1 < input.length() - 1) {
          matchList.add(input.substring(index + 1, input.length() - 1));
        }
      }
      else {
        getLogger().warning("Invalid quoted line in input.");
        if (index < input.length()) {
          matchList.add(input.substring(index, input.length()));
        }
      }
    }
    else {
      if (index < input.length()) {
        matchList.add(input.substring(index, input.length()));
      }
    }
    // Return
    return matchList;
  }

  /**
   * Get the logger for this class.
   * 
   * @return Logger.
   */
  protected abstract Logging getLogger();

  /**
   * Returns a string representation of the object.
   * 
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return getClass().getName();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Stores the column separator pattern
     */
    protected Pattern colSep = null;

    /**
     * Stores the quotation character
     */
    protected char quoteChar = QUOTE_CHAR;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter colParam = new PatternParameter(COLUMN_SEPARATOR_ID, DEFAULT_SEPARATOR);
      if(config.grab(colParam)) {
        colSep = colParam.getValue();
      }
      StringParameter quoteParam = new StringParameter(QUOTE_ID, String.valueOf(QUOTE_CHAR));
      quoteParam.addConstraint(new StringLengthConstraint(1, 1));
      if(config.grab(quoteParam)) {
        quoteChar = quoteParam.getValue().charAt(0);
      }
    }

    @Override
    protected abstract AbstractParser makeInstance();
  }
}