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
package elki.distance.external;

import java.io.*;

import elki.database.ids.DBID;
import elki.database.ids.DBIDRange;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.AbstractDBIDRangeDistance;
import elki.logging.Logging;
import elki.utilities.exceptions.AbortException;
import elki.utilities.io.FileUtil;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.FileParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;

/**
 * Distance function that is based on double distances given by a distance
 * matrix of an external ASCII file.
 * <p>
 * Note: parsing an ASCII file is rather expensive.
 * <p>
 * See {@link AsciiDistanceParser} for the default input format.
 * <p>
 * TODO: use a {@code double[]} instead of the hash map?
 *
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.1
 *
 * @composed - - - DistanceCacheWriter
 */
public class FileBasedSparseDoubleDistance extends AbstractDBIDRangeDistance {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(FileBasedSparseDoubleDistance.class);

  /**
   * The distance cache
   */
  private Long2DoubleOpenHashMap cache;

  /**
   * Distance parser
   */
  private DistanceParser parser;

  /**
   * Input file of distance matrix
   */
  private File matrixfile;

  /**
   * Minimum and maximum IDs seen.
   */
  private int min, max;

  /**
   * Distance to return when not defined otherwise.
   */
  protected double defaultDistance = Double.POSITIVE_INFINITY;

  /**
   * Constructor.
   *
   * @param parser Parser
   * @param matrixfile input file
   * @param defaultDistance Default distance (when undefined)
   */
  public FileBasedSparseDoubleDistance(DistanceParser parser, File matrixfile, double defaultDistance) {
    super();
    this.parser = parser;
    this.matrixfile = matrixfile;
    this.defaultDistance = defaultDistance;
  }

  @Override
  public <O extends DBID> DistanceQuery<O> instantiate(Relation<O> relation) {
    if(cache == null) {
      int size = relation.size();
      try {
        loadCache(size, new BufferedInputStream(FileUtil.tryGzipInput(new FileInputStream(matrixfile))));
      }
      catch(IOException e) {
        throw new AbortException("Could not load external distance file: " + matrixfile.toString(), e);
      }
    }
    return super.instantiate(relation);
  }

  @Override
  public double distance(int i1, int i2) {
    return (i1 == i2) ? 0. : cache.get(makeKey(i1 + min, i2 + min));
  }

  /**
   * Fill cache from an input stream.
   * 
   * @param size Expected size
   * @param in Input stream
   * @throws IOException
   */
  protected void loadCache(int size, InputStream in) throws IOException {
    // Expect a sparse matrix here.
    cache = new Long2DoubleOpenHashMap(size * 20);
    cache.defaultReturnValue(Double.POSITIVE_INFINITY);
    min = Integer.MAX_VALUE;
    max = Integer.MIN_VALUE;
    parser.parse(in, new DistanceCacheWriter() {
      @Override
      public void put(int id1, int id2, double distance) {
        if(id1 < id2) {
          min = id1 < min ? id1 : min;
          max = id2 > max ? id2 : max;
        }
        else {
          min = id2 < min ? id2 : min;
          max = id1 > max ? id1 : max;
        }
        cache.put(makeKey(id1, id2), distance);
      }
    });
    if(min != 0 && LOG.isVerbose()) {
      LOG.verbose("Distance matrix is supposed to be 0-indexed. Choosing offset " + min + " to compensate.");
    }
    if(max + 1 - min != size) {
      LOG.warning("ID range is not consistent with relation size.");
    }
  }

  /**
   * Combine two integer ids into a long value.
   *
   * @param i1 First id
   * @param i2 Second id
   * @return Combined value
   */
  protected static final long makeKey(int i1, int i2) {
    return (i1 < i2) //
        ? ((((long) i1) << 32) | i2)//
        : ((((long) i2) << 32) | i1);
  }

  @Override
  public void checkRange(DBIDRange range) {
    final int size = max + 1 - min;
    if(size < range.size()) {
      LOG.warning("Distance matrix has size " + size + " but range has size: " + range.size());
    }
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    FileBasedSparseDoubleDistance other = (FileBasedSparseDoubleDistance) obj;
    return this.cache.equals(other.cache);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter that specifies the name of the distance matrix file.
     */
    public static final OptionID MATRIX_ID = new OptionID("distance.matrix", //
        "The name of the file containing the distance matrix.");

    /**
     * Optional parameter to specify the parsers to provide a database, must
     * extend {@link DistanceParser}. If this parameter is not set,
     * {@link AsciiDistanceParser} is used as parser for all input files.
     */
    public static final OptionID PARSER_ID = new OptionID("distance.parser", //
        "Parser used to load the distance matrix.");

    /**
     * Optional parameter to specify the distance to return when no distance was
     * given in the file. Defaults to infinity.
     */
    public static final OptionID DEFAULTDIST_ID = new OptionID("distance.default", //
        "Default distance to use for undefined values.");

    /**
     * Input file.
     */
    protected File matrixfile = null;

    /**
     * Parser for input file.
     */
    protected DistanceParser parser = null;

    /**
     * Distance to return when not defined otherwise.
     */
    protected double defaultDistance = Double.POSITIVE_INFINITY;

    @Override
    public void configure(Parameterization config) {
      new FileParameter(MATRIX_ID, FileParameter.FileType.INPUT_FILE) //
          .grab(config, x -> matrixfile = x);
      new ObjectParameter<DistanceParser>(PARSER_ID, DistanceParser.class, AsciiDistanceParser.class) //
          .grab(config, x -> parser = x);
      new DoubleParameter(DEFAULTDIST_ID, Double.POSITIVE_INFINITY) //
          .grab(config, x -> defaultDistance = x);
    }

    @Override
    public FileBasedSparseDoubleDistance make() {
      return new FileBasedSparseDoubleDistance(parser, matrixfile, defaultDistance);
    }
  }
}
