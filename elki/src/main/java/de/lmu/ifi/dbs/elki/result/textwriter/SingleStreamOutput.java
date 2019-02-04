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

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.zip.GZIPOutputStream;

/**
 * Class to output all result data to a single stream (e.g. Stdout, single file)
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public class SingleStreamOutput implements StreamFactory {
  /**
   * Output stream
   */
  private PrintStream stream;

  /**
   * Constructor using stdout.
   * 
   * @throws IOException on IO error
   */
  public SingleStreamOutput() throws IOException {
    this(System.out);
  }

  /**
   * Constructor using stdout
   * 
   * @param gzip Use gzip compression
   * @throws IOException on IO error
   */
  public SingleStreamOutput(boolean gzip) throws IOException {
    this(FileDescriptor.out, gzip);
  }

  /**
   * Constructor with given file name.
   * 
   * @param out filename
   * @throws IOException on IO error
   */
  public SingleStreamOutput(File out) throws IOException {
    this(new FileOutputStream(out));
  }

  /**
   * Constructor with given file name.
   * 
   * @param out filename
   * @param gzip Use gzip compression
   * @throws IOException on IO error
   */
  public SingleStreamOutput(File out, boolean gzip) throws IOException {
    this(new FileOutputStream(out), gzip);
  }

  /**
   * Constructor with given FileDescriptor
   * 
   * @param out file descriptor
   * @throws IOException on IO error
   */
  public SingleStreamOutput(FileDescriptor out) throws IOException {
    this(new FileOutputStream(out));
  }

  /**
   * Constructor with given FileDescriptor
   * 
   * @param out file descriptor
   * @param gzip Use gzip compression
   * @throws IOException on IO error
   */
  public SingleStreamOutput(FileDescriptor out, boolean gzip) throws IOException {
    this(new FileOutputStream(out), gzip);
  }

  /**
   * Constructor with given FileOutputStream.
   * 
   * @param out File output stream
   * @throws IOException on IO error
   */
  public SingleStreamOutput(FileOutputStream out) throws IOException {
    this(out, false);
  }

  /**
   * Constructor with given FileOutputStream.
   * 
   * @param out File output stream
   * @param gzip Use gzip compression
   * @throws IOException on IO error
   */
  public SingleStreamOutput(FileOutputStream out, boolean gzip) throws IOException {
    OutputStream os = out;
    if(gzip) {
      // wrap into gzip stream.
      os = new GZIPOutputStream(os);
    }
    this.stream = new PrintStream(os);
  }

  /**
   * Constructor with a print stream.
   *
   * @param os Output stream
   */
  public SingleStreamOutput(PrintStream os) {
    this.stream = os;
  }

  /**
   * Return the objects shared print stream.
   * 
   * @param filename ignored filename for SingleStreamOutput, as the name
   *        suggests
   */
  @Override
  public PrintStream openStream(String filename) {
    return stream;
  }

  @Override
  public void closeStream(PrintStream stream) {
    // Do NOT close. We may still need it.
  }

  @Override
  public void close() throws IOException {
    if(stream == System.out) {
      stream.flush();
      return;
    }
    stream.close();
  }
}
