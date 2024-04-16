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
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import elki.data.DoubleVector;
import elki.data.FloatVector;
import elki.data.IntegerVector;
import elki.data.LabelList;
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
 * 
 * @author Andreas Lang
 */
public class NumpyArrayReader extends AbstractDatabaseConnection {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(NumpyArrayReader.class);

  URI infile;

  URI labelfile;

  /**
   * Constructor.
   * 
   * @param filters Filters to use
   */
  public NumpyArrayReader(URI infile, URI labelfile, List<? extends ObjectFilter> filters) {
    super(filters);
    this.infile = infile;
    this.labelfile = labelfile;
  }

  private void readNumpy(FileChannel file, MultipleObjectsBundle bundle, boolean label) throws IOException {
    Charset cs = Charset.forName("UTF-8");
      MappedByteBuffer buffer = file.map(MapMode.READ_ONLY,0,10);
      if(buffer.get() != -109) {
        throw new IOException("Invalid magic string");
      }
      byte[] magic = new byte[5];
      buffer.get(magic);
      String magic_str = new String(magic, cs);
      if(!magic_str.equals("NUMPY")) {
        throw new IOException("Invalid magic string");
      }
      byte major = buffer.get();
      byte minor = buffer.get();
      System.out.println("major: " + major);
      System.out.println("minor: " + minor);
      byte[] header_len = new byte[2];
      buffer.get(header_len);
      int len = (header_len[0] & 0xff) + (header_len[1] & 0xff) * 256;
      System.out.println("header len: " + len);
      buffer = file.map(MapMode.READ_ONLY, 10, len);
      byte[] header = new byte[len];
      buffer.get(header);
      String headerString = new String(header, cs);
      headerString = headerString.replaceAll(" ", "");
      headerString = headerString.substring(1).split("\\}")[0];
      System.out.println("header: " + headerString);
      String dtype = headerString.split("'descr':")[1].split("'")[1];
      boolean fOrder = Boolean.parseBoolean(headerString.split("'fortran_order':")[1].split(",")[0]);
      if(fOrder) {
        throw new IOException("Fortran order not supported");
      }
      String shape = headerString.split("'shape':\\(")[1];
      shape = shape.split("\\)")[0];
      String[] row_col = shape.split(",");
      int rows = Integer.parseInt(row_col[0]);
      int cols = 1;
      if (row_col.length == 2) {
        cols = Integer.parseInt(row_col[1]);
      }
      boolean littleEndian = dtype.startsWith("<");
      if(dtype.endsWith("f4")) {
        List<FloatVector> vectors = new ArrayList<>(rows);
        System.out.println("float32");
        long columnSize = 4 * cols;
        float[][] data = new float[rows][cols];
        int remaining = rows;
        int i = 0;
        while(remaining > 0) {
          long read = Math.min(Integer.MAX_VALUE / columnSize, remaining);
          buffer = file.map(MapMode.READ_ONLY, 10L + len + i * columnSize, read * columnSize);
          buffer.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
          FloatBuffer floatBuffer = buffer.asFloatBuffer();
          remaining -= read;
          for(int j = 0; j < read; j++) {
            floatBuffer.get(data[i], 0, cols);
            vectors.add(FloatVector.wrap(data[i++]));
          }
          System.gc();
        }
        if(label) {
          ArrayList<LabelList> labellist = new ArrayList<>();
          for(i = 0; i < data.length; i++) {
            List<String> row = new ArrayList<>();
            for(int j = 0; j < data[i].length; j++) {
              row.add(Float.toString(data[i][j]));
            }
            labellist.add(LabelList.make(row));
          }
          bundle.appendColumn(TypeUtil.LABELLIST, labellist);
          return;
        }
        VectorFieldTypeInformation<FloatVector> type = new VectorFieldTypeInformation<>(FloatVector.FACTORY, cols);
        System.out.println("reading Numpy Array Successfull");
        bundle.appendColumn(type, vectors);
      }
      else if(dtype.endsWith("f8")) {
        List<DoubleVector> vectors = new ArrayList<>(rows);
        System.out.println("float64");
        long columnSize = 8 * cols;
        double[][] data = new double[rows][cols];
        int remaining = rows;
        int i = 0;
        while(remaining > 0) {
          long read = Math.min(Integer.MAX_VALUE / columnSize, remaining);
          buffer = file.map(MapMode.READ_ONLY, 10L + len + i * columnSize, read * columnSize);
          buffer.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
          DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
          remaining -= read;
          for(int j = 0; j < read; j++) {
            doubleBuffer.get(data[i],0,cols);
            vectors.add(DoubleVector.wrap(data[i++]));
          }
          System.gc();
        }
        if(label) {
          ArrayList<LabelList> labellist = new ArrayList<>();
          for(i = 0; i < data.length; i++) {
            List<String> row = new ArrayList<>();
            for(int j = 0; j < data[i].length; j++) {
              row.add(Double.toString(data[i][j]));
            }
            labellist.add(LabelList.make(row));
          }
          bundle.appendColumn(TypeUtil.LABELLIST, labellist);
          return;
        }
        VectorFieldTypeInformation<DoubleVector> type = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, cols);
        System.out.println("reading Numpy Array Succressfull");
        bundle.appendColumn(type, vectors);
      }
      else if(dtype.endsWith("i4")){
        List<IntegerVector> vectors = new ArrayList<>(rows);
        System.out.println("int32");
        long columnSize = 4 * cols;
        int[][] data = new int[rows][cols];
        int remaining = rows;
        int i = 0;
        while(remaining >0){
          long read = Math.min(Integer.MAX_VALUE / columnSize, remaining);
          buffer = file.map(MapMode.READ_ONLY, 10L + len + i * columnSize, read * columnSize);
          buffer.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
          IntBuffer intBuffer = buffer.asIntBuffer();
          remaining -= read;
          for(int j = 0; j < read; j++) {
            intBuffer.get(data[i], 0, cols);
            vectors.add(IntegerVector.wrap(data[i++]));
          }
          System.gc();
        }
        if(label) {
          ArrayList<LabelList> labellist = new ArrayList<>();
          for(i = 0; i < data.length; i++) {
            List<String> row = new ArrayList<>();
            for(int j = 0; j < data[i].length; j++) {
              row.add(Integer.toString(data[i][j]));
            }
            labellist.add(LabelList.make(row));
          }
          bundle.appendColumn(TypeUtil.LABELLIST, labellist);
          return;
        }
        VectorFieldTypeInformation<IntegerVector> type = new VectorFieldTypeInformation<>(IntegerVector.STATIC, cols);
        System.out.println("reading Numpy Array Succressfull");
        bundle.appendColumn(type, vectors);
      }
      else if (dtype.contains("U")){
        Charset scs;
        if(littleEndian){
          scs = Charset.forName("UTF-32LE");
        } else {
          scs = Charset.forName("UTF-32BE");
        }
        int size = Integer.parseInt(dtype.split("U")[1]);
        System.out.println("Char "+size);
        long columnSize = 4 * size * cols;
        ArrayList<ArrayList<String>> data = new ArrayList<>();
        char[] char_data = new char[size];
        int remaining = rows;
        int i = 0;
        while(remaining > 0) {
          long read = Math.min(Integer.MAX_VALUE / columnSize, remaining);
          buffer = file.map(MapMode.READ_ONLY, 10L + len + i * columnSize, read * columnSize);
          buffer.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
          CharBuffer charBuffer = scs.decode(buffer);
          remaining -= read;
          for(int j = 0; j < read; j++) {
            ArrayList<String> row = new ArrayList<>();
            for(int c = 0; c < cols ; c++){
              charBuffer.get(char_data, 0, size);
              row.add(new String(char_data));
            }
            data.add(row);
          }
          System.gc();
        }
        if(label){
          ArrayList<LabelList> labellist = new ArrayList<>();
          for(i = 0; i < data.size(); i++) {
            labellist.add(LabelList.make(data.get(i)));
          }
          bundle.appendColumn(TypeUtil.LABELLIST, labellist);
          return;
        }
        ArrayList<ArrayList<String>> col_list = new ArrayList<>();
        for (i = 0; i < cols; i++) {
          col_list.add(new ArrayList<>());
        }
        for(i = 0; i < rows; i++) {
          for(int j = 0; j < cols; j++) {
            col_list.get(j).add(data.get(i).get(j));
          }
        }
        for (i = 0; i < cols; i++) {
          bundle.appendColumn(TypeUtil.STRING, col_list.get(i));
        }
      }
      else if(dtype.endsWith("i8")) {
                List<IntegerVector> vectors = new ArrayList<>(rows);
        System.out.println("int64 - long will be converted to int");
        long columnSize = 8 * cols;
        int[][] data = new int[rows][cols];
        int remaining = rows;
        int i = 0;
        long[] row = new long[cols];
        while(remaining >0){
          long read = Math.min(Integer.MAX_VALUE / columnSize, remaining);
          buffer = file.map(MapMode.READ_ONLY, 10L + len + i * columnSize, read * columnSize);
          buffer.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
          LongBuffer longBuffer = buffer.asLongBuffer();
          remaining -= read;
          for(int j = 0; j < read; j++) {
            longBuffer.get(row, 0, cols);
            for (int k = 0; k < cols; k++) {
              if (Integer.MAX_VALUE < row[k]) {
                throw new IOException("Value too large for int aborting");
              }
              data[i][k] =(int) row[k];
            }
            vectors.add(IntegerVector.wrap(data[i++]));
          }
          System.gc();
        }
        if(label) {
          ArrayList<LabelList> labellist = new ArrayList<>();
          for(i = 0; i < data.length; i++) {
            List<String> label_row = new ArrayList<>();
            for(int j = 0; j < data[i].length; j++) {
              label_row.add(Integer.toString(data[i][j]));
            }
            labellist.add(LabelList.make(label_row));
          }
          bundle.appendColumn(TypeUtil.LABELLIST, labellist);
          return;
        }
        VectorFieldTypeInformation<IntegerVector> type = new VectorFieldTypeInformation<>(IntegerVector.STATIC, cols);
        System.out.println("reading Numpy Array Succressfull");
        bundle.appendColumn(type, vectors);
      }
      else {
        throw new IOException("Invalid dtype" + dtype);
      }
  }

  @Override
  public MultipleObjectsBundle loadData() {
    // test of numpy reading
    System.out.println("Reading numpy file");
    MultipleObjectsBundle result = new MultipleObjectsBundle();
    Path path = Paths.get(infile);
    try (FileChannel channel = FileChannel.open(path)) {
      Duration loadingTime = LOG.newDuration(getClass().getName() + ".loadtime").begin();
      readNumpy(channel, result, false);
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
    try(FileChannel channel = FileChannel.open(path)) {
      Duration loadingTime = LOG.newDuration(getClass().getName() + ".loadtime").begin();
      readNumpy(channel, result, true);
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
    public static final OptionID LABEL_ID = new OptionID("dbc.lbl", "The name of the label file to be parsed.");

    /**
     * Random generator.
     */
    protected URI infile;

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
    public NumpyArrayReader make() {
      return new NumpyArrayReader(infile, labelfile, filters);
    }
  }
}
