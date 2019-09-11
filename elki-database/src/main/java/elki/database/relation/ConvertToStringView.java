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
package elki.database.relation;

import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.logging.Logging;
import elki.result.Metadata;

/**
 * Representation adapter that uses toString() to produce a string
 * representation.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public class ConvertToStringView extends AbstractRelation<String> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ConvertToStringView.class);

  /**
   * The database we use
   */
  final Relation<?> existing;

  /**
   * Constructor.
   *
   * @param existing Existing representation
   */
  public ConvertToStringView(Relation<?> existing) {
    super();
    this.existing = existing;
  }

  @Override
  public String get(DBIDRef id) {
    return existing.get(id).toString();
  }

  @Override
  public DBIDs getDBIDs() {
    return existing.getDBIDs();
  }

  @Override
  public DBIDIter iterDBIDs() {
    return existing.iterDBIDs();
  }

  @Override
  public int size() {
    return existing.size();
  }

  @Override
  public SimpleTypeInformation<String> getDataTypeInformation() {
    return TypeUtil.STRING;
  }

  @Override
  public String getLongName() {
    return "toString(" + Metadata.of(existing).getLongName() + ")";
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}