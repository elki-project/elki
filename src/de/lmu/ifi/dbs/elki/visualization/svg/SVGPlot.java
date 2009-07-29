package de.lmu.ifi.dbs.elki.visualization.svg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
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
import org.apache.batik.svggen.SVGSyntax;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.Base64EncoderStream;
import org.apache.batik.util.SVGConstants;
import org.apache.fop.render.ps.EPSTranscoder;
import org.apache.fop.render.ps.PSTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.xml.XMLNodeListIterator;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.colors.PropertiesBasedColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager;

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
   * CSS class manager
   */
  private CSSClassManager cssman;
  
  /**
   * Default color library
   */
  private ColorLibrary colors;

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
    DocumentType dt = domImpl.createDocumentType(SVGConstants.SVG_SVG_TAG, SVGConstants.SVG_PUBLIC_ID, SVGConstants.SVG_SYSTEM_ID);
    // Workaround: sometimes DocumentType doesn't work right, which
    // causes problems with
    // serialization...
    if(dt.getName() == null) {
      dt = null;
    }

    document = (SVGDocument) domImpl.createDocument(SVGConstants.SVG_NAMESPACE_URI, SVGConstants.SVG_SVG_TAG, dt);

    root = document.getDocumentElement();
    // setup common SVG namespaces
    root.setAttribute(SVGConstants.XMLNS_PREFIX, SVGConstants.SVG_NAMESPACE_URI);
    root.setAttributeNS(SVGConstants.XMLNS_NAMESPACE_URI, SVGConstants.XMLNS_PREFIX + ":" + SVGConstants.XLINK_PREFIX, SVGConstants.XLINK_NAMESPACE_URI);

    // create element for SVG definitions
    defs = svgElement(SVGConstants.SVG_DEFS_TAG);
    root.appendChild(defs);

    // create element for Stylesheet information.
    style = SVGUtil.makeStyleElement(document);
    root.appendChild(style);

    // create a CSS class manager.
    cssman = new CSSClassManager();
    
    // create a default color library
    colors = new PropertiesBasedColorLibrary();
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
   * Create a SVG rectangle
   * 
   * @param x X coordinate
   * @param y Y coordinate
   * @param w Width
   * @param h Height
   * @return new element
   */
  public Element svgRect(double x, double y, double w, double h) {
    return SVGUtil.svgRect(document, x, y, w, h);
  }
  
  /**
   * Create a SVG circle
   * 
   * @param cx center X
   * @param cy center Y
   * @param r radius
   * @return new element
   */
  public Element svgCircle(double cx, double cy, double r) {
    return SVGUtil.svgCircle(document, cx, cy, r);
  }
  
  /**
   * Create a SVG line element
   * 
   * @param x1 first point x
   * @param y1 first point y
   * @param x2 second point x
   * @param y2 second point y
   * @return new element
   */
  public Element svgLine(double x1, double y1, double x2, double y2) {
    return SVGUtil.svgLine(document, x1, y1, x2, y2);
  }

  /**
   * Create a SVG text element.
   * 
   * @param x first point x
   * @param y first point y
   * @param text Content of text element.
   * @return New text element.
   */
  public Element svgText(double x, double y, String text) {
    return SVGUtil.svgText(document, x, y, text);
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
   * @return stylesheet DOM element
   * @deprecated Contents will be overwritten by CSS class manager!
   */
  @Deprecated
  public Element getStyle() {
    return style;
  }

  /**
   * Get the plots CSS class manager.
   * 
   * Note that you need to invoke {@link #updateStyleElement()} to make changes
   * take effect.
   * 
   * @return CSS class manager.
   */
  public CSSClassManager getCSSClassManager() {
    return cssman;
  }

  /**
   * @return The color library
   */
  protected ColorLibrary getColorLibrary() {
    return colors;
  }

  /**
   * @param colors new color library
   */
  protected void setColorLibrary(ColorLibrary colors) {
    // TODO: update existing content, e.g. background color?
    this.colors = colors;
  }

  /**
   * Update style element - invoke this appropriately after any change to the
   * CSS styles.
   */
  public void updateStyleElement() {
    // TODO: this should be sufficient - why does Batik occasionally not pick up the 
    // changes unless we actually replace the style element itself?
    //cssman.updateStyleElement(document, style);
    Element newstyle = cssman.makeStyleElement(document);
    style.getParentNode().replaceChild(newstyle, style);
    style = newstyle;
  }

  /**
   * Save document into a SVG file.
   * 
   * References PNG images from the temporary files will be inlined automatically.
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
    // deep clone document
    SVGDocument doc = (SVGDocument) DOMUtilities.deepCloneDocument(getDocument(), getDocument().getImplementation());
    NodeList imgs = doc.getElementsByTagNameNS(SVGConstants.SVG_NAMESPACE_URI, SVGConstants.SVG_IMAGE_TAG);
    final String tmpurl = new File(System.getProperty("java.io.tmpdir") + File.separator).toURI().toString();
    for(Node img : new XMLNodeListIterator(imgs)) {
      if(img instanceof Element) {
        try {
          Element i = (Element) img;
          String href = i.getAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_ATTRIBUTE);
          if(href.startsWith(tmpurl) && href.endsWith(".png")) {
            // need to convert the image into an inline image.
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Base64EncoderStream encoder = new Base64EncoderStream(os);
            File in = new File(new URI(href));
            FileInputStream instream = new FileInputStream(in);
            byte[] buf = new byte[4096];
            while(true) {
              int read = instream.read(buf, 0, buf.length);
              if (read <= 0) { break; }
              encoder.write(buf, 0, read);
            }
            instream.close();
            encoder.close();
            // replace HREF with inlined image data.
            i.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_ATTRIBUTE, SVGSyntax.DATA_PROTOCOL_PNG_PREFIX + os.toString());
          }
        }
        catch(URISyntaxException e) {
          LoggingUtil.warning("Error in embedding PNG image.");
        }
      }
    }
    // Use a transformer for pretty printing
    Transformer xformer = TransformerFactory.newInstance().newTransformer();
    xformer.setOutputProperty(OutputKeys.INDENT, "yes");
    xformer.transform(new DOMSource(doc), result);
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
    // which would then break display. Thus we need to deep clone the document
    // first.
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
  
  /**
   * Get all used DOM Ids in this plot.
   * 
   * @return Collection of DOM element IDs.
   */
  protected Collection<String> getAllIds() {
    return objWithId.keySet();
  }
}
