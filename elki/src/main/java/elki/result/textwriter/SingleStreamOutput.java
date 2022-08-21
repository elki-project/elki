/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.result.textwriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

/**
 * Class to output all result data to a single stream (e.g., Stdout, single
 * file)
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
   * Constructor with given file name.
   *
   * @param out filename
   * @throws IOException on IO error
   */
  public SingleStreamOutput(Path out) throws IOException {
    this(Files.newOutputStream(out));
  }

  /**
   * Constructor with given file name.
   *
   * @param out filename
   * @param gzip Use gzip compression
   * @throws IOException on IO error
   */
  public SingleStreamOutput(Path out, boolean gzip) throws IOException {
    this(Files.newOutputStream(out), gzip);
  }

  /**
   * Constructor with given FileOutputStream.
   *
   * @param out File output stream
   * @throws IOException on IO error
   */
  public SingleStreamOutput(OutputStream out) throws IOException {
    this(out, false);
  }

  /**
   * Constructor with given FileOutputStream.
   *
   * @param out File output stream
   * @param gzip Use gzip compression
   * @throws IOException on IO error
   */
  public SingleStreamOutput(OutputStream out, boolean gzip) throws IOException {
    this.stream = new PrintStream(gzip ? new GZIPOutputStream(out) : out);
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
