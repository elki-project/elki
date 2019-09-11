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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

import elki.data.type.SimpleTypeInformation;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;
import elki.database.relation.AbstractRelation;
import elki.logging.Logging;
import elki.utilities.exceptions.AbortException;

/**
 * Relation representing the Lucene documents.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class LuceneDocumentRelation extends AbstractRelation<Document> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(LuceneDocumentRelation.class);

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

  @Override
  public RangeQuery<Document> getRangeQuery(DistanceQuery<Document> distanceQuery, Object... hints) {
    if(distanceQuery.getDistance().getClass() == LuceneDistance.class) {
      return (RangeQuery<Document>) new LuceneDistanceRangeQuery(reader, ids);
    }
    return super.getRangeQuery(distanceQuery, hints);
  }

  @Override
  public KNNQuery<Document> getKNNQuery(DistanceQuery<Document> distanceQuery, Object... hints) {
    if(distanceQuery.getDistance().getClass() == LuceneDistance.class) {
      return (KNNQuery<Document>) new LuceneDistanceKNNQuery(reader, ids);
    }
    return super.getKNNQuery(distanceQuery, hints);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
