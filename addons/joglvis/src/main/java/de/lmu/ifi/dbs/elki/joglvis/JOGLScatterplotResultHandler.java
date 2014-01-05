package de.lmu.ifi.dbs.elki.joglvis;

import java.awt.BorderLayout;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.joglvis.scatterplot.ScatterData;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;

public class JOGLScatterplotResultHandler implements ResultHandler {
  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    Database db = ResultUtil.findDatabase(newResult);
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
