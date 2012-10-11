package de.lmu.ifi.dbs.elki.distance.distancefunction.external;

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDBIDDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.FloatDistance;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Provides a DistanceFunction that is based on float distances given by a
 * distance matrix of an external file.
 * 
 * @author Elke Achtert
 */
@Title("File based float distance for database objects.")
@Description("Loads float distance values from an external text file.")
public class FileBasedFloatDistanceFunction extends AbstractDBIDDistanceFunction<FloatDistance> {
  /**
   * Parameter that specifies the name of the distance matrix file.
   * <p>
   * Key: {@code -distance.matrix}
   * </p>
   */
  public static final OptionID MATRIX_ID = OptionID.getOrCreateOptionID("distance.matrix", "The name of the file containing the distance matrix.");

  /**
   * Optional parameter to specify the parsers to provide a database, must
   * extend {@link DistanceParser}. If this parameter is not set,
   * {@link NumberDistanceParser} is used as parser for all input files.
   * <p>
   * Key: {@code -distance.parser}
   * </p>
   */
  public static final OptionID PARSER_ID = OptionID.getOrCreateOptionID("distance.parser", "Parser used to load the distance matrix.");

  /**
   * The distance cache
   */
  private Map<DBIDPair, FloatDistance> cache;

  /**
   * Constructor.
   * 
   * @param parser Parser
   * @param matrixfile input file
   */
  public FileBasedFloatDistanceFunction(DistanceParser<FloatDistance> parser, File matrixfile) {
    super();
    try {
      loadCache(parser, matrixfile);
    }
    catch(IOException e) {
      throw new AbortException("Could not load external distance file: " + matrixfile.toString(), e);
    }
  }

  /**
   * Returns the distance between the two objects specified by their objects
   * ids. If a cache is used, the distance value is looked up in the cache. If
   * the distance does not yet exists in cache, it will be computed an put to
   * cache. If no cache is used, the distance is computed.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objects specified by their objects ids
   */
  @Override
  public FloatDistance distance(DBIDRef id1, DBIDRef id2) {
    if(id1 == null) {
      return getDistanceFactory().undefinedDistance();
    }
    if(id2 == null) {
      return getDistanceFactory().undefinedDistance();
    }
    // the smaller id is the first key
    if(DBIDUtil.compare(id1, id2) > 0) {
      return distance(id2, id1);
    }

    return cache.get(DBIDUtil.newPair(id1, id2));
  }

  private void loadCache(DistanceParser<FloatDistance> parser, File matrixfile) throws IOException {
    InputStream in = new BufferedInputStream(FileUtil.tryGzipInput(new FileInputStream(matrixfile)));
    DistanceParsingResult<FloatDistance> res = parser.parse(in);
    cache = res.getDistanceCache();
  }

  @Override
  public FloatDistance getDistanceFactory() {
    return FloatDistance.FACTORY;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    FileBasedFloatDistanceFunction other = (FileBasedFloatDistanceFunction) obj;
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
    protected File matrixfile = null;

    protected DistanceParser<FloatDistance> parser = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final FileParameter MATRIX_PARAM = new FileParameter(MATRIX_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(MATRIX_PARAM)) {
        matrixfile = MATRIX_PARAM.getValue();
      }
      final ObjectParameter<DistanceParser<FloatDistance>> PARSER_PARAM = new ObjectParameter<DistanceParser<FloatDistance>>(PARSER_ID, DistanceParser.class, NumberDistanceParser.class);
      if(config.grab(PARSER_PARAM)) {
        ListParameterization parserConfig = new ListParameterization();
        parserConfig.addParameter(DistanceParser.DISTANCE_ID, FloatDistance.class);
        ChainedParameterization combinedConfig = new ChainedParameterization(parserConfig, config);
        combinedConfig.errorsTo(config);
        parser = PARSER_PARAM.instantiateClass(config);
      }
    }

    @Override
    protected FileBasedFloatDistanceFunction makeInstance() {
      return new FileBasedFloatDistanceFunction(parser, matrixfile);
    }
  }
}