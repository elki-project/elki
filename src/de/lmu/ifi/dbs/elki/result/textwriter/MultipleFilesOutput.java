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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Manage output to multiple files.
 * 
 * @author Erich Schubert
 */
public class MultipleFilesOutput implements StreamFactory {
  /**
   * File name extension.
   */
  private final static String EXTENSION = ".txt";

  /**
   * GZip extra file extension
   */
  private final static String GZIP_EXTENSION = ".gz";

  /**
   * Default stream to write to when no name is supplied.
   */
  private PrintStream defaultStream = null;

  /**
   * Base file name.
   */
  private File basename;

  /**
   * HashMap of open print streams.
   */
  private HashMap<String, PrintStream> map = new HashMap<String, PrintStream>();

  /**
   * Control gzip compression of output.
   */
  private boolean usegzip = false;
  
  /**
   * Logger for debugging.
   */
  private final static Logging logger = Logging.getLogger(MultipleFilesOutput.class);

  /**
   * Constructor
   * 
   * @param base Base file name (folder name)
   */
  public MultipleFilesOutput(File base) {
    this(base, false);
  }

  /**
   * Constructor
   * 
   * @param base Base file name (folder name)
   * @param gzip Use gzip compression.
   */
  public MultipleFilesOutput(File base, boolean gzip) {
    this.basename = base;
    this.usegzip = gzip;
  }

  /**
   * Retrieve/open the default output stream.
   * 
   * @return default output stream
   * @throws IOException
   */
  private PrintStream getDefaultStream() throws IOException {
    if(defaultStream != null) {
      return defaultStream;
    }
    defaultStream = newStream("default");
    return defaultStream;
  }

  /**
   * Open a new stream of the given name
   * 
   * @param name file name (which will be appended to the base name)
   * @return stream object for the given name
   * @throws IOException
   */
  private PrintStream newStream(String name) throws IOException {
    if (logger.isDebuggingFiner()) {
      logger.debugFiner("Requested stream: "+name);
    }
    PrintStream res = map.get(name);
    if(res != null) {
      return res;
    }
    // Ensure the directory exists:
    if(!basename.exists()) {
      basename.mkdirs();
    }
    String fn = basename.getAbsolutePath() + File.separator + name + EXTENSION;
    if (usegzip) {
      fn = fn + GZIP_EXTENSION;
    }
    File n = new File(fn);
    OutputStream os = new FileOutputStream(n);
    if (usegzip) {
      // wrap into gzip stream.
      os = new GZIPOutputStream(os);
    }
    res = new PrintStream(os);
    if (logger.isDebuggingFiner()) {
      logger.debugFiner("Opened new output stream:"+fn);
    }
    // cache.
    map.put(name, res);
    return res;
  }

  /**
   * Retrieve the output stream for the given file name.
   */
  @Override
  public PrintStream openStream(String filename) throws IOException {
    if(filename == null) {
      return getDefaultStream();
    }
    return newStream(filename);
  }

  /**
   * Get GZIP compression flag.
   * 
   * @return if GZIP compression is enabled
   */
  protected boolean isGzipCompression() {
    return usegzip;
  }

  /**
   * Set GZIP compression flag.
   * 
   * @param usegzip use GZIP compression
   */
  protected void setGzipCompression(boolean usegzip) {
    this.usegzip = usegzip;
  }

  /**
   * Close (and forget) all output streams.
   */
  @Override
  public synchronized void closeAllStreams() {
    for (PrintStream s : map.values()) {
      s.close();
    }
    map.clear();
  }
}
