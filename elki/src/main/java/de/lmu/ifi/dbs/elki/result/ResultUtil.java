package de.lmu.ifi.dbs.elki.result;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelOrAllInOneClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;

/**
 * Utilities for handling result objects
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @apiviz.uses Result oneway - - filters
 */
public class ResultUtil {
  /**
   * Collect all Annotation results from a Result
   *
   * @param r Result
   * @return List of all annotation results
   */
  public static List<Relation<?>> getRelations(Result r) {
    if(r instanceof Relation<?>) {
      List<Relation<?>> anns = new ArrayList<>(1);
      anns.add((Relation<?>) r);
      return anns;
    }
    if(r instanceof HierarchicalResult) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, filterResults(((HierarchicalResult) r).getHierarchy(), r, Relation.class));
    }
    return Collections.emptyList();
  }

  /**
   * Collect all ordering results from a Result
   *
   * @param r Result
   * @return List of ordering results
   */
  public static List<OrderingResult> getOrderingResults(Result r) {
    if(r instanceof OrderingResult) {
      List<OrderingResult> ors = new ArrayList<>(1);
      ors.add((OrderingResult) r);
      return ors;
    }
    if(r instanceof HierarchicalResult) {
      return filterResults(((HierarchicalResult) r).getHierarchy(), r, OrderingResult.class);
    }
    return Collections.emptyList();
  }

  /**
   * Collect all clustering results from a Result
   *
   * @param r Result
   * @return List of clustering results
   */
  public static List<Clustering<? extends Model>> getClusteringResults(Result r) {
    if(r instanceof Clustering<?>) {
      List<Clustering<?>> crs = new ArrayList<>(1);
      crs.add((Clustering<?>) r);
      return crs;
    }
    if(r instanceof HierarchicalResult) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, filterResults(((HierarchicalResult) r).getHierarchy(), r, Clustering.class));
    }
    return Collections.emptyList();
  }

  /**
   * Collect all collection results from a Result
   *
   * @param r Result
   * @return List of collection results
   */
  public static List<CollectionResult<?>> getCollectionResults(Result r) {
    if(r instanceof CollectionResult<?>) {
      List<CollectionResult<?>> crs = new ArrayList<>(1);
      crs.add((CollectionResult<?>) r);
      return crs;
    }
    if(r instanceof HierarchicalResult) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, filterResults(((HierarchicalResult) r).getHierarchy(), r, CollectionResult.class));
    }
    return Collections.emptyList();
  }

  /**
   * Return all Iterable results
   *
   * @param r Result
   * @return List of iterable results
   */
  public static List<IterableResult<?>> getIterableResults(Result r) {
    if(r instanceof IterableResult<?>) {
      List<IterableResult<?>> irs = new ArrayList<>(1);
      irs.add((IterableResult<?>) r);
      return irs;
    }
    if(r instanceof HierarchicalResult) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, filterResults(((HierarchicalResult) r).getHierarchy(), r, IterableResult.class));
    }
    return Collections.emptyList();
  }

  /**
   * Collect all outlier results from a Result
   *
   * @param r Result
   * @return List of outlier results
   */
  public static List<OutlierResult> getOutlierResults(Result r) {
    if(r instanceof OutlierResult) {
      List<OutlierResult> ors = new ArrayList<>(1);
      ors.add((OutlierResult) r);
      return ors;
    }
    if(r instanceof HierarchicalResult) {
      return filterResults(((HierarchicalResult) r).getHierarchy(), r, OutlierResult.class);
    }
    return Collections.emptyList();
  }

  /**
   * Collect all settings results from a Result
   *
   * @param r Result
   * @return List of settings results
   */
  public static List<SettingsResult> getSettingsResults(Result r) {
    if(r instanceof SettingsResult) {
      List<SettingsResult> ors = new ArrayList<>(1);
      ors.add((SettingsResult) r);
      return ors;
    }
    if(r instanceof HierarchicalResult) {
      return filterResults(((HierarchicalResult) r).getHierarchy(), r, SettingsResult.class);
    }
    return Collections.emptyList();
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
  // We can't ensure that restrictionClass matches C exactly.
  @SuppressWarnings("unchecked")
  public static <C extends Result> ArrayList<C> filterResults(ResultHierarchy hier, Result r, Class<? super C> restrictionClass) {
    ArrayList<C> res = new ArrayList<>();
    if(restrictionClass.isInstance(r)) {
      res.add((C) restrictionClass.cast(r));
    }
    for(Hierarchy.Iter<Result> iter = hier.iterDescendants(r); iter.valid(); iter.advance()) {
      Result result = iter.get();
      if(restrictionClass.isInstance(result)) {
        res.add((C) restrictionClass.cast(result));
      }
    }
    return res;
  }

  /**
   * Return only results of the given restriction class
   *
   * @param <C> Class type
   * @param hier Result hierarchy
   * @param restrictionClass Class restriction
   * @return filtered results list
   */
  // We can't ensure that restrictionClass matches C exactly.
  @SuppressWarnings("unchecked")
  public static <C extends Result> ArrayList<C> filterResults(ResultHierarchy hier, Class<? super C> restrictionClass) {
    ArrayList<C> res = new ArrayList<>();
    for(Hierarchy.Iter<Result> iter = hier.iterAll(); iter.valid(); iter.advance()) {
      Result result = iter.get();
      if(restrictionClass.isInstance(result)) {
        res.add((C) restrictionClass.cast(result));
      }
    }
    return res;
  }

  /**
   * Ensure that the result contains at least one Clustering.
   *
   * @param db Database to process
   * @param result result
   */
  public static void ensureClusteringResult(final Database db, final Result result) {
    Collection<Clustering<?>> clusterings = ResultUtil.filterResults(db.getHierarchy(), result, Clustering.class);
    if(clusterings.size() == 0) {
      ClusteringAlgorithm<Clustering<Model>> split = new ByLabelOrAllInOneClustering();
      Clustering<Model> c = split.run(db);
      addChildResult(db, c);
    }
  }

  /**
   * Ensure that there also is a selection container object.
   *
   * @param db Database
   * @return selection result
   */
  public static SelectionResult ensureSelectionResult(final Database db) {
    List<SelectionResult> selections = ResultUtil.filterResults(db.getHierarchy(), db, SelectionResult.class);
    if(!selections.isEmpty()) {
      return selections.get(0);
    }
    SelectionResult sel = new SelectionResult();
    addChildResult(db, sel);
    return sel;
  }

  /**
   * Get the sampling result attached to a relation
   *
   * @param rel Relation
   * @return Sampling result.
   */
  public static SamplingResult getSamplingResult(final Relation<?> rel) {
    Collection<SamplingResult> selections = ResultUtil.filterResults(rel.getHierarchy(), rel, SamplingResult.class);
    if(selections.size() == 0) {
      final SamplingResult newsam = new SamplingResult(rel);
      addChildResult(rel, newsam);
      return newsam;
    }
    return selections.iterator().next();
  }

  /**
   * Get (or create) a scales result for a relation.
   *
   * @param rel Relation
   * @return associated scales result
   */
  public static ScalesResult getScalesResult(final Relation<? extends SpatialComparable> rel) {
    Collection<ScalesResult> scas = ResultUtil.filterResults(rel.getHierarchy(), rel, ScalesResult.class);
    if(scas.size() == 0) {
      final ScalesResult newsca = new ScalesResult(rel);
      addChildResult(rel, newsca);
      return newsca;
    }
    return scas.iterator().next();
  }

  /**
   * Add a child result.
   *
   * @param parent Parent
   * @param child Child
   */
  public static void addChildResult(HierarchicalResult parent, Result child) {
    parent.getHierarchy().add(parent, child);
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
   * @param hierarchy Result hierarchy
   * @param child Result to remove
   */
  public static void removeRecursive(ResultHierarchy hierarchy, Result child) {
    for(Hierarchy.Iter<Result> iter = hierarchy.iterParents(child); iter.valid(); iter.advance()) {
      hierarchy.remove(iter.get(), child);
    }
    for(Hierarchy.Iter<Result> iter = hierarchy.iterChildren(child); iter.valid(); iter.advance()) {
      removeRecursive(hierarchy, iter.get());
    }
  }
}
