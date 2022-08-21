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
package elki.index.preprocessed.knn;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import elki.application.cache.CacheDoubleDistanceKNNLists;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.KNNHeap;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.utilities.exceptions.AbortException;
import elki.utilities.io.ByteArrayUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Preprocessor that loads an existing cached kNN result.
 *
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <O> Object type
 */
public class CachedDoubleDistanceKNNPreprocessor<O> extends AbstractMaterializeKNNPreprocessor<O> {
  /**
   * File to load.
   */
  private Path filename;

  /**
   * Constructor.
   *
   * @param relation Relation to index
   * @param distance Distance function
   * @param k K
   * @param file File to load
   */
  public CachedDoubleDistanceKNNPreprocessor(Relation<O> relation, Distance<? super O> distance, int k, Path file) {
    super(relation, distance, k);
    this.filename = file;
  }

  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(CachedDoubleDistanceKNNPreprocessor.class);

  @Override
  protected void preprocess() {
    createStorage();
    // open file.
    try (FileChannel channel = FileChannel.open(filename, //
        StandardOpenOption.READ);) {
      MappedByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
      // check magic header
      int header = buffer.getInt();
      if(header != CacheDoubleDistanceKNNLists.KNN_CACHE_MAGIC) {
        throw new AbortException("Cache magic number does not match.");
      }
      for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
        int dbid = ByteArrayUtil.readUnsignedVarint(buffer);
        int nnsize = ByteArrayUtil.readUnsignedVarint(buffer);
        if(nnsize < k) {
          throw new AbortException("kNN cache contains fewer than k objects!");
        }
        // FIXME: avoid the KNNHeap to KNNList roundtrip.
        // FIXME: use a DBIDVar instead of importInteger.
        KNNHeap knn = DBIDUtil.newHeap(k);
        for(int i = 0; i < nnsize; i++) {
          int nid = ByteArrayUtil.readUnsignedVarint(buffer);
          double dist = buffer.getDouble();
          knn.insert(dist, DBIDUtil.importInteger(nid));
        }
        storage.put(DBIDUtil.importInteger(dbid), knn.toKNNList());
      }
      if(buffer.hasRemaining()) {
        LOG.warning("kNN cache has " + buffer.remaining() + " bytes remaining!");
      }
    }
    catch(IOException e) {
      throw new AbortException("I/O error in loading kNN cache: " + e.getMessage(), e);
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * The parameterizable factory.
   *
   * @author Erich Schubert
   *
   * @opt nodefillcolor LemonChiffon
   * @stereotype factory
   * @navassoc - create - MaterializeKNNPreprocessor
   * 
   * @param <O> The object type
   */
  public static class Factory<O> extends AbstractMaterializeKNNPreprocessor.Factory<O> {
    /**
     * Filename to load.
     */
    private Path filename;

    /**
     * Index factory.
     *
     * @param k k parameter
     * @param distance distance function
     * @param filename Cache file
     */
    public Factory(int k, Distance<? super O> distance, Path filename) {
      super(k, distance);
      this.filename = filename;
    }

    @Override
    public CachedDoubleDistanceKNNPreprocessor<O> instantiate(Relation<O> relation) {
      CachedDoubleDistanceKNNPreprocessor<O> instance = new CachedDoubleDistanceKNNPreprocessor<>(relation, distance, k, filename);
      return instance;
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Par<O> extends AbstractMaterializeKNNPreprocessor.Factory.Par<O> {
      /**
       * Option ID for the kNN file.
       */
      public static final OptionID CACHE_ID = new OptionID("external.knnfile", "Filename with the precomputed k nearest neighbors.");

      /**
       * Filename to load.
       */
      private Path filename;

      @Override
      public void configure(Parameterization config) {
        super.configure(config);
        // Input file parameter
        new FileParameter(CACHE_ID, FileParameter.FileType.INPUT_FILE) //
            .grab(config, x -> filename = Paths.get(x));
      }

      @Override
      public Factory<O> make() {
        return new Factory<>(k, distance, filename);
      }
    }
  }
}
