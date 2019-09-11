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
package elki.database.lucene;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import elki.database.AbstractDatabase;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDUtil;
import elki.database.relation.DBIDView;
import elki.logging.Logging;
import elki.result.ResultUtil;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;

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
   * The DBID representation we use
   */
  private DBIDView idrep;

  /**
   * Lucene database directory.
   */
  private Directory directory;

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
      IndexReader reader = IndexReader.open(directory);
      DBIDRange ids = DBIDUtil.generateStaticDBIDRange(reader.maxDoc());

      // ID relation:
      idrep = new DBIDView(ids);
      relations.add(idrep);
      ResultUtil.addChildResult(this, idrep);

      // Documents relation:
      docrep = new LuceneDocumentRelation(ids, reader);
      relations.add(docrep);
      ResultUtil.addChildResult(this, docrep);

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
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static final class Par implements Parameterizer {
    /**
     * Option ID for the index folder.
     */
    public static final OptionID INDEX_DIR_ID = new OptionID("lucene.index", "Lucene index directory.");

    /**
     * Index folder.
     */
    File idir;

    @Override
    public void configure(Parameterization config) {
      new FileParameter(INDEX_DIR_ID, FileParameter.FileType.INPUT_FILE) //
          .grab(config, x -> idir = x);
    }

    @Override
    public LuceneDatabase make() {
      try {
        return new LuceneDatabase(FSDirectory.open(idir));
      }
      catch(IOException e) {
        throw new AbortException("I/O error opening index.", e);
      }
    }
  }
}
