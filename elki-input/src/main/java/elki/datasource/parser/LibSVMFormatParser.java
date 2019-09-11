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

import java.util.regex.Pattern;

import elki.data.LabelList;
import elki.data.SparseFloatVector;
import elki.data.SparseNumberVector;
import elki.logging.Logging;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Parser to read libSVM format files.
 * <p>
 * The format of libSVM is roughly specified in the README given:
 * 
 * <pre>
 * &lt;label&gt; &lt;index1&gt;:&lt;value1&gt; &lt;index2&gt;:&lt;value2&gt; ...
 * </pre>
 * 
 * i.e. a mandatory integer class label in the beginning followed by a classic
 * sparse vector representation of the data. indexes are integers, starting at 1
 * (Note that ELKI uses 0-based indexing, so we will map these to index-1) to
 * not always have a constant-0 dimension 0.
 * <p>
 * The libSVM FAQ states that you can also put comments into the file, separated
 * by a hash: <tt>#</tt>, but they must not contain colons and are not
 * officially supported.<br>
 * ELKI will simply stop parsing a line when encountering a <tt>#</tt>.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <V> Vector type
 */
@Title("libSVM Format Parser")
public class LibSVMFormatParser<V extends SparseNumberVector> extends SparseNumberVectorLabelParser<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(LibSVMFormatParser.class);

  /**
   * LibSVM uses whitespace and colons for separation.
   */
  public static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+|:)");

  /**
   * Comment pattern.
   */
  public static final Pattern COMMENT_PATTERN = Pattern.compile("#");

  /**
   * Constructor.
   * 
   * @param factory Vector factory
   */
  public LibSVMFormatParser(SparseNumberVector.Factory<V> factory) {
    super(WHITESPACE_PATTERN, null, COMMENT_PATTERN, null, factory);
  }

  @Override
  protected boolean parseLineInternal() {
    /* tokenizer initialized by nextLineExceptComments() */
    int thismax = 0;

    // TODO: rely on the string being numeric for performance
    // But it might be missing sometimes, or "?"
    labels.add(tokenizer.getSubstring());
    tokenizer.advance();
    haslabels = true; // libSVM always has labels.

    while(tokenizer.valid()) {
      try {
        int index = tokenizer.getIntBase10();
        tokenizer.advance();
        double attribute = tokenizer.getDouble();
        tokenizer.advance();
        thismax = Math.max(thismax, index + 1);
        values.put(index, attribute);
      }
      catch(NumberFormatException e) {
        String comment = tokenizer.getSubstring();
        if(comment.charAt(0) == '#') {
          break;
        }
        throw new RuntimeException("Parsing error in line " + reader.getLineNumber() + ": expected data, got " + comment);
      }
    }
    curvec = sparsefactory.newNumberVector(values, thismax);
    curlbl = LabelList.make(labels);
    values.clear();
    labels.clear();
    return true;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<V extends SparseNumberVector> extends NumberVectorLabelParser.Par<V> {
    @Override
    protected void getFactory(Parameterization config) {
      new ObjectParameter<SparseNumberVector.Factory<V>>(VECTOR_TYPE_ID, SparseNumberVector.Factory.class, SparseFloatVector.Factory.class) //
          .grab(config, x -> factory = x);
    }

    @Override
    public void configure(Parameterization config) {
      // Avoid additional options: super.configure(config);
      getFactory(config);
    }

    @Override
    public LibSVMFormatParser<V> make() {
      return new LibSVMFormatParser<>((SparseNumberVector.Factory<V>) factory);
    }
  }
}
