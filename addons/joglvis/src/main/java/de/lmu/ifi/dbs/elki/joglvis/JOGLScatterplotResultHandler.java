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
package de.lmu.ifi.dbs.elki.joglvis;

import java.awt.BorderLayout;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.joglvis.scatterplot.ScatterData;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;

/**
 * Find results to visualize using OpenGL Scatterplots.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class JOGLScatterplotResultHandler implements ResultHandler {
  @Override
  public void processNewResult(ResultHierarchy baseResult, Result newResult) {
    Database db = ResultUtil.findDatabase(baseResult);
    if(db == null) {
      return;
    }
    // Build OpenGL data loader:
    ScatterData data = null;
    for(Relation<?> rel : db.getRelations()) {
      if(data == null) {
        data = new ScatterData(rel.getDBIDs());
      }
      data.addRelation(rel);
    }
    if(data == null) {
      return;
    }

    final JFrame jframe = new JFrame("OpenGL Scatterplot");
    jframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final ScatterPlot3DVisualization plot = new ScatterPlot3DVisualization(data);
    GLCanvas glcanvas = new GLCanvas(new GLCapabilities(GLProfile.getDefault()));
    glcanvas.addGLEventListener(plot);
    jframe.getContentPane().add(glcanvas, BorderLayout.CENTER);
    jframe.setSize(640, 480);
    jframe.setVisible(true);
    plot.start(glcanvas);
  }
}
