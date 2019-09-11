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
package elki.result;

import java.util.ArrayList;
import java.util.List;

import elki.database.Database;
import elki.database.relation.Relation;
import elki.result.Metadata.Hierarchy;
import elki.utilities.datastructures.iterator.It;

/**
 * Utilities for handling result objects
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @navassoc - filters - Result
 */
public final class ResultUtil {
  /**
   * Private constructor. Static methods only.
   */
  private ResultUtil() {
    // Do not use.
  }

  /**
   * Collect all Annotation results from a Result
   *
   * @param r Result
   * @return List of all annotation results
   */
  public static List<Relation<?>> getRelations(Object r) {
    return Metadata.hierarchyOf(r).iterDescendantsSelf()//
        .<Relation<?>> filter(Relation.class).collect(new ArrayList<>());
  }

  /**
   * Collect all ordering results from a Result
   *
   * @param r Result
   * @return List of ordering results
   */
  public static List<OrderingResult> getOrderingResults(Object r) {
    return Metadata.hierarchyOf(r).iterDescendantsSelf()//
        .filter(OrderingResult.class).collect(new ArrayList<>());
  }

  /**
   * Collect all collection results from a Result
   *
   * @param r Result
   * @return List of collection results
   */
  public static List<CollectionResult<?>> getCollectionResults(Object r) {
    return Metadata.hierarchyOf(r).iterDescendantsSelf()//
        .<CollectionResult<?>> filter(CollectionResult.class).collect(new ArrayList<>());
  }

  /**
   * Return all Iterable results
   *
   * @param r Result
   * @return List of iterable results
   */
  public static List<IterableResult<?>> getIterableResults(Object r) {
    return Metadata.hierarchyOf(r).iterDescendantsSelf()//
        .<IterableResult<?>> filter(IterableResult.class).collect(new ArrayList<>());
  }

  /**
   * Return only results of the given restriction class
   * 
   * @param r Starting position
   * @param restrictionClass Class restriction
   *
   * @param <C> Class type
   * @return filtered results list
   */
  public static <C> ArrayList<C> filterResults(Object r, Class<? super C> restrictionClass) {
    return Metadata.hierarchyOf(r).iterDescendantsSelf()//
        .<C> filter(restrictionClass).collect(new ArrayList<C>());
  }

  /**
   * Add a child result.
   *
   * @param parent Parent
   * @param child Child
   */
  public static void addChildResult(Object parent, Object child) {
    Metadata.hierarchyOf(parent).addChild(child);
  }

  /**
   * Find the first database result in the tree.
   *
   * @param result Result hierarchy.
   * @return Database
   */
  public static Database findDatabase(Object result) {
    It<Database> it = Metadata.hierarchyOf(result).iterAncestorsSelf().filter(Database.class);
    return it.valid() ? it.get() : null;
  }

  /**
   * Recursively remove a result and its children.
   * 
   * @param child Result to remove
   */
  public static void removeRecursive(Object child) {
    final Hierarchy h = Metadata.hierarchyOf(child);
    for(It<Object> iter = h.iterParents(); iter.valid(); iter.advance()) {
      Metadata.hierarchyOf(iter.get()).removeChild(child);
    }
    for(It<Object> iter = h.iterChildren(); iter.valid(); iter.advance()) {
      removeRecursive(iter.get());
    }
  }
}
