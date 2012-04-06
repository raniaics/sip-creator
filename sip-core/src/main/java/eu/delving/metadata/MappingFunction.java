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

package eu.delving.metadata;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;

import java.io.InputStream;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class describes a utility function (closure) which is available in the
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("mapping-function")
public class MappingFunction implements Comparable<MappingFunction> {

    @XStreamAsAttribute
    public String name;

    @XStreamAlias("sample-input")
    public List<String> sampleInput;

    @XStreamAlias("documentation")
    public List<String> documentation;

    @XStreamAlias("groovy-code")
    public List<String> groovyCode;

    public MappingFunction() {
    }

    public MappingFunction(String name) {
        this.name = name;
    }

    public String getDocumentation() {
        return StringUtil.linesToString(documentation);
    }

    public void setDocumentation(String documentation) {
        this.documentation = StringUtil.stringToLines(documentation);
    }

    public void setSampleInput(String sampleInput) {
        this.sampleInput = StringUtil.stringToLines(sampleInput);
    }

    public void setGroovyCode(String groovyCode) {
        this.groovyCode = StringUtil.stringToLines(groovyCode);
    }

    public String getSampleInputString() {
        return StringUtil.linesToString(sampleInput);
    }

    public String getUserCode(String editedCode) {
        CodeOut codeOut = CodeOut.create();
        toUserCode(codeOut, editedCode);
        return codeOut.toString();
    }

    public String getUserCode() {
        return getUserCode(null);
    }

    public String toCode(String editedCode) {
        CodeOut codeOut = CodeOut.create();
        toCode(codeOut, editedCode);
        return codeOut.toString();
    }

    public void toCode(CodeOut codeOut) {
        toCode(codeOut, null);
    }

    public void toCode(CodeOut codeOut, String editedCode) {
        codeOut.line_(String.format("def %s = { it ->", name));
        toUserCode(codeOut, editedCode);
        codeOut._line("}");
    }

    private void toUserCode(CodeOut codeOut, String editedCode) {
        if (editedCode != null) {
            StringUtil.indentCode(editedCode, codeOut);
        }
        else if (groovyCode != null) {
            StringUtil.indentCode(groovyCode, codeOut);
        }
        else {
            codeOut.line("it");
        }
    }

    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingFunction function = (MappingFunction) o;
        return !(name != null ? !name.equals(function.name) : function.name != null);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public int compareTo(MappingFunction mappingFunction) {
        return this.name.compareTo(mappingFunction.name);
    }

    @XStreamAlias("mapping-function-list")
    public static class FunctionList {
        @XStreamImplicit
        public SortedSet<MappingFunction> functions = new TreeSet<MappingFunction>();
    }
    
    public static FunctionList read(InputStream inputStream) {
        return (FunctionList) stream().fromXML(inputStream);
    }

    private static XStream stream() {
        XStream stream = new XStream(new PureJavaReflectionProvider());
        stream.processAnnotations(FunctionList.class);
        return stream;
    }

}

