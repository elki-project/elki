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
package de.lmu.ifi.dbs.elki.result.textwriter;

import java.io.PrintStream;

import de.lmu.ifi.dbs.elki.utilities.HandlerList;

/**
 * Representation of an output stream to a text file.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @assoc - - - de.lmu.ifi.dbs.elki.result.textwriter.StreamFactory
 */
public class TextWriterStream {
  /**
   * Actual stream to write to.
   */
  private PrintStream outStream;

  /**
   * Buffer for inline data to output.
   */
  private StringBuilder inline;

  /**
   * Buffer for comment data to output.
   */
  private StringBuilder comment;

  /**
   * Handlers for various object types.
   */
  private HandlerList<TextWriterWriterInterface<?>> writers;

  /**
   * String to separate different entries while printing.
   */
  public static final String SEPARATOR = " ";

  /**
   * String to separate different entries while printing.
   */
  public static final String QUOTE = "# ";

  /**
   * Comment separator line. Since this will be printed without {@link #QUOTE}
   * infront, it should be quoted string itself.
   */
  public static final String COMMENTSEP = "###############################################################";

  /**
   * System newline character(s)
   */
  private static final String NEWLINE = System.getProperty("line.separator");

  /**
   * Marker used in text serialization (and re-parsing)
   */
  public static final String SER_MARKER = "Serialization class:";

  /**
   * Fallback writer, using toString.
   */
  private TextWriterWriterInterface<?> fallbackwriter;

  /**
   * Constructor.
   * 
   * @param out Actual stream to write to
   * @param writers Handlers for various data types
   * @param fallback Fallback writer
   */
  public TextWriterStream(PrintStream out, HandlerList<TextWriterWriterInterface<?>> writers, TextWriterWriterInterface<?> fallback) {
    this.outStream = out;
    this.writers = writers;
    this.fallbackwriter = fallback;
    inline = new StringBuilder();
    comment = new StringBuilder();
  }

  /**
   * Print an object into the comments section
   * 
   * @param line object to print into commments
   */
  public void commentPrint(Object line) {
    comment.append(line);
  }

  /**
   * Print an object into the comments section
   * 
   * @param line object to print into commments
   */
  public void commentPrint(CharSequence line) {
    comment.append(line);
  }

  /**
   * Print an object into the comments section with trailing newline.
   * 
   * @param line object to print into comments
   */
  public void commentPrintLn(CharSequence line) {
    comment.append(line).append(NEWLINE);
  }

  /**
   * Print an object into the comments section with trailing newline.
   * 
   * @param line object to print into comments
   */
  public void commentPrintLn(Object line) {
    comment.append(line).append(NEWLINE);
  }

  /**
   * Print a newline into the comments section.
   */
  public void commentPrintLn() {
    comment.append(NEWLINE);
  }

  /**
   * Print a separator line in the comments section.
   */
  public void commentPrintSeparator() {
    comment.append(COMMENTSEP).append(NEWLINE);
  }

  /**
   * Print data into the inline part of the file. Data is sanitized: newlines
   * are replaced with spaces, and text containing separators is put in quotes.
   * Quotes and escape characters are escaped.
   * 
   * @param o object to print
   */
  public void inlinePrint(Object o) {
    if(inline.length() > 0) {
      inline.append(SEPARATOR);
    }
    // remove newlines
    String str = o.toString().replace(NEWLINE, " ");
    // escaping
    str = str.replace("\\", "\\\\").replace("\"", "\\\"");
    // when needed, add quotes.
    if(str.contains(SEPARATOR)) {
      inline.append('"').append(str).append('"');
    }
    else {
      inline.append(str);
    }
  }

  /**
   * Print data into the inline part of the file WITHOUT checking for separators
   * (and thus quoting).
   * 
   * @param o object to print.
   */
  public void inlinePrintNoQuotes(Object o) {
    if(inline.length() > 0) {
      inline.append(SEPARATOR);
    }
    // remove newlines
    String str = o.toString().replace(NEWLINE, " ");
    // escaping
    str = str.replace("\\", "\\\\").replace("\"", "\\\"");
    inline.append(str);
  }

  /**
   * Flush output: write inline data, then write comment section. Reset streams.
   */
  public void flush() {
    if(inline.length() > 0) {
      outStream.println(inline);
    }
    inline.setLength(0);
    if(comment.length() > 0) {
      quotePrintln(outStream, comment.toString());
    }
    comment.setLength(0);
  }

  /**
   * Quoted println. All lines written will be prefixed with {@link #QUOTE}
   * 
   * @param outStream output stream to write to
   * @param data data to print
   */
  private void quotePrintln(PrintStream outStream, String data) {
    String[] lines = data.split("\n");
    for(String line : lines) {
      if(!line.equals(COMMENTSEP)) {
        outStream.append(QUOTE);
      }
      outStream.append(line).append(NEWLINE);
    }
  }

  /**
   * Retrieve an appropriate writer from the handler list.
   * 
   * @param o query object
   * @return appropriate write, if available
   */
  public TextWriterWriterInterface<?> getWriterFor(Object o) {
    if(o == null) {
      return null;
    }
    TextWriterWriterInterface<?> writer = writers.getHandler(o);
    if(writer != null) {
      return writer;
    }
    try {
      final Class<?> decl = o.getClass().getMethod("toString").getDeclaringClass();
      if(decl == Object.class) {
        return null; // TODO: cache this, too
      }
      writers.insertHandler(decl, fallbackwriter);
      return fallbackwriter;
    }
    catch(NoSuchMethodException | SecurityException e) {
      return null;
    }
  }
}
