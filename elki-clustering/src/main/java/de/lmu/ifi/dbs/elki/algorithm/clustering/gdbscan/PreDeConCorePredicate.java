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
package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.PreDeConNeighborPredicate.PreDeConModel;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.PreDeCon;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * The PreDeCon core point predicate -- having at least minpts. neighbors, and a
 * maximum preference dimensionality of lambda.
 * <p>
 * Reference:
 * <p>
 * Christian Böhm, Karin Kailing, Hans-Peter Kriegel, Peer Kröger<br>
 * Density Connected Clustering with Local Subspace Preferences.<br>
 * Proc. 4th IEEE Int. Conf. on Data Mining (ICDM'04)
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - Instance
 */
@Reference(authors = "Christian Böhm, Karin Kailing, Hans-Peter Kriegel, Peer Kröger", //
    title = "Density Connected Clustering with Local Subspace Preferences", //
    booktitle = "Proc. 4th IEEE Int. Conf. on Data Mining (ICDM'04)", //
    url = "https://doi.org/10.1109/ICDM.2004.10087", //
    bibkey = "DBLP:conf/icdm/BohmKKK04")
public class PreDeConCorePredicate implements CorePredicate<PreDeConModel> {
  /**
   * The PreDeCon settings class.
   */
  protected PreDeCon.Settings settings;

  /**
   * Default constructor.
   * 
   * @param settings PreDeCon settings
   */
  public PreDeConCorePredicate(PreDeCon.Settings settings) {
    super();
    this.settings = settings;
  }

  @Override
  public Instance instantiate(Database database) {
    return new Instance(settings);
  }

  @Override
  public boolean acceptsType(SimpleTypeInformation<? extends PreDeConModel> type) {
    return (type.getRestrictionClass().isAssignableFrom(PreDeConModel.class));
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   */
  public static class Instance implements CorePredicate.Instance<PreDeConModel> {
    /**
     * The PreDeCon settings class.
     */
    protected PreDeCon.Settings settings;

    /**
     * Constructor for this predicate.
     * 
     * @param settings PreDeCon settings
     */
    public Instance(PreDeCon.Settings settings) {
      super();
      this.settings = settings;
    }

    @Override
    public boolean isCorePoint(DBIDRef point, PreDeConModel model) {
      return (model.pdim <= settings.lambda) && (model.ids.size() >= settings.minpts);
    }
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * The PreDeCon settings class.
     */
    protected PreDeCon.Settings settings;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      settings = config.tryInstantiate(PreDeCon.Settings.class);
    }

    @Override
    protected PreDeConCorePredicate makeInstance() {
      return new PreDeConCorePredicate(settings);
    }
  }
}
