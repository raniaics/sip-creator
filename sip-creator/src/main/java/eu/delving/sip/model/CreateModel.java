/*
 * Copyright 2011 DELVING BV
 *
 *  Licensed under the EUPL, Version 1.0 or? as soon they
 *  will be approved by the European Commission - subsequent
 *  versions of the EUPL (the "Licence");
 *  you may not use this work except in compliance with the
 *  Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  http://ec.europa.eu/idabc/eupl
 *
 *  Unless required by applicable law or agreed to in
 *  writing, software distributed under the Licence is
 *  distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied.
 *  See the Licence for the specific language governing
 *  permissions and limitations under the Licence.
 */

package eu.delving.sip.model;

import eu.delving.metadata.NodeMapping;
import eu.delving.metadata.Path;
import eu.delving.sip.base.StatsTreeNode;

import javax.swing.tree.TreePath;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This model holds the source and destination of a node mapping, and is observable.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class CreateModel {
    private SipModel sipModel;
    private StatsTreeNode statsTreeNode;
    private RecDefTreeNode recDefTreeNode;
    private NodeMapping nodeMapping;

    public CreateModel(SipModel sipModel) {
        this.sipModel = sipModel;
    }

    public StatsTreeNode getStatsTreeNode() {
        return statsTreeNode;
    }

    public void setStatsTreeNode(StatsTreeNode statsTreeNode) {
        if (statsTreeNode != null && statsTreeNode.getParent() == null) statsTreeNode = null;
        this.statsTreeNode = statsTreeNode;
        for (Listener listener : listeners) listener.statsTreeNodeSet(this);
    }

    public RecDefTreeNode getRecDefTreeNode() {
        return recDefTreeNode;
    }

    public void setRecDefTreeNode(RecDefTreeNode recDefTreeNode) {
        if (recDefTreeNode != null && recDefTreeNode.getParent() == null) recDefTreeNode = null;
        this.recDefTreeNode = recDefTreeNode;
        for (Listener listener : listeners) listener.recDefTreeNodeSet(this);
        if (nodeMapping != null && recDefTreeNode != null && nodeMapping.recDefNode != recDefTreeNode.getRecDefNode()) setNodeMapping(null);
    }

    public void setRecDefTreePath(Path path) {
        TreePath treePath = sipModel.getMappingModel().getTreePath(path);
        RecDefTreeNode node = ((RecDefTreeNode)(treePath.getLastPathComponent()));
        setRecDefTreeNode(node);
    }

    public NodeMapping getNodeMapping() {
        return nodeMapping;
    }

    public void setNodeMapping(NodeMapping nodeMapping) {
        this.nodeMapping = nodeMapping;
        for (Listener listener : listeners) listener.nodeMappingSet(this);
        if (nodeMapping != null) {
            TreePath treePath = sipModel.getMappingModel().getTreePath(nodeMapping.outputPath);
            RecDefTreeNode rdn = (RecDefTreeNode) treePath.getLastPathComponent();
            if (recDefTreeNode != rdn) setRecDefTreeNode(rdn);
            StatsTreeNode stn = sipModel.getStatsModel().findNodeForInputPath(nodeMapping.inputPath);
            if (statsTreeNode != stn) setStatsTreeNode(stn);
        }
    }
    
    public boolean canCreate() {
        return statsTreeNode != null && recDefTreeNode != null && nodeMapping == null;
    }
    
    public void createMapping() {
        if (!canCreate()) throw new RuntimeException("Should have checked");
        setNodeMapping(recDefTreeNode.addStatsTreeNode(statsTreeNode));
    }

    // observable

    public interface Listener {

        void statsTreeNodeSet(CreateModel createModel);
        void recDefTreeNodeSet(CreateModel createModel);
        void nodeMappingSet(CreateModel createModel);

    }

    private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

    public void addListener(Listener listener) {
        listeners.add(listener);
    }
}
