/*
 * Copyright 2011, 2012 Delving BV
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

package eu.delving;

import eu.delving.groovy.MappingException;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

/**
 * Provide a mapping service to take text XML.
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public interface MappingEngine {

    /**
     * Take the XML record and execute the mapping, with a result that has various ways of accessing
     * the mapped record data.
     *
     * @param id the record id
     * @param recordXML what gets parsed and mapped
     * @return a result with
     * @throws XMLStreamException
     * @throws MappingException
     * @throws IOException
     * @throws SAXException
     */

    MappingResult execute(String id, String recordXML) throws XMLStreamException, MappingException, IOException, SAXException;

}
