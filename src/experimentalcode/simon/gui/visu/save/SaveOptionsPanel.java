/**
 * 
 */
package experimentalcode.simon.gui.visu.save;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/** A component (JPanel) which can be displayed in the save dialog to 
 * show additional options when saving as JPEG or PNG.
 * @author simon
 *
 */
public class SaveOptionsPanel extends JPanel implements ItemListener, ChangeListener, ActionListener {

	/** The fileChooser on which this panel is installed. */
	private JFileChooser fc;

	/** The width of the exported image (if exported to JPEG or PNG). Default is 1024. */
	private int width = 1024;
	/** The height of the exported image (if exported to JPEG or PNG). Default is 768. */
	private int height = 768;
	
	
	private JPanel mainPanel;
	/** Shows quality info when saving as JPEG. */
	private JPanel qualPanel;
	/** If saving as JPEG/PNG show width/height infos here. */
	private JPanel sizePanel;

	private JCheckBox cbLockAspectRatio;
	//private JCheckBox cbAutoHeight;
	
	private JSpinner spinnerWidth;
	private JSpinner spinnerHeight;
	private JSpinner spinnerQual;
	
	private SpinnerNumberModel modelWidth;
	private SpinnerNumberModel modelHeight;
	private SpinnerNumberModel modelQuality;
	
	private JButton resetRatioButton;
	
	//private JTextField widthField;
	//private JTextField heightField;

	private JComboBox cbFormat;


	String[] formats = { "png", "svg", "jpeg", "pdf", "ps", "eps" };

	double ratio = 16.0/10.0;

	public SaveOptionsPanel(JFileChooser fc, int width, int height) {
		this.width = width;
		this.height = height;
		this.ratio = (double)width/(double)height;
		this.fc = fc;
		initUI();
	}
	
	/** Creates the components. */
	private void initUI() {
		
		//setSize(200, 250);
		
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

		sizePanel = new JPanel();
		sizePanel.setLayout(new BoxLayout(sizePanel, BoxLayout.Y_AXIS));
		sizePanel.setBorder(BorderFactory.createTitledBorder("Size options"));
		
		qualPanel = new JPanel();
		qualPanel.setLayout(new BoxLayout(qualPanel, BoxLayout.Y_AXIS));
		qualPanel.setBorder(BorderFactory.createTitledBorder("Quality:"));
		//qualPanel.add(new JLabel("Quality:"));
		modelQuality = new SpinnerNumberModel(1.0, //initial value
                0.1, //min
                1.0, //max
                0.1);  //step
		spinnerQual = new JSpinner(modelQuality);
		spinnerQual.addChangeListener(this);
		qualPanel.add(spinnerQual);
		
		// FORMATS:
		JLabel l = new JLabel("Choose format:");
		mainPanel.add(l);

		cbFormat = new JComboBox(formats);
		cbFormat.setSelectedIndex(0);
		cbFormat.addItemListener(this);
		mainPanel.add(cbFormat);

		// SIZE:
		JPanel widthPanel = new JPanel();
		JLabel widthLabel = new JLabel("Width:");
		//widthField = new JTextField(8);
		//widthField.setEditable(true);
		//widthField.addKeyListener(this);
		//widthField.setText("" + width); //visu.getCanvas().getWidth() + "");
		widthPanel.add(widthLabel);
		//widthPanel.add(widthField);
		modelWidth = new SpinnerNumberModel(width, //initial value
	                               0, //min
	                               100000, //max
	                               1);  //step
		spinnerWidth = new JSpinner(modelWidth);
		spinnerWidth.addChangeListener(this);
		widthPanel.add(spinnerWidth);
		
		sizePanel.add(widthPanel);
		
		
		
		JPanel heightPanel = new JPanel();
		JLabel heightLabel = new JLabel("Height:");
		//heightField = new JTextField(8);
		//heightField.setText("" + height); //visu.getCanvas().getHeight() + "");
		//heightField.addKeyListener(this);
		//heightField.setEditable(false);
		heightPanel.add(heightLabel);
		//heightPanel.add(heightField);
		modelHeight = new SpinnerNumberModel(height, //initial value
                0, //min
                100000, //max
                1);  //step
		spinnerHeight = new JSpinner(modelHeight);
		spinnerHeight.addChangeListener(this);
		heightPanel.add(spinnerHeight);
		sizePanel.add(heightPanel);

		
		cbLockAspectRatio = new JCheckBox("Lock aspect ratio");
		cbLockAspectRatio.setSelected(true);
		cbLockAspectRatio.addActionListener(this);
		sizePanel.add(cbLockAspectRatio);
		
		
		
//		cbAutoHeight = new JCheckBox("Auto height");
//		cbAutoHeight.addActionListener(this);
//		cbAutoHeight.setSelected(true);
//		
//		heightPanel.add(cbAutoHeight);
		
		
		resetRatioButton = new JButton("Reset aspect ratio");
		resetRatioButton.addActionListener(this);
		sizePanel.add(resetRatioButton);
		
		
		
		mainPanel.add(sizePanel);
		qualPanel.setVisible(false);
		mainPanel.add(qualPanel);

		
		add(mainPanel);
		//pack();
	}
	
	
	public void itemStateChanged(ItemEvent evt) {
		if (evt.getItem() != null) {
			String format = (String) cbFormat.getSelectedItem();
			if (format != null) {
				if (format.equals("jpeg")) {
					sizePanel.setVisible(true);
					qualPanel.setVisible(true);
				} else if (format.equals("png")) {
					sizePanel.setVisible(true);
					qualPanel.setVisible(false);
				} else if (format.equals("pdf")) {
					sizePanel.setVisible(false);
					qualPanel.setVisible(false);
					mainPanel.validate();
				} else if (format.equals("ps")) {
					sizePanel.setVisible(false);
					qualPanel.setVisible(false);
					mainPanel.validate();
				} else if (format.equals("svg")) {
					sizePanel.setVisible(false);
					qualPanel.setVisible(false);
					mainPanel.validate();
				} else {

				}
			}
		} else {

		}
	}

	/** Returns the extension of the selected file. Example: "test.PnG" => <code>png</code>
	 * @return Returns the extension of the selected file in lower case.
	 * If no file is selected, <code>null</code> is returned.
	 */
	public String getImageFormat() {
		File file = fc.getSelectedFile();
		if (file == null) {
			return null;
		}
		return file.getName().substring(file.getName().lastIndexOf(".")+1).toLowerCase();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == spinnerWidth) {
			if (cbLockAspectRatio.isSelected()) {
				Integer val = (Integer) modelWidth.getValue();
				spinnerHeight.setValue(new Integer((int) Math.round(val/ratio)));
				//TO DO: check width/height
			}
		} else if (e.getSource() == spinnerHeight) {
			if (cbLockAspectRatio.isSelected()) {
				Integer val = (Integer) modelHeight.getValue();
				spinnerWidth.setValue(new Integer((int) Math.round(val*ratio)));
				//TO DO: check width/height
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() == resetRatioButton) {
			modelWidth.setValue(width);
			modelHeight.setValue(height);
			cbLockAspectRatio.setSelected(true);
			
		}
	}

	/** Returns the quality value in the quality field.
	 * @return The quality of the JPEG. 
	 * It is ensured that return value is greater or equal than 0 and lower or equal than 1.
	 */
	public double getQuality() {
		Double qual = 1.0;
		try {
			qual = (Double) modelQuality.getValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (qual > 1.0) {
			qual = 1.0;
		}
		if (qual < 0) {
			qual = 0.0;
		}
		return qual;
	}

	


	
}
