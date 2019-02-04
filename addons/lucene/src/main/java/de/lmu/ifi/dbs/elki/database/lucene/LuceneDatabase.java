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
package de.lmu.ifi.dbs.elki.database.lucene;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import de.lmu.ifi.dbs.elki.database.AbstractDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.DBIDView;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Database backend using Lucene 3.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class LuceneDatabase extends AbstractDatabase {
  /**
   * Class logger.
   */
  public static final Logging LOG = Logging.getLogger(LuceneDatabase.class);

  /**
   * IDs of this database
   */
  private DBIDRange ids;

  /**
   * The DBID representation we use
   */
  private DBIDView idrep;

  /**
   * Lucene database directory.
   */
  private Directory directory;

  /**
   * Lucene index reader.
   */
  private IndexReader reader;

  /**
   * Document representation.
   */
  private LuceneDocumentRelation docrep;

  /**
   * Constructor.
   *
   * @param directory Lucene directory.
   */
  public LuceneDatabase(Directory directory) {
    super();
    this.directory = directory;
  }

  @Override
  public void initialize() {
    try {
      reader = IndexReader.open(directory);
      ids = DBIDUtil.generateStaticDBIDRange(reader.maxDoc());

      // ID relation:
      idrep = new DBIDView(ids);
      relations.add(idrep);
      getHierarchy().add(this, idrep);

      // Documents relation:
      docrep = new LuceneDocumentRelation(ids, reader);
      relations.add(docrep);
      getHierarchy().add(this, docrep);

      eventManager.fireObjectsInserted(ids);
    }
    catch(CorruptIndexException e) {
      throw new AbortException("Index is corrupt.", e);
    }
    catch(IOException e) {
      throw new AbortException("I/O error reading index.", e);
    }
  }

  @Override
  public <O> RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    if(distanceQuery.getDistanceFunction() instanceof LuceneDistanceFunction) {
      @SuppressWarnings("unchecked")
      final RangeQuery<O> rq = (RangeQuery<O>) new LuceneDistanceRangeQuery((DistanceQuery<DBID>) distanceQuery, reader, ids);
      return rq;
    }
    return super.getRangeQuery(distanceQuery, hints);
  }

  @Override
  public <O> KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    if(distanceQuery.getDistanceFunction() instanceof LuceneDistanceFunction) {
      @SuppressWarnings("unchecked")
      final KNNQuery<O> kq = (KNNQuery<O>) new LuceneDistanceKNNQuery((DistanceQuery<DBID>) distanceQuery, reader, ids);
      return kq;
    }
    return super.getKNNQuery(distanceQuery, hints);
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
  public static final class Parameterizer extends AbstractParameterizer {
    /**
     * Option ID for the index folder.
     */
    public static final OptionID INDEX_DIR_ID = new OptionID("lucene.index", "Lucene index directory.");

    /**
     * Index folder.
     */
    File idir;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter fileP = new FileParameter(INDEX_DIR_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(fileP)) {
        idir = fileP.getValue();
      }
    }

    @Override
    protected LuceneDatabase makeInstance() {
      try {
        return new LuceneDatabase(FSDirectory.open(idir));
      }
      catch(IOException e) {
        throw new AbortException("I/O error opening index.", e);
      }
    }
  }
}
