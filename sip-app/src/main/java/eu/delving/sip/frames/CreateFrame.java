/*
 * Copyright 2011, 2012 Delving BV
 *
 * Licensed under the EUPL, Version 1.0 or as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.delving.sip.frames;

import eu.delving.sip.base.*;
import eu.delving.sip.model.*;
import eu.delving.sip.panels.HtmlPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.SortedSet;

/**
 * Choices have been made in the trees, now we actually make the mapping
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class CreateFrame extends FrameBase {
    private static final String SELECT_STATS = "<html><center><h3>Select Source</h3></center>" +
            "<p>When you select from the source tree, this panel will" +
            "show statistical information about your selection</p></html>";
    private static final String SELECT_RECDEF = "<html><center><h3>Select Target</h3></center>" +
            "<p>When you select from the target tree, this panel will" +
            "show documentation about your selection</p></html>";
    private CreateModel createModel;
    private HtmlPanel statsHtml = new HtmlPanel("Source Statistics");
    private HtmlPanel recDefHtml = new HtmlPanel("Target Documentation");
    private JTextArea hintDocArea = new JTextArea();
    private JList mappingHintsList;
    private CreateMappingAction createMappingAction;
    private CopyMappingAction copyMappingAction;

    public CreateFrame(JDesktopPane desktop, SipModel sipModel) {
        super(Which.CREATE, desktop, sipModel, "Create", false);
        createModel = sipModel.getCreateModel();
        mappingHintsList = sipModel.getMappingHintsModel().getNodeMappingListModel().createJList();
        createMappingAction = new CreateMappingAction();
        copyMappingAction = new CopyMappingAction();
        statsHtml.setHtml(SELECT_STATS);
        recDefHtml.setHtml(SELECT_RECDEF);
        wireUp();
    }

    public CreateMappingAction getCreateMappingAction() {
        return createMappingAction;
    }

    @Override
    protected void buildContent(Container content) {
        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Select", createSelectPanel());
        tabs.addTab("Hints", createHintsPanel());
        content.add(tabs);
    }

    private JComponent createHintsPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.add(createHintsCenter(), BorderLayout.CENTER);
        p.add(new JButton(copyMappingAction), BorderLayout.SOUTH);
        return p;
    }

    private JComponent createHintsCenter() {
        JPanel p = new JPanel(new GridLayout(0, 1));
        p.add(SwingHelper.scrollV("Mapping Hints", mappingHintsList));
        p.add(SwingHelper.scrollV("Hint Documentation", hintDocArea));
        return p;
    }

    private JComponent createSelectPanel() {
        JPanel grid = new JPanel(new GridLayout(0, 1, 5, 5));
        grid.add(statsHtml);
        grid.add(recDefHtml);
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.add(grid, BorderLayout.CENTER);
        p.add(new JButton(createMappingAction), BorderLayout.SOUTH);
        return p;
    }

    private void wireUp() {
        mappingHintsList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (listSelectionEvent.getValueIsAdjusting()) return;
                NodeMappingEntry selectedHint = null;
                for (Object nodeMappingObject : mappingHintsList.getSelectedValues()) {
                    NodeMappingEntry entry = (NodeMappingEntry) nodeMappingObject;
                    if (selectedHint == null) selectedHint = entry;
                }
                if (selectedHint != null) {
                    hintDocArea.setText(selectedHint.getNodeMapping().getDocumentation());
                }
                else {
                    hintDocArea.setText("Select hint(s) above.");
                }
            }
        });
        createModel.addListener(new CreateModel.Listener() {
            @Override
            public void transition(CreateModel createModel, final CreateTransition transition) {
                final SortedSet<SourceTreeNode> sourceTreeNodes = createModel.getSourceTreeNodes();
                final RecDefTreeNode recDefTreeNode = createModel.getRecDefTreeNode();
                Exec.run(new Swing() {
                    @Override
                    public void run() {
                        if (transition.sourceChanged) {
                            if (sourceTreeNodes != null) {
                                StringBuilder out = new StringBuilder("<html><table>");
                                for (SourceTreeNode node : sourceTreeNodes) {
                                    out.append("<tr><td>");
                                    out.append(node.getHtmlDetails());
                                    out.append("</td></tr>");
                                }
                                out.append("</table></html>");
                                statsHtml.setHtml(out.toString());
                            }
                            else {
                                statsHtml.setHtml(SELECT_STATS);
                            }
                        }
                        if (transition.targetChanged) {
                            if (recDefTreeNode != null) {
                                recDefHtml.setHtml(recDefTreeNode.toHtml());
                            }
                            else {
                                recDefHtml.setHtml(SELECT_RECDEF);
                            }
                        }
                        switch (transition) {
                            case NOTHING_TO_SOURCE:
                            case SOURCE_TO_NOTHING:
                            case NOTHING_TO_TARGET:
                            case TARGET_TO_NOTHING:
                            case ARMED_TO_TARGET:
                            case ARMED_TO_SOURCE:
                            case SOURCE_TO_SOURCE:
                            case TARGET_TO_TARGET:
                                createMappingAction.setIncomplete();
                                break;
                            case SOURCE_TO_ARMED:
                            case TARGET_TO_ARMED:
                            case COMPLETE_TO_ARMED_SOURCE:
                            case COMPLETE_TO_ARMED_TARGET:
                            case ARMED_TO_ARMED_SOURCE:
                            case ARMED_TO_ARMED_TARGET:
                                createMappingAction.setArmed();
                                break;
                            case NOTHING_TO_COMPLETE:
                            case TARGET_TO_COMPLETE:
                            case SOURCE_TO_COMPLETE:
                            case CREATE_COMPLETE:
                            case COMPLETE_TO_COMPLETE:
                            case ARMED_TO_COMPLETE_SOURCE:
                            case ARMED_TO_COMPLETE_TARGET:
                                createMappingAction.setComplete();
                                break;
                        }
                    }

                });
            }
        });
    }

    private class CopyMappingAction extends AbstractAction {
        private CopyMappingAction() {
            super("<html><h3>Use selected mapping hint</h3></html>");
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            final Object[] selectedValues = mappingHintsList.getSelectedValues();
            if (selectedValues != null && selectedValues.length > 0) {
                Exec.run(new Work() {
                    @Override
                    public void run() {
                        for (Object selectedValue : selectedValues) {
                            NodeMappingEntry nodeMappingEntry = (NodeMappingEntry) selectedValue;
                            createModel.addMapping(nodeMappingEntry.getNodeMapping());
                        }
                    }
                });
            }
        }
    }

    private class CreateMappingAction extends AbstractAction {
        private static final String CREATE = "<html><h3>Create this mapping</h3></html>";
        private static final String EXISTS = "<html><h3>Mapping has been created</h3></html>";
        private static final String SELECT = "<html><h3>Make selections first</h3></html>";

        private CreateMappingAction() {
            super(SELECT);
            setIncomplete();
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            Exec.run(new Work() {
                @Override
                public void run() {
                    if (createModel.canCreate()) createModel.createMapping();
                }
            });
        }

        public void setIncomplete() {
            setEnabled(false);
            putValue(Action.NAME, SELECT);
            putValue(Action.SHORT_DESCRIPTION, "<html>Source and target not yet selected");
        }

        public void setComplete() {
            setEnabled(false);
            putValue(Action.NAME, EXISTS);
            putValue(Action.SHORT_DESCRIPTION, "<html>This mapping already exists so it cannot be created");
        }

        public void setArmed() {
            setEnabled(true);
            SortedSet<SourceTreeNode> sourceTreeNodes = createModel.getSourceTreeNodes();
            RecDefTreeNode recDefTreeNode = createModel.getRecDefTreeNode();
            if (sourceTreeNodes == null || recDefTreeNode == null) throw new RuntimeException("Expected raw materials");
            StringBuilder tooltip = new StringBuilder("<html><table cellpadding=10><tr><td><h3>From:</h3><ul>");
            for (SourceTreeNode node : sourceTreeNodes) {
                tooltip.append("<li>").append(node.getPath(false).toString()).append("</li>");
            }
            tooltip.append("</ul><h3>To:</h3><ul>");
            tooltip.append("<li>").append(recDefTreeNode.getRecDefPath().getTagPath().toString()).append("</li>");
            tooltip.append("</ul></td></tr></table></html>");
            putValue(Action.NAME, CREATE);
            putValue(Action.SHORT_DESCRIPTION, tooltip.toString());
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(600, 440);
    }
}
