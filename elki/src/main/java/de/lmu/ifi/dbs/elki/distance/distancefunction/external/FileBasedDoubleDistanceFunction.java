package de.lmu.ifi.dbs.elki.distance.distancefunction.external;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import gnu.trove.map.TLongDoubleMap;
import gnu.trove.map.hash.TLongDoubleHashMap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDBIDRangeDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Distance function that is based on double distances given by a distance
 * matrix of an external ASCII file.
 *
 * Note: parsing an ASCII file is rather expensive.
 *
 * See {@link AsciiDistanceParser} for the default input format.
 *
 * TODO: use a {@code double[]} instead of the hash map.
 *
 * @author Elke Achtert
 * @author Erich Schubert
 *
 * @apiviz.composedOf DistanceCacheWriter
 */
@Title("File based double distance for database objects.")
@Description("Loads double distance values from an external text file.")
public class FileBasedDoubleDistanceFunction extends AbstractDBIDRangeDistanceFunction {
  /**
   * The distance cache
   */
  private TLongDoubleMap cache;

  /**
   * Distance parser
   */
  private DistanceParser parser;

  /**
   * Input file of distance matrix
   */
  private File matrixfile;

  /**
   * Constructor.
   *
   * @param parser Parser
   * @param matrixfile input file
   */
  public FileBasedDoubleDistanceFunction(DistanceParser parser, File matrixfile) {
    super();
    this.parser = parser;
    this.matrixfile = matrixfile;
  }

  @Override
  public <O extends DBID> DistanceQuery<O> instantiate(Relation<O> database) {
    if(cache == null) {
      try {
        loadCache(parser, matrixfile);
      }
      catch(IOException e) {
        throw new AbortException("Could not load external distance file: " + matrixfile.toString(), e);
      }
    }
    return super.instantiate(database);
  }

  @Override
  public double distance(int i1, int i2) {
    if(i1 == i2) {
      return 0.;
    }
    return cache.get(makeKey(i1, i2));
  }

  private void loadCache(DistanceParser parser, File matrixfile) throws IOException {
    InputStream in = new BufferedInputStream(FileUtil.tryGzipInput(new FileInputStream(matrixfile)));
    cache = new TLongDoubleHashMap();
    parser.parse(in, new DistanceCacheWriter() {
      @Override
      public void put(int id1, int id2, double distance) {
        cache.put(makeKey(id1, id2), distance);
      }

      @Override
      public boolean containsKey(int id1, int id2) {
        return cache.containsKey(makeKey(id1, id2));
      }
    });
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
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    FileBasedDoubleDistanceFunction other = (FileBasedDoubleDistanceFunction) obj;
    return this.cache.equals(other.cache);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter that specifies the name of the distance matrix file.
     * <p>
     * Key: {@code -distance.matrix}
     * </p>
     */
    public static final OptionID MATRIX_ID = new OptionID("distance.matrix", //
    "The name of the file containing the distance matrix.");

    /**
     * Optional parameter to specify the parsers to provide a database, must
     * extend {@link DistanceParser}. If this parameter is not set,
     * {@link AsciiDistanceParser} is used as parser for all input files.
     * <p>
     * Key: {@code -distance.parser}
     * </p>
     */
    public static final OptionID PARSER_ID = new OptionID("distance.parser", //
    "Parser used to load the distance matrix.");

    /**
     * Input file.
     */
    protected File matrixfile = null;

    /**
     * Parser for input file.
     */
    protected DistanceParser parser = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final FileParameter MATRIX_PARAM = new FileParameter(MATRIX_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(MATRIX_PARAM)) {
        matrixfile = MATRIX_PARAM.getValue();
      }

      final ObjectParameter<DistanceParser> PARSER_PARAM = new ObjectParameter<>(PARSER_ID, DistanceParser.class, AsciiDistanceParser.class);
      if(config.grab(PARSER_PARAM)) {
        parser = PARSER_PARAM.instantiateClass(config);
      }
    }

    @Override
    protected FileBasedDoubleDistanceFunction makeInstance() {
      return new FileBasedDoubleDistanceFunction(parser, matrixfile);
    }
  }
}