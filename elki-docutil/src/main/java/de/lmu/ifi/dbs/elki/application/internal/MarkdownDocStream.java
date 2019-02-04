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
package de.lmu.ifi.dbs.elki.application.internal;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Helper to write documentation to a Markdown file.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class MarkdownDocStream implements AutoCloseable {
  /**
   * Output stream.
   */
  private PrintStream out;

  /**
   * Indentation depth
   */
  private int indent = 0;

  /**
   * Newline state
   */
  private Newline newline = Newline.NONE;

  /**
   * Possible newline states
   */
  private enum Newline {
    NONE, BREAK, NEWLINE, PAR
  }

  /**
   * Constructor
   * 
   * @param of Output stream
   * @throws IOException On buffer open errors
   */
  public MarkdownDocStream(FileOutputStream of) throws IOException {
    this.out = new PrintStream(new BufferedOutputStream(of), false, "UTF-8"); //
    this.newline = Newline.NONE;
  }

  /**
   * Must be length 8 for the code below!
   */
  private static final String WHITESPACES = "        ";

  /**
   * Output any pending line breaks.
   *
   * @return {@code this}
   */
  private MarkdownDocStream pendingBreak() {
    if(newline == Newline.NONE) {
      return this;
    }
    out.append(newline == Newline.BREAK ? "\\\n" : newline == Newline.PAR ? "\n\n" : "\n");
    for(int i = indent, j = i; i > 0; i -= j) {
      out.append(WHITESPACES, 0, (j = i > WHITESPACES.length() ? WHITESPACES.length() : i));
    }
    newline = Newline.NONE;
    return this;
  }

  /**
   * Append a single character.
   *
   * @param c Char
   * @return {@code this}
   */
  public MarkdownDocStream append(char c) {
    if(c == '\n') {
      newline = newline == Newline.NONE ? Newline.NEWLINE : Newline.PAR;
      return this;
    }
    pendingBreak();
    out.append(c);
    return this;
  }

  /**
   * Output a string.
   *
   * @param p String
   * @return {@code this}
   */
  public MarkdownDocStream append(CharSequence p) {
    return append(p, 0, p.length());
  }

  /**
   * Output part of a string.
   *
   * @param p String
   * @param start Begin
   * @param end End
   * @return {@code this}
   */
  public MarkdownDocStream append(CharSequence p, int start, int end) {
    for(int pos = start; pos < end; ++pos) {
      final char c = p.charAt(pos);
      if(c == '\r') {
        continue;
      }
      append(c); // Uses \n magic.
    }
    return this;
  }

  /**
   * Output a string, escaped.
   *
   * @param p String
   * @return {@code this}
   */
  public MarkdownDocStream escaped(CharSequence p) {
    return escaped(p, 0, p.length());
  }

  /**
   * Output part of a string, escaped.
   *
   * @param p String
   * @param start Begin
   * @param end End
   * @return {@code this}
   */
  public MarkdownDocStream escaped(CharSequence p, int start, int end) {
    for(int pos = start; pos < end; ++pos) {
      final char c = p.charAt(pos);
      if(c == '\r') {
        continue;
      }
      if(c == '*' || c == '<' || c == '>' || c == '\\') {
        append('\\');
      }
      append(c); // Uses \n magic.
    }
    return this;
  }

  /**
   * Line feed (explicit break)
   *
   * @return {@code this}
   */
  public MarkdownDocStream lf() {
    newline = Newline.BREAK;
    return this;
  }

  /**
   * Newline (may or may not be a break)
   *
   * @return {@code this}
   */
  public MarkdownDocStream nl() {
    newline = Newline.NEWLINE;
    return this;
  }

  /**
   * Paragraph (large break).
   *
   * @return {@code this}
   */
  public MarkdownDocStream par() {
    newline = Newline.PAR;
    return this;
  }

  /**
   * Set the indent depth.
   *
   * @param newindent Indent depth
   * @return {@code this}
   */
  public MarkdownDocStream indent(int newindent) {
    if(newindent < indent) {
      newline = newline == Newline.BREAK ? Newline.NEWLINE : Newline.PAR;
    }
    indent = newindent;
    return this;
  }

  @Override
  public void close() {
    out.close();
  }
}