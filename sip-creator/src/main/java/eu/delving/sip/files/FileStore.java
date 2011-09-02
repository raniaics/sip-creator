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

package eu.delving.sip.files;

import eu.delving.metadata.MetadataModel;
import eu.delving.metadata.Path;
import eu.delving.metadata.RecordMapping;
import eu.delving.sip.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * This interface describes how files are stored by the sip-creator
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public interface FileStore {

    void setTemplate(String name, RecordMapping recordMapping) throws FileStoreException;

    Map<String, RecordMapping> getTemplates() throws FileStoreException;

    void deleteTemplate(String name) throws FileStoreException;

    Map<String, DataSetStore> getDataSetStores();

    DataSetStore createDataSetStore(String spec) throws FileStoreException;

    public enum StoreState {
        EMPTY,
        IMPORTED_PENDING_ANALYZE,
        IMPORTED_PENDING_CONVERT,
        SOURCED,
        ANALYZED,
        MAPPED,
        VALIDATED,
        PHANTOM
    }

    public interface DataSetStore {

        String getSpec();

        String getLatestPrefix();

        RecordMapping setLatestPrefix(String prefix) throws FileStoreException;

        MetadataModel getMetadataModel() throws FileStoreException;

        StoreState getState();

        Map<String,String> getDataSetFacts();

        Map<String, String> getHints();

        void setHints(Map<String, String> hints) throws FileStoreException;

        boolean isRecentlyImported();

        InputStream importedInput() throws FileStoreException;

        InputStream sourceInput() throws FileStoreException;

        Statistics getLatestStatistics();

        Statistics getStatistics(boolean sourceFormat);

        void setStatistics(Statistics statistics) throws FileStoreException;

        RecordMapping getRecordMapping(String metadataPrefix) throws FileStoreException;

        void setRecordMapping(RecordMapping recordMapping) throws FileStoreException;

        void setValidation(String metadataPrefix, BitSet validation, int recordCount) throws FileStoreException;

        PrintWriter reportWriter(RecordMapping recordMapping) throws FileStoreException;

        List<String> getReport(RecordMapping recordMapping) throws FileStoreException;

        void externalToImported(File inputFile, ProgressListener progressListener) throws FileStoreException;

        void importedToSource(ProgressListener progressListener) throws FileStoreException;

        List<File> getUploadFiles() throws FileStoreException;

        void fromSipZip(ZipInputStream zipInputStream, ProgressListener progressListener) throws IOException, FileStoreException;

        void remove() throws FileStoreException;

    }

    String IMPORTED_FILE_NAME = "imported.xml.gz";
    String SOURCE_FILE_NAME = "source.xml.gz";
    String ANALYSIS_STATS_FILE_NAME = "analysis_stats.ser";
    String SOURCE_STATS_FILE_NAME = "source_stats.ser";
    String FACTS_FILE_NAME = "facts.txt";
    String HINTS_FILE_NAME = "hints.txt";
    String PHANTOM_FILE_NAME = "phantom.txt";
    String MAPPING_FILE_PATTERN = "mapping_%s.xml";
    String MAPPING_FILE_PREFIX = "mapping_";
    String MAPPING_FILE_SUFFIX = ".xml";
    String VALIDATION_FILE_PATTERN = "validation_%s.int";
    String REPORT_FILE_PATTERN = "report_%s.txt";

    String RECORD_ROOT_PATH = "recordRootPath";
    String RECORD_COUNT = "recordCount";
    String UNIQUE_ELEMENT_PATH = "uniqueElementPath";

    String ENVELOPE_TAG = "delving-sip-source";
    String RECORD_TAG = "input";
    Path RECORD_ROOT = new Path(String.format("/%s/%s", ENVELOPE_TAG, RECORD_TAG));
}
