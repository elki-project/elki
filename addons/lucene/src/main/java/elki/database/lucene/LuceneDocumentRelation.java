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
package elki.database.lucene;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

import elki.data.type.SimpleTypeInformation;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.index.KNNIndex;
import elki.index.RangeIndex;
import elki.result.Metadata;
import elki.utilities.exceptions.AbortException;

/**
 * Relation representing the Lucene documents.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class LuceneDocumentRelation implements Relation<Document> {
  /**
   * Static type for Lucene document references.
   */
  public static final SimpleTypeInformation<Document> LUCENE_DOCUMENT_TYPE = new SimpleTypeInformation<>(Document.class);

  /**
   * Lucene reader.
   */
  private final IndexReader reader;

  /**
   * The DBIDs this is supposed to be defined for.
   */
  private final DBIDRange ids;

  /**
   * Constructor.
   *
   * @param ids IDs
   * @param reader index reader
   */
  protected LuceneDocumentRelation(DBIDRange ids, IndexReader reader) {
    super();
    this.ids = ids;
    this.reader = reader;
    Metadata.hierarchyOf(this).addChild(new LuceneIndex());
  }

  @Override
  public Document get(DBIDRef id) {
    try {
      return reader.document(ids.getOffset(id));
    }
    catch(IOException e) {
      throw new AbortException("I/O error in lucene.", e);
    }
  }

  @Override
  public SimpleTypeInformation<Document> getDataTypeInformation() {
    return LUCENE_DOCUMENT_TYPE;
  }

  @Override
  public DBIDs getDBIDs() {
    return ids;
  }

  @Override
  public DBIDIter iterDBIDs() {
    return ids.iter();
  }

  @Override
  public int size() {
    return ids.size();
  }

  @Override
  public String getLongName() {
    return "Lucene documents";
  }

  /**
   * Pseudo index that simply accesses Lucene.
   *
   * @author Erich Schubert
   */
  public class LuceneIndex implements KNNIndex<Document>, RangeIndex<Document> {
    @Override
    public void initialize() {
      // Handled by the database already.
    }

    @Override
    public RangeSearcher<Document> rangeByObject(DistanceQuery<Document> distanceQuery, double maxrange, int flags) {
      return null; // FIXME: currently not supported.
    }

    @Override
    public RangeSearcher<DBIDRef> rangeByDBID(DistanceQuery<Document> distanceQuery, double maxradius, int flags) {
      return distanceQuery.getDistance().getClass() == LuceneDistance.class && distanceQuery.getRelation() == this ? //
          new LuceneDistanceRangeSearcher(reader, ids) : null;
    }

    @Override
    public KNNSearcher<Document> kNNByObject(DistanceQuery<Document> distanceQuery, int maxk, int flags) {
      return null; // FIXME: currently not supported.
    }

    @Override
    public KNNSearcher<DBIDRef> kNNByDBID(DistanceQuery<Document> distanceQuery, int maxk, int flags) {
      return distanceQuery.getDistance().getClass() == LuceneDistance.class && distanceQuery.getRelation() == this ? //
          new LuceneDistanceKNNSearcher(reader, ids) : null;
    }

    @Override
    public void logStatistics() {
      // TODO: Log some stats from lucene?
    }
  }
}
