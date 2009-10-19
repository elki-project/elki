package de.lmu.ifi.dbs.elki.visualization;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.batik.swing.svg.AbstractJSVGComponent;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.batikutil.JSVGSynchronizedCanvas;
import de.lmu.ifi.dbs.elki.visualization.batikutil.LazyCanvasResizer;
import de.lmu.ifi.dbs.elki.visualization.batikutil.NodeReplacer;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.savedialog.SVGSaveDialog;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * Visualize a ROC curve.
 * 
 * Use as {@code -outputhandler} for {@link de.lmu.ifi.dbs.elki.evaluation.ComputeROCCurve}
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 */
// TODO: maybe add a way to process clustering results as well?
public class ResultROCCurveVisualizer<O extends DatabaseObject> extends AbstractParameterizable implements ResultHandler<O, MultiResult> {
  public ResultROCCurveVisualizer() {
    super();
  }

  @Override
  public void processResult(@SuppressWarnings("unused") Database<O> db, MultiResult result) throws IllegalStateException {
    IterableResult<Pair<Double,Double>> curve = findCurveResult(result);
    if (curve != null) {
      (new ROCWindow(curve)).run();
    } else {
      throw new AbortException(this.getClass().getName()+" could not find a ROC curve to visualize.");
    }
  }

  @SuppressWarnings("unchecked")
  private IterableResult<Pair<Double,Double>> findCurveResult(MultiResult result) {
    List<IterableResult<?>> iterables = ResultUtil.getIterableResults(result);
    for(IterableResult<?> iterable : iterables) {
      Iterator<?> iterator = iterable.iterator();
      if(iterator.hasNext()) {
        Object o = iterator.next();
        if(o instanceof Pair) {
          Pair<?, ?> p = (Pair<?, ?>) o;
          if(p.getFirst() != null && p.getFirst() instanceof Double && p.getSecond() != null && p.getSecond() instanceof Double) {
            return (IterableResult<Pair<Double,Double>>) iterable;
          }
        }
      }
    }
    return null;
  }

  @Override
  public void setNormalization(@SuppressWarnings("unused") Normalization<O> normalization) {
    // Nothing to do here.
  }

  @Override
  public String shortDescription() {
    return "Visualize the resulting ROC curve.";
  }

  class ROCWindow extends AbstractLoggable {
    /**
     * SVG graph object ID (for replacing)
     */
    private static final String SERIESID = "series";

    // CSS class for the frame
    private static final String FRAMEID = "frame";

    // The frame.
    protected JFrame frame = new JFrame("ROC curve (ELKI)");

    // The "Quit" button, to close the application.
    protected JButton quitButton = new JButton("Quit");

    // The "Export" button, to save the image
    protected JButton saveButton = new JButton("Export");

    // The SVG canvas.
    protected JSVGSynchronizedCanvas svgCanvas = new JSVGSynchronizedCanvas();

    // The plot
    SVGPlot plot;

    // Viewport
    Element viewport;

    // Canvas scaling ratio
    protected double ratio;

    // Curve
    protected Iterable<Pair<Double, Double>> curve;

    public ROCWindow(Iterable<Pair<Double, Double>> curve) {
      super(false);
      this.curve = curve;

      // Create a panel and add the button, status label and the SVG canvas.
      final JPanel bigpanel = new JPanel(new BorderLayout());

      // button panel
      JPanel buttonPanel = new JPanel(new BorderLayout());
      buttonPanel.add(BorderLayout.WEST, saveButton);
      buttonPanel.add(BorderLayout.EAST, quitButton);
      bigpanel.add(BorderLayout.NORTH, buttonPanel);
      bigpanel.add(BorderLayout.CENTER, svgCanvas);

      frame.getContentPane().add(bigpanel);

      saveButton.addActionListener(new ActionListener() {
        public void actionPerformed(@SuppressWarnings("unused") ActionEvent ae) {
          SVGSaveDialog.showSaveDialog(plot, 512, 512);
        }
      });

      quitButton.addActionListener(new ActionListener() {
        public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
          frame.setVisible(false);
          frame.dispose();
        }
      });

      // close handler
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      // display
      frame.setSize(600, 600);

      // resize listener
      LazyCanvasResizer listener = new LazyCanvasResizer(frame) {
        @Override
        public void executeResize(double newratio) {
          ratio = newratio;
          updateSize();
          updateFrame();
          updateCurve();
        }
      };
      ratio = listener.getActiveRatio();
      frame.addComponentListener(listener);
    }

    public void updateSize() {
      SVGUtil.setAtt(plot.getRoot(), SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + ratio + " 1");
      SVGUtil.setAtt(viewport, SVGConstants.SVG_WIDTH_ATTRIBUTE, ratio);
      SVGUtil.setAtt(viewport, SVGConstants.SVG_HEIGHT_ATTRIBUTE, "1");
      SVGUtil.setAtt(viewport, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "-0.05 -0.05 " + (ratio + 0.1) + " 1.1");
    }

    public void run() {
      plot = new SVGPlot();
      viewport = plot.svgElement(SVGConstants.SVG_SVG_TAG);
      plot.getRoot().appendChild(viewport);

      try {
        CSSClass csscls = new CSSClass(this, SERIESID);
        csscls.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.2%");
        csscls.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, "red");
        csscls.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
        plot.getCSSClassManager().addClass(csscls);
        CSSClass frmcls = new CSSClass(this, FRAMEID);
        frmcls.setStatement(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "0.2%");
        frmcls.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, "silver");
        frmcls.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
        plot.getCSSClassManager().addClass(frmcls);
      }
      catch(CSSNamingConflict e) {
        logger.exception(e);
      }
      plot.updateStyleElement();

      // insert a dummy for the frame
      Element frame = plot.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.setAtt(frame, SVGConstants.SVG_ID_ATTRIBUTE, FRAMEID);
      viewport.appendChild(frame);
      plot.putIdElement(FRAMEID, frame);
      
      // insert a dummy for the data series
      Element egroup = plot.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.setAtt(egroup, SVGConstants.SVG_ID_ATTRIBUTE, SERIESID);
      viewport.appendChild(egroup);
      plot.putIdElement(SERIESID, egroup);

      updateSize();
      updateFrame();
      updateCurve();

      svgCanvas.setDocumentState(AbstractJSVGComponent.ALWAYS_DYNAMIC);
      svgCanvas.setPlot(plot);

      this.frame.setVisible(true);
    }

    protected void updateCurve() {
      // prepare replacement tag.
      Element newe = plot.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.setAtt(newe, SVGConstants.SVG_ID_ATTRIBUTE, SERIESID);

      SVGPath path = new SVGPath();
      for(Pair<Double, Double> pair : curve) {
        path.drawTo(ratio * pair.getFirst(), 1.0 - pair.getSecond());
      }
      Element line = path.makeElement(plot);
      line.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, SERIESID);
      newe.appendChild(line);
      plot.scheduleUpdate(new NodeReplacer(newe, plot, SERIESID));
    }
    
    public void updateFrame() {
      // prepare replacement tag.
      Element newe = plot.svgRect(0, 0, ratio, 1);
      SVGUtil.setAtt(newe, SVGConstants.SVG_ID_ATTRIBUTE, FRAMEID);
      SVGUtil.setAtt(newe, SVGConstants.SVG_CLASS_ATTRIBUTE, FRAMEID);
      plot.scheduleUpdate(new NodeReplacer(newe, plot, FRAMEID));
    }
  }  
}
