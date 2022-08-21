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

import elki.logging.Logging;

/**
 * Manage output to multiple files.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public class MultipleFilesOutput implements StreamFactory {
  /**
   * File name extension.
   */
  private static final String EXTENSION = ".txt";

  /**
   * GZip extra file extension
   */
  private static final String GZIP_EXTENSION = ".gz";

  /**
   * Base file name.
   */
  private Path basename;

  /**
   * Control gzip compression of output.
   */
  private boolean usegzip = false;

  /**
   * Logger for debugging.
   */
  private static final Logging LOG = Logging.getLogger(MultipleFilesOutput.class);

  /**
   * Constructor
   * 
   * @param base Base file name (folder name)
   */
  public MultipleFilesOutput(Path base) {
    this(base, false);
  }

  /**
   * Constructor
   * 
   * @param base Base file name (folder name)
   * @param gzip Use gzip compression.
   */
  public MultipleFilesOutput(Path base, boolean gzip) {
    this.basename = base;
    this.usegzip = gzip;
  }

  /**
   * Open a new stream of the given name
   * 
   * @param name file name (which will be appended to the base name)
   * @return stream object for the given name
   * @throws IOException
   */
  private PrintStream newStream(String name) throws IOException {
    if(LOG.isDebuggingFiner()) {
      LOG.debugFiner("Requested stream: " + name);
    }
    // Ensure the directory exists:
    Files.createDirectories(basename);
    Path fn = basename.resolve(name + (usegzip ? GZIP_EXTENSION : EXTENSION));
    OutputStream os = Files.newOutputStream(fn);
    // Both PrintStream and GZIPOutputStream call close()
    PrintStream res = new PrintStream(usegzip ? new GZIPOutputStream(os) : os);
    if(LOG.isDebuggingFiner()) {
      LOG.debugFiner("Opened new output stream:" + fn);
    }
    // cache.
    return res;
  }

  /**
   * Retrieve the output stream for the given file name.
   */
  @Override
  public PrintStream openStream(String filename) throws IOException {
    return newStream(filename);
  }

  @Override
  public void closeStream(PrintStream stream) {
    stream.close();
  }

  @Override
  public void close() throws IOException {
    // TODO: should we keep track of all streams, and force close them?
  }
}
