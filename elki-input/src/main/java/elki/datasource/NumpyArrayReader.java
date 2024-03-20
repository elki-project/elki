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
import java.nio.FloatBuffer;
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
 * Produce a database of random double vectors with each dimension in [0:1].
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class NumpyArrayReader extends AbstractDatabaseConnection {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(NumpyArrayReader.class);

  URI infile;

  /**
   * Constructor.
   * 
   * @param dim Dimensionality
   * @param size Database size
   * @param rnd Random generator
   * @param filters Filters to use
   */
  public NumpyArrayReader(URI infile, List<? extends ObjectFilter> filters) {
    super(filters);
    this.infile = infile;
  }

  private MultipleObjectsBundle readNumpy(FileChannel file) throws IOException {
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
      headerString = headerString.substring(1).split("}")[0];
      System.out.println("header: " + headerString);
      String dtype = headerString.split("'descr':")[1].split("'")[1];
      String shape = headerString.split("'shape':\\(")[1];
      shape = shape.split("\\)")[0];
      int rows = Integer.parseInt(shape.split(",")[0]);
      int cols = Integer.parseInt(shape.split(",")[1]);
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
        VectorFieldTypeInformation<FloatVector> type = new VectorFieldTypeInformation<>(FloatVector.FACTORY, cols);
        System.out.println("reading Numpy Array Successfull");
        return MultipleObjectsBundle.makeSimple(type, vectors);
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
          remaining -= read;
          for(int j = 0; j < read; j++) {
            buffer.asDoubleBuffer().get(data[i],0,cols);
            vectors.add(DoubleVector.wrap(data[i++]));
          }
          System.gc();
        }
        VectorFieldTypeInformation<DoubleVector> type = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, cols);
        System.out.println("reading Numpy Array Succressfull");
        return MultipleObjectsBundle.makeSimple(type, vectors);
      }
      else if(dtype.endsWith("i4")){
        List<IntegerVector> vectors = new ArrayList<>(rows);
        System.out.println("int32");
        long columnSize = 8 * cols;
        int[][] data = new int[rows][cols];
        int remaining = rows;
        int i = 0;
        while(remaining >0){
          long read = Math.min(Integer.MAX_VALUE / columnSize, remaining);
          buffer = file.map(MapMode.READ_ONLY, 10L + len + i * columnSize, read * columnSize);
          buffer.order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
          remaining -= read;
          for(int j = 0; j < read; j++) {
            buffer.asIntBuffer().get(data[i], 0, cols);
            vectors.add(IntegerVector.wrap(data[i++]));
          }
          System.gc();
        }
        VectorFieldTypeInformation<IntegerVector> type = new VectorFieldTypeInformation<>(IntegerVector.STATIC, cols);
        System.out.println("reading Numpy Array Succressfull");
        return MultipleObjectsBundle.makeSimple(type, vectors);
      }
      // else if(dtype.endsWith("i8") {
      //   System.out.println("int64");
      // }
      else {
        throw new IOException("Invalid dtype" + dtype);
      }
  }

  @Override
  public MultipleObjectsBundle loadData() {
    // test of numpy reading
    System.out.println("Reading numpy file");
    Path path = Paths.get(infile);
    try (FileChannel channel = FileChannel.open(path)) {
      Duration loadingTime = LOG.newDuration(getClass().getName() + ".loadtime").begin();
      MultipleObjectsBundle result = readNumpy(channel);
      LOG.statistics(loadingTime.end());
      channel.close();
      System.gc();
      return result;
    }
    catch(IOException e) {
      throw new AbortException("IO error loading numpy file", e);
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par extends AbstractDatabaseConnection.Par {

    /**
     * Parameter that specifies the name of the input file to be parsed.
     */
    public static final OptionID INPUT_ID = new OptionID("dbc.in", "The name of the input file to be parsed.");

    /**
     * Random generator.
     */
    protected URI infile;

    @Override
    public void configure(Parameterization config) {
      new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE) //
          .grab(config, x -> infile = x);
      configFilters(config);
    }

    @Override
    public NumpyArrayReader make() {
      return new NumpyArrayReader(infile, filters);
    }
  }
}
