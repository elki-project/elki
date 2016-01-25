package de.lmu.ifi.dbs.elki.algorithm.clustering.meta;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.parser.CSVReaderFormat;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.TokenizedReader;
import de.lmu.ifi.dbs.elki.utilities.io.Tokenizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Read an external clustering result from a file, such as produced by
 * {@link de.lmu.ifi.dbs.elki.result.ClusteringVectorDumper}.
 *
 * The input format of this parser is text-based:
 *
 * <pre>
 * # Optional comment
 * 1 1 1 2 2 2 -1 Example label
 * </pre>
 *
 * Where non-negative numbers are cluster assignments, negative numbers are
 * considered noise clusters.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @apiviz.composedOf CSVReaderFormat
 * @apiviz.has Clustering
 */
@Description("Load clustering results from an external file. "//
+ "Each line is expected to consists of one clustering, one integer per point "//
+ "and an (optional) non-numeric label.")
public class ExternalClustering extends AbstractAlgorithm<Clustering<? extends Model>>implements ClusteringAlgorithm<Clustering<? extends Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ExternalClustering.class);

  /**
   * The comment character.
   */
  public static final String COMMENT = "#";

  /**
   * The file to be reparsed.
   */
  private File file;

  /**
   * Constructor.
   *
   * @param file File to load
   */
  public ExternalClustering(File file) {
    super();
    this.file = file;
  }

  /**
   * Run the algorithm.
   *
   * @param database Database to use
   * @return Result
   */
  @Override
  public Clustering<? extends Model> run(Database database) {
    Clustering<? extends Model> m = null;
    try (InputStream in = FileUtil.tryGzipInput(new FileInputStream(file)); //
        TokenizedReader reader = CSVReaderFormat.DEFAULT_FORMAT.makeReader()) {
      Tokenizer tokenizer = reader.getTokenizer();
      reader.reset(in);
      TIntArrayList assignment = new TIntArrayList(database.getRelation(TypeUtil.DBID).size());
      ArrayList<String> name = new ArrayList<>();
      line: while(reader.nextLineExceptComments()) {
        for(/* initialized by nextLineExceptComments */; tokenizer.valid(); tokenizer.advance()) {
          try {
            assignment.add((int) tokenizer.getLongBase10());
          }
          catch(NumberFormatException e) {
            name.add(tokenizer.getSubstring());
          }
        }
        if(LOG.isDebuggingFinest()) {
          LOG.debugFinest("Read " + assignment.size() + " assignments and " + name.size() + " labels.");
        }
        for(Relation<?> r : database.getRelations()) {
          if(r.size() == assignment.size()) {
            attachToRelation(database, r, assignment, name);
            assignment.clear();
            name.clear();
            continue line;
          }
        }
        throw new AbortException("No relation found to match with clustering of size " + assignment.size());
      }
    }
    catch(IOException e) {
      throw new AbortException("Could not load outlier scores: " + e.getMessage() + " when loading " + file, e);
    }
    return m;
  }

  /**
   * Build a clustering from the file result.
   *
   * @param database Database
   * @param r Result to attach to
   * @param assignment Cluster assignment
   * @param name Name
   */
  private void attachToRelation(Database database, Relation<?> r, TIntArrayList assignment, ArrayList<String> name) {
    DBIDs ids = r.getDBIDs();
    if(!(ids instanceof ArrayDBIDs)) {
      throw new AbortException("External clusterings can only be used with static DBIDs.");
    }
    TIntIntMap sizes = new TIntIntHashMap();
    for(TIntIterator it = assignment.iterator(); it.hasNext();) {
      sizes.adjustOrPutValue(it.next(), 1, 1);
    }
    TIntObjectHashMap<ArrayModifiableDBIDs> cids = new TIntObjectHashMap<>(sizes.size());
    for(TIntIntIterator it = sizes.iterator(); it.hasNext();) {
      it.advance();
      cids.put(it.key(), DBIDUtil.newArray(it.value()));
    }
    {
      DBIDArrayIter it = ((ArrayDBIDs) ids).iter();
      for(int i = 0; i < assignment.size(); i++) {
        cids.get(assignment.get(i)).add(it.seek(i));
      }
    }
    String nam = FormatUtil.format(name, " ");
    String snam = nam.toLowerCase().replace(' ', '-');
    Clustering<ClusterModel> result = new Clustering<>(nam, snam);
    for(TIntObjectIterator<ArrayModifiableDBIDs> it = cids.iterator(); it.hasNext();) {
      it.advance();
      boolean noise = it.key() < 0;
      result.addToplevelCluster(new Cluster<>(it.value(), noise, ClusterModel.CLUSTER));
    }
    database.getHierarchy().add(r, result);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY);
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter that specifies the name of the file to be re-parsed.
     * <p>
     * Key: {@code -externalcluster.file}
     * </p>
     */
    public static final OptionID FILE_ID = new OptionID("externalcluster.file", "The file name containing the (external) cluster vector.");

    /**
     * The file to be reparsed
     */
    private File file;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      FileParameter fileP = new FileParameter(FILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(fileP)) {
        file = fileP.getValue();
      }
    }

    @Override
    protected ExternalClustering makeInstance() {
      return new ExternalClustering(file);
    }
  }
}