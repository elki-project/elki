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
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.util.Version;

import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDRef;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.query.range.RangeQuery;
import elki.utilities.exceptions.AbortException;

/**
 * Perform range similarity search using lucene.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class LuceneDistanceRangeQuery implements RangeQuery<Document> {
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
  DBIDRange ids;

  /**
   * Constructor.
   *
   * @param ir Index reader
   * @param ids ID range
   */
  public LuceneDistanceRangeQuery(IndexReader ir, DBIDRange ids) {
    super();
    this.ids = ids;
    this.mlt = new MoreLikeThis(ir);
    this.is = new IndexSearcher(ir);
    mlt.setAnalyzer(new StandardAnalyzer(Version.LUCENE_36));
  }

  @Override
  public ModifiableDoubleDBIDList getRangeForObject(Document obj, double range, ModifiableDoubleDBIDList result) {
    throw new UnsupportedOperationException("More-like-this only works on IDs right now.");
  }

  @Override
  public ModifiableDoubleDBIDList getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
    try {
      is.search(mlt.like(ids.getOffset(id)), new DocumentsCollector(ids, result, range));
      return result;
    }
    catch(IOException e) {
      throw new AbortException("I/O error in lucene.", e);
    }
  }

  /**
   * Class to collect Lucene results.
   * 
   * @author Erich Schubert
   */
  private class DocumentsCollector extends Collector {
    /**
     * Iterator to convert docid to DBIDs.
     */
    private final DBIDArrayIter iter;

    /**
     * Offset for docid conversion.
     */
    private int docBase = 0;

    /**
     * Scorer class
     */
    private Scorer scorer = null;

    /**
     * Result collector.
     */
    final private ModifiableDoubleDBIDList result;

    /**
     * Threshold range.
     */
    final private double range;

    /**
     * Constructor.
     * 
     * @param ids IDs
     * @param result Result collection
     * @param range Radius
     */
    public DocumentsCollector(DBIDRange ids, ModifiableDoubleDBIDList result, double range) {
      super();
      this.iter = ids.iter();
      this.result = result;
      this.range = range;
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
      return true;
    }

    @Override
    public void collect(int docid) throws IOException {
      double score = scorer.score();
      double dist = (score > 0.) ? (1. / score) : Double.POSITIVE_INFINITY;
      if(dist <= range) {
        iter.seek(docBase + docid);
        result.add(dist, iter);
      }
    }

    @Override
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
      this.docBase = docBase;
    }

    @Override
    public void setScorer(Scorer scorer) throws IOException {
      this.scorer = scorer;
    }
  }
}
