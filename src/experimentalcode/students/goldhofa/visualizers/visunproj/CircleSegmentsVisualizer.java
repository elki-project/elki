package experimentalcode.students.goldhofa.visualizers.visunproj;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MouseEvent;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import experimentalcode.students.goldhofa.ClusteringComparison;
import experimentalcode.students.goldhofa.ClusteringComparisonResult;
import experimentalcode.students.goldhofa.Segment;
import experimentalcode.students.goldhofa.SegmentSelection;
import experimentalcode.students.goldhofa.Segments;
import experimentalcode.students.goldhofa.visualization.batikutil.CheckBox;
import experimentalcode.students.goldhofa.visualization.batikutil.CheckBoxListener;
import experimentalcode.students.goldhofa.visualization.batikutil.SwitchEvent;
import experimentalcode.students.goldhofa.visualization.batikutil.UnorderedList;
import experimentalcode.students.goldhofa.visualization.style.CSStylingPolicy;

/**
 * Visualizer to draw circle segments of clusterings
 * 
 * @author Sascha Goldhofer
 * @author Erich Schubert
 */
public class CircleSegmentsVisualizer extends AbstractVisualization implements ResultListener {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(CircleSegmentsVisualizer.class);

  /**
   * CircleSegments visualizer name
   */
  private static final String NAME = "CircleSegments";

  /** Minimum width (radian) of Segment */
  private final static double SEGMENT_MIN_ANGLE = 0.01;

  /** Gap (radian) between segments */
  private final static double SEGMENT_MIN_SEP_ANGLE = 0.005;

  /** Offset from center to first ring */
  private final static double RADIUS_INNER = 0.04 * StyleLibrary.SCALE;

  /** Margin between two rings */
  private final static double RADIUS_DISTANCE = 0.01 * StyleLibrary.SCALE;

  /** Radius of whole CircleSegments except selection border */
  private final static double RADIUS_OUTER = 0.46 * StyleLibrary.SCALE;

  /** Radius of highlight selection (outer ring) */
  private final static double RADIUS_SELECTION = 0.02 * StyleLibrary.SCALE;

  /**
   * Color coding of CircleSegments
   */
  private static final String BORDER_COLOR = "#FF0073";

  private static final String HOVER_SELECTION_COLOR = "#73ff00";

  /**
   * CSS class name for the clusterings.
   */
  private static final String CLUSTERID = "cluster";

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
   * First color for producing segment-cluster colors
   */
  public static final String STYLE_GRADIENT_FIRST = "segments.cluster.first";

  /**
   * Second color for producing segment-cluster colors
   */
  public static final String STYLE_GRADIENT_SECOND = "segments.cluster.second";

  /**
   * Comparison Result of {@link ClusteringComparison}
   */
  private ClusteringComparisonResult ccr;

  /**
   * Segmentation of Clusterings
   */
  public Segments segments;

  /**
   * Number of clusterings (rings)
   */
  private int clusterings;

  /**
   * 
   */
  private Element visLayer, ctrlLayer;

  protected CSSClass[] clusterClasses;

  public Map<Segment, List<Element>> segmentToElements = new HashMap<Segment, List<Element>>();

  /**
   * Segment selection manager
   */
  public SegmentSelection selection;

  /**
   * Show unclustered Pairs in CircleSegments
   */
  boolean showUnclusteredPairs = false;

  /**
   * Constructor
   */
  public CircleSegmentsVisualizer(VisualizationTask task) {
    super(task);
    ccr = task.getResult();
    // Listen for result changes (Selection changed)
    context.addResultListener(this);
  }

  public void showUnclusteredPairs(boolean show) {
    if(showUnclusteredPairs == show) {
      return;
    }
    showUnclusteredPairs = show;

    // store attributes & delete old visLayer
    Node parent = visLayer.getParentNode();
    parent.removeChild(visLayer);

    // add new node to draw
    visLayer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    // visLayer.setAttribute(SVGConstants.SVG_TRANSFORM_ATTRIBUTE,
    // attTranslate);
    parent.appendChild(visLayer);

    draw();

    selection.reselect();
  }

  /**
   * Create the segments
   */
  private void draw() {
    int refClustering = 0;
    int refSegment = Segment.UNCLUSTERED;
    double offsetAngle = 0.0;

    int numsegments = segments.getSegments().size();

    double angle_pair = (MathUtil.TWOPI - (SEGMENT_MIN_SEP_ANGLE * numsegments)) / segments.getPairCount(showUnclusteredPairs);
    final int pair_min_count = (int) Math.ceil(SEGMENT_MIN_ANGLE / angle_pair);

    // number of segments needed to be resized
    int cluster_min_count = 0;
    for(Segment segment : segments.getSegments()) {
      if(segment.getPairCount() <= pair_min_count) {
        cluster_min_count++;
      }
    }

    // update width of a pair
    angle_pair = (MathUtil.TWOPI - (SEGMENT_MIN_SEP_ANGLE * numsegments + cluster_min_count * SEGMENT_MIN_ANGLE)) / (segments.getPairCount(showUnclusteredPairs) - cluster_min_count);
    double radius_delta = (RADIUS_OUTER - RADIUS_INNER - clusterings * RADIUS_DISTANCE) / clusterings;
    double border_width = SEGMENT_MIN_SEP_ANGLE;

    this.segmentToElements.clear();

    for(final Segment id : segments.getSegments()) {
      long currentPairCount = id.getPairCount();

      // resize small segments if below minimum
      double alpha = SEGMENT_MIN_ANGLE;
      if(currentPairCount > pair_min_count) {
        alpha = angle_pair * currentPairCount;
      }

      // ITERATE OVER ALL SEGMENT-CLUSTERS

      ArrayList<Element> elems = new ArrayList<Element>(clusterings);
      segmentToElements.put(id, elems);
      // draw segment for every clustering

      for(int i = 0; i < clusterings; i++) {
        double currentRadius = i * (radius_delta + RADIUS_DISTANCE) + RADIUS_INNER;

        // Add border if the next segment is a different cluster in the
        // reference clustering
        if((refSegment != id.get(refClustering)) && refClustering == i) {
          Element border = SVGUtil.svgCircleSegment(svgp, 0, 0, offsetAngle - SEGMENT_MIN_SEP_ANGLE, border_width, currentRadius, RADIUS_OUTER - RADIUS_DISTANCE);
          border.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CLR_BORDER_CLASS);
          visLayer.appendChild(border);

          if(id.get(refClustering) == Segment.UNCLUSTERED) {
            refClustering = Math.min(refClustering + 1, clusterings - 1);
          }
          refSegment = id.get(refClustering);
        }

        int cluster = id.get(i);

        // create ring segment
        Element segment = SVGUtil.svgCircleSegment(svgp, 0, 0, offsetAngle, alpha, currentRadius, currentRadius + radius_delta);
        elems.add(segment);

        // MouseEvents on segment cluster
        EventListener listener = new SegmentListenerProxy(id, i);
        EventTarget targ = (EventTarget) segment;
        targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, listener, false);
        targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, listener, false);
        targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, listener, false);

        // Coloring based on clusterID
        if(cluster >= 0) {
          segment.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, clusterClasses[cluster].getName());
        }
        // if its an unpaired cluster set color to white
        else {
          segment.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CLR_UNPAIRED_CLASS);
        }

        visLayer.appendChild(segment);
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
      visLayer.appendChild(extension);

      // calculate angle for next segment
      offsetAngle += alpha + SEGMENT_MIN_SEP_ANGLE;
    }
  }

  @Override
  public void resultChanged(Result current) {
    super.resultChanged(current);
    // Redraw on style result changes.
    if(current == context.getStyleResult()) {
      // FIXME: use the usual redrawing function, to allow other tools to change
      // the segment selection?
      // synchronizedRedraw();
    }
  }

  @Override
  public void redraw() {
    this.segments = ccr.getSegments();
    this.clusterings = segments.getClusterings();
    // initialize css (needs clusterSize!)
    addCSSClasses(segments.getHighestClusterCount());

    // Setup scaling for canvas: 0 to StyleLibrary.SCALE (usually 100 to avoid a
    // Java drawing bug!)
    // Plus some margin
    String transform = SVGUtil.makeMarginTransform(task.width, task.height, StyleLibrary.SCALE, StyleLibrary.SCALE, 0.1) + "translate(" + (.5 * StyleLibrary.SCALE) + " " + (.5 * StyleLibrary.SCALE) + ")";
    this.layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    this.layer.setAttribute(SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    this.visLayer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    this.ctrlLayer = svgp.svgElement(SVGConstants.SVG_G_TAG);

    // create selection helper. SegmentSelection initializes and manages
    // CSStylingPolicy. Could completely replace SegmentSelection
    this.selection = new SegmentSelection(task, segments);

    // and create svg elements
    draw();

    //
    // Build Interface
    //
    CheckBox checkbox = new CheckBox(svgp, showUnclusteredPairs, "Show unclustered pairs");
    checkbox.addCheckBoxListener(new CheckBoxListener() {
      public void switched(SwitchEvent evt) {
        showUnclusteredPairs(evt.isOn());
      }
    });

    // list to store all elements
    UnorderedList info = new UnorderedList(svgp);

    // Add ring:clustering info
    Element clrInfo = getClusteringInfo();
    info.addItem(clrInfo, Integer.valueOf(clrInfo.getAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE)));
    // checkbox
    info.addItem(checkbox.asElement(), 20);

    ctrlLayer.setAttribute(SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "translate(" + (-.5 * StyleLibrary.SCALE) + " " + (-.5 * StyleLibrary.SCALE) + ") scale(0.12)");
    ctrlLayer.appendChild(info.asElement());

    layer.appendChild(visLayer);
    layer.appendChild(ctrlLayer);
  }

  /**
   * Define and add required CSS classes
   */
  protected void addCSSClasses(int maxClusterSize) {
    StyleLibrary style = context.getStyleLibrary();
    // TODO: use style library!

    // CLUSTER BORDER
    CSSClass cssReferenceBorder = new CSSClass(this.getClass(), CLR_BORDER_CLASS);
    cssReferenceBorder.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, BORDER_COLOR);
    svgp.addCSSClassOrLogError(cssReferenceBorder);

    // Note: !important is needed to override the regular color assignment

    // CLUSTER HOVER
    CSSClass cluster_hover = new CSSClass(this.getClass(), CLR_HOVER_CLASS);
    cluster_hover.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, HOVER_SELECTION_COLOR + " !important");
    cluster_hover.setStatement(SVGConstants.SVG_CURSOR_TAG, SVGConstants.SVG_POINTER_VALUE);
    svgp.addCSSClassOrLogError(cluster_hover);

    // Unpaired cluster segment
    CSSClass cluster_unpaired = new CSSClass(this.getClass(), CLR_UNPAIRED_CLASS);
    cluster_unpaired.setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.CSS_WHITE_VALUE);
    cluster_unpaired.setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.CSS_NONE_VALUE);
    svgp.addCSSClassOrLogError(cluster_unpaired);

    // create Color shades for clusters
    String firstcol = style.getColor(STYLE_GRADIENT_FIRST);
    String secondcol = style.getColor(STYLE_GRADIENT_SECOND);
    String[] clusterColorShades = makeGradient(maxClusterSize, new String[]{firstcol, secondcol});

    clusterClasses = new CSSClass[maxClusterSize];
    for(int i = 0; i < maxClusterSize; i++) {
      clusterClasses[i] = new CSSClass(this, CLUSTERID + "_" + (i + 1));
      clusterClasses[i].setStatement(SVGConstants.SVG_FILL_ATTRIBUTE, clusterColorShades[i]);
      clusterClasses[i].setStatement(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      svgp.addCSSClassOrLogError(clusterClasses[i]);
    }
  }

  protected void redrawSelection() {
    CSStylingPolicy policy = selection.policy;
    for(Entry<Segment, List<Element>> entry : segmentToElements.entrySet()) {
      Segment segment = entry.getKey();
      Element extension = entry.getValue().get(clusterings);
      if(segment.isUnpaired()) {
        if(selection.segmentLabels.containsKey(segment)) {
          SVGUtil.addCSSClass(extension, SEG_UNPAIRED_SELECTED_CLASS);
        }
        else {
          // Remove highlight
          SVGUtil.removeCSSClass(extension, SEG_UNPAIRED_SELECTED_CLASS);
        }
      }
      else {
        int idx = policy.getSelectedSegments().indexOf(segment);
        if(idx >= 0) {
          String color = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT).getColor(idx);
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
   * @return array of colors for css: "rgb(red, green, blue)"
   */
  protected static String[] makeGradient(int shades, String[] colors) {
    if(shades <= colors.length) {
      return colors;
    }

    // Convert SVG colors into AWT colors for math
    Color[] cols = new Color[colors.length];
    for (int i = 0; i < colors.length; i++) {
      cols[i] = SVGUtil.stringToColor(colors[i]);
      if (cols[i] == null) {
        throw new AbortException("Error parsing color: "+colors[i]);
      }
    }

    // Step size
    double increment = (cols.length - 1.) / shades;

    String[] colorShades = new String[shades];
        
    for(int s = 0; s < shades; s++) {
      final int ppos = Math.min((int) Math.floor(increment * s), cols.length);
      final int npos = Math.min((int) Math.ceil(increment * s), cols.length);
      if (ppos == npos) {
        colorShades[s] = colors[ppos];
      } else {
        Color prev = cols[ppos];
        Color next = cols[npos];
        final double mix = (increment * s - ppos) / (npos - ppos);
        final int r = (int) ((1 - mix) * prev.getRed() + mix * next.getRed());
        final int g = (int) ((1 - mix) * prev.getGreen() + mix * next.getGreen());
        final int b = (int) ((1 - mix) * prev.getBlue() + mix * next.getBlue());
        colorShades[s] = SVGUtil.colorToString(((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
      }
    }

    return colorShades;
  }

  protected Element getClusteringInfo() {
    Element thumbnail = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);

    // build thumbnail
    int startRadius = 4;
    int singleHeight = 12;
    int margin = 4;
    int radius = clusterings * (singleHeight + margin) + startRadius;

    SVGUtil.setAtt(thumbnail, SVGConstants.SVG_HEIGHT_ATTRIBUTE, radius);

    for(int i = 0; i < clusterings; i++) {
      double innerRadius = i * singleHeight + margin * i + startRadius;
      Element clr = SVGUtil.svgCircleSegment(svgp, radius - startRadius, radius - startRadius, Math.PI * 1.5, Math.PI * 0.5, innerRadius, innerRadius + singleHeight);
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
      if(logger.isDebugging()) {
        logger.debug("Hover on segment: " + segment + " unpaired: " + segment.isUnpaired());
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
        // unpaired
        // segment
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
      selection.deselectAllSegments();
    }
    selection.select(segment, ctrl);
    // update stylePolicy
    // update stylePolicy
    context.getStyleResult().setStylingPolicy(selection.policy);
    // fire changed event to trigger redraw
    context.getHierarchy().resultChanged(context.getStyleResult());
    // redraw
    redrawSelection();
  }

  /**
   * Proxy element to connect signals.
   * 
   * @author Erich Schubert
   */
  private class SegmentListenerProxy implements EventListener {
    /**
     * Mouse double click time window in milliseconds
     * 
     * TODO: does Batik have double click events?
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
        // Check Double Click
        boolean dblClick = false;
        long time = java.util.Calendar.getInstance().getTimeInMillis();
        if(time - lastClick <= EVT_DBLCLICK_DELAY) {
          dblClick = true;
        }
        lastClick = time;

        segmentClick(id, evt, dblClick);
      }
    }
  }

  /**
   * Factory for visualizers for a circle segment
   * 
   * @author Sascha Goldhofer
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses CircleSegmentsVisualizer oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new CircleSegmentsVisualizer(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      // If no comparison result found abort
      List<ClusteringComparisonResult> ccr = ResultUtil.filterResults(result, ClusteringComparisonResult.class);
      for(ClusteringComparisonResult ccResult : ccr) {
        // create task for visualization
        final VisualizationTask task = new VisualizationTask(NAME, ccResult, null, this);
        task.width = 2.0;
        task.height = 2.0;
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
        baseResult.getHierarchy().add(ccResult, task);
      }
    }
  };
}