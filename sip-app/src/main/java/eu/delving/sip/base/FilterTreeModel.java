package eu.delving.sip.base;

import eu.delving.sip.model.FilterTreeNode;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
* Generic filter tree model
*
* @author Gerald de Jong <gerald@delving.eu>
*/

public class FilterTreeModel extends DefaultTreeModel {

    public FilterTreeModel(FilterTreeNode root) {
        super(root);
    }

    public void setFilter(Pattern pattern) {
        node(root).setPassesFilter(false);
        node(root).filter(pattern);
        fireTreeStructureChanged(this, new TreeNode[]{root}, new int[]{}, new Object[]{});
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public Object getChild(Object nodeObject, int index) {
        return filterChildren(nodeObject).get(index);
    }

    @Override
    public int getChildCount(Object nodeObject) {
        FilterTreeNode node = node(nodeObject);
        int count = 0;
        for (FilterTreeNode sub : node.getChildren()) if (sub.passesFilter()) count++;
        return count;
    }

    @Override
    public boolean isLeaf(Object nodeObject) {
        FilterTreeNode node = node(nodeObject);
        return node.isLeaf();
    }

    @Override
    public int getIndexOfChild(Object nodeObject, Object child) {
        return filterChildren(nodeObject).indexOf(node(child));
    }

    private FilterTreeNode node(Object nodeObject) {
        return (FilterTreeNode) nodeObject;
    }

    private List<FilterTreeNode> filterChildren(Object nodeObject) {
        List<FilterTreeNode> filtered = new ArrayList<FilterTreeNode>();
        for (FilterTreeNode sub : node(nodeObject).getChildren()) if (sub.passesFilter()) filtered.add(sub);
        return filtered;
    }

}
