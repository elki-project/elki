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
package elki.index.preprocessed.knn;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

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
  private File filename;

  /**
   * Constructor.
   * 
   * @param relation Relation to index
   * @param distanceFunction Distance function
   * @param k K
   * @param file File to load
   */
  public CachedDoubleDistanceKNNPreprocessor(Relation<O> relation, Distance<? super O> distanceFunction, int k, File file) {
    super(relation, distanceFunction, k);
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
    try (RandomAccessFile file = new RandomAccessFile(filename, "rw");
        FileChannel channel = file.getChannel()) {
      // check magic header
      int header = file.readInt();
      if(header != CacheDoubleDistanceKNNLists.KNN_CACHE_MAGIC) {
        throw new AbortException("Cache magic number does not match.");
      }
      MappedByteBuffer buffer = channel.map(MapMode.READ_ONLY, 4, file.length() - 4);
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

  @Override
  public String getLongName() {
    return "cached-knn";
  }

  @Override
  public String getShortName() {
    return "cached-knn";
  }

  @Override
  public void logStatistics() {
    // No statistics to log.
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
    private File filename;

    /**
     * Index factory.
     * 
     * @param k k parameter
     * @param distanceFunction distance function
     * @param filename Cache file
     */
    public Factory(int k, Distance<? super O> distanceFunction, File filename) {
      super(k, distanceFunction);
      this.filename = filename;
    }

    @Override
    public CachedDoubleDistanceKNNPreprocessor<O> instantiate(Relation<O> relation) {
      CachedDoubleDistanceKNNPreprocessor<O> instance = new CachedDoubleDistanceKNNPreprocessor<>(relation, distanceFunction, k, filename);
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
      private File filename;

      @Override
      public void configure(Parameterization config) {
        super.configure(config);
        // Input file parameter
        new FileParameter(CACHE_ID, FileParameter.FileType.INPUT_FILE) //
            .grab(config, x -> filename = x);
      }

      @Override
      public Factory<O> make() {
        return new Factory<>(k, distanceFunction, filename);
      }
    }
  }
}
