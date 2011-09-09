package eu.delving.sip;

import groovy.util.Node;
import groovy.xml.QName;

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

    public static IndexDocument fromNode(Node node) {
        IndexDocument doc = new IndexDocument();
        List traversal = node.depthFirst();
        for (Object nodeObject : traversal) {
            Node n = (Node) nodeObject;
            if (n.value() instanceof List) {
                continue;
            }
            doc.put(mungePath(n), n.text()); // todo: probably have to check for List and Node in here
        }
        return doc;
    }

    private static String mungePath(Node node) {
        QName qname = (QName) node.name();
        return String.format("%s_%s", qname.getPrefix(), qname.getLocalPart());
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
}
