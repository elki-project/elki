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
package de.lmu.ifi.dbs.elki.visualization.svg;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.XMLAbstractTranscoder;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.FileUtil;
import de.lmu.ifi.dbs.elki.visualization.batikutil.CloneInlineImages;
import de.lmu.ifi.dbs.elki.visualization.batikutil.ThumbnailTranscoder;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;

/**
 * Base class for SVG plots. Provides some basic functionality such as element
 * creation, axis plotting, markers and number formatting for SVG.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @opt nodefillcolor LemonChiffon
 * @composed - - - CSSClassManager
 * @composed - - - UpdateRunner
 * @composed - - - SVGDocument
 * @navhas - contains - Element
 * @navhas - synchronizesWith - UpdateSynchronizer
 */
public class SVGPlot {
  /**
   * Default JPEG quality setting
   */
  public static final float DEFAULT_QUALITY = 0.85f;

  /**
   * Attribute to block export of element.
   */
  public static final String NO_EXPORT_ATTRIBUTE = "noexport";

  /**
   * Batik DOM implementation.
   */
  private static final DOMImplementation BATIK_DOM;

  /**
   * DOM implementations to try.
   */
  private static final String[] BATIK_DOMS = { //
      "org.apache.batik.anim.dom.SVGDOMImplementation", // Batik 1.8
      "org.apache.batik.dom.svg.SVGDOMImplementation", // Batik 1.7
      "com.sun.org.apache.xerces.internal.dom.DOMImplementationImpl", // Untested
  };

  // Locate a usable DOM implementation.
  static {
    DOMImplementation dom = null;
    for(String s : BATIK_DOMS) {
      try {
        Class<?> c = Class.forName(s);
        Method m = c.getDeclaredMethod("getDOMImplementation");
        DOMImplementation ret = DOMImplementation.class.cast(m.invoke(null));
        if(ret != null) {
          dom = ret;
          break;
        }
      }
      catch(Exception e) {
        continue;
      }
    }
    BATIK_DOM = dom;
  }

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
   * Manage objects with an id.
   */
  private HashMap<String, WeakReference<Element>> objWithId = new HashMap<>();

  /**
   * Registers changes of this SVGPlot.
   */
  private UpdateRunner runner = new UpdateRunner(this);

  /**
   * Flag whether Batik interactions should be disabled.
   */
  private boolean disableInteractions = false;

  /**
   * Create a new plotting document.
   */
  public SVGPlot() {
    super();
    // Get a DOMImplementation.
    DOMImplementation domImpl = getDomImpl();
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
  }

  /**
   * Get a suitable SVG DOM implementation from Batik 1.7 or 1.8.
   *
   * @return DOM implementation
   */
  public static DOMImplementation getDomImpl() {
    if(BATIK_DOM == null) {
      throw new AbortException("No usable Apache Batik SVG DOM could be located.");
    }
    return BATIK_DOM;
  }

  /**
   * Clean up the plot.
   */
  public void dispose() {
    runner.clear();
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
   * Create a SVG element in the SVG namespace. Non-static version.
   *
   * @param name node name
   * @param cssclass CSS class
   * @return new SVG element.
   */
  public Element svgElement(String name, String cssclass) {
    Element elem = SVGUtil.svgElement(document, name);
    if(cssclass != null) {
      elem.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, cssclass);
    }
    return elem;
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
   * Convert screen coordinates to element coordinates.
   *
   * @param tag Element to convert the coordinates for
   * @param evt Event object
   * @return Coordinates
   */
  public SVGPoint elementCoordinatesFromEvent(Element tag, Event evt) {
    return SVGUtil.elementCoordinatesFromEvent(document, tag, evt);
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
   * Getter for root element.
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
   * Convenience method to add a CSS class or log an error.
   *
   * @param cls CSS class to add.
   */
  public void addCSSClassOrLogError(CSSClass cls) {
    try {
      cssman.addClass(cls);
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception(e);
    }
  }

  /**
   * Update style element - invoke this appropriately after any change to the
   * CSS styles.
   */
  public void updateStyleElement() {
    // TODO: this should be sufficient - why does Batik occasionally not pick up
    // the changes unless we actually replace the style element itself?
    // cssman.updateStyleElement(document, style);
    Element newstyle = cssman.makeStyleElement(document);
    style.getParentNode().replaceChild(newstyle, style);
    style = newstyle;
  }

  /**
   * Save document into a SVG file.
   *
   * References PNG images from the temporary files will be inlined
   * automatically.
   *
   * @param file Output filename
   * @throws IOException On write errors
   * @throws TransformerFactoryConfigurationError Transformation error
   * @throws TransformerException Transformation error
   */
  public void saveAsSVG(File file) throws IOException, TransformerFactoryConfigurationError, TransformerException {
    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
    // TODO embed linked images.
    javax.xml.transform.Result result = new StreamResult(out);
    SVGDocument doc = cloneDocument();
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
    // Disable validation, performance is more important here (thumbnails!)
    transcoder.addTranscodingHint(XMLAbstractTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
    SVGDocument doc = cloneDocument();
    TranscoderInput input = new TranscoderInput(doc);
    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
    TranscoderOutput output = new TranscoderOutput(out);
    transcoder.transcode(input, output);
    out.flush();
    out.close();
  }

  /**
   * Clone the SVGPlot document for transcoding.
   *
   * This will usually be necessary for exporting the SVG document if it is
   * currently being displayed: otherwise, we break the Batik rendering trees.
   * (Discovered by Simon).
   *
   * @return cloned document
   */
  protected SVGDocument cloneDocument() {
    return (SVGDocument) new CloneInlineImages() {
      @Override
      public Node cloneNode(Document doc, Node eold) {
        // Skip elements with noexport attribute set
        if(eold instanceof Element) {
          Element eeold = (Element) eold;
          String vis = eeold.getAttribute(NO_EXPORT_ATTRIBUTE);
          if(vis != null && vis.length() > 0) {
            return null;
          }
        }
        return super.cloneNode(doc, eold);
      }
    }.cloneDocument(getDomImpl(), document);
  }

  /**
   * Transcode file to PDF.
   *
   * @param file Output filename
   * @throws IOException On write errors
   * @throws TranscoderException On input/parsing errors.
   * @throws ClassNotFoundException PDF transcoder not installed
   */
  public void saveAsPDF(File file) throws IOException, TranscoderException, ClassNotFoundException {
    try {
      Object t = Class.forName("org.apache.fop.svg.PDFTranscoder").newInstance();
      transcode(file, (Transcoder) t);
    }
    catch(InstantiationException | IllegalAccessException e) {
      throw new ClassNotFoundException("Could not instantiate PDF transcoder - is Apache FOP installed?", e);
    }
  }

  /**
   * Transcode file to PS.
   *
   * @param file Output filename
   * @throws IOException On write errors
   * @throws TranscoderException On input/parsing errors.
   * @throws ClassNotFoundException PS transcoder not installed
   */
  public void saveAsPS(File file) throws IOException, TranscoderException, ClassNotFoundException {
    try {
      Object t = Class.forName("org.apache.fop.render.ps.PSTranscoder").newInstance();
      transcode(file, (Transcoder) t);
    }
    catch(InstantiationException | IllegalAccessException e) {
      throw new ClassNotFoundException("Could not instantiate PS transcoder - is Apache FOP installed?", e);
    }
  }

  /**
   * Transcode file to EPS.
   *
   * @param file Output filename
   * @throws IOException On write errors
   * @throws TranscoderException On input/parsing errors.
   * @throws ClassNotFoundException EPS transcoder not installed
   */
  public void saveAsEPS(File file) throws IOException, TranscoderException, ClassNotFoundException {
    try {
      Object t = Class.forName("org.apache.fop.render.ps.EPSTranscoder").newInstance();
      transcode(file, (Transcoder) t);
    }
    catch(InstantiationException | IllegalAccessException e) {
      throw new ClassNotFoundException("Could not instantiate EPS transcoder - is Apache FOP installed?", e);
    }
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
  public void saveAsJPEG(File file, int width, int height, float quality) throws IOException, TranscoderException {
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
    saveAsJPEG(file, width, height, DEFAULT_QUALITY);
  }

  /**
   * Save a file trying to auto-guess the file type.
   *
   * @param file File name
   * @param width Width (for pixel formats)
   * @param height Height (for pixel formats)
   * @param quality Quality (for lossy compression)
   * @throws IOException on file write errors or unrecognized file extensions
   * @throws TranscoderException on transcoding errors
   * @throws TransformerFactoryConfigurationError on transcoding errors
   * @throws TransformerException on transcoding errors
   * @throws ClassNotFoundException when the transcoder was not installed
   */
  public void saveAsANY(File file, int width, int height, float quality) throws IOException, TranscoderException, TransformerFactoryConfigurationError, TransformerException, ClassNotFoundException {
    String extension = FileUtil.getFilenameExtension(file);
    if("svg".equals(extension)) {
      saveAsSVG(file);
    }
    else if("pdf".equals(extension)) {
      saveAsPDF(file);
    }
    else if("ps".equals(extension)) {
      saveAsPS(file);
    }
    else if("eps".equals(extension)) {
      saveAsEPS(file);
    }
    else if("png".equals(extension)) {
      saveAsPNG(file, width, height);
    }
    else if("jpg".equals(extension) || "jpeg".equals(extension)) {
      saveAsJPEG(file, width, height, quality);
    }
    else {
      throw new IOException("Unknown file extension: " + extension);
    }
  }

  /**
   * Convert the SVG to a thumbnail image.
   *
   * @param width Width of thumbnail
   * @param height Height of thumbnail
   * @return Buffered image
   */
  public BufferedImage makeAWTImage(int width, int height) throws TranscoderException {
    ThumbnailTranscoder t = new ThumbnailTranscoder();
    t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(width));
    t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, new Float(height));
    // Don't clone. Assume this is used safely.
    TranscoderInput input = new TranscoderInput(document);
    t.transcode(input, null);
    return t.getLastImage();
  }

  /**
   * Dump the SVG plot to a debug file.
   */
  public void dumpDebugFile() {
    try {
      File f = File.createTempFile("elki-debug", ".svg");
      f.deleteOnExit();
      this.saveAsSVG(f);
      LoggingUtil.warning("Saved debug file to: " + f.getAbsolutePath());
    }
    catch(Throwable err) {
      // Ignore.
    }
  }

  /**
   * Add an object id.
   *
   * @param id ID
   * @param obj Element
   */
  public void putIdElement(String id, Element obj) {
    objWithId.put(id, new WeakReference<>(obj));
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

  /**
   * Schedule an update.
   *
   * @param runnable Runnable to schedule
   */
  public void scheduleUpdate(Runnable runnable) {
    runner.invokeLater(runnable);
  }

  /**
   * Assign an update synchronizer.
   *
   * @param sync Update synchronizer
   */
  public void synchronizeWith(UpdateSynchronizer sync) {
    runner.synchronizeWith(sync);
  }

  /**
   * Detach from synchronization.
   *
   * @param sync Update synchronizer to detach from.
   */
  public void unsynchronizeWith(UpdateSynchronizer sync) {
    runner.unsynchronizeWith(sync);
  }

  /**
   * Get Batik disable default interactions flag.
   *
   * @return true when Batik default interactions are disabled
   */
  public boolean getDisableInteractions() {
    return disableInteractions;
  }

  /**
   * Disable Batik predefined interactions.
   *
   * @param disable Flag
   */
  public void setDisableInteractions(boolean disable) {
    disableInteractions = disable;
  }
}