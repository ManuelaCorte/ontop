package it.unibz.inf.ontop.protege.views;

/*
 * #%L
 * ontop-protege
 * %%
 * Copyright (C) 2009 - 2013 KRDB Research Centre. Free University of Bozen-Bolzano.
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

import it.unibz.inf.ontop.owlapi.connection.OntopOWLStatement;
import it.unibz.inf.ontop.owlapi.connection.impl.DefaultOntopOWLStatement;
import it.unibz.inf.ontop.owlapi.resultset.BooleanOWLResultSet;
import it.unibz.inf.ontop.owlapi.resultset.GraphOWLResultSet;
import it.unibz.inf.ontop.owlapi.resultset.TupleOWLResultSet;
import it.unibz.inf.ontop.protege.core.OBDAEditorKitSynchronizerPlugin;
import it.unibz.inf.ontop.protege.core.OBDAModelManager;
import it.unibz.inf.ontop.protege.gui.models.OWLResultSetTableModel;
import it.unibz.inf.ontop.protege.utils.OBDADataQueryAction;
import it.unibz.inf.ontop.protege.panels.QueryInterfacePanel;
import it.unibz.inf.ontop.protege.panels.ResultViewTablePanel;
import it.unibz.inf.ontop.protege.panels.SavedQueriesPanelListener;
import it.unibz.inf.ontop.protege.utils.DialogUtils;
import it.unibz.inf.ontop.protege.gui.dialogs.TextQueryResultsDialog;
import it.unibz.inf.ontop.protege.workers.OntopQuerySwingWorker;
import it.unibz.inf.ontop.protege.workers.OntopQuerySwingWorkerFactory;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class QueryInterfaceView extends AbstractOWLViewComponent implements SavedQueriesPanelListener {

    private static final long serialVersionUID = 1L;

    private QueryInterfacePanel queryEditorPanel;

    private ResultViewTablePanel resultTablePanel;

    private OWLOntologyChangeListener ontologyListener;

    private OBDAModelManager obdaModelManager;

    private OWLResultSetTableModel tableModel;

    private static final Logger log = LoggerFactory.getLogger(QueryInterfaceView.class);

    @Override
    protected void disposeOWLView() {
        getOWLModelManager().removeOntologyChangeListener(ontologyListener);

        QueryInterfaceViewsList queryInterfaceViews = (QueryInterfaceViewsList) this.getOWLEditorKit().get(QueryInterfaceViewsList.class.getName());
        if ((queryInterfaceViews != null)) {
            queryInterfaceViews.remove(this);
        }

        QueryManagerViewsList queryManagerViews = (QueryManagerViewsList) this.getOWLEditorKit().get(QueryManagerViewsList.class.getName());
        if ((queryManagerViews != null) && (!queryManagerViews.isEmpty())) {
            for (QueryManagerView queryInterfaceView : queryManagerViews) {
                queryInterfaceView.removeListener(this);
            }
        }
    }

    private final OntopQuerySwingWorkerFactory<String> retrieveUCQExpansionAction = (ontop, query) -> new OntopQuerySwingWorker<String>(this, ontop, "Rewriting query", query) {

        @Override
        protected String runQuery(OntopOWLStatement statement, String query) throws Exception {
            return statement.getRewritingRendering(query);
        }

        @Override
        protected void onCompletion(String result, String sqlQuery) {
            setSQLTranslation(sqlQuery);
            TextQueryResultsDialog dialog = new TextQueryResultsDialog(getWorkspace(),
                    "Intermediate Query",
                    result,
                    "Processing time: " + DialogUtils.renderElapsedTime(elapsedTimeMillis()));
            dialog.setVisible(true);
        }
    };

    private final OntopQuerySwingWorkerFactory<String> retrieveUCQUnfoldingAction = (ontop, query) -> new OntopQuerySwingWorker<String>(this, ontop, "Rewriting query", query) {

        @Override
        protected String runQuery(OntopOWLStatement statement, String query) throws Exception {
            // TODO: should we show the SQL query only?
            return statement.getExecutableQuery(query).toString();
        }

        @Override
        protected void onCompletion(String result, String sqlQuery) {
            setSQLTranslation(sqlQuery);
            TextQueryResultsDialog dialog = new TextQueryResultsDialog(getWorkspace(),
                    "SQL Translation",
                    result,
                    "Processing time: " + DialogUtils.renderElapsedTime(elapsedTimeMillis()));
            dialog.setVisible(true);
        }
    };

    @Override
    protected void initialiseOWLView() {
        obdaModelManager = OBDAEditorKitSynchronizerPlugin.getOBDAModelManager(getOWLEditorKit());

        queryEditorPanel = new QueryInterfacePanel(getOWLEditorKit(), obdaModelManager, retrieveUCQExpansionAction, retrieveUCQUnfoldingAction);
        queryEditorPanel.setPreferredSize(new Dimension(400, 250));
        queryEditorPanel.setMinimumSize(new Dimension(400, 250));

        resultTablePanel = new ResultViewTablePanel(getOWLEditorKit(), queryEditorPanel);
        resultTablePanel.setMinimumSize(new java.awt.Dimension(400, 250));
        resultTablePanel.setPreferredSize(new java.awt.Dimension(400, 250));

        JSplitPane splQueryInterface = new JSplitPane();
        splQueryInterface.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splQueryInterface.setResizeWeight(0.5);
        splQueryInterface.setDividerLocation(0.5);
        splQueryInterface.setOneTouchExpandable(true);
        splQueryInterface.setTopComponent(queryEditorPanel);
        splQueryInterface.setBottomComponent(resultTablePanel);
        JPanel pnlQueryInterfacePane = new JPanel();
        pnlQueryInterfacePane.setLayout(new BorderLayout());
        pnlQueryInterfacePane.add(splQueryInterface, BorderLayout.CENTER);
        setLayout(new BorderLayout());
        add(pnlQueryInterfacePane, BorderLayout.CENTER);


        // Setting up model listeners
        ontologyListener = changes ->
                SwingUtilities.invokeLater(() -> resultTablePanel.setTableModel(new DefaultTableModel()));

        this.getOWLModelManager().addOntologyChangeListener(ontologyListener);
        setupListeners();

        // Setting up actions for all the buttons of this view.

        //action clicking on execute button with a select query
        queryEditorPanel.setExecuteSelectAction(new OBDADataQueryAction<TupleOWLResultSet>("Executing queries...", QueryInterfaceView.this) {

            @Override
            public OWLEditorKit getEditorKit(){
                return getOWLEditorKit();
            }

            @Override
            public void handleResult(TupleOWLResultSet result) throws OWLException{
                createTableModelFromResultSet(result);
                showTupleResultInTablePanel();
            }

            @Override
            public void handleSQLTranslation(String sqlQuery) {
                resultTablePanel.setSQLTranslation(sqlQuery);
            }

            @Override
            public void run(String query){
                removeResultTable();
                super.run(query);
            }

            @Override
            public int getNumberOfRows() {
                if (tableModel == null)
                    return 0;
                return tableModel.getRowCount();
            }

            @Override
            public boolean isRunning() {
                return tableModel != null && tableModel.isFetching();
            }

            @Override
            public TupleOWLResultSet executeQuery(OntopOWLStatement st,
                                                  String queryString) throws OWLException {
                if(queryEditorPanel.isFetchAllSelect()) {
                    return st.executeSelectQuery(queryString);
                }
                else {
                    DefaultOntopOWLStatement defaultOntopOWLStatement = (DefaultOntopOWLStatement) st;
                    defaultOntopOWLStatement.setMaxRows(queryEditorPanel.getFetchSize());
                    return defaultOntopOWLStatement.executeSelectQuery(queryString);
                }
            }
        });


        //action clicking on execute button with an ask query
        queryEditorPanel.setExecuteAskAction(new OBDADataQueryAction<BooleanOWLResultSet>("Executing queries...", QueryInterfaceView.this) {

            @Override
            public OWLEditorKit getEditorKit(){
                return getOWLEditorKit();
            }

            @Override
            public void handleResult(BooleanOWLResultSet result) throws OWLException{
                queryEditorPanel.showBooleanActionResultInTextInfo("Result:", result);
            }

            @Override
            public void handleSQLTranslation(String sqlQuery) {
                resultTablePanel.setSQLTranslation(sqlQuery);
            }

            @Override
            public int getNumberOfRows() {
                return -1;
            }

            @Override
            public boolean isRunning() {
                return tableModel != null && tableModel.isFetching();
            }
            @Override
            public BooleanOWLResultSet executeQuery(OntopOWLStatement st,
                                                    String queryString) throws OWLException {
                removeResultTable();
                if(queryEditorPanel.isFetchAllSelect())
                    return st.executeAskQuery(queryString);

                DefaultOntopOWLStatement defaultOntopOWLStatement = (DefaultOntopOWLStatement) st;
                defaultOntopOWLStatement.setMaxRows(queryEditorPanel.getFetchSize());
                return defaultOntopOWLStatement.executeAskQuery(queryString);
            }

        });

        //action clicking on execute button with an graph query (describe or construct)
        queryEditorPanel.setExecuteGraphQueryAction(
                new OBDADataQueryAction<GraphOWLResultSet>("Executing queries...", QueryInterfaceView.this) {

                    @Override
                    public OWLEditorKit getEditorKit(){
                        return getOWLEditorKit();
                    }

                    @Override
                    public GraphOWLResultSet executeQuery(OntopOWLStatement st, String queryString) throws OWLException {
                        removeResultTable();
                        if(queryEditorPanel.isFetchAllSelect())
                            return st.executeGraphQuery(queryString);

                        DefaultOntopOWLStatement defaultOntopOWLStatement = (DefaultOntopOWLStatement) st;
                        defaultOntopOWLStatement.setMaxRows(queryEditorPanel.getFetchSize());
                        return defaultOntopOWLStatement.executeGraphQuery(queryString);
                    }

                    @Override
                    public void handleResult(GraphOWLResultSet result) throws OWLException{
                        OWLAxiomToTurtleVisitor owlVisitor = new OWLAxiomToTurtleVisitor(obdaModelManager.getTriplesMapCollection().getMutablePrefixManager());
                        if (result != null) {
                            while (result.hasNext()) {
                                result.next().accept(owlVisitor);
                            }
                            result.close();
                        }
                        showGraphResultInTextPanel(owlVisitor.getString());
                    }

                    @Override
                    public void handleSQLTranslation(String sqlQuery) {
                        resultTablePanel.setSQLTranslation(sqlQuery);
                    }

                    @Override
                    public int getNumberOfRows() {
                        return 0;
                    }

                    @Override
                    public boolean isRunning() {
                        return tableModel != null && tableModel.isFetching();
                    }
                });

        log.debug("Query Manager view initialized");
    }




    private void showTupleResultInTablePanel() {
        if (tableModel != null)
            SwingUtilities.invokeLater(() -> resultTablePanel.setTableModel(tableModel));
    }

    private void setSQLTranslation(String s) {
        resultTablePanel.setSQLTranslation(s);
    }

    private synchronized void createTableModelFromResultSet(TupleOWLResultSet result) throws OWLException {
        if (result == null)
            throw new NullPointerException("An error occurred. createTableModelFromResultSet cannot use a null QuestOWLResultSet");
        tableModel = new OWLResultSetTableModel(result, obdaModelManager.getTriplesMapCollection().getMutablePrefixManager(),
                queryEditorPanel.isShortURISelect(),
                queryEditorPanel.isFetchAllSelect(),
                queryEditorPanel.getFetchSize());
        tableModel.addTableModelListener(queryEditorPanel);
    }

    /**
     * removes the result table.
     * Could be called at data query execution, or at cancelling
     * Not necessary when replacing with a new result, just to remove old
     * results that are outdated
     */
    private synchronized void removeResultTable(){
        if (tableModel != null) {
            tableModel.close();
        }
        resultTablePanel.setTableModel(new DefaultTableModel());
    }


    private synchronized void showGraphResultInTextPanel(String s) {
        SwingUtilities.invokeLater(() -> {
            TextQueryResultsDialog panel = new TextQueryResultsDialog(getWorkspace(), "SPARQL Graph Query (CONSTRUCT/DESCRIBE) Result", s, "");
            panel.setVisible(true);
        });
    }

    @Override
    public void selectedQueryChanged(String groupId, String queryId, String query) {
        queryEditorPanel.selectedQueryChanged(groupId, queryId, query);
    }

    /**
     * On creation of a new view, we register it globally and make sure that its selector is listened
     * by all other instances of query view in this editor kit. Also, we make this new instance listen
     * to the selection of all other query selectors in the views.
     */
    public void setupListeners() {

        // Getting the list of views
        QueryInterfaceViewsList queryInterfaceViews = (QueryInterfaceViewsList) getOWLEditorKit().get(QueryInterfaceViewsList.class.getName());
        if (queryInterfaceViews == null) {
            queryInterfaceViews = new QueryInterfaceViewsList();
            getOWLEditorKit().put(QueryInterfaceViewsList.class.getName(), queryInterfaceViews);
        }

        // Adding the new instance (this)
        queryInterfaceViews.add(this);

        // Registering the current query view with all existing query manager views
        QueryManagerViewsList queryManagerViews = (QueryManagerViewsList) this.getOWLEditorKit().get(QueryManagerViewsList.class.getName());
        if ((queryManagerViews != null) && (!queryManagerViews.isEmpty())) {
            for (QueryManagerView queryInterfaceView : queryManagerViews) {
                queryInterfaceView.addListener(this);
            }
        }
    }
}
