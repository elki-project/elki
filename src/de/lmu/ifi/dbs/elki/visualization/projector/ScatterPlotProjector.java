package de.lmu.ifi.dbs.elki.visualization.projector;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.AffineTransformation;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.visualization.projections.AffineProjection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.projections.Simple2D;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;

/**
 * ScatterPlotProjector is responsible for producing a set of scatterplot
 * visualizations.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
// FIXME: re-add column labels
public class ScatterPlotProjector<V extends NumberVector<?, ?>> extends AbstractHierarchicalResult implements Projector {
  /**
   * Relation we project
   */
  Relation<V> rel;

  /**
   * Database dimensionality
   */
  int dmax;

  /**
   * Axis scales
   */
  LinearScale[] scales;

  /**
   * Constructor.
   * 
   * @param rel Relation
   * @param maxdim Maximum dimension to use
   */
  public ScatterPlotProjector(Relation<V> rel, int maxdim) {
    super();
    this.rel = rel;
    this.dmax = maxdim;
    this.scales = Scales.calcScales(rel);
    assert (maxdim <= DatabaseUtil.dimensionality(rel)) : "Requested dimensionality larger than data dimensionality?!?";
  }

  @Override
  public double[] getShape() {
    return new double[] { 0, 0, dmax - 1, dmax - 1 };
  }

  @Override
  public Collection<LayoutObject> arrange() {
    List<LayoutObject> layout = new ArrayList<LayoutObject>(dmax - 1 * dmax / 2 + 2 * dmax + 1);

    for(int d1 = 1; d1 <= dmax; d1++) {
      for(int d2 = d1 + 1; d2 <= dmax; d2++) {
        Projection2D proj = new Simple2D(scales, d1, d2);
        layout.add(new LayoutObject(d1 - 1, d2 - 2, 1., 1., proj));
      }
    }
    if(dmax >= 3) {
      AffineTransformation p = AffineProjection.axisProjection(DatabaseUtil.dimensionality(rel), 1, 2);
      p.addRotation(0, 2, Math.PI / 180 * -10.);
      p.addRotation(1, 2, Math.PI / 180 * 15.);
      // Wanna try 4d? go ahead:
      // p.addRotation(0, 3, Math.PI / 180 * -20.);
      // p.addRotation(1, 3, Math.PI / 180 * 30.);
      final double sizeh = Math.ceil((dmax - 1) / 2.0);
      Projection2D proj = new AffineProjection(scales, p);
      layout.add(new LayoutObject(Math.ceil((dmax - 1) / 2.0), 0.0, sizeh, sizeh, proj));
    }
    return layout;
  }

  @Override
  public String getLongName() {
    return "Scatterplot";
  }

  @Override
  public String getShortName() {
    return "scatterplot";
  }

  /**
   * The relation we project.
   * 
   * @return Relation
   */
  public Relation<V> getRelation() {
    return rel;
  }
}