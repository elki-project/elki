package de.lmu.ifi.dbs.elki.result.textwriter;

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

import java.io.PrintStream;

import de.lmu.ifi.dbs.elki.utilities.HandlerList;

/**
 * Representation of an output stream to a text file.
 * 
 * @author Erich Schubert
 *
 * @apiviz.uses de.lmu.ifi.dbs.elki.result.textwriter.StreamFactory oneway - - wraps
 */
public class TextWriterStream {
  /**
   * Actual stream to write to.
   */
  private PrintStream outStream;

  /**
   * Buffer for inline data to output.
   */
  private StringBuffer inline;

  /**
   * Buffer for comment data to output.
   */
  private StringBuffer comment;

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
   * Comment separator line.
   * Since this will be printed without {@link #QUOTE} infront, it should be quoted string itself. 
   */
  public static final String COMMENTSEP = "###############################################################";

  /**
   * System newline character(s)
   */
  private final static String NEWLINE = System.getProperty("line.separator");

  /**
   * Marker used in text serialization (and re-parsing)
   */
  public final static String SER_MARKER = "Serialization class:";
  
  /**
   * Force incomments flag
   */
  // TODO: solve this more gracefully
  private boolean forceincomments = false; 

  /**
   * Constructor.
   * 
   * @param out Actual stream to write to
   * @param writers Handlers for various data types
   */
  public TextWriterStream(PrintStream out, HandlerList<TextWriterWriterInterface<?>> writers) {
    this.outStream = out;
    this.writers = writers;
    inline = new StringBuffer();
    comment = new StringBuffer();
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
   * Print an object into the comments section with trailing newline.
   * 
   * @param line object to print into comments
   */
  public void commentPrintLn(Object line) {
    comment.append(line);
    comment.append(NEWLINE);
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
    comment.append(COMMENTSEP + NEWLINE);
  }

  /**
   * Print data into the inline part of the file.
   * Data is sanitized: newlines are replaced with spaces, and text
   * containing separators is put in quotes. Quotes and escape characters
   * are escaped.
   * 
   * @param o object to print
   */
  public void inlinePrint(Object o) {
    if (forceincomments) {
      commentPrint(o);
      return;
    }
    if (inline.length() > 0) {
      inline.append(SEPARATOR);
    }
    // remove newlines
    String str = o.toString().replace(NEWLINE," ");
    // escaping
    str = str.replace("\\","\\\\").replace("\"","\\\"");
    // when needed, add quotes.
    if (str.contains(SEPARATOR)) {
      str = "\""+str+"\"";
    }
    inline.append(str);
  }

  /**
   * Print data into the inline part of the file WITHOUT checking for
   * separators (and thus quoting).
   * 
   * @param o object to print.
   */
  public void inlinePrintNoQuotes(Object o) {
    if (forceincomments) {
      commentPrint(o);
      return;
    }
    if (inline.length() > 0) {
      inline.append(SEPARATOR);
    }
    // remove newlines
    String str = o.toString().replace(NEWLINE," ");
    // escaping
    str = str.replace("\\","\\\\").replace("\"","\\\"");
    inline.append(str);
  }

  /**
   * Flush output:
   * write inline data, then write comment section. Reset streams.
   */
  public void flush() {
    if (inline.length() > 0) {
      outStream.println(inline);
    }
    inline.setLength(0);
    if (comment.length() > 0) {
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
    String[] lines = data.split("\r\n|\r|\n");
    for (String line : lines) {
      if (line.equals(COMMENTSEP)) {
        outStream.println(COMMENTSEP);
      } else {
        outStream.println(QUOTE + line);
      }
    }
  }

  /**
   * Retrieve an appropriate writer from the handler list.
   * 
   * @param o query object
   * @return appropriate write, if available
   */
  public TextWriterWriterInterface<?> getWriterFor(Object o) {
    return writers.getHandler(o);
  }

  /**
   * Restore a vector undoing any normalization that was applied.
   * (This class does not support normalization, it is only provided
   * by derived classes, which will then have to use generics.)
   * 
   * @param <O> Object class
   * @param v vector to restore
   * @return restored value.
   */
  public <O> O normalizationRestore(O v) {
    return v;
  }

  /**
   * Test force-in-comments flag.
   * 
   * @return flag value
   */
  protected boolean isForceincomments() {
    return forceincomments;
  }

  /**
   * Set the force-in-comments flag.
   * 
   * @param forceincomments the new flag value
   */
  protected void setForceincomments(boolean forceincomments) {
    this.forceincomments = forceincomments;
  }
}
