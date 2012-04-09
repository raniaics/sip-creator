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

import javax.swing.tree.TreePath;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This model holds the source and destination of a node mapping, and is observable.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class CreateModel {
    private SipModel sipModel;
    private SortedSet<SourceTreeNode> sourceTreeNodes;
    private RecDefTreeNode recDefTreeNode;
    private NodeMapping nodeMapping;
    private boolean settingNodeMapping;
    private List<NodeMapping> highlightedNodeMappings = new ArrayList<NodeMapping>();

    public CreateModel(SipModel sipModel) {
        this.sipModel = sipModel;
    }

    public void setSourceTreeNodes(SortedSet<SourceTreeNode> sourceTreeNodes) {
        if (sourceTreeNodes != null && sourceTreeNodes.isEmpty()) sourceTreeNodes = null;
        this.sourceTreeNodes = sourceTreeNodes;
        if (!settingNodeMapping) {
            setNodeMapping(null);
            clearHighlights();
            sipModel.getMappingModel().getRecDefTreeRoot().clearHighlighted();
            if (sourceTreeNodes != null) {
                for (SourceTreeNode node : sourceTreeNodes) {
                    for (NodeMapping nodeMapping : node.getNodeMappings()) {
                        highlight(nodeMapping);
                        RecDefTreeNode recDefTreeNode = sipModel.getMappingModel().getRecDefTreeRoot().getRecDefTreeNode(nodeMapping.recDefNode);
                        recDefTreeNode.setHighlighted();
                    }
                }
            }
        }
        for (Listener listener : listeners) listener.sourceTreeNodesSet(this);
    }

    public void setRecDefTreePath(Path path) {
        TreePath treePath = sipModel.getMappingModel().getTreePath(path);
        RecDefTreeNode node = ((RecDefTreeNode) (treePath.getLastPathComponent()));
        setRecDefTreeNode(node);
    }

    public void setRecDefTreeNode(RecDefTreeNode recDefTreeNode) {
        if (recDefTreeNode != null && recDefTreeNode.getParent() == null) recDefTreeNode = null;
        this.recDefTreeNode = recDefTreeNode;
        if (!settingNodeMapping) {
            setNodeMapping(null);
            clearHighlights();
            sipModel.getStatsModel().getSourceTree().clearHighlighted();
            if (recDefTreeNode != null) {
                for (NodeMapping nodeMapping : recDefTreeNode.getRecDefNode().getNodeMappings().values()) {
                    highlight(nodeMapping);
                    for (Object sourceTreeNodeObject : nodeMapping.getSourceTreeNodes()) {
                        ((SourceTreeNode) sourceTreeNodeObject).setHighlighted();
                    }
                }
            }
        }
        for (Listener listener : listeners) listener.recDefTreeNodeSet(this);
    }

    public void setNodeMapping(NodeMapping nodeMapping) {
        this.nodeMapping = nodeMapping;
        if (nodeMapping != null) {
            settingNodeMapping = true;
            setSourceTreeNodes(sipModel.getStatsModel().findNodesForInputPaths(nodeMapping));
            TreePath treePath = sipModel.getMappingModel().getTreePath(nodeMapping.outputPath);
            if (treePath != null) setRecDefTreeNode((RecDefTreeNode) treePath.getLastPathComponent());
            clearHighlights();
            for (Object sourceTreeNodeObject : nodeMapping.getSourceTreeNodes()) {
                ((SourceTreeNode) sourceTreeNodeObject).setHighlighted();
            }
            RecDefTreeNode recDefTreeNode = sipModel.getMappingModel().getRecDefTreeRoot().getRecDefTreeNode(nodeMapping.recDefNode);
            recDefTreeNode.setHighlighted();
            settingNodeMapping = false;
        }
        for (Listener listener : listeners) listener.nodeMappingSet(this);
    }

    public SortedSet<SourceTreeNode> getSourceTreeNodes() {
        return sourceTreeNodes;
    }

    public RecDefTreeNode getRecDefTreeNode() {
        return recDefTreeNode;
    }

    public NodeMapping getNodeMapping() {
        return nodeMapping;
    }

    public void revertToOriginal() {
        if (nodeMapping != null) nodeMapping.setGroovyCode(null, null);
    }

    public boolean canCreate() {
        if (recDefTreeNode == null || sourceTreeNodes == null) return false;
        if (nodeMapping == null) {
            nextNodeMapping:
            for (NodeMapping mapping : recDefTreeNode.getRecDefNode().getNodeMappings().values()) {
                Iterator<Path> pathIterator = mapping.getInputPaths().iterator();
                for (Path inputPath : mapping.getInputPaths()) {
                    if (!pathIterator.hasNext() || !inputPath.equals(pathIterator.next())) continue nextNodeMapping;
                }
                nodeMapping = mapping;
                break;
            }
        }
        return nodeMapping == null;
    }

    public void createMapping() {
        if (!canCreate()) throw new RuntimeException("Should have checked");
        NodeMapping created = new NodeMapping().setOutputPath(recDefTreeNode.getRecDefPath().getTagPath());
        created.recDefNode = recDefTreeNode.getRecDefNode();
        SourceTreeNode.setStatsTreeNodes(sourceTreeNodes, created);
        recDefTreeNode.addNodeMapping(created);
        setNodeMapping(created);
    }

    public void createMapping(NodeMapping nodeMapping) {
        TreePath treePath = sipModel.getMappingModel().getTreePath(nodeMapping.outputPath);
        RecDefTreeNode recDefTreeNode = (RecDefTreeNode) treePath.getLastPathComponent();
        nodeMapping.recDefNode = recDefTreeNode.getRecDefNode();
        SourceTreeNode.setStatsTreeNodes(sipModel.getStatsModel().findNodesForInputPaths(nodeMapping), nodeMapping);
        recDefTreeNode.addNodeMapping(nodeMapping);
        setNodeMapping(nodeMapping);
    }

    public static boolean isDictionaryPossible(NodeMapping nodeMapping) {
        if (nodeMapping == null || nodeMapping.recDefNode == null || !nodeMapping.hasOneStatsTreeNode()) return false;
        SourceTreeNode sourceTreeNode = (SourceTreeNode) nodeMapping.getSingleStatsTreeNode();
        if (sourceTreeNode.getStatistics() == null) return false;
        Set<String> values = sourceTreeNode.getStatistics().getHistogramValues();
        List<String> options = nodeMapping.recDefNode.getOptions();
        return values != null && options != null && nodeMapping.dictionary == null;
    }

    public static boolean refreshDictionary(NodeMapping nodeMapping) {
        if (!isDictionaryPossible(nodeMapping)) throw new RuntimeException("Should have checked");
        SourceTreeNode sourceTreeNode = (SourceTreeNode) nodeMapping.getSingleStatsTreeNode();
        if (sourceTreeNode.getStatistics() == null) return false;
        return nodeMapping.setDictionaryDomain(sourceTreeNode.getStatistics().getHistogramValues());
    }

    public void removeDictionary() {
        if (nodeMapping.removeDictionary()) fireNodeMappingChanged();
    }

    public boolean isDictionaryPossible() {
        return isDictionaryPossible(nodeMapping);
    }

    public boolean isDictionaryPresent() {
        return nodeMapping != null && nodeMapping.dictionary != null;
    }

    public void fireDictionaryEntriesChanged() {
        fireNodeMappingChanged();
    }

    public void refreshDictionary() {
        if (refreshDictionary(nodeMapping)) fireNodeMappingChanged();
    }

    public int countNonemptyDictionaryEntries() {
        int nonemptyEntries = 0;
        if (isDictionaryPresent()) {
            for (String value : nodeMapping.dictionary.values()) if (!value.trim().isEmpty()) nonemptyEntries++;
        }
        return nonemptyEntries;
    }

    private void clearHighlights() {
        highlightedNodeMappings.clear();
        sipModel.getStatsModel().getSourceTree().clearHighlighted();
        sipModel.getMappingModel().getRecDefTreeRoot().clearHighlighted();
    }

    private void highlight(NodeMapping nodeMapping) {
        highlightedNodeMappings.add(nodeMapping);
    }

    // observable

    public interface Listener {

        void sourceTreeNodesSet(CreateModel createModel);

        void recDefTreeNodeSet(CreateModel createModel);

        void nodeMappingSet(CreateModel createModel);

        void nodeMappingChanged(CreateModel createModel);

    }

    private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    private void fireNodeMappingChanged() {
        for (Listener listener : listeners) listener.nodeMappingChanged(this);
    }
}
