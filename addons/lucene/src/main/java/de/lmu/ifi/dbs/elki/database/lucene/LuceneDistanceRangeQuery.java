package de.lmu.ifi.dbs.elki.database.lucene;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.util.Version;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.distance.ModifiableDoubleDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.integer.DoubleDistanceIntegerDBIDKNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Perform range similarity search using lucene.
 * 
 * @author Erich Schubert
 */
public class LuceneDistanceRangeQuery extends AbstractDistanceRangeQuery<DBID, DoubleDistance> {
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
   * @param distanceQuery Distance query
   */
  public LuceneDistanceRangeQuery(DistanceQuery<DBID, DoubleDistance> distanceQuery, IndexReader ir, DBIDRange ids) {
    super(distanceQuery);
    this.ids = ids;
    this.mlt = new MoreLikeThis(ir);
    this.is = new IndexSearcher(ir);
    mlt.setAnalyzer(new StandardAnalyzer(Version.LUCENE_36));
    mlt.setFieldNames(new String[] { "content" });
  }

  @Override
  public DistanceDBIDList<DoubleDistance> getRangeForDBID(DBIDRef id, DoubleDistance range) {
    try {
      Query query = mlt.like(ids.getOffset(id));
      ModifiableDoubleDistanceDBIDList result = new DoubleDistanceIntegerDBIDKNNList();
      is.search(query, new DocumentsCollector(ids, result, range.doubleValue()));
      return result;
    } catch (IOException e) {
      throw new AbortException("I/O error in lucene.", e);
    }
  }

  @Override
  public DistanceDBIDList<DoubleDistance> getRangeForObject(DBID obj, DoubleDistance range) {
    return getRangeForDBID(obj, range);
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
    final private ModifiableDoubleDistanceDBIDList result;

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
    public DocumentsCollector(DBIDRange ids, ModifiableDoubleDistanceDBIDList result, double range) {
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
      if (dist <= range) {
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
