/*
 * Copyright 2011 DELVING BV
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

import eu.delving.metadata.FieldStatistics;
import eu.delving.metadata.Histogram;
import eu.delving.metadata.RandomSample;
import eu.delving.sip.base.FrameBase;
import eu.delving.sip.base.Utility;
import eu.delving.sip.model.CreateModel;
import eu.delving.sip.model.SipModel;
import eu.delving.sip.model.SourceTreeNode;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * Show statistics in an html panel, with special tricks for separately threading the html generation
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class StatisticsFrame extends FrameBase {
    private final String EMPTY = "<html><center><h3>No Statistics</h3><b>Select an item from the document structure<br>or an input variable</b><br><br>";
    private JLabel summaryLabel = new JLabel(EMPTY, JLabel.CENTER);
    private HistogramModel histogramModel = new HistogramModel();
    private RandomSampleModel randomSampleModel = new RandomSampleModel();

    public StatisticsFrame(JDesktopPane desktop, SipModel sipModel) {
        super(Which.STATISTICS, desktop, sipModel, "Statistics", false);
        summaryLabel.setFont(new Font(summaryLabel.getFont().getFamily(), Font.BOLD, summaryLabel.getFont().getSize()));
        sipModel.getCreateModel().addListener(new CreateModel.Listener() {
            @Override
            public void sourceTreeNodesSet(CreateModel createModel, boolean internal) {
                SortedSet<SourceTreeNode> nodes = createModel.getSourceTreeNodes();
                if (nodes != null && nodes.size() == 1) {
                    setStatistics(nodes.iterator().next().getStatistics());
                }
            }

            @Override
            public void recDefTreeNodeSet(CreateModel createModel, boolean internal) {
            }

            @Override
            public void nodeMappingSet(CreateModel createModel, boolean internal) {
            }

            @Override
            public void nodeMappingChanged(CreateModel createModel) {
            }
        });
    }

    public void setStatistics(FieldStatistics fieldStatistics) {
        setSummary(fieldStatistics);
        if (fieldStatistics == null) {
            histogramModel.setHistogram(null);
            randomSampleModel.setRandomSample(null);
        }
        else {
            histogramModel.setHistogram(fieldStatistics.getHistogram());
            randomSampleModel.setRandomSample(fieldStatistics.getRandomSample());
        }
    }

    @Override
    protected void buildContent(Container content) {
        add(createSummary(), BorderLayout.NORTH);
        add(createListPanels(), BorderLayout.CENTER);
    }

    private void setSummary(FieldStatistics fieldStatistics) {
        if (fieldStatistics == null) {
            summaryLabel.setText(EMPTY);
        }
        else {
            summaryLabel.setText(String.format("<html><center><h3>%s</h3><b>%s</b><br><br>", fieldStatistics.getPath(), fieldStatistics.getSummary()));
        }
    }

    private JComponent createSummary() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Summary"));
        p.add(summaryLabel, BorderLayout.CENTER);
        p.setMinimumSize(new Dimension(300, 400));
        return p;
    }

    private JComponent createListPanels() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createRandomSamplePanel(), createHistogramPanel());
        split.setDividerLocation(0.5);
        split.setResizeWeight(0.5);
        return split;
    }

    private JComponent createRandomSamplePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Random Sample"));
        JList list = new JList(randomSampleModel);
        p.add(Utility.scrollV(list));
        return p;
    }

    private JComponent createHistogramPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Histogram"));
        JList list = new JList(histogramModel);
        p.add(Utility.scrollV(list));
        return p;
    }

    private class HistogramModel extends AbstractListModel {

        private List<Histogram.Counter> list = new ArrayList<Histogram.Counter>();

        public void setHistogram(Histogram histogram) {
            int size = getSize();
            list.clear();
            fireIntervalRemoved(this, 0, size);
            if (histogram != null) {
                list.addAll(histogram.getCounters());
                fireIntervalAdded(this, 0, getSize());
            }
        }

        @Override
        public int getSize() {
            return list.size();
        }

        @Override
        public Object getElementAt(int i) {
            Histogram.Counter counter = list.get(i);
            return String.format("   %d (%s) : '%s'", counter.getCount(), counter.getPercentage(), counter.getValue());
        }
    }

    private class RandomSampleModel extends AbstractListModel {

        private List<String> list = new ArrayList<String>();

        public void setRandomSample(RandomSample randomSample) {
            int size = getSize();
            list.clear();
            fireIntervalRemoved(this, 0, size);
            if (randomSample != null) {
                list.addAll(randomSample.getValues());
                fireIntervalAdded(this, 0, getSize());
            }
        }

        @Override
        public int getSize() {
            return list.size();
        }

        @Override
        public Object getElementAt(int i) {
            return "   " + list.get(i);
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(400, 250);
    }
}
