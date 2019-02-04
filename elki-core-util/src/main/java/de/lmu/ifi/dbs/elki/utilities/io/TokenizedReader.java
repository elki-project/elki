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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reader that will tokenize the input data as desired.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @composed - - - Tokenizer
 */
public class TokenizedReader extends BufferedLineReader {
  /**
   * Comment pattern.
   */
  private Matcher comment = null;

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
  public TokenizedReader(Pattern colSep, String quoteChars, Pattern comment) {
    super();
    this.tokenizer = new Tokenizer(colSep, quoteChars);
    this.comment = comment.matcher("");
  }

  /**
   * Read the next line into the tokenizer.
   * 
   * @return The next line, or {@code null}.
   */
  public boolean nextLineExceptComments() throws IOException {
    while(nextLine()) {
      if(comment == null || !comment.reset(buf).matches()) {
        tokenizer.initialize(buf, 0, buf.length());
        return true;
      }
    }
    return false;
  }

  /**
   * Cleanup the internal state of the tokenized reader.
   * 
   * This also closes the input stream, but the TokenizerReader can still be
   * applied to a new stream using any of the {@link #reset} methods.
   */
  @Override
  public void reset() {
    super.reset();
    if(comment != null) {
      comment.reset("");
    }
    tokenizer.cleanup();
  }

  @Override
  public void close() throws IOException {
    reset();
    super.close();
  }

  /**
   * Get the tokenizer of the reader.
   * 
   * @return Tokenizer
   */
  public Tokenizer getTokenizer() {
    return tokenizer;
  }
}