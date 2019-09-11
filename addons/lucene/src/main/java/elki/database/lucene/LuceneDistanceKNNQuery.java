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

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.util.Version;

import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDFactory;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDRef;
import elki.database.ids.KNNHeap;
import elki.database.ids.KNNList;
import elki.database.query.knn.KNNQuery;
import elki.utilities.exceptions.AbortException;

/**
 * Perform similarity search using lucene.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class LuceneDistanceKNNQuery implements KNNQuery<Document> {
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
   * @param ir Index reader
   * @param range ID range
   */
  public LuceneDistanceKNNQuery(IndexReader ir, DBIDRange range) {
    this.range = range;
    this.mlt = new MoreLikeThis(ir);
    this.is = new IndexSearcher(ir);
    mlt.setAnalyzer(new StandardAnalyzer(Version.LUCENE_36));
  }

  @Override
  public KNNList getKNNForObject(Document obj, int k) {
    throw new UnsupportedOperationException("More-like-this only works on IDs right now.");
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
}
