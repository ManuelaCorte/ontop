/***
 * Copyright (c) 2008, Mariano Rodriguez-Muro.
 * All rights reserved.
 *
 * The OBDA-API is licensed under the terms of the Lesser General Public
 * License v.3 (see OBDAAPI_LICENSE.txt for details). The components of this
 * work include:
 * 
 * a) The OBDA-API developed by the author and licensed under the LGPL; and, 
 * b) third-party components licensed under terms that may be different from 
 *   those of the LGPL.  Information about such licenses can be found in the 
 *   file named OBDAAPI_3DPARTY-LICENSES.txt.
 */

package it.unibz.krdb.obda.gui.swing.panel;

import it.unibz.krdb.obda.gui.swing.OBDADataQueryAction;
import it.unibz.krdb.obda.gui.swing.OBDASaveQueryResultToFileAction;
import it.unibz.krdb.obda.gui.swing.frame.GetOutputFileDialog;
import it.unibz.krdb.obda.gui.swing.treemodel.IncrementalResultSetTableModel;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.table.TableModel;


//import edu.stanford.smi.protegex.owl.ui.ProtegeUI;

/**
 * 
 * @author mariano
 */
public class ResultViewTablePanel extends javax.swing.JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8494558136315031084L;
	private OBDADataQueryAction			countAllTuplesAction	= null;
	private OBDADataQueryAction			countAllTuplesActionEQL	= null;
	private QueryInterfacePanel 		querypanel = null;
	private OBDASaveQueryResultToFileAction saveToFileAction =null; 
	
	/** Creates new form ResultViewTablePanel */
	public ResultViewTablePanel(QueryInterfacePanel panel) {
		querypanel = panel;
		initComponents();
		addPopUpMenu();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrQueryResult = new javax.swing.JScrollPane();
        tblQueryResult = new javax.swing.JTable();
        pnlCommandButton = new javax.swing.JPanel();
        cmdExportResult = new javax.swing.JButton();

        setMinimumSize(new java.awt.Dimension(400, 480));
        setLayout(new java.awt.BorderLayout());

        tblQueryResult.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Results"
            }
        ));
        scrQueryResult.setViewportView(tblQueryResult);

        add(scrQueryResult, java.awt.BorderLayout.CENTER);

        pnlCommandButton.setMinimumSize(new java.awt.Dimension(500, 32));
        pnlCommandButton.setPreferredSize(new java.awt.Dimension(500, 32));
        pnlCommandButton.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        cmdExportResult.setText("Export Result to CSV File");
        cmdExportResult.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdExportResultActionPerformed(evt);
            }
        });
        pnlCommandButton.add(cmdExportResult);

        add(pnlCommandButton, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents


	private void cmdExportResultActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_buttonSaveResultsActionPerformed
		GetOutputFileDialog dialog = new GetOutputFileDialog();
		final File file = dialog.getOutPutFile();
		if(file != null){
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					saveToFileAction.run(querypanel.getQuery(), file);
				}
			});
		}
	}// GEN-LAST:event_buttonSaveResultsActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cmdExportResult;
    private javax.swing.JPanel pnlCommandButton;
    private javax.swing.JScrollPane scrQueryResult;
    private javax.swing.JTable tblQueryResult;
    // End of variables declaration//GEN-END:variables
	
	public void setTableModel(final TableModel newmodel) {
		Runnable updateModel = new Runnable() {
			public void run() {
				
				tblQueryResult.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
				ToolTipManager.sharedInstance().unregisterComponent(tblQueryResult);
				ToolTipManager.sharedInstance().unregisterComponent(tblQueryResult.getTableHeader());

				TableModel oldmodel = tblQueryResult.getModel();
				if (oldmodel != null) {
					oldmodel.removeTableModelListener(tblQueryResult);
					if (oldmodel instanceof IncrementalResultSetTableModel) {
						IncrementalResultSetTableModel incm = (IncrementalResultSetTableModel) oldmodel;
						incm.close();
					}
				}
				tblQueryResult.setModel(newmodel);

				addNotify();

				tblQueryResult.invalidate();
				tblQueryResult.repaint();
			}
		};
		SwingUtilities.invokeLater(updateModel);
	}
	
	private void addPopUpMenu(){
		JPopupMenu menu = new JPopupMenu();
		JMenuItem countAll = new JMenuItem(); 
		countAll.setText("count all tuples");
		countAll.addActionListener(new ActionListener(){

//			@Override
			public void actionPerformed(ActionEvent e) {
				
				Thread thread = new Thread() {
					public void run() {
						String query = querypanel.getQuery();
						getCountAllTuplesActionForUCQ().run(query, querypanel);
					}
				};
				thread.start();
			}
			
		});
		menu.add(countAll);
		tblQueryResult.setComponentPopupMenu(menu);
	}

	public OBDADataQueryAction getCountAllTuplesActionForUCQ() {
		return countAllTuplesAction;
	}

	public void setCountAllTuplesActionForUCQ(OBDADataQueryAction countAllTuples) {
		this.countAllTuplesAction = countAllTuples;
	}
	
	public OBDADataQueryAction getCountAllTuplesActionForEQL() {
		return countAllTuplesActionEQL;
	}

	public void setCountAllTuplesActionForEQL(OBDADataQueryAction countAllTuples) {
		this.countAllTuplesActionEQL = countAllTuples;
	}
	
	public void setOBDASaveQueryToFileAction(OBDASaveQueryResultToFileAction action){
		this.saveToFileAction = action;
	}		
}
