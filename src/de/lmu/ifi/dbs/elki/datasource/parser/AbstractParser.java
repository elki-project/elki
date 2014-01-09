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

import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Abstract superclass for all parsers providing the option handler for handling
 * options.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * 
 * @apiviz.composedOf Tokenizer
 */
public abstract class AbstractParser {
  /**
   * A pattern defining whitespace.
   */
  public static final String DEFAULT_SEPARATOR = "(\\s+|\\s*[,;]\\s*)";

  /**
   * A quote pattern
   */
  public static final String QUOTE_CHARS = "\"'";

  /**
   * A pattern catching most numbers that can be parsed using
   * Double.parseDouble:
   * 
   * Some examples: <code>1</code> <code>1.</code> <code>1.2</code>
   * <code>.2</code> <code>-.2e-03</code>
   */
  public static final String NUMBER_PATTERN = "[+-]?(?:\\d+\\.?|\\d*\\.\\d+)?(?:[eE][-]?\\d+)?";

  /**
   * Default pattern for comments.
   */
  public static final String COMMENT_PATTERN = "^\\s*(#|//|;).*$";

  /**
   * A sign to separate attributes.
   */
  public static final String ATTRIBUTE_CONCATENATION = " ";

  /**
   * Comment pattern.
   */
  protected Pattern comment = null;

  /**
   * String tokenizer.
   */
  protected Tokenizer tokenizer;

  /**
   * Constructor.
   * 
   * @param colSep Column separator
   * @param quoteChars Quote character
   * @param comment Comment pattern
   */
  public AbstractParser(Pattern colSep, String quoteChars, Pattern comment) {
    super();
    this.tokenizer = new Tokenizer(colSep, quoteChars);
    this.comment = comment;
  }

  public static int lengthWithoutLinefeed(String line) {
    int length = line.length();
    while(length > 0) {
      char last = line.charAt(length - 1);
      if(last != '\n' && last != '\r') {
        break;
      }
      --length;
    }
    return length;
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
     * OptionID for the column separator parameter (defaults to whitespace as in
     * {@link #DEFAULT_SEPARATOR}.
     */
    public static final OptionID COLUMN_SEPARATOR_ID = new OptionID("parser.colsep", "Column separator pattern. The default assumes whitespace separated data.");

    /**
     * OptionID for the quote character parameter (defaults to a double
     * quotation mark as in {@link AbstractParser#QUOTE_CHARS}.
     */
    public static final OptionID QUOTE_ID = new OptionID("parser.quote", "Quotation characters. By default, both double and single ASCII quotes are accepted.");

    /**
     * Comment pattern.
     */
    public static final OptionID COMMENT_ID = new OptionID("string.comment", "Ignore lines in the input file that satisfy this pattern.");

    /**
     * Stores the column separator pattern
     */
    protected Pattern colSep = null;

    /**
     * Stores the quotation character
     */
    protected String quoteChars = QUOTE_CHARS;

    /**
     * Comment pattern.
     */
    protected Pattern comment = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter colParam = new PatternParameter(COLUMN_SEPARATOR_ID, DEFAULT_SEPARATOR);
      if(config.grab(colParam)) {
        colSep = colParam.getValue();
      }
      StringParameter quoteParam = new StringParameter(QUOTE_ID, QUOTE_CHARS);
      if(config.grab(quoteParam)) {
        quoteChars = quoteParam.getValue();
      }
      PatternParameter commentP = new PatternParameter(COMMENT_ID, COMMENT_PATTERN);
      if(config.grab(commentP)) {
        comment = commentP.getValue();
      }
    }

    @Override
    protected abstract AbstractParser makeInstance();
  }
}
