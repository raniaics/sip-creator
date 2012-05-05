/*
 * Copyright 2011, 2012 Delving BV
 *
 * Licensed under the EUPL, Version 1.0 or? as soon they
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

import eu.delving.metadata.Path;
import eu.delving.sip.base.Exec;
import eu.delving.sip.base.FrameBase;
import eu.delving.sip.files.DataSet;
import eu.delving.sip.files.DataSetState;
import eu.delving.sip.model.DataSetModel;
import eu.delving.sip.model.SipModel;
import eu.delving.sip.model.StatsNode;
import eu.delving.stats.ChartHelper;
import eu.delving.stats.Stats;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;

import static eu.delving.sip.base.SwingHelper.scrollV;

/**
 * Show statistics in an html panel, with special tricks for separately threading the html generation
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class StatsFrame extends FrameBase {
    private StatsSet[] statsSets = {
            new StatsSet("Import"),
            new StatsSet("Source"),
            new StatsSet("Result"),
    };
    private JPanel wordCountPanel = emptyPanel();
    private JPanel fieldFrequencyPanel = emptyPanel();
    private JPanel presencePanel = emptyPanel();
    private JPanel fieldCountPanel = emptyPanel();
    private JPanel valueHistogramPanel = emptyPanel();

    public StatsFrame(JDesktopPane desktop, SipModel sipModel) {
        super(Which.STATS, desktop, sipModel, "Statistics", false);
        wireUp();
        getAction().putValue(
                Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())
        );
    }

    @Override
    protected void buildContent(Container content) {
        add(createWest(), BorderLayout.WEST);
        add(createCenter(), BorderLayout.CENTER);
    }

    private JComponent createWest() {
        final JTabbedPane tabs = new JTabbedPane();
        for (StatsSet statsSet : statsSets) tabs.addTab(statsSet.statsSetName, statsSet.treePanel);
        int width = getFontMetrics(getFont()).stringWidth("this is a very long string to determine width");
        tabs.setPreferredSize(new Dimension(width, 10));
        tabs.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                int index = tabs.getSelectedIndex();
                statsSets[index].select();

            }
        });
        statsSets[0].select();
        return tabs;
    }

    private JComponent createCenter() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Presence in Record", presencePanel);
        tabs.addTab("Field Count", fieldCountPanel);
        tabs.addTab("Word Count", wordCountPanel);
        tabs.addTab("Field Frequency", fieldFrequencyPanel);
        tabs.addTab("Values Histogram", valueHistogramPanel);
        return tabs;
    }

    private void clearPanel(JPanel panel) {
        panel.removeAll();
        panel.add(emptyLabel());
        panel.validate();
    }

    private void setPanelContent(JPanel panel, JComponent content) {
        if (content == null) {
            clearPanel(panel);
        }
        else {
            panel.removeAll();
            panel.add(content);
            panel.validate();
        }
    }

    private JPanel emptyPanel() {
        JPanel p = new JPanel(new BorderLayout());
        clearPanel(p);
        return p;
    }

    private JComponent emptyLabel() {
        return new JLabel("Not Available", JLabel.CENTER);
    }

    private void wireUp() {
        sipModel.getDataSetModel().addListener(new DataSetModel.Listener() {
            @Override
            public void dataSetChanged(DataSet dataSet) {
                dataSetStateChanged(dataSet, dataSet.getState());
            }

            @Override
            public void dataSetRemoved() {
                for (StatsSet statsSet : statsSets) statsSet.setStats(null);
            }

            @Override
            public void dataSetStateChanged(DataSet dataSet, DataSetState dataSetState) {
                switch (dataSetState) {
                    case ABSENT:
                    case EMPTY:
                    case IMPORTED:
                        for (StatsSet statsSet : statsSets) statsSet.setStats(null);
                        break;
                    case ANALYZED_IMPORT:
                    case DELIMITED:
                    case SOURCED:
                        statsSets[0].setStats(dataSet.getStats(false, null));
                        break;
                    case MAPPING:
                    case ANALYZED_SOURCE:
                        statsSets[0].setStats(dataSet.getStats(false, null));
                        statsSets[1].setStats(dataSet.getStats(true, null));
                        break;
                    case VALIDATED:
                        statsSets[0].setStats(dataSet.getStats(false, null));
                        statsSets[1].setStats(dataSet.getStats(true, null));
                        statsSets[2].setStats(dataSet.getStats(true, sipModel.getMappingModel().getRecMapping().getPrefix()));
                        break;
                }
            }
        });
    }

    private class StatsSet {
        private String statsSetName;
        private DefaultTreeModel treeModel = new DefaultTreeModel(StatsNode.create("Empty"));
        private JTree tree = new JTree(treeModel);
        private JPanel treePanel = emptyPanel();
        private ChartHelper chartHelper;
        private boolean expand = false;

        private StatsSet(String statsSetName) {
            this.statsSetName = statsSetName;
            tree.setCellRenderer(new StatsNode.Renderer());
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
                @Override
                public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
                    TreePath treePath = tree.getSelectionPath();
                    if (treePath == null) {
                        setPath(null);
                    }
                    else {
                        setPath(((StatsNode) treePath.getLastPathComponent()).getPath(true));
                    }
                }
            });
        }

        private void setStats(Stats stats) {
            Exec.checkSwing();
            if (stats == null) {
                chartHelper = null;
                treeModel.setRoot(StatsNode.create("Empty"));
                clearPanel(treePanel);
            }
            else {
                String name = (stats.name == null) ? sipModel.getDataSetModel().getDataSet().getSpec() : stats.name;
                chartHelper = new ChartHelper(stats, statsSetName, name);
                final StatsNode root = StatsNode.create(stats.fieldValueMap.keySet());
                setPanelContent(treePanel, scrollV(tree));
                treeModel.setRoot(root);
                expand = true;
                setRecordStatPanels();
            }
        }

        private void select() {
            setRecordStatPanels();
            TreePath treePath = tree.getSelectionModel().getSelectionPath();
            setPath(treePath == null ? null : ((StatsNode) treePath.getLastPathComponent()).getPath(true));
            expandIfNecessary();
        }

        private void setRecordStatPanels() {
            if (chartHelper != null) {
                setPanelContent(fieldCountPanel, chartHelper.hasFieldCountChart() ? chartHelper.getFieldCountPanel() : null);
                setPanelContent(presencePanel, chartHelper.hasPresentAbsentChart() ? chartHelper.getPresencePanel() : null);
            }
            else {
                clearPanel(fieldCountPanel);
                clearPanel(presencePanel);
            }
        }

        private void expandIfNecessary() {
            if (!expand) return;
            StatsNode root = (StatsNode) tree.getModel().getRoot();
            root.expand(tree);
            expand = false;
        }

        private void setPath(Path path) {
            if (path != null) {
                if (chartHelper == null) return;
                Stats.ValueStats valueStats = chartHelper.setPath(path);
                setPanelContent(fieldFrequencyPanel, chartHelper.hasFrequencyChart() ? chartHelper.getFieldFrequencyPanel() : null);
                setPanelContent(wordCountPanel, chartHelper.hasWordCountChart() ? chartHelper.getWordCountPanel() : null);
                Stats.Histogram values = valueStats == null ? null : valueStats.values;
                setPanelContent(valueHistogramPanel, values != null ? createHistogramList(values) : null);
            }
            else {
                clearPanel(fieldFrequencyPanel);
                clearPanel(wordCountPanel);
                clearPanel(valueHistogramPanel);
            }
        }
    }

    private JComponent createHistogramList(Stats.Histogram values) {
        HistogramModel histogramModel = new HistogramModel();
        histogramModel.setHistogram(values);
        JList list = new JList(histogramModel);
        return scrollV(list);
    }

    private static class HistogramModel extends AbstractListModel {
        private java.util.List<Stats.Counter> list = new ArrayList<Stats.Counter>();

        public void setHistogram(Stats.Histogram histogram) {
            int size = getSize();
            list.clear();
            fireIntervalRemoved(this, 0, size);
            if (histogram != null) {
                list.addAll(histogram.counterMap.values());
                Collections.sort(list);
                fireIntervalAdded(this, 0, getSize());
            }
        }

        @Override
        public int getSize() {
            return list.size();
        }

        @Override
        public Object getElementAt(int i) {
            Stats.Counter counter = list.get(i);
            return String.format("   %d (%s) : '%s'", counter.count, counter.percentage, counter.value);
        }
    }

}