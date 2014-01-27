package de.lmu.ifi.dbs.elki.database.lucene;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Relation representing the Lucene document.
 * 
 * @author Erich Schubert
 */
public class LuceneDocumentRelation extends AbstractHierarchicalResult implements Relation<Document> {
  /**
   * Static type for Lucene document references.
   */
  public static final SimpleTypeInformation<Document> LUCENE_DOCUMENT_TYPE = new SimpleTypeInformation<>(Document.class);

  /**
   * Our database
   */
  private final LuceneDatabase database;

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
   * @param database Database
   * @param ids IDs
   * @param reader index reader
   */
  protected LuceneDocumentRelation(LuceneDatabase database, DBIDRange ids, IndexReader reader) {
    super();
    this.database = database;
    this.ids = ids;
    this.reader = reader;
  }

  @Override
  public Database getDatabase() {
    return database;
  }

  @Override
  public Document get(DBIDRef id) {
    try {
      return reader.document(ids.getOffset(id));
    } catch (IOException e) {
      throw new AbortException("I/O error in lucene.", e);
    }
  }

  @Override
  public void set(DBIDRef id, Document val) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(DBIDRef id) {
    throw new UnsupportedOperationException();
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
    return "Lucene document";
  }

  @Override
  public String getShortName() {
    return "lucene-document";
  }
}
