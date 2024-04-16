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
package elki.datasource;

import java.io.IOException;
import java.net.URI;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import elki.data.*;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.datasource.filter.ObjectFilter;
import elki.logging.Logging;
import elki.logging.statistics.Duration;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Class to import Numpy arrays (<tt>.npy</tt> files) into ELKI.
 * <p>
 * This currently only supports C order only (no Fortran order), and the data
 * types <tt>f4</tt> (float), <tt>f8</tt> (double), <tt>i4</tt> (signed int),
 * <tt>i8</tt> (signed long), <tt>U</tt> (unicode string).
 * <p>
 * Both endianesses should be supported. But since Java does not have unsigned
 * primitives, we currently do not support these. It would be possible to
 * up-cast them to the next larger signed type, for example.
 * <p>
 * For further information, please see the
 * <a href="https://numpy.org/doc/stable/reference/arrays.interface.html">Numpy
 * arrays interface documentation</a>.
 * <p>
 * Object labels can be loaded from a second file.
 * 
 * @author Andreas Lang
 * @author Erich Schubert
 */
public class NumpyDatabaseConnection extends AbstractDatabaseConnection {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(NumpyDatabaseConnection.class);

  /**
   * Vector input file.
   */
  URI infile;

  /**
   * Labels file name.
   */
  URI labelfile;

  /**
   * Constructor.
   * 
   * @param filters Filters to use
   */
  public NumpyDatabaseConnection(URI infile, URI labelfile, List<? extends ObjectFilter> filters) {
    super(filters);
    this.infile = infile;
    this.labelfile = labelfile;
  }

  /**
   * Read a numpy file.
   *
   * @param file File channel
   * @param bundle Bundle
   * @throws IOException on IO error
   */
  private static void readNumpy(FileChannel file, MultipleObjectsBundle bundle) throws IOException {
    // Verify the numpy magic and header size
    MappedByteBuffer buffer = file.map(MapMode.READ_ONLY, 0, 12);
    if(buffer.get() != (byte) 0x93 || buffer.get() != 'N' || buffer.get() != 'U' //
        || buffer.get() != 'M' || buffer.get() != 'P' || buffer.get() != 'Y') {
      throw new IOException("Invalid numpy magic string");
    }
    byte major = buffer.get(), minor = buffer.get();
    if(major <= 0 || major > 3 || minor != 0) {
      throw new IOException("Only numpy array version 1.0 to 3.0 are supported.");
    }
    int len = (buffer.get() & 0xff) + ((buffer.get() & 0xff) << 8);
    if(major >= 2) { // Larger headers
      len += ((buffer.get() & 0xff) << 16) + ((buffer.get() & 0xff) << 24);
    }
    if(len < 0) {
      throw new IOException("Maximum header size 2^31.");
    }
    long header_start = buffer.position(); // 10 for v1, 12 for v2
    /// Map and process the header:
    CharBuffer headerbuf = Charset.forName(major < 3 ? "ISO8859-1" : "UTF-8") //
        .decode(file.map(MapMode.READ_ONLY, header_start, len));
    Map<String, String> header = parseHeader(headerbuf);
    String dtype = header.get("descr");
    if(dtype == null) {
      throw new IOException("No data type found in the header.");
    }
    if(header.get("fortran_order") == null) {
      throw new IOException("No information on matrix order found in the header.");
    }
    if(Boolean.parseBoolean(header.get("fortran_order"))) {
      throw new IOException("Fortran order is currently not supported.");
    }
    String shape = header.get("shape");
    if(shape == null) {
      throw new IOException("No shape found in the header.");
    }
    String[] row_col = shape.substring(1, shape.length() - 1).replace(" ", "").split(",");
    if(row_col.length > 2) {
      throw new IOException("Only one- and two-dimensional arrays are supported.");
    }
    int rows = Integer.parseInt(row_col[0]);
    int cols = row_col.length == 2 ? Integer.parseInt(row_col[1]) : 1;
    boolean littleEndian = dtype.startsWith("<");
    final long start = header_start + len;
    // Load in data mode:
    if(dtype.endsWith("f4")) {
      List<FloatVector> vectors = new ArrayList<>(rows);
      loadFloats(rows, cols, file, start, littleEndian, buf -> {
        float[] f = new float[cols];
        buf.get(f, 0, cols);
        vectors.add(FloatVector.wrap(f));
      });
      bundle.appendColumn(new VectorFieldTypeInformation<>(FloatVector.FACTORY, cols), vectors);
    }
    else if(dtype.endsWith("f8")) {
      List<DoubleVector> vectors = new ArrayList<>(rows);
      loadDoubles(rows, cols, file, start, littleEndian, buf -> {
        double[] f = new double[cols];
        buf.get(f, 0, cols);
        vectors.add(DoubleVector.wrap(f));
      });
      bundle.appendColumn(new VectorFieldTypeInformation<>(DoubleVector.FACTORY, cols), vectors);
    }
    else if(dtype.endsWith("i1")) {
      List<ByteVector> vectors = new ArrayList<>(rows);
      loadBytes(rows, cols, file, start, buf -> {
        byte[] f = new byte[cols];
        buf.get(f, 0, cols);
        vectors.add(ByteVector.wrap(f));
      });
      bundle.appendColumn(new VectorFieldTypeInformation<>(ByteVector.FACTORY, cols), vectors);
    }
    else if(dtype.endsWith("i2")) {
      List<ShortVector> vectors = new ArrayList<>(rows);
      loadShorts(rows, cols, file, start, littleEndian, buf -> {
        short[] f = new short[cols];
        buf.get(f, 0, cols);
        vectors.add(ShortVector.wrap(f));
      });
      bundle.appendColumn(new VectorFieldTypeInformation<>(ShortVector.FACTORY, cols), vectors);
    }
    else if(dtype.endsWith("i4")) {
      List<IntegerVector> vectors = new ArrayList<>(rows);
      loadIntegers(rows, cols, file, start, littleEndian, buf -> {
        int[] f = new int[cols];
        buf.get(f, 0, cols);
        vectors.add(IntegerVector.wrap(f));
      });
      bundle.appendColumn(new VectorFieldTypeInformation<>(IntegerVector.FACTORY, cols), vectors);
    }
    else if(dtype.endsWith("i8")) {
      List<LongVector> vectors = new ArrayList<>(rows);
      loadLongs(rows, cols, file, start, littleEndian, buf -> {
        long[] f = new long[cols];
        buf.get(f, 0, cols);
        vectors.add(LongVector.wrap(f));
      });
      bundle.appendColumn(new VectorFieldTypeInformation<>(LongVector.FACTORY, cols), vectors);
    }
    else if(dtype.contains("U")) {
      final int size = Integer.parseInt(dtype.split("U")[1]);
      ArrayList<ArrayList<String>> columns = new ArrayList<>(cols);
      for(int i = 0; i < cols; i++) {
        columns.add(new ArrayList<>(rows));
      }
      char[] char_data = new char[size];
      loadStrings(rows, cols, size, file, start, littleEndian, buf -> {
        for(int c = 0; c < cols; c++) {
          buf.get(char_data, 0, size);
          int nonzero = 0;
          while(nonzero < size && char_data[nonzero] != 0) {
            nonzero++;
          }
          columns.get(c).add(new String(char_data, 0, nonzero));
        }
      });
      for(int i = 0; i < cols; i++) {
        bundle.appendColumn(TypeUtil.STRING, columns.get(i));
      }
    }
    else {
      throw new IOException("Unsupported dtype " + dtype);
    }
  }

  /**
   * Read a numpy file.
   *
   * @param file File channel
   * @param bundle Bundle
   * @throws IOException on IO error
   */
  private static void readNumpyLabels(FileChannel file, MultipleObjectsBundle bundle) throws IOException {
    // Verify the numpy magic and header size
    MappedByteBuffer buffer = file.map(MapMode.READ_ONLY, 0, 12);
    if(buffer.get() != (byte) 0x93 || buffer.get() != 'N' || buffer.get() != 'U' //
        || buffer.get() != 'M' || buffer.get() != 'P' || buffer.get() != 'Y') {
      throw new IOException("Invalid numpy magic string");
    }
    byte major = buffer.get(), minor = buffer.get();
    if(major <= 0 || major > 3 || minor != 0) {
      throw new IOException("Only numpy array version 1.0 to 3.0 are supported.");
    }
    int len = (buffer.get() & 0xff) + ((buffer.get() & 0xff) << 8);
    if(major >= 2) { // Larger headers
      len += ((buffer.get() & 0xff) << 16) + ((buffer.get() & 0xff) << 24);
    }
    if(len < 0) {
      throw new IOException("Maximum header size 2^31.");
    }
    long header_start = buffer.position(); // 10 for v1, 12 for v2
    /// Map and process the header:
    CharBuffer headerbuf = Charset.forName(major < 3 ? "ISO8859-1" : "UTF-8") //
        .decode(file.map(MapMode.READ_ONLY, header_start, len));
    Map<String, String> header = parseHeader(headerbuf);
    String dtype = header.get("descr");
    if(dtype == null) {
      throw new IOException("No data type found in the header.");
    }
    if(header.get("fortran_order") == null) {
      throw new IOException("No information on matrix order found in the header.");
    }
    if(Boolean.parseBoolean(header.get("fortran_order"))) {
      throw new IOException("Fortran order is currently not supported.");
    }
    String shape = header.get("shape");
    if(shape == null) {
      throw new IOException("No shape found in the header.");
    }
    String[] row_col = shape.substring(1, shape.length() - 1).replace(" ", "").split(",");
    if(row_col.length > 2) {
      throw new IOException("Only one- and two-dimensional arrays are supported.");
    }
    int rows = Integer.parseInt(row_col[0]);
    int cols = row_col.length == 2 ? Integer.parseInt(row_col[1]) : 1;
    if(bundle.dataLength() > 0 && bundle.dataLength() != rows) {
      throw new IOException("Expected " + bundle.dataLength() + " rows, but label file has " + rows + " rows.");
    }
    boolean littleEndian = dtype.startsWith("<");
    final long start = header_start + len;
    // Load and convert to labels:
    if(dtype.endsWith("f4")) {
      ArrayList<LabelList> labellist = new ArrayList<>();
      float[] f = new float[cols];
      loadFloats(rows, cols, file, start, littleEndian, buf -> {
        buf.get(f, 0, cols);
        labellist.add(LabelList.make(IntStream.range(0, f.length) //
            .mapToObj(i -> Float.toString(f[i])).collect(Collectors.toList())));
      });
      bundle.appendColumn(TypeUtil.LABELLIST, labellist);
      return;
    }
    else if(dtype.endsWith("f8")) {
      ArrayList<LabelList> labellist = new ArrayList<>();
      double[] f = new double[cols];
      loadDoubles(rows, cols, file, start, littleEndian, buf -> {
        buf.get(f, 0, cols);
        labellist.add(LabelList.make(IntStream.range(0, f.length) //
            .mapToObj(i -> Double.toString(f[i])).collect(Collectors.toList())));
      });
      bundle.appendColumn(TypeUtil.LABELLIST, labellist);
      return;
    }
    else if(dtype.endsWith("i1")) {
      ArrayList<LabelList> labellist = new ArrayList<>();
      byte[] f = new byte[cols];
      loadBytes(rows, cols, file, start, buf -> {
        buf.get(f, 0, cols);
        labellist.add(LabelList.make(IntStream.range(0, f.length) //
            .mapToObj(i -> Byte.toString(f[i])).collect(Collectors.toList())));
      });
      bundle.appendColumn(TypeUtil.LABELLIST, labellist);
      return;
    }
    else if(dtype.endsWith("i2")) {
      ArrayList<LabelList> labellist = new ArrayList<>();
      short[] f = new short[cols];
      loadShorts(rows, cols, file, start, littleEndian, buf -> {
        buf.get(f, 0, cols);
        labellist.add(LabelList.make(IntStream.range(0, f.length) //
            .mapToObj(i -> Short.toString(f[i])).collect(Collectors.toList())));
      });
      bundle.appendColumn(TypeUtil.LABELLIST, labellist);
      return;
    }
    else if(dtype.endsWith("i4")) {
      ArrayList<LabelList> labellist = new ArrayList<>();
      int[] f = new int[cols];
      loadIntegers(rows, cols, file, start, littleEndian, buf -> {
        buf.get(f, 0, cols);
        labellist.add(LabelList.make(IntStream.range(0, f.length) //
            .mapToObj(i -> Integer.toString(f[i])).collect(Collectors.toList())));
      });
      bundle.appendColumn(TypeUtil.LABELLIST, labellist);
      return;
    }
    else if(dtype.endsWith("i8")) {
      ArrayList<LabelList> labellist = new ArrayList<>();
      long[] f = new long[cols];
      loadLongs(rows, cols, file, start, littleEndian, buf -> {
        buf.get(f, 0, cols);
        labellist.add(LabelList.make(IntStream.range(0, f.length) //
            .mapToObj(i -> Long.toString(f[i])).collect(Collectors.toList())));
      });
      bundle.appendColumn(TypeUtil.LABELLIST, labellist);
      return;
    }
    else if(dtype.contains("U")) {
      final int size = Integer.parseInt(dtype.split("U")[1]);
      ArrayList<LabelList> labellist = new ArrayList<>();
      char[] char_data = new char[size];
      loadStrings(rows, cols, size, file, start, littleEndian, buf -> {
        List<String> row = new ArrayList<>(cols);
        for(int c = 0; c < cols; c++) {
          buf.get(char_data, 0, size);
          int nonzero = 0;
          while(nonzero < size && char_data[nonzero] != 0) {
            nonzero++;
          }
          row.add(new String(char_data, 0, nonzero));
        }
        labellist.add(LabelList.make(row));
      });
      bundle.appendColumn(TypeUtil.LABELLIST, labellist);
      return;
    }
    else {
      throw new IOException("Unsupported dtype " + dtype);
    }
  }

  /**
   * Parse the header dictionary.
   *
   * @param buf Header buffer
   * @return Map
   * @throws IOException
   */
  private static Map<String, String> parseHeader(CharBuffer buf) throws IOException {
    int start = 0, end = buf.limit();
    // Tolerate some whitespace
    start = consumeSpace(buf, start);
    if(start == end || buf.get(start) != '{') {
      throw new IOException("Expected dictionary");
    }
    while(end > start && Character.isWhitespace(buf.get(end - 1))) {
      end--;
    }
    if(start == end || buf.get(end - 1) != '}') {
      throw new IOException("Expected dictionary to be closed at " + (end - 1) + " " + buf.get(end - 1));
    }
    Map<String, String> map = new HashMap<>();
    start++;
    while(start < end) {
      char ch = buf.get(start);
      if(Character.isWhitespace(ch)) {
        start++;
        continue;
      }
      // New key, in quotes
      if(ch == '"' || ch == '\'') {
        int ks = start + 1, ke = ks + 1;
        char ch2;
        while(ke < end && (ch2 = buf.get(ke)) != ch) {
          ke += ch2 == '\\' ? 2 : 1;
        }
        if(ke >= end) {
          throw new IOException("Unclosed key in header?");
        }
        int pos = consumeSpace(buf, ke + 1);
        if(pos >= end || buf.get(pos) != ':') {
          throw new IOException("Expected separator at " + pos);
        }
        pos = consumeSpace(buf, pos + 1);
        // New value
        int vs = pos, ve = vs + 1, next = ve;
        ch = buf.get(vs);
        if(ch == '"' || ch == '\'') {
          vs++; // omit quote
          while(ve < end && (ch2 = buf.get(ve)) != ch) {
            ve += ch2 == '\\' ? 2 : 1;
          }
          next = ve + 1;
        }
        else if(ch == '(') {
          while(ve < end && (ch2 = buf.get(ve)) != ')') {
            ve += ch2 == '\\' ? 2 : 1;
          }
          ve++; // include closing bracket
          next = ve; // incremented already
        }
        else {
          while(ve < end && (ch2 = buf.get(ve)) != ',') {
            ve += 1;
          }
          next = ve; // the comma
        }
        if(next >= end) {
          throw new IOException("Unclosed value in header");
        }
        next = consumeSpace(buf, next);
        ch = buf.get(next);
        if(ch != ',' && ch != '}') {
          throw new IOException("Trailing characters in header at position " + next);
        }
        map.put(buf.subSequence(ks, ke).toString(), buf.subSequence(vs, ve).toString());
        start = next + 1;
      }
      else if(start == end - 1 && ch == '}') {
        break;
      }
      else {
        throw new IOException("Unexpected character: " + ch);
      }
    }
    return map;
  }

  /**
   * Consume space.
   * 
   * @param header Header
   * @param start Starting position
   * @return
   */
  private static int consumeSpace(CharBuffer header, int start) {
    while(start < header.limit() && Character.isWhitespace(header.get(start))) {
      start++;
    }
    return start;
  }

  /**
   * Read float data, using chunked memory maps.
   * 
   * @param rows Number of rows
   * @param cols Number of columns
   * @param file File to map
   * @param start Start offset
   * @param littleEndian Endianess
   * @param callback Callback, with a temporary buffer
   * @throws IOException on IO Errors
   */
  private static void loadFloats(int rows, int cols, FileChannel file, long start, boolean littleEndian, Consumer<FloatBuffer> callback) throws IOException {
    int columnSize = Float.BYTES * cols;
    while(rows > 0) {
      long read = Math.min(Integer.MAX_VALUE / columnSize, rows);
      ByteBuffer buffer = file.map(MapMode.READ_ONLY, start, read * columnSize);
      buffer.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
      FloatBuffer floatBuffer = buffer.asFloatBuffer();
      for(int j = 0; j < read; j++) {
        callback.accept(floatBuffer);
      }
      System.gc();
      start += read * columnSize;
      rows -= read;
    }
  }

  /**
   * Read double data, using chunked memory maps.
   * 
   * @param rows Number of rows
   * @param cols Number of columns
   * @param file File to map
   * @param start Start offset
   * @param littleEndian Endianess
   * @param callback Callback, with a temporary buffer
   * @throws IOException on IO Errors
   */
  private static void loadDoubles(int rows, int cols, FileChannel file, long start, boolean littleEndian, Consumer<DoubleBuffer> callback) throws IOException {
    int columnSize = Double.BYTES * cols;
    while(rows > 0) {
      long read = Math.min(Integer.MAX_VALUE / columnSize, rows);
      ByteBuffer buffer = file.map(MapMode.READ_ONLY, start, read * columnSize);
      buffer.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
      DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
      for(int j = 0; j < read; j++) {
        callback.accept(doubleBuffer);
      }
      System.gc();
      start += read * columnSize;
      rows -= read;
    }
  }

  /**
   * Read integer data, using chunked memory maps.
   * 
   * @param rows Number of rows
   * @param cols Number of columns
   * @param file File to map
   * @param start Start offset
   * @param littleEndian Endianess
   * @param callback Callback, with a temporary buffer
   * @throws IOException on IO Errors
   */
  private static void loadIntegers(int rows, int cols, FileChannel file, long start, boolean littleEndian, Consumer<IntBuffer> callback) throws IOException {
    int columnSize = Integer.BYTES * cols;
    while(rows > 0) {
      long read = Math.min(Integer.MAX_VALUE / columnSize, rows);
      ByteBuffer buffer = file.map(MapMode.READ_ONLY, start, read * columnSize);
      buffer.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
      IntBuffer intBuffer = buffer.asIntBuffer();
      for(int j = 0; j < read; j++) {
        callback.accept(intBuffer);
      }
      System.gc();
      start += read * columnSize;
      rows -= read;
    }
  }

  /**
   * Read bytes data, using chunked memory maps.
   * 
   * @param rows Number of rows
   * @param cols Number of columns
   * @param file File to map
   * @param start Start offset
   * @param littleEndian Endianess
   * @param callback Callback, with a temporary buffer
   * @throws IOException on IO Errors
   */
  private static void loadBytes(int rows, int cols, FileChannel file, long start, Consumer<ByteBuffer> callback) throws IOException {
    int columnSize = Integer.BYTES * cols;
    while(rows > 0) {
      long read = Math.min(Integer.MAX_VALUE / columnSize, rows);
      ByteBuffer buffer = file.map(MapMode.READ_ONLY, start, read * columnSize);
      for(int j = 0; j < read; j++) {
        callback.accept(buffer);
      }
      System.gc();
      start += read * columnSize;
      rows -= read;
    }
  }

  /**
   * Read short data, using chunked memory maps.
   * 
   * @param rows Number of rows
   * @param cols Number of columns
   * @param file File to map
   * @param start Start offset
   * @param littleEndian Endianess
   * @param callback Callback, with a temporary buffer
   * @throws IOException on IO Errors
   */
  private static void loadShorts(int rows, int cols, FileChannel file, long start, boolean littleEndian, Consumer<ShortBuffer> callback) throws IOException {
    int columnSize = Short.BYTES * cols;
    while(rows > 0) {
      long read = Math.min(Integer.MAX_VALUE / columnSize, rows);
      ByteBuffer buffer = file.map(MapMode.READ_ONLY, start, read * columnSize);
      buffer.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
      ShortBuffer intBuffer = buffer.asShortBuffer();
      for(int j = 0; j < read; j++) {
        callback.accept(intBuffer);
      }
      System.gc();
      start += read * columnSize;
      rows -= read;
    }
  }

  /**
   * Read long data, using chunked memory maps.
   * 
   * @param rows Number of rows
   * @param cols Number of columns
   * @param file File to map
   * @param start Start offset
   * @param littleEndian Endianess
   * @param callback Callback, with a temporary buffer
   * @throws IOException on IO Errors
   */
  private static void loadLongs(int rows, int cols, FileChannel file, long start, boolean littleEndian, Consumer<LongBuffer> callback) throws IOException {
    int columnSize = Long.BYTES * cols;
    while(rows > 0) {
      long read = Math.min(Integer.MAX_VALUE / columnSize, rows);
      ByteBuffer buffer = file.map(MapMode.READ_ONLY, start, read * columnSize);
      buffer.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
      LongBuffer longBuffer = buffer.asLongBuffer();
      for(int j = 0; j < read; j++) {
        callback.accept(longBuffer);
      }
      System.gc();
      start += read * columnSize;
      rows -= read;
    }
  }

  /**
   * Read string data, using chunked memory maps.
   * 
   * @param rows Number of rows
   * @param cols Number of columns
   * @param file File to map
   * @param start Start offset
   * @param littleEndian Endianess
   * @param callback Callback, with a temporary buffer
   * @throws IOException on IO Errors
   */
  private static void loadStrings(int rows, int cols, int size, FileChannel file, long start, boolean littleEndian, Consumer<CharBuffer> callback) throws IOException {
    Charset scs = Charset.forName(littleEndian ? "UTF-32LE" : "UTF-32BE");
    int columnSize = size * 4 * cols;
    while(rows > 0) {
      long read = Math.min(Integer.MAX_VALUE / columnSize, rows);
      ByteBuffer buffer = file.map(MapMode.READ_ONLY, start, read * columnSize);
      buffer.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
      CharBuffer chars = scs.decode(buffer);
      for(int j = 0; j < read; j++) {
        callback.accept(chars);
      }
      System.gc();
      start += read * columnSize;
      rows -= read;
    }
  }

  @Override
  public MultipleObjectsBundle loadData() {
    // test of numpy reading
    MultipleObjectsBundle result = new MultipleObjectsBundle();
    Path path = Paths.get(infile);
    try (FileChannel channel = FileChannel.open(path)) {
      Duration loadingTime = LOG.newDuration(getClass().getName() + ".loadtime").begin();
      readNumpy(channel, result);
      LOG.statistics(loadingTime.end());
      channel.close();
    }
    catch(IOException e) {
      throw new AbortException("IO error loading numpy file", e);
    }
    System.gc();
    if(labelfile == null) {
      return result;
    }
    path = Paths.get(labelfile);
    try (FileChannel channel = FileChannel.open(path)) {
      Duration loadingTime = LOG.newDuration(getClass().getName() + ".loadtime").begin();
      readNumpyLabels(channel, result);
      LOG.statistics(loadingTime.end());
      channel.close();
    }
    catch(IOException e) {
      throw new AbortException("IO error loading numpy file", e);
    }
    return result;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Andreas Lang
   */
  public static class Par extends AbstractDatabaseConnection.Par {

    /**
     * Parameter that specifies the name of the input file to be parsed.
     */
    public static final OptionID INPUT_ID = new OptionID("dbc.in", "The name of the input file to be parsed.");

    /**
     * Parameter that specifies the name of label file to be parsed.
     */
    public static final OptionID LABEL_ID = new OptionID("dbc.labels", "The name of the label file to be parsed.");

    /**
     * Random generator.
     */
    protected URI infile;

    /**
     * File for labels
     */
    protected URI labelfile;

    @Override
    public void configure(Parameterization config) {
      new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE) //
          .grab(config, x -> infile = x);
      new FileParameter(LABEL_ID, FileParameter.FileType.INPUT_FILE) //
          .setOptional(true)//
          .grab(config, x -> labelfile = x);
      configFilters(config);
    }

    @Override
    public NumpyDatabaseConnection make() {
      return new NumpyDatabaseConnection(infile, labelfile, filters);
    }
  }
}
