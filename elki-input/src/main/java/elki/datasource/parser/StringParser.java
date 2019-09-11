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
package elki.datasource.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import elki.data.LabelList;
import elki.data.type.TypeUtil;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Parser that loads a text file for use with string similarity measures.
 * 
 * The parser produces two relations: the first of type String, the second of
 * type label list, which contains the same data for convenience.
 * 
 * @author Felix Stahlberg
 * @author Erich Schubert
 * @since 0.6.0
 */
@Title("String Parser")
@Description("Parses new line separated strings")
public class StringParser implements Parser {
  /**
   * Comment pattern.
   */
  Matcher comment;

  /**
   * Flag to trim whitespace.
   */
  boolean trimWhitespace;

  /**
   * Constructor.
   * 
   * @param comment Pattern for comments.
   * @param trimWhitespace Trim leading and trailing whitespace.
   */
  public StringParser(Pattern comment, boolean trimWhitespace) {
    super();
    this.comment = (comment != null) ? comment.matcher("") : null;
    this.trimWhitespace = trimWhitespace;
  }

  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    List<String> data = new ArrayList<>();
    List<LabelList> labels = new ArrayList<>();
    ArrayList<String> ll = new ArrayList<>(1);
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        // Skip empty lines and comments
        if(line.length() <= 0 || (comment != null && comment.reset(line).matches())) {
          continue;
        }
        final String val = trimWhitespace ? line.trim() : line;
        data.add(val);
        ll.clear();
        ll.add(val);
        labels.add(LabelList.make(ll));
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }
    return MultipleObjectsBundle.makeSimple(TypeUtil.STRING, data, TypeUtil.LABELLIST, labels);
  }

  @Override
  public void cleanup() {
    comment.reset("");
  }

  /**
   * Parameterization class.
   * 
   * @author Felix Stahlberg
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Flag to trim whitespace.
     */
    public static final OptionID TRIM_ID = new OptionID("string.trim", "Remove leading and trailing whitespace from each line.");

    /**
     * Comment pattern.
     */
    Pattern comment = null;

    /**
     * Flag to trim whitespace.
     */
    boolean trimWhitespace = false;

    @Override
    public void configure(Parameterization config) {
      new PatternParameter(CSVReaderFormat.Par.COMMENT_ID, "^\\s*#.*$") //
          .grab(config, x -> comment = x);
      new Flag(TRIM_ID).grab(config, x -> trimWhitespace = x);
    }

    @Override
    public StringParser make() {
      return new StringParser(comment, trimWhitespace);
    }
  }
}
