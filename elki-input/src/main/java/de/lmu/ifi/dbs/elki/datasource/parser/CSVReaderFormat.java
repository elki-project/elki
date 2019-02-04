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
package de.lmu.ifi.dbs.elki.datasource.parser;

import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.utilities.io.TokenizedReader;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Basic format factory for parsing CSV-like formats.
 * 
 * To read CSV files into ELKI, see {@link NumberVectorLabelParser}.
 * 
 * This class encapsulates csv format settings, that need to be parsed in
 * multiple places from ELKI, not only on input vector files.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.1
 * 
 * @has - - - TokenizedReader
 */
public class CSVReaderFormat {
  /**
   * A pattern defining whitespace.
   */
  public static final String DEFAULT_SEPARATOR = "\\s*[,;\\s]\\s*";

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
   * Default CSV input format.
   */
  public static final CSVReaderFormat DEFAULT_FORMAT = new CSVReaderFormat(Pattern.compile(DEFAULT_SEPARATOR), QUOTE_CHARS, Pattern.compile(COMMENT_PATTERN));

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

  /**
   * Constructor.
   * 
   * @param colSep Column separator
   * @param quoteChars Quote character
   * @param comment Comment pattern
   */
  public CSVReaderFormat(Pattern colSep, String quoteChars, Pattern comment) {
    super();
    this.colSep = colSep;
    this.quoteChars = quoteChars;
    this.comment = comment;
  }

  /**
   * Make a reader for the configured format.
   * 
   * @return A tokenized reader for this format.
   */
  public TokenizedReader makeReader() {
    return new TokenizedReader(colSep, quoteChars, comment);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * OptionID for the column separator parameter (defaults to whitespace as in
     * {@link #DEFAULT_SEPARATOR}.
     */
    public static final OptionID COLUMN_SEPARATOR_ID = new OptionID("parser.colsep", "Column separator pattern. The default assumes whitespace separated data.");

    /**
     * OptionID for the quote character parameter (defaults to a double
     * quotation mark as in {@link CSVReaderFormat#QUOTE_CHARS}.
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
    protected CSVReaderFormat makeInstance() {
      return new CSVReaderFormat(colSep, quoteChars, comment);
    }
  }
}
