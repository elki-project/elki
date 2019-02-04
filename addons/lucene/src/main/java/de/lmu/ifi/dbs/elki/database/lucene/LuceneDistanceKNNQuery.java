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

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.util.Version;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Perform similarity search using lucene.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class LuceneDistanceKNNQuery extends AbstractDistanceKNNQuery<DBID> {
  /**
   * Lucene search function.
   */
  MoreLikeThis mlt;

  /**
   * Index searcher.
   */
  IndexSearcher is;

  /**
   * DBID range.
   */
  DBIDRange range;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance query
   */
  public LuceneDistanceKNNQuery(DistanceQuery<DBID> distanceQuery, IndexReader ir, DBIDRange range) {
    super(distanceQuery);
    this.range = range;
    this.mlt = new MoreLikeThis(ir);
    this.is = new IndexSearcher(ir);
    mlt.setAnalyzer(new StandardAnalyzer(Version.LUCENE_36));
  }

  @Override
  public KNNList getKNNForDBID(DBIDRef id, int k) {
    try {
      Query query = mlt.like(range.getOffset(id));
      TopDocs topDocs = is.search(query, k);

      KNNHeap res = DBIDFactory.FACTORY.newHeap(k);
      DBIDArrayIter it = range.iter();
      for(ScoreDoc scoreDoc : topDocs.scoreDocs) {
        double dist = (scoreDoc.score > 0.) ? (1. / scoreDoc.score) : Double.POSITIVE_INFINITY;
        res.insert(dist, it.seek(scoreDoc.doc));
      }
      return res.toKNNList();
    }
    catch(IOException e) {
      throw new AbortException("I/O error in lucene.", e);
    }
  }

  @Override
  public KNNList getKNNForObject(DBID obj, int k) {
    return getKNNForDBID((DBIDRef) obj, k);
  }
}
