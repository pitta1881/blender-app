package blender.distributed.Cliente.view;

import blender.distributed.Cliente.controller.Controller;
import blender.distributed.Records.RTrabajo;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.Toolkit;


import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import static java.lang.Integer.valueOf;
/**/
//Generated by GuiGenie - Copyright (c) 2004 Mario Awad.
//Home Page http://guigenie.cjb.net - Check often for new versions!


public class GUICliente extends JPanel implements ActionListener  {
	private JButton btnChooser;
	private JLabel labelFileName;
	private JTextField fileName;
	private JLabel labelStartFrame;
	private JTextField startFrame;
	private JLabel labelDash;
	private JTextField endFrame;
	private JButton btnStart;
	private JButton btnWorkList;
	private JFileChooser fc;
	JFrame frame;
	Controller controlador;
	List<RTrabajo> listaTrabajos;

	public GUICliente(Controller controlador, List<RTrabajo> listaTrabajos){
		this.controlador = controlador;
		this.listaTrabajos = listaTrabajos;
		frame = new JFrame ("Renderizado distribuido");
		frame.setResizable(false);
		frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - frame.getWidth())/5);
        int y = (int) ((dimension.getHeight() - frame.getHeight())/5);
        frame.setLocation(x, y);
        
   	 //construct preComponents
	      //construct components
			btnChooser = new JButton ("Cargar Archivo");
	      	btnChooser.addActionListener(this);
		  	labelFileName = new JLabel ("Nombre Archivo:");
	      	fileName =  new JFormattedTextField("");
		  	fileName.setEnabled(false);
	      	labelStartFrame = new JLabel ("Frames (Inicial-Final):");
		  	startFrame =  new JFormattedTextField(valueOf(1));
	      	labelDash = new JLabel ("-");
	      	endFrame =  new JFormattedTextField(valueOf(100));
	      	btnStart = new JButton ("Empezar a renderizar");
	      	btnStart.addActionListener(this);
	      	btnStart.setEnabled(false);
		  	btnWorkList = new JButton ("Lista de Trabajos");
		  	btnWorkList.addActionListener(this);
		  	btnWorkList.setEnabled(false);

		//adjust size and set layout
		  	setPreferredSize (new Dimension (400, 170));
		  	setLayout (null);
		//add components
		  	add (btnChooser);
		  	add (labelFileName);
		  	add (fileName);
		  	add (labelStartFrame);
		  	add (startFrame);
		  	add (labelDash);
		  	add (endFrame);
		  	add (btnStart);
			add (btnWorkList);
	  	//set component bounds (only needed by Absolute Positioning)
		  	btnChooser.setBounds (15, 10, 370, 30);
		  	labelFileName.setBounds(15,50,170,30);
		  	fileName.setBounds(150,50,235,30);
		  	labelStartFrame.setBounds (15, 90, 170, 30);
		  	startFrame.setBounds (150, 90, 70, 30);
		  	labelDash.setBounds (230, 90, 10, 30);
		  	endFrame.setBounds (250, 90, 70, 30);
			btnWorkList.setBounds (15, 130, 175, 30);
			btnStart.setBounds (210, 130, 175, 30);

		  	frame.getContentPane().add(this);
		  	frame.pack();
		  	frame.setVisible (true);
	  	//Create a file chooser
		  	fc = new JFileChooser();
	}
	public void actionPerformed(ActionEvent e) {
		 if (e.getSource() == this.btnChooser) {
			fc = new JFileChooser();
			FileFilter filter = new FileNameExtensionFilter("Blender File","blend");
			this.fc.setFileFilter(filter);
			int returnVal = this.fc.showOpenDialog(null);
	        if (returnVal == JFileChooser.APPROVE_OPTION) {
				this.fileName.setText(fc.getSelectedFile().getName());
	        	this.controlador.setFile(fc.getSelectedFile());
	        }
		 }
		 if (e.getSource() == this.btnStart) {
			 int startFrame = valueOf(this.startFrame.getText().replace(".", ""));
			 int endFrame = valueOf(this.endFrame.getText().replace(".", ""));
			
			 if(startFrame <= 0) {
				 JOptionPane.showMessageDialog(null, "ERROR: El frame inicial debe ser mayor a 0.", "Error", JOptionPane.ERROR_MESSAGE);
			 } else if (startFrame > endFrame) {
				 JOptionPane.showMessageDialog(null, "ERROR: El frame inicial no puede ser mayor al frame final.", "Error", JOptionPane.ERROR_MESSAGE);
			 } else {
				 this.frame.setTitle("Procesando...");
				 this.controlador.enviarFile(startFrame, endFrame);
				 this.frame.setTitle("Renderizado Distribuido");
			 }
			 this.btnWorkList.setEnabled(true);
		 }
		if (e.getSource() == this.btnWorkList) {
			new GUITable(this.listaTrabajos);
		}
		 this.btnStart.setEnabled(this.controlador.isReady());
	 }
}