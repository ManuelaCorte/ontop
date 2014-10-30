package org.semanticweb.ontop.protege4.panels;

/*
 * #%L
 * ontop-protege4
 * %%
 * Copyright (C) 2009 - 2013 KRDB Research Centre. Free University of Bozen Bolzano.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.semanticweb.ontop.model.OBDADataSource;
import org.semanticweb.ontop.protege4.core.OBDAModelWrapper;
import org.semanticweb.ontop.protege4.core.OBDAModelListener;
import org.semanticweb.ontop.protege4.utils.DatasourceSelectorListener;

/**
 * A combo box component to select a data source.
 */
public class DatasourceSelector extends javax.swing.JPanel implements OBDAModelListener {

	private static final long serialVersionUID = -7495302335571575036L;

	private OBDADataSource previousSource;

	private DefaultComboBoxModel cboModelDatasource;
	private DatasourceCellRenderer cboRendererDatasource;

	private Vector<DatasourceSelectorListener> listeners = new Vector<DatasourceSelectorListener>();

	private OBDAModelWrapper obdaModel = null;

	/** 
	 * Creates new form DatasourceListSelector 
	 */
	public DatasourceSelector(OBDAModelWrapper model) {
		setDatasourceController(model);		
		initComponents();
		cboDatasource.setFocusable(false);
	}

	public void setDatasourceController(OBDAModelWrapper model) {
		if (obdaModel != null) {
			obdaModel.removeSourcesListener(this);
		}
		obdaModel = model;
		obdaModel.addSourcesListener(this);
		initSources();
	}

	public void initSources() {
		Vector<OBDADataSource> vecDatasource = new Vector<OBDADataSource>(obdaModel.getSources());
		if (cboModelDatasource == null) {
			cboModelDatasource = new DefaultComboBoxModel(vecDatasource.toArray());
			cboRendererDatasource = new DatasourceCellRenderer();
		}
		cboModelDatasource.removeAllElements();
		for (OBDADataSource ds : vecDatasource) {
			cboModelDatasource.addElement(ds);
		}
	}

	public OBDADataSource getSelectedDataSource() {
		return (OBDADataSource) cboDatasource.getSelectedItem();
	}

	public void addDatasourceListListener(DatasourceSelectorListener l) {
		listeners.add(l);
	}

	@Override
	public void datasourceAdded(OBDADataSource source) {
		DefaultComboBoxModel model = (DefaultComboBoxModel) cboDatasource.getModel();
		model.addElement(source);
	}

	@Override
	public void datasourceDeleted(OBDADataSource source) {
		DefaultComboBoxModel model = (DefaultComboBoxModel) cboDatasource.getModel();
		model.removeElement(source);
	}

	@Override
	public void datasourceUpdated(String oldSourceUri, OBDADataSource newSource) {
		// TODO Change the interface?
	}

	@Override
	public void alldatasourcesDeleted() {
		DefaultComboBoxModel model = (DefaultComboBoxModel) cboDatasource.getModel();
		model.removeAllElements();
	}

	@Override
	public void datasourcParametersUpdated() {
		// TODO Change the interface?
	}

	public void set(OBDADataSource dataSource) {
		cboDatasource.setSelectedItem(dataSource);
	}
	
	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cboDatasource = new JComboBox();

        setMinimumSize(new Dimension(23, 21));
        setPreferredSize(new Dimension(28, 21));
        setLayout(new BorderLayout());

        cboDatasource.setModel(cboModelDatasource);
        cboDatasource.setMinimumSize(new Dimension(23, 23));
        cboDatasource.setPreferredSize(new Dimension(28, 23));
        cboDatasource.setRenderer(cboRendererDatasource);
        cboDatasource.setSelectedIndex(-1);
        cboDatasource.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cboDatasourceSelected(evt);
            }
        });
        add(cboDatasource, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

	private void cboDatasourceSelected(ActionEvent evt) {// GEN-FIRST:event_cboDatasourceSelected
		JComboBox cb = (JComboBox) evt.getSource();
		OBDADataSource currentSource = (OBDADataSource) cb.getSelectedItem();
		for (DatasourceSelectorListener listener : listeners) {
			listener.datasourceChanged(previousSource, currentSource);
		}
		// After the listeners have been notified, update the previousSource
		// to be as the same as the currentSource, so that we have a historical
		// record of data sources.
		previousSource = (OBDADataSource) cb.getSelectedItem();
	}// GEN-LAST:event_cboDatasourceSelected

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JComboBox cboDatasource;
    // End of variables declaration//GEN-END:variables

	private class DatasourceCellRenderer extends JLabel implements ListCellRenderer {

		private static final long	serialVersionUID	= -8521494988522078279L;

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			if (value == null) {
				setText("<Select a datasource>");
			} 
			else {
				OBDADataSource datasource = (OBDADataSource) value;
				String datasourceUri = datasource.getSourceID().toString();
				setText(datasourceUri);
			}
			return this;
		}
	}
}
