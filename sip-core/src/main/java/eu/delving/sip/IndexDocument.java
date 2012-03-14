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

package eu.delving.sip;

import eu.delving.metadata.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This is an object which contains the output of mapping/validation followed by
 * some path munging, so that it can be used as food for the indexing machine.
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class IndexDocument {
    private Map<String, List<Value>> map = new TreeMap<String, List<Value>>();

    public static IndexDocument fromNode(Node inputNode, RecDefTree recordDefinition) {
        IndexDocument doc = new IndexDocument();
        NodeList kids = inputNode.getChildNodes();
        for (int walk = 0; walk < kids.getLength(); walk++) {
            Node kid = kids.item(walk);
            switch (kid.getNodeType()) {
                case Node.ATTRIBUTE_NODE:
                    throw new RuntimeException("Attributes not implemented");
                case Node.TEXT_NODE:
                    throw new RuntimeException("Text Nodes not implemented");
                case Node.ELEMENT_NODE:
                    NodeList grandKid = kid.getChildNodes();
                    if (grandKid.getLength() != 1) throw new RuntimeException("Expected only one grandchild node");
                    Node textNode = grandKid.item(0);
                    if (textNode.getNodeType() != Node.TEXT_NODE) throw new RuntimeException("Expected text grandchild node");
                    Path path = Path.empty().extend(Tag.element(recordDefinition.getRecDef().prefix, "record"));
                    path = path.extend(Tag.element(kid.getPrefix(), kid.getLocalName()));
                    RecDefNode recDefNode = recordDefinition.getRecDefNode(path);
                    if (recDefNode == null) throw new RuntimeException("No recdef node for "+path);
                    doc.put(String.format("%s_%s_%s", kid.getPrefix(), kid.getLocalName(), recDefNode.getFieldType()), textNode.getNodeValue());
                    SummaryField summaryField = recDefNode.getSummaryField();
                    if (summaryField != null) doc.put(summaryField.tag, textNode.getNodeValue());
                    break;
                default:
                    throw new RuntimeException("Node type not implemented: " + kid.getNodeType());
            }
        }
        return doc;
    }

    private IndexDocument() {
    }

    public void put(String key, String value) {
        List<Value> list = map.get(key);
        if (list == null) {
            list = new ArrayList<Value>(4);
            map.put(key, list);
        }
        list.add(new Value(value));
    }

    public Map<String, List<Value>> getMap() {
        return map;
    }

    public class Value {
        // attributes as well sometime in the future
        private String text;

        public Value(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public String toString() {
        StringBuilder out = new StringBuilder("IndexDocument {\n");
        for (Map.Entry<String, List<Value>> entry : map.entrySet()) {
            if (entry.getValue().size() == 1) {
                out.append(String.format("\t%s -> %s\n", entry.getKey(), entry.getValue().get(0)));
            }
            else {
                out.append(String.format("\t%s ->\n", entry.getKey()));
                for (Value value : entry.getValue()) {
                    out.append(String.format("\t\t%s\n", value.toString()));
                }
            }
        }
        out.append("}\n");
        return out.toString();
    }

//    private static void traverse(Node node, int level) {
//        NodeList childNodes = node.getChildNodes();
//        for (int walk = 0; walk < childNodes.getLength(); walk++) {
//            Node child = childNodes.item(walk);
//            String type = "?";
//            switch (child.getNodeType()) {
//                case Node.ATTRIBUTE_NODE:
//                    type = "attr";
//                    break;
//                case Node.ELEMENT_NODE:
//                    type = "elem";
//                    break;
//                case Node.TEXT_NODE:
//                    type = "text";
//                    break;
//            }
//            String string = String.format("%d: %s: %s = %s", level, type, child.getNodeName(), child.getNodeValue());
//            System.out.println(StringUtils.leftPad(string, level + 1, '\t'));
//            traverse(child, level + 1);
//        }
//    }
}

