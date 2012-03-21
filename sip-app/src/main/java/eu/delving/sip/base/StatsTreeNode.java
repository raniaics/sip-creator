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

package eu.delving.sip.base;

import eu.delving.metadata.FieldStatistics;
import eu.delving.metadata.Path;
import eu.delving.metadata.Tag;
import org.antlr.stringtemplate.StringTemplate;

import javax.swing.BorderFactory;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * A node of the analysis tree
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class StatsTreeNode implements TreeNode, Comparable<StatsTreeNode> {
    private StatsTreeNode parent;
    private List<StatsTreeNode> children = new ArrayList<StatsTreeNode>();
    private Tag tag;
    private boolean recordRoot, uniqueElement;
    private FieldStatistics fieldStatistics;
    private String htmlChunk;

    StatsTreeNode(StatsTreeNode parent, String name, String htmlChunk) {
        this.parent = parent;
        this.tag = Tag.create(name);
        this.htmlChunk = htmlChunk;
    }

    StatsTreeNode(String name, String htmlChunk) {
        this(null, name, htmlChunk);
    }

    StatsTreeNode(StatsTreeNode parent, Tag tag) {
        if ((this.parent = parent) != null) parent.children.add(this);
        this.tag = tag;
    }

    StatsTreeNode(StatsTreeNode parent, Map.Entry<String, String> entry) {
        this(parent, Tag.create(entry.getKey()));
        StringTemplate template = Utility.getTemplate("fact-brief");
        template.setAttribute("fact", entry);
        this.htmlChunk = template.toString();
    }

    StatsTreeNode(StatsTreeNode parent, FieldStatistics fieldStatistics) {
        this(parent, fieldStatistics.getPath().peek());
        setStatistics(fieldStatistics);
    }

    public void setStatistics(FieldStatistics fieldStatistics) {
        this.fieldStatistics = fieldStatistics;
        StringTemplate template = Utility.getTemplate("stats-brief");
        template.setAttribute("stats", fieldStatistics);
        this.htmlChunk = template.toString();
    }

//    public StatsTreeNode extractChild() {
//        if (getChildren().size() != 1) throw new IllegalStateException("One child expected");
//        children.get(0).parent = null;
//        return children.get(0);
//    }

    public List<StatsTreeNode> getChildren() {
        return children;
    }

    public boolean hasStatistics() {
        return fieldStatistics != null;
    }

    public FieldStatistics getStatistics() {
        return fieldStatistics;
    }

    public TreePath getTreePath() {
        List<StatsTreeNode> list = new ArrayList<StatsTreeNode>();
        compilePathList(list, true);
        return new TreePath(list.toArray());
    }

    public Tag getTag() {
        return tag;
    }

    public Path getPath(boolean fromRoot) {
        List<StatsTreeNode> list = new ArrayList<StatsTreeNode>();
        compilePathList(list, fromRoot);
        Path path = Path.empty();
        for (StatsTreeNode node : list) path.push(node.getTag());
        return path;
    }

    public boolean setRecordRoot(Path recordRoot) {
        boolean oldValue = this.recordRoot;
        this.recordRoot = recordRoot != null && getPath(true).equals(recordRoot);
        return this.recordRoot || this.recordRoot != oldValue;
    }

    public boolean setUniqueElement(Path uniqueElement) {
        boolean oldValue = this.uniqueElement;
        this.uniqueElement = uniqueElement != null && getPath(true).equals(uniqueElement);
        return this.uniqueElement != oldValue;
    }

    public boolean isRecordRoot() {
        return recordRoot;
    }

    public boolean isUniqueElement() {
        return uniqueElement;
    }

    public Iterable<? extends StatsTreeNode> getChildNodes() {
        return children;
    }

    public boolean couldBeRecordRoot() {
        return fieldStatistics != null && !fieldStatistics.hasValues();
    }

    public boolean couldBeUniqueElement() {
        if (couldBeRecordRoot()) return false;
        StatsTreeNode walk = parent;
        while (walk != null) { // ancestor must be record root
            if (walk.isRecordRoot()) return true;
            walk = walk.parent;
        }
        return false;
    }

    public void showPath(final JTree tree, final Path pathToShow) {
        final Path here = getPath(true);
        Timer timer = new Timer(30, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if ((pathToShow.equals(here) || pathToShow.isAncestorOf(here)) && !tree.isExpanded(getTreePath())) {
                    tree.expandPath(getTreePath());
                }
                else if ((here.size() <= pathToShow.size() && !here.isAncestorOf(pathToShow)) && !tree.isCollapsed(getTreePath())) {
                    tree.collapsePath(getTreePath());
                }
                for (StatsTreeNode sub : children) sub.showPath(tree, pathToShow);
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void compilePathList(List<StatsTreeNode> list, boolean fromRoot) {
        if (fromRoot) {
            if (parent != null) parent.compilePathList(list, fromRoot);
            list.add(this);
        }
        else if (parent != null) { // only take nodes with parents
            parent.compilePathList(list, fromRoot);
            list.add(this);
        }
    }

    @Override
    public TreeNode getChildAt(int index) {
        return children.get(index);
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public TreeNode getParent() {
        return parent;
    }

    @Override
    public int getIndex(TreeNode treeNode) {
        StatsTreeNode qNameNode = (StatsTreeNode) treeNode;
        return children.indexOf(qNameNode);
    }

    @Override
    public boolean getAllowsChildren() {
        return !children.isEmpty();
    }

    @Override
    public boolean isLeaf() {
        return children.isEmpty();
    }

    @Override
    public Enumeration children() {
        return new Vector<StatsTreeNode>(children).elements();
    }

    @Override
    public int compareTo(StatsTreeNode other) {
        return getPath(true).compareTo(other.getPath(true));
    }

    public String toHtml() {
        return String.format("<html>%s</html>", toHtmlChunk());
    }

    public String toHtmlChunk() {
        return htmlChunk;
    }

    public String toString() {
        if (tag == null) {
            return "?";
        }
        else if (fieldStatistics != null) {
            return String.format("%s (%d)", tag.toString(), fieldStatistics.getTotal());
        }
        else {
            return tag.toString();
        }
    }

    public static class Renderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof StatsTreeNode) {
                StatsTreeNode node = (StatsTreeNode) value;
                if (node.getTag().isAttribute()) {
                    setIcon(Utility.ATTRIBUTE_ICON);
                }
                else if (node.getChildNodes().iterator().hasNext()) {
                    setIcon(Utility.COMPOSITE_ELEMENT_ICON);
                }
                else {
                    setIcon(Utility.VALUE_ELEMENT_ICON);
                }
                if (node.isRecordRoot() || node.isUniqueElement()) {
                    setOpaque(!sel);
                    setBackground(sel ? Color.WHITE : Color.YELLOW);
                    setForeground(sel ? Color.YELLOW : Color.BLACK);
                    setText(String.format("%s << %s", node.toString(), node.isRecordRoot() ? "Record Root" : "Unique Element"));
                    setBorder(BorderFactory.createEtchedBorder());
                }
                else {
                    setOpaque(false);
                    setBorder(null);
                }
            }
            return component;
        }
    }

}
