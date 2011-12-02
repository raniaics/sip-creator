/*
 * Copyright 2010 DELVING BV
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

package eu.delving.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrap a recdef to contain a tree and the recdef for reference
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class RecDefTree implements RecDefNode.Listener {
    private RecDef recDef;
    private RecDefNode root;
    private RecDefNode.Listener listener;

    public static RecDefTree create(RecDef recDef) {
        return new RecDefTree(recDef);
    }

    private RecDefTree(RecDef recDef) {
        this.recDef = recDef;
        this.root = RecDefNode.create(this, recDef);
    }

    public void setListener(RecDefNode.Listener listener) {
        this.listener = listener;
    }

    public RecDef getRecDef() {
        return recDef;
    }

    public RecDefNode getRoot() {
        return root;
    }

    public RecDefNode getRecDefNode(Path path) {
        return root.getNode(new Path(), path);
    }

    public List<NodeMapping> getNodeMappings() {
        List<NodeMapping> nodeMappings = new ArrayList<NodeMapping>();
        root.collect(new Path(), nodeMappings);
        return nodeMappings;
    }

    public String toCode(Path selectedPath, String editedCode) {
        StringBuilder out = new StringBuilder();
        // todo: write the code!
        return out.toString();
    }

    public List<RecDef.Namespace> getNamespaces() {
        return recDef.namespaces;
    }

    @Override
    public void nodeMappingSet(RecDefNode recDefNode) {
        if (listener != null) listener.nodeMappingSet(recDefNode);
    }
}
