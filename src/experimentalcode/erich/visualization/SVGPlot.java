package experimentalcode.erich.visualization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.SVGConstants;
import org.apache.fop.render.ps.EPSTranscoder;
import org.apache.fop.render.ps.PSTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

import experimentalcode.erich.scales.LinearScale;
import experimentalcode.erich.visualization.svg.SVGUtil;

/**
 * Base class for SVG plots. Provides some basic functionality such as element
 * creation, axis plotting, markers and number formatting for SVG.
 * 
 * @author Erich Schubert
 * 
 */
public class SVGPlot {
  /**
   * SVG document we plot to.
   */
  private SVGDocument document;

  /**
   * Root element of the document.
   */
  private Element root;

  /**
   * Definitions element of the document.
   */
  private Element defs;

  /**
   * Primary style information
   */
  private Element style;
  
  /**
   * Manage objects with an id.
   */
  private HashMap<String, Element> objWithId = new HashMap<String, Element>();

  /**
   * Create a new plotting document.
   */
  public SVGPlot() {
    super();
    // Get a DOMImplementation.
    DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
    DocumentType dt = domImpl.createDocumentType("svg", SVGConstants.SVG_PUBLIC_ID, SVGConstants.SVG_SYSTEM_ID);
    // Workaround: sometimes DocumentType doesn't work right, which
    // causes problems with
    // serialization...
    if(dt.getName() == null) {
      dt = null;
    }

    document = (SVGDocument) domImpl.createDocument(SVGConstants.SVG_NAMESPACE_URI, "svg", dt);

    root = document.getDocumentElement();
    // setup common SVG namespaces
    root.setAttribute("xmlns", SVGConstants.SVG_NAMESPACE_URI);
    root.setAttributeNS(SVGConstants.XMLNS_NAMESPACE_URI, SVGConstants.XMLNS_PREFIX + ":" + SVGConstants.XLINK_PREFIX, SVGConstants.XLINK_NAMESPACE_URI);

    // create element for SVG definitions
    defs = svgElement(root, "defs");

    // create element for Stylesheet information.
    style = svgElement(root, "style");
    SVGUtil.setAtt(style, "type", "text/css");
  }

  /**
   * Flag for axis label position. First char: right-hand or left-hand side of
   * line. Second char: text alignment
   * 
   */
  private enum ALIGNMENT {
    LL, RL, LC, RC, LR, RR
  }

  /**
   * Plot an axis with appropriate scales
   * 
   * @param parent Containing element
   * @param scale axis scale information
   * @param x1 starting coordinate
   * @param y1 starting coordinate
   * @param x2 ending coordinate
   * @param y2 ending coordinate
   * @param labels control whether labels are printed.
   * @param righthanded control whether to print labels on the right hand side or left hand side
   */
  public void drawAxis(Element parent, LinearScale scale, double x1, double y1, double x2, double y2, boolean labels, boolean righthanded) {
    Element line = svgElement(parent, "line");
    SVGUtil.setAtt(line, "x1", x1);
    SVGUtil.setAtt(line, "y1", -y1);
    SVGUtil.setAtt(line, "x2", x2);
    SVGUtil.setAtt(line, "y2", -y2);
    SVGUtil.setAtt(line, "style", "stroke:silver; stroke-width:0.2%;");

    double tx = x2 - x1;
    double ty = y2 - y1;
    // ticks are orthogonal
    double tw = ty * 0.01;
    double th = tx * 0.01;

    // choose where to print labels.
    ALIGNMENT pos = ALIGNMENT.LL;
    if(labels) {
      double angle = Math.atan2(-ty, tx);
      //System.err.println(tx + " " + (-ty) + " " + angle);
      if(angle < -2.6) { // -pi .. -2.6 = -180 .. -150
        pos = righthanded ? ALIGNMENT.RC : ALIGNMENT.LC;
      }
      else if(angle < -0.5) { // -2.3 .. -0.7 = -130 .. -40
        pos = righthanded ? ALIGNMENT.RL : ALIGNMENT.LR;
      }
      else if(angle < 0.5) { // -0.5 .. 0.5 = -30 .. 30
        pos = righthanded ? ALIGNMENT.RC : ALIGNMENT.LC;
      }
      else if(angle < 2.6) { // 0.5 .. 2.6 = 30 .. 150
        pos = righthanded ? ALIGNMENT.RR : ALIGNMENT.LL;
      }
      else { // 2.6 .. pi = 150 .. 180
        pos = righthanded ? ALIGNMENT.RC : ALIGNMENT.LC;
      }
    }
    // vertical text offset; align approximately with middle instead of
    // baseline.
    double textvoff = 0.007;

    // draw ticks on x axis
    for(double tick = scale.getMin(); tick <= scale.getMax(); tick += scale.getRes()) {
      Element tickline = svgElement(parent, "line");
      double x = x1 + tx * scale.getScaled(tick);
      double y = y1 + ty * scale.getScaled(tick);
      SVGUtil.setAtt(tickline, "x1", x - tw);
      SVGUtil.setAtt(tickline, "y1", -y - th);
      SVGUtil.setAtt(tickline, "x2", x + tw);
      SVGUtil.setAtt(tickline, "y2", -y + th);
      SVGUtil.setAtt(tickline, "style", "stroke:black; stroke-width:0.1%;");
      // draw labels
      if(labels) {
        Element text = svgElement(parent, "text");
        SVGUtil.setAtt(text, "style", "font-size: 0.2%");
        switch(pos){
        case LL:
        case LC:
        case LR:
          SVGUtil.setAtt(text, "x", x - tw * 2);
          SVGUtil.setAtt(text, "y", -y - th * 3 + textvoff);
          break;
        case RL:
        case RC:
        case RR:
          SVGUtil.setAtt(text, "x", x + tw * 2);
          SVGUtil.setAtt(text, "y", -y + th * 3 + textvoff);
        }
        switch(pos){
        case LL:
        case RL:
          SVGUtil.setAtt(text, "text-anchor", "start");
          break;
        case LC:
        case RC:
          SVGUtil.setAtt(text, "text-anchor", "middle");
          break;
        case LR:
        case RR:
          SVGUtil.setAtt(text, "text-anchor", "end");
          break;
        }
        text.setTextContent(scale.formatValue(tick));
      }
    }
  }

  /**
   * Create a SVG element in the SVG namespace. Non-static version.
   * 
   * @param parent parent node. May be null.
   * @param name node name
   * @return new SVG element.
   */
  public Element svgElement(Element parent, String name) {
    return SVGUtil.svgElement(document, parent, name);
  }

  /**
   * Create a SVG element in the SVG namespace. Non-static version.
   * 
   * @param name node name
   * @return new SVG element.
   */
  public Element svgElement(String name) {
    return SVGUtil.svgElement(document, null, name);
  }

  /**
   * Retrieve the SVG document.
   * 
   * @return resulting document.
   */
  public SVGDocument getDocument() {
    return document;
  }

  /**
   * Getter for definitions section
   * 
   * @return DOM element
   */
  public Element getRoot() {
    return root;
  }

  /**
   * Getter for definitions section
   * 
   * @return DOM element
   */
  public Element getDefs() {
    return defs;
  }

  /**
   * Getter for style element.
   * 
   * @return Stylesheet DOM element
   */
  public Element getStyle() {
    return style;
  }

  /**
   * Save document into a SVG file.
   * 
   * TODO: Handle embedded PNG images appropriately,
   * they might currently be stored in temp files, and then get lost.
   * 
   * @param file Output filename
   * @throws IOException On write errors
   * @throws TransformerFactoryConfigurationError 
   * @throws TransformerException 
   */
  public void saveAsSVG(File file) throws IOException, TransformerFactoryConfigurationError, TransformerException {
    OutputStream out = new FileOutputStream(file);
    // TODO embed linked images.
    javax.xml.transform.Result result = new StreamResult(out);
    // Use a transformer for pretty printing.
    Transformer xformer = TransformerFactory.newInstance().newTransformer();
    xformer.setOutputProperty(OutputKeys.INDENT, "yes");
    xformer.transform(new DOMSource(getDocument()), result);    
    out.flush();
    out.close();
  }

  /**
   * Transcode a document into a file using the given transcoder.
   * 
   * @param file Output file
   * @param transcoder Transcoder to use
   * @throws IOException On write errors
   * @throws TranscoderException On input/parsing errors
   */
  protected void transcode(File file, Transcoder transcoder) throws IOException, TranscoderException {
    TranscoderInput input = new TranscoderInput(getDocument());
    OutputStream out = new FileOutputStream(file);
    TranscoderOutput output = new TranscoderOutput(out);
    transcoder.transcode(input, output);
    out.flush();
    out.close();
  }

  /**
   * Transcode file to PDF.
   * 
   * @param file Output filename
   * @throws IOException On write errors
   * @throws TranscoderException On input/parsing errors.
   */
  public void saveAsPDF(File file) throws IOException, TranscoderException {
    transcode(file,new PDFTranscoder());
  }

  /**
   * Transcode file to PS.
   * 
   * @param file Output filename
   * @throws IOException On write errors
   * @throws TranscoderException On input/parsing errors.
   */
  public void saveAsPS(File file) throws IOException, TranscoderException {
    transcode(file,new PSTranscoder());
  }
  
  /**
   * Transcode file to EPS.
   * 
   * @param file Output filename
   * @throws IOException On write errors
   * @throws TranscoderException On input/parsing errors.
   */
  public void saveAsEPS(File file) throws IOException, TranscoderException {
    transcode(file,new EPSTranscoder());
  }

  /**
   * Transcode file to PNG.
   * 
   * @param file Output filename
   * @param width Width
   * @param height Height
   * @throws IOException On write errors
   * @throws TranscoderException On input/parsing errors.
   */
  public void saveAsPNG(File file, int width, int height) throws IOException, TranscoderException {
    PNGTranscoder t = new PNGTranscoder();
    t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(width));
    t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, new Float(height));
    transcode(file,t);
  }

  /**
   * Transcode file to JPEG.
   * 
   * @param file Output filename
   * @param width Width
   * @param height Height
   * @param quality JPEG quality setting, between 0.0 and 1.0
   * @throws IOException On write errors
   * @throws TranscoderException On input/parsing errors.
   */
  public void saveAsJPEG(File file, int width, int height, double quality) throws IOException, TranscoderException {
    JPEGTranscoder t = new JPEGTranscoder();
    t.addTranscodingHint(JPEGTranscoder.KEY_WIDTH, new Float(width));
    t.addTranscodingHint(JPEGTranscoder.KEY_HEIGHT, new Float(height));
    t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(quality));
    transcode(file,t);
  }

  /**
   * Transcode file to JPEG.
   * 
   * @param file Output filename
   * @param width Width
   * @param height Height
   * @throws IOException On write errors
   * @throws TranscoderException On input/parsing errors.
   */
  public void saveAsJPEG(File file, int width, int height) throws IOException, TranscoderException {
    saveAsJPEG(file,width,height,0.85);
  }
  
  /**
   * Add an object id.
   * 
   * @param id ID
   * @param obj Element
   */
  public void putIdElement(String id, Element obj) {
    objWithId.put(id, obj);
  }
  
  /**
   * Get an element by its id.
   * 
   * @param id ID
   * @return Element
   */
  public Element getIdElement(String id) {
    return objWithId.get(id);
  }
}
