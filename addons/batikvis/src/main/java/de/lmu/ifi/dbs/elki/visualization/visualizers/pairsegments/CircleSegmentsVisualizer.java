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
package de.lmu.ifi.dbs.elki.visualization.visualizers.pairsegments;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MouseEvent;

import de.lmu.ifi.dbs.elki.evaluation.clustering.pairsegments.Segment;
import de.lmu.ifi.dbs.elki.evaluation.clustering.pairsegments.Segments;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGCheckbox;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualizer to draw circle segments of clusterings and enable interactive
 * selection of segments. For "empty" segments, all related segments are
 * selected instead, to visualize the differences.
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Sascha Goldhofer, Hans-Peter Kriegel, Erich Schubert,
 * Arthur Zimek<br>
 * Evaluation of Clusterings – Metrics and Visual Support<br>
 * Proc. 28th International Conference on Data Engineering (ICDE 2012)
 *
 * @author Sascha Goldhofer
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @opt nodefillcolor LemonChiffon
 * @stereotype factory
 * @navassoc - create - Instance
 */
@Reference(authors = "Elke Achtert, Sascha Goldhofer, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek", //
    title = "Evaluation of Clusterings - Metrics and Visual Support", //
    booktitle = "Proc. 28th International Conference on Data Engineering (ICDE 2012)", //
    url = "https://doi.org/10.1109/ICDE.2012.128", //
    bibkey = "DBLP:conf/icde/AchtertGKSZ12")
public class CircleSegmentsVisualizer implements VisFactory {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(CircleSegmentsVisualizer.class);

  /**
   * CircleSegments visualizer name
   */
  private static final String NAME = "CircleSegments";

  /**
   * Constructor
   */
  public CircleSegmentsVisualizer() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findNewResults(context, start).filter(Segments.class).forEach(segmentResult -> {
      SegmentsStylingPolicy policy;
      It<SegmentsStylingPolicy> it = VisualizationTree.findVis(context, segmentResult).filter(SegmentsStylingPolicy.class);
      if(it.valid()) {
        policy = it.get();
      }
      else {
        policy = new SegmentsStylingPolicy(segmentResult);
        context.addVis(segmentResult, policy);
      }
      context.addVis(segmentResult, new VisualizationTask(this, NAME, policy, null) //
          .requestSize(2.0, 2.0).level(VisualizationTask.LEVEL_INTERACTIVE) //
          .with(UpdateFlag.ON_STYLEPOLICY));
    });
  }

  /**
   * Instance
   *
   * @author Sascha Goldhofer
   * @author Erich Schubert
   *
   * @assoc - - - Segments
   * @has - - - SegmentsStylingPolicy
   *
   */
  public class Instance extends AbstractVisualization implements ResultListener {
    /** Minimum width (radian) of Segment */
    private static final double SEGMENT_MIN_ANGLE = 0.01;

    /** Gap (radian) between segments */
    private static final double SEGMENT_MIN_SEP_ANGLE = 0.005;

    /** Offset from center to first ring */
    private static final double RADIUS_INNER = 0.04 * StyleLibrary.SCALE;

    /** Margin between two rings */
    private static final double RADIUS_DISTANCE = 0.01 * StyleLibrary.SCALE;

    /** Radius of whole CircleSegments except selection border */
    private static final double RADIUS_OUTER = 0.47 * StyleLibrary.SCALE;

    /** Radius of highlight selection (outer ring) */
    private static final double RADIUS_SELECTION = 0.02 * StyleLibrary.SCALE;

    /**
     * CSS class name for the clusterings.
     */
    private static final String CLR_CLUSTER_CLASS_PREFIX = "clusterSegment";

    /**
     * CSS border class of a cluster
     */
    public static final String CLR_BORDER_CLASS = "clusterBorder";

    /**
     * CSS hover class for clusters of hovered segment
     */
    public static final String CLR_UNPAIRED_CLASS = "clusterUnpaired";

    /**
     * CSS hover class of a segment cluster
     */
    public static final String CLR_HOVER_CLASS = "clusterHover";

    /**
     * CSS class of selected Segment
     */
    public static final String SEG_UNPAIRED_SELECTED_CLASS = "unpairedSegmentSelected";

    /**
     * Style prefix
     */
    public static final String STYLE = "segments";

    /**
     * Style for border lines
     */
    public static final String STYLE_BORDER = STYLE + ".border";

    /**
     * Style for hover effect
     */
    public static final String STYLE_HOVER = STYLE + ".hover";

    /**
     * First color for producing segment-cluster colors
     */
    public static final String STYLE_GRADIENT_FIRST = STYLE + ".cluster.first";

    /**
     * Second color for producing segment-cluster colors
     */
    public static final String STYLE_GRADIENT_SECOND = STYLE + ".cluster.second";

    /**
     * Segmentation of Clusterings
     */
    protected final Segments segments;

    /**
     * The two main layers
     */
    private Element visLayer, ctrlLayer;

    /**
     * Map to connect segments to their visual elements
     */
    public Map<Segment, List<Element>> segmentToElements = new HashMap<>();

    /**
     * Show unclustered Pairs in CircleSegments
     */
    boolean showUnclusteredPairs = false;

    /**
     * Styling policy
     */
    protected final SegmentsStylingPolicy policy;

    /**
     * Flag to disallow an incremental redraw
     */
    private boolean noIncrementalRedraw = true;

    /**
     * Constructor
     *
     * @param context Visualizer context
     * @param task Task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     */
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height) {
      super(context, task, plot, width, height);
      policy = task.getResult();
      segments = policy.segments;
      // FIXME: handle this more generally.
      policy.setStyleLibrary(context.getStyleLibrary());
      addListeners();
    }

    public void toggleUnclusteredPairs(boolean show) {
      noIncrementalRedraw = true;
      showUnclusteredPairs = show;
      svgp.requestRedraw(this.task, this);
    }

    @Override
    public void resultChanged(Result current) {
      super.resultChanged(current);
      if(current == context.getStylingPolicy()) {
        // When switching to a different policy, unhighlight segments.
        if(context.getStylingPolicy() != policy) {
          policy.deselectAllSegments();
        }
      }
    }

    @Override
    public void incrementalRedraw() {
      if(noIncrementalRedraw) {
        super.incrementalRedraw();
      }
      else {
        redrawSelection();
      }
    }

    @Override
    public void fullRedraw() {
      LOG.debug("Full redraw");
      noIncrementalRedraw = false; // Done that.

      // initialize css (needs clusterSize!)
      addCSSClasses(segments.getHighestClusterCount());

      layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
      visLayer = svgp.svgElement(SVGConstants.SVG_G_TAG);
      // Setup scaling for canvas: 0 to StyleLibrary.SCALE (usually 100 to avoid
      // a Java drawing bug!)
      String transform = SVGUtil.makeMarginTransform(getWidth(), getHeight(), StyleLibrary.SCALE, StyleLibrary.SCALE, 0) + "  translate(" + (StyleLibrary.SCALE * .5) + " " + (StyleLibrary.SCALE * .5) + ")";
      visLayer.setAttribute(SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
      ctrlLayer = svgp.svgElement(SVGConstants.SVG_G_TAG);

      // and create svg elements
      drawSegments();

      // Build Interface
      SVGCheckbox checkbox = new SVGCheckbox(showUnclusteredPairs, "Show unclustered pairs");
      checkbox.addCheckBoxListener((e) -> toggleUnclusteredPairs(((SVGCheckbox) e.getSource()).isChecked()));

      // Add ring:clustering info
      Element clrInfo = drawClusteringInfo();
      Element c = checkbox.renderCheckBox(svgp, 1., 5. + ParseUtil.parseDouble(clrInfo.getAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE)), 11);
      ctrlLayer.appendChild(clrInfo);
      ctrlLayer.appendChild(c);

      ctrlLayer.setAttribute(SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "scale(" + (0.25 / StyleLibrary.SCALE) + ")");

      layer.appendChild(visLayer);
      layer.appendChild(ctrlLayer);
    }

    /**
     * Define and add required CSS classes
     */
    protected void addCSSClasses(int maxClusterSize) {
      StyleLibrary style = context.getStyleLibrary();

      // Cluster separation lines
      CSSClass cssReferenceBorder = new CSSClass(this.getClass(), CLR_BORDER_CLASS);
      cssReferenceBorder.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, style.getColor(STYLE_BORDER));
      svgp.addCSSClassOrLogError(cssReferenceBorder);

      // Hover effect for clusters
      CSSClass cluster_hover = new CSSClass(this.getClass(), CLR_HOVER_CLASS);
      // Note: !important is needed to override the regular color assignment
      cluster_hover.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, style.getColor(STYLE_HOVER) + " !important");
      cluster_hover.setStatement(SVGConstants.SVG_CURSOR_TAG, SVGConstants.SVG_POINTER_VALUE);
      svgp.addCSSClassOrLogError(cluster_hover);

      // Unpaired cluster segment
      CSSClass cluster_unpaired = new CSSClass(this.getClass(), CLR_UNPAIRED_CLASS);
      cluster_unpaired.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, style.getBackgroundColor(STYLE));
      cluster_unpaired.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.CSS_NONE_VALUE);
      svgp.addCSSClassOrLogError(cluster_unpaired);

      // Selected unpaired cluster segment
      CSSClass cluster_unpaired_s = new CSSClass(this.getClass(), SEG_UNPAIRED_SELECTED_CLASS);
      cluster_unpaired_s.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, style.getColor(STYLE_HOVER) + " !important");
      svgp.addCSSClassOrLogError(cluster_unpaired_s);

      // create Color shades for clusters
      String firstcol = style.getColor(STYLE_GRADIENT_FIRST);
      String secondcol = style.getColor(STYLE_GRADIENT_SECOND);
      String[] clusterColorShades = makeGradient(maxClusterSize, new String[] { firstcol, secondcol });

      for(int i = 0; i < maxClusterSize; i++) {
        CSSClass clusterClasses = new CSSClass(CircleSegmentsVisualizer.class, CLR_CLUSTER_CLASS_PREFIX + "_" + i);
        clusterClasses.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, clusterColorShades[i]);
        clusterClasses.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
        svgp.addCSSClassOrLogError(clusterClasses);
      }
    }

    /**
     * Create the segments
     */
    private void drawSegments() {
      final StyleLibrary style = context.getStyleLibrary();
      final int clusterings = segments.getClusterings();

      // Reinitialize
      this.segmentToElements.clear();

      double angle_pair = (MathUtil.TWOPI - (SEGMENT_MIN_SEP_ANGLE * segments.size())) / segments.getPairCount(showUnclusteredPairs);
      final int pair_min_count = (int) Math.ceil(SEGMENT_MIN_ANGLE / angle_pair);

      // number of segments needed to be resized
      int cluster_min_count = 0;
      for(Segment segment : segments) {
        if(segment.getPairCount() <= pair_min_count) {
          cluster_min_count++;
        }
      }

      // update width of a pair
      angle_pair = (MathUtil.TWOPI - (SEGMENT_MIN_SEP_ANGLE * segments.size() + cluster_min_count * SEGMENT_MIN_ANGLE)) / (segments.getPairCount(showUnclusteredPairs) - cluster_min_count);
      double radius_delta = (RADIUS_OUTER - RADIUS_INNER - clusterings * RADIUS_DISTANCE) / clusterings;
      double border_width = SEGMENT_MIN_SEP_ANGLE;

      int refClustering = 0, refSegment = Segment.UNCLUSTERED;
      double offsetAngle = 0.0;

      for(final Segment segment : segments) {
        long currentPairCount = segment.getPairCount();

        // resize small segments if below minimum
        double alpha = SEGMENT_MIN_ANGLE;
        if(currentPairCount > pair_min_count) {
          alpha = angle_pair * currentPairCount;
        }

        // ITERATE OVER ALL SEGMENT-CLUSTERS

        ArrayList<Element> elems = new ArrayList<>(clusterings);
        segmentToElements.put(segment, elems);
        // draw segment for every clustering

        for(int i = 0; i < clusterings; i++) {
          double currentRadius = i * (radius_delta + RADIUS_DISTANCE) + RADIUS_INNER;

          // Add border if the next segment is a different cluster in the
          // reference clustering
          if((refSegment != segment.get(refClustering)) && refClustering == i) {
            Element border = SVGUtil.svgCircleSegment(svgp, 0, 0, offsetAngle - SEGMENT_MIN_SEP_ANGLE, border_width, currentRadius, RADIUS_OUTER - RADIUS_DISTANCE);
            border.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CLR_BORDER_CLASS);
            visLayer.appendChild(border);

            if(segment.get(refClustering) == Segment.UNCLUSTERED) {
              refClustering = Math.min(refClustering + 1, clusterings - 1);
            }
            refSegment = segment.get(refClustering);
          }

          int cluster = segment.get(i);

          // create ring segment
          Element segelement = SVGUtil.svgCircleSegment(svgp, 0, 0, offsetAngle, alpha, currentRadius, currentRadius + radius_delta);
          elems.add(segelement);

          // MouseEvents on segment cluster
          EventListener listener = new SegmentListenerProxy(segment, i);
          EventTarget targ = (EventTarget) segelement;
          targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, listener, false);
          targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, listener, false);
          targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, listener, false);

          // Coloring based on clusterID
          if(cluster >= 0) {
            segelement.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CLR_CLUSTER_CLASS_PREFIX + "_" + cluster);
          }
          // if its an unpaired cluster set color to white
          else {
            segelement.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CLR_UNPAIRED_CLASS);
          }

          visLayer.appendChild(segelement);
        }

        //
        // Add a extended strip for each segment to emphasis selection
        // (easier to track thin segments and their color coding and
        // differentiates them from cluster border lines)
        //

        double currentRadius = clusterings * (radius_delta + RADIUS_DISTANCE) + RADIUS_INNER;
        Element extension = SVGUtil.svgCircleSegment(svgp, 0, 0, offsetAngle, alpha, currentRadius, currentRadius + RADIUS_SELECTION);
        extension.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CLR_UNPAIRED_CLASS);
        elems.add(extension);

        if(segment.isUnpaired()) {
          if(policy.isSelected(segment)) {
            SVGUtil.addCSSClass(extension, SEG_UNPAIRED_SELECTED_CLASS);
          }
          else {
            // Remove highlight
            SVGUtil.removeCSSClass(extension, SEG_UNPAIRED_SELECTED_CLASS);
          }
        }
        else {
          int idx = policy.indexOfSegment(segment);
          if(idx >= 0) {
            String color = style.getColorSet(StyleLibrary.PLOT).getColor(idx);
            extension.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, SVGConstants.CSS_FILL_PROPERTY + ":" + color);
          }
          else {
            // Remove styling
            extension.removeAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE);
          }
        }

        visLayer.appendChild(extension);

        // calculate angle for next segment
        offsetAngle += alpha + SEGMENT_MIN_SEP_ANGLE;
      }
    }

    private void redrawSelection() {
      final StyleLibrary style = context.getStyleLibrary();
      LOG.debug("Updating selection only.");
      for(Entry<Segment, List<Element>> entry : segmentToElements.entrySet()) {
        Segment segment = entry.getKey();
        // The selection marker is the extra element in the list
        Element extension = entry.getValue().get(segments.getClusterings());
        if(segment.isUnpaired()) {
          if(policy.isSelected(segment)) {
            SVGUtil.addCSSClass(extension, SEG_UNPAIRED_SELECTED_CLASS);
          }
          else {
            // Remove highlight
            SVGUtil.removeCSSClass(extension, SEG_UNPAIRED_SELECTED_CLASS);
          }
        }
        else {
          int idx = policy.indexOfSegment(segment);
          if(idx >= 0) {
            String color = style.getColorSet(StyleLibrary.PLOT).getColor(idx);
            extension.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, SVGConstants.CSS_FILL_PROPERTY + ":" + color);
          }
          else {
            // Remove styling
            extension.removeAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE);
          }
        }
      }
    }

    /**
     * Creates a gradient over a set of colors
     *
     * @param shades number of colors in the gradient
     * @param colors colors for the gradient
     * @return array of colors for CSS
     */
    protected String[] makeGradient(int shades, String[] colors) {
      if(shades <= colors.length) {
        return colors;
      }

      // Convert SVG colors into AWT colors for math
      Color[] cols = new Color[colors.length];
      for(int i = 0; i < colors.length; i++) {
        cols[i] = SVGUtil.stringToColor(colors[i]);
        if(cols[i] == null) {
          throw new AbortException("Error parsing color: " + colors[i]);
        }
      }

      // Step size
      double increment = (cols.length - 1.) / shades;
      String[] colorShades = new String[shades];

      for(int s = 0; s < shades; s++) {
        final int ppos = Math.min((int) Math.floor(increment * s), cols.length - 1);
        final int npos = Math.min((int) Math.ceil(increment * s), cols.length - 1);
        if(ppos == npos) {
          colorShades[s] = colors[ppos];
        }
        else {
          Color prev = cols[ppos], next = cols[npos];
          final double mix = (increment * s - ppos) / (npos - ppos);
          final int r = (int) ((1 - mix) * prev.getRed() + mix * next.getRed());
          final int g = (int) ((1 - mix) * prev.getGreen() + mix * next.getGreen());
          final int b = (int) ((1 - mix) * prev.getBlue() + mix * next.getBlue());
          colorShades[s] = SVGUtil.colorToString(((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
        }
      }

      return colorShades;
    }

    protected Element drawClusteringInfo() {
      Element thumbnail = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);

      // build thumbnail
      int startRadius = 4, singleHeight = 12, margin = 4;
      int radius = segments.getClusterings() * (singleHeight + margin) + startRadius;

      SVGUtil.setAtt(thumbnail, SVGConstants.SVG_HEIGHT_ATTRIBUTE, radius);

      for(int i = 0; i < segments.getClusterings(); i++) {
        double innerRadius = i * singleHeight + margin * i + startRadius;
        Element clr = SVGUtil.svgCircleSegment(svgp, radius - startRadius, radius - startRadius, Math.PI * 1.5, Math.PI * 0.5, innerRadius, innerRadius + singleHeight);
        // FIXME: Use StyleLibrary
        clr.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#d4e4f1");
        clr.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#a0a0a0");
        clr.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, "1.0");

        String labelText = segments.getClusteringDescription(i);
        Element label = svgp.svgText(radius + startRadius, radius - innerRadius - startRadius, labelText);
        thumbnail.appendChild(label);
        thumbnail.appendChild(clr);
      }

      return thumbnail;
    }

    protected void segmentHover(Segment segment, int ringid, boolean active) {
      if(active) {
        // abort if this are the unclustered pairs
        if(segment.isNone()) {
          return;
        }
        if(LOG.isDebugging()) {
          LOG.debug("Hover on segment: " + segment + " unpaired: " + segment.isUnpaired());
        }

        if(!segment.isUnpaired()) {
          //
          // STANDARD CLUSTER SEGMENT
          // highlight all ring segments in this clustering and this cluster
          //
          // highlight all corresponding ring Segments
          for(Entry<Segment, List<Element>> entry : segmentToElements.entrySet()) {
            Segment other = entry.getKey();
            // Same cluster in same clustering?
            if(other.get(ringid) != segment.get(ringid)) {
              continue;
            }
            Element ringSegment = entry.getValue().get(ringid);
            SVGUtil.addCSSClass(ringSegment, CLR_HOVER_CLASS);
          }
        }
        else {
          //
          // UNPAIRED SEGMENT
          // highlight all ring segments in this clustering responsible for
          // unpaired segment
          //
          // get the paired segments corresponding to the unpaired segment
          List<Segment> paired = segments.getPairedSegments(segment);

          for(Segment other : paired) {
            Element ringSegment = segmentToElements.get(other).get(ringid);
            SVGUtil.addCSSClass(ringSegment, CLR_HOVER_CLASS);
          }
        }
      }
      else {
        for(List<Element> elems : segmentToElements.values()) {
          for(Element current : elems) {
            SVGUtil.removeCSSClass(current, CLR_HOVER_CLASS);
          }
        }
      }
    }

    protected void segmentClick(Segment segment, Event evt, boolean dblClick) {
      MouseEvent mouse = (MouseEvent) evt;

      // CTRL (add) pressed?
      boolean ctrl = false;
      if(mouse.getCtrlKey()) {
        ctrl = true;
      }

      // Unselect others on double click
      if(dblClick) {
        policy.deselectAllSegments();
      }
      policy.select(segment, ctrl);
      // update stylePolicy
      context.setStylingPolicy(policy);
    }

    /**
     * Proxy element to connect signals.
     *
     * @author Erich Schubert
     */
    private class SegmentListenerProxy implements EventListener {
      /**
       * Mouse double click time window in milliseconds
       */
      public static final int EVT_DBLCLICK_DELAY = 350;

      /**
       * Segment we are attached to
       */
      private Segment id;

      /**
       * Segment ring we are
       */
      private int ringid;

      /**
       * For detecting double clicks.
       */
      private long lastClick = 0;

      /**
       * Constructor.
       *
       * @param id Segment id
       * @param ringid Ring id
       */
      public SegmentListenerProxy(Segment id, int ringid) {
        super();
        this.id = id;
        this.ringid = ringid;
      }

      @Override
      public void handleEvent(Event evt) {
        if(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE.equals(evt.getType())) {
          segmentHover(id, ringid, true);
        }
        if(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE.equals(evt.getType())) {
          segmentHover(id, ringid, false);
        }
        if(SVGConstants.SVG_CLICK_EVENT_TYPE.equals(evt.getType())) {
          long time = java.util.Calendar.getInstance().getTimeInMillis();
          boolean dblClick = time - lastClick <= EVT_DBLCLICK_DELAY;
          lastClick = time;

          segmentClick(id, evt, dblClick);
        }
      }
    }
  }
}
