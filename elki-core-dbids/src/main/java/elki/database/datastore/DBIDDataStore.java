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
package elki.database.datastore;

import elki.database.ids.DBID;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDVar;

/**
 * DBID-valued data store (avoids boxing/unboxing).
 * 
 * @author Erich Schubert
 * @since 0.5.5
 */
public interface DBIDDataStore extends DataStore<DBID> {
  /**
   * Getter, but using objects.
   * 
   * @deprecated Use {@link #assignVar} and a {@link DBIDVar} instead, to avoid boxing/unboxing cost.
   */
  @Override
  @Deprecated
  DBID get(DBIDRef id);

  /**
   * Retrieves an object from the storage.
   * 
   * @param id Database ID.
   * @param var Variable to update.
   * @return {@code var}
   */
  DBIDVar assignVar(DBIDRef id, DBIDVar var);
}
