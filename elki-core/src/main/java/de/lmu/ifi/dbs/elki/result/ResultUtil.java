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
package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.Metadata.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;

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
  public static List<Relation<?>> getRelations(Result r) {
    return Metadata.of(r).hierarchy().iterDescendantsSelf()//
        .<Relation<?>> filter(Relation.class).collect(new ArrayList<>());
  }

  /**
   * Collect all ordering results from a Result
   *
   * @param r Result
   * @return List of ordering results
   */
  public static List<OrderingResult> getOrderingResults(Result r) {
    return Metadata.of(r).hierarchy().iterDescendantsSelf()//
        .filter(OrderingResult.class).collect(new ArrayList<>());
  }

  /**
   * Collect all collection results from a Result
   *
   * @param r Result
   * @return List of collection results
   */
  public static List<CollectionResult<?>> getCollectionResults(Result r) {
    return Metadata.of(r).hierarchy().iterDescendantsSelf()//
        .<CollectionResult<?>> filter(CollectionResult.class).collect(new ArrayList<>());
  }

  /**
   * Return all Iterable results
   *
   * @param r Result
   * @return List of iterable results
   */
  public static List<IterableResult<?>> getIterableResults(Result r) {
    return Metadata.of(r).hierarchy().iterDescendantsSelf()//
        .<IterableResult<?>> filter(IterableResult.class).collect(new ArrayList<>());
  }

  /**
   * Return only results of the given restriction class
   *
   * @param <C> Class type
   * @param hier Result hierarchy
   * @param r Starting position
   * @param restrictionClass Class restriction
   * @return filtered results list
   */
  public static <C extends Result> ArrayList<C> filterResults(ResultHierarchy hier, Result r, Class<? super C> restrictionClass) {
    return Metadata.of(r).hierarchy().iterDescendantsSelf()//
        .<C> filter(restrictionClass).collect(new ArrayList<C>());
  }

  /**
   * Return only results of the given restriction class
   *
   * @param <C> Class type
   * @param hier Result hierarchy
   * @param restrictionClass Class restriction
   * @return filtered results list
   */
  public static <C extends Result> ArrayList<C> filterResults(ResultHierarchy hier, Class<? super C> restrictionClass) {
    ArrayList<C> res = new ArrayList<>();
    It<C> it = hier.iterAll().filter(restrictionClass);
    it.forEach(res::add);
    return res;
  }

  /**
   * Add a child result.
   *
   * @param parent Parent
   * @param child Child
   */
  public static void addChildResult(Result parent, Result child) {
    Metadata.of(parent).hierarchy().addChild(child);
  }

  /**
   * Find the first database result in the tree.
   *
   * @param baseResult Result tree base.
   * @return Database
   */
  public static Database findDatabase(ResultHierarchy hier, Result baseResult) {
    final List<Database> dbs = filterResults(hier, baseResult, Database.class);
    return (!dbs.isEmpty()) ? dbs.get(0) : null;
  }

  /**
   * Find the first database result in the tree.
   *
   * @param hier Result hierarchy.
   * @return Database
   */
  public static Database findDatabase(ResultHierarchy hier) {
    final List<Database> dbs = filterResults(hier, Database.class);
    return (!dbs.isEmpty()) ? dbs.get(0) : null;
  }

  /**
   * Recursively remove a result and its children.
   * 
   * @param child Result to remove
   */
  public static void removeRecursive(Object child) {
    final Hierarchy h = Metadata.of(child).hierarchy();
    for(It<Object> iter = h.iterParents(); iter.valid(); iter.advance()) {
      Metadata.of(iter.get()).hierarchy().removeChild(child);
    }
    for(It<Object> iter = h.iterChildren(); iter.valid(); iter.advance()) {
      removeRecursive(iter.get());
    }
  }
}
