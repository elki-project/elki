package experimentalcode.erich.visualization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.dom.util.DOMUtilities;
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
  private HashMap<String, WeakReference<Element>> objWithId = new HashMap<String, WeakReference<Element>>();

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
    defs = svgElement("defs");
    root.appendChild(defs);

    // create element for Stylesheet information.
    style = svgElement("style");
    SVGUtil.setAtt(style, "type", "text/css");
    root.appendChild(style);
  }

  /**
   * Create a SVG element in the SVG namespace. Non-static version.
   * 
   * @param parent parent node. May be null.
   * @param name node name
   * @return new SVG element.
   */
  @Deprecated
  public Element svgElement(Element parent, String name) {
    Element e = SVGUtil.svgElement(document, name);
    if(parent != null) {
      parent.appendChild(e);
    }
    return e;
  }

  /**
   * Create a SVG element in the SVG namespace. Non-static version.
   * 
   * @param name node name
   * @return new SVG element.
   */
  public Element svgElement(String name) {
    return SVGUtil.svgElement(document, name);
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
   * TODO: Handle embedded PNG images appropriately, they might currently be
   * stored in temp files, and then get lost.
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
    // Since the Transcoder is Batik-based, it will replace the rendering tree,
    // which would then break display. Thus we need to deep clone the document first.
    // -- found by Simon.
    SVGDocument doc = (SVGDocument) DOMUtilities.deepCloneDocument(getDocument(), getDocument().getImplementation());
    TranscoderInput input = new TranscoderInput(doc);
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
    transcode(file, new PDFTranscoder());
  }

  /**
   * Transcode file to PS.
   * 
   * @param file Output filename
   * @throws IOException On write errors
   * @throws TranscoderException On input/parsing errors.
   */
  public void saveAsPS(File file) throws IOException, TranscoderException {
    transcode(file, new PSTranscoder());
  }

  /**
   * Transcode file to EPS.
   * 
   * @param file Output filename
   * @throws IOException On write errors
   * @throws TranscoderException On input/parsing errors.
   */
  public void saveAsEPS(File file) throws IOException, TranscoderException {
    transcode(file, new EPSTranscoder());
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
    transcode(file, t);
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
    transcode(file, t);
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
    saveAsJPEG(file, width, height, 0.85);
  }

  /**
   * Add an object id.
   * 
   * @param id ID
   * @param obj Element
   */
  public void putIdElement(String id, Element obj) {
    objWithId.put(id, new WeakReference<Element>(obj));
  }

  /**
   * Get an element by its id.
   * 
   * @param id ID
   * @return Element
   */
  public Element getIdElement(String id) {
    WeakReference<Element> ref = objWithId.get(id);
    return (ref != null) ? ref.get() : null;
  }
}
