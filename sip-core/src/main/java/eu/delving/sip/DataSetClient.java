package eu.delving.sip;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipInputStream;
import javax.swing.*;

import com.thoughtworks.xstream.XStream;
import eu.delving.metadata.Hasher;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

/**
 * The tab related to interacting with the metadata repository
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class DataSetClient {
    private static final int LIST_FETCH_DELAY_MILLIS = 5000;
    private Logger log = Logger.getLogger(getClass());
    private Executor executor = Executors.newFixedThreadPool(2);
    private boolean fetching;
    private Timer periodicListFetchTimer;
    private Context context;

    public interface Context {
        String getServerUrl();

        String getAccessToken();

        void setInfo(DataSetInfo dataSetInfo);

        void setList(List<DataSetInfo> list);

        void tellUser(String message);

        void disconnected();
    }

    public DataSetClient(Context context) {
        this.context = context;
        periodicListFetchTimer = new Timer(LIST_FETCH_DELAY_MILLIS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executor.execute(new ListFetcher());
            }
        });
        periodicListFetchTimer.setRepeats(false);
    }

    public void setListFetchingEnabled(boolean enable) {
        fetching = enable;
        if (enable) {
            executor.execute(new ListFetcher());
            periodicListFetchTimer.restart();
        } else {
            periodicListFetchTimer.stop();
        }
    }

    public boolean isConnected() {
        return fetching;
    }

    public void sendCommand(String spec, DataSetCommand command) {
        executor.execute(new CommandSender(spec, command));
    }

    public void uploadFile(FileType fileType, String spec, File file, boolean zipStream, ProgressListener progressListener, UploadCallback callback) {
        executor.execute(new FileUploader(fileType, spec, file, zipStream, progressListener, callback));
    }

    public void uploadXMLStream(InputStream stream, String spec, String uploadType, String contentType, String contentName, ProgressListener progressListener, UploadCallback callback) {
        executor.execute(new XMLStreamUploader(stream, spec, uploadType, contentType, contentName, progressListener, callback));
    }


    public void downloadDataSet(FileStore.DataSetStore dataSetStore, ProgressListener progressListener) {
        executor.execute(new DataSetDownloader(dataSetStore, progressListener));
    }

    private class ListFetcher implements Runnable {
        @Override
        public void run() {
            doRun();
        }

        private void doRun() {
            fetching = true;
            String url = String.format(
                    "%s?accessKey=%s",
                    context.getServerUrl(),
                    context.getAccessToken()
            );
            final DataSetResponse response = execute(new HttpGet(url));
            if (response != null) {
                if (response.isEverythingOk()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            context.setList(response.getDataSetList());
                            periodicListFetchTimer.restart();
                        }
                    });
                } else if(response.isAccessTokenExpired()) {
                    // the OAuth2 client will handle this
                    doRun();
                } else {
                    fetching = false;
                    periodicListFetchTimer.stop();
                    notifyUser(response.getResponseCode());
                }
            }
        }
    }

    private class CommandSender implements Runnable {
        private String spec;
        private DataSetCommand command;

        private CommandSender(String spec, DataSetCommand command) {
            this.spec = spec;
            this.command = command;
        }

        @Override
        public void run() {
            doRun();
        }

        private void doRun() {
            String url = String.format(
                    "%s/%s/%s?accessKey=%s",
                    context.getServerUrl(),
                    spec,
                    command,
                    context.getAccessToken()
            );
            final DataSetResponse response = execute(new HttpGet(url));
            if (response != null) {
                if (response.isEverythingOk()) {
                    if (response.getDataSetList() == null || response.getDataSetList().size() != 1) {
                        throw new RuntimeException("Expected exactly one Info object");
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            context.setInfo(response.getDataSetList().get(0));
                        }
                    });
                } else if(response.isAccessTokenExpired()) {
                    // the OAuth2 client will handle this
                    doRun();
                } else {
                    notifyUser(response);
                }
            } // otherwise we will have disconnected
        }
    }

    /**
     * Uploads things
     *
     * @author Gerald de Jong <geralddejong@gmail.com>
     * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
     */
    public abstract class Uploader implements Runnable {
        protected Logger log = Logger.getLogger(getClass());
        protected String spec;
        protected ProgressListener progressListener;
        protected UploadCallback callback;
        protected String uploadType;

        public Uploader(String spec, String uploadType, ProgressListener progressListener, UploadCallback callback) {
            this.spec = spec;
            this.callback = callback;
            this.progressListener = progressListener;
            this.uploadType = uploadType;
        }

        @Override
        public void run() {
            progressListener.setTotal(getTotalSize());
            try {
                beforeUpload();
                final DataSetResponse response = doUpload();
                boolean success = response != null && response.getResponseCode() == DataSetResponseCode.THANK_YOU;
                progressListener.finished(success);
                if (!success) {
                    notifyUser(response);
                }
                if (callback != null) {
                    callback.onResponseReceived(response);
                }

            } catch (Exception e) {
                handleException(e);
                progressListener.finished(false);
                notifyUser(e);
            }
        }

        protected String createRequestUrl() {
            return String.format(
                    "%s/submit/%s/%s/%s?accessKey=%s",
                    context.getServerUrl(),
                    spec,
                    uploadType,
                    getContentName(),
                    context.getAccessToken()
            );
        }

        protected DataSetResponse doUpload() {
            HttpPost httpPost = new HttpPost(createRequestUrl());
            httpPost.setEntity(createPayload());
            return execute(httpPost);
        }

        protected abstract void beforeUpload() throws Exception;

        protected abstract HttpEntity createPayload();

        protected abstract void handleException(Exception e);

        protected abstract int getTotalSize();

        protected abstract String getContentName();

        protected abstract String getContentType();
    }

    /**
     * Zip up a file and upload it
     *
     * @author Gerald de Jong <geralddejong@gmail.com>
     */

    public class FileUploader extends Uploader {
        protected static final int BLOCK_SIZE = 4096;

        private File file;
        private FileType fileType;
        private boolean zipStream;

        public FileUploader(FileType fileType, String spec, File file, boolean zipStream, ProgressListener progressListener, UploadCallback callback) {
            super(spec, fileType.toString(), progressListener, callback);
            this.fileType = fileType;
            this.file = file;
            this.zipStream = zipStream;
        }

        @Override
        protected int getTotalSize() {
            return (int) (file.length() / BLOCK_SIZE);
        }

        @Override
        protected void beforeUpload() throws Exception {
            file = Hasher.ensureFileHashed(file);
            log.info("Uploading " + file);
        }

        @Override
        protected String getContentName() {
            return file.getName();
        }

        @Override
        protected String getContentType() {
            return zipStream ? "application/x-gzip" : fileType.getContentType();
        }

        @Override
        protected HttpEntity createPayload() {
            FileEntity fileEntity = new FileEntity(file, getContentType(), zipStream);
            fileEntity.setChunked(true);
            return fileEntity;
        }

        @Override
        protected void handleException(Exception e) {
            log.warn("Unable to upload file " + file.getAbsolutePath(), e);
        }

        private class FileEntity extends AbstractHttpEntity implements Cloneable {

            private final File file;
            private long bytesSent;
            private int blocksReported;
            private boolean abort = false;
            private final boolean gzip;

            public FileEntity(final File file, final String contentType, final boolean gzip) {
                if (file == null) {
                    throw new IllegalArgumentException("File may not be null");
                }
                this.file = file;
                this.gzip = gzip;
                setContentType(contentType);
            }

            public boolean isRepeatable() {
                return true;
            }

            public long getContentLength() {
                return this.file.length();
            }

            public InputStream getContent() throws IOException {
                return new FileInputStream(this.file);
            }

            public void writeTo(final OutputStream outputStream) throws IOException {
                if (outputStream == null) {
                    throw new IllegalArgumentException("Output stream may not be null");
                }
                final OutputStream stream = gzip ? new GZIPOutputStream(outputStream) : outputStream;
                InputStream inputStream = new FileInputStream(this.file);
                try {
                    byte[] buffer = new byte[BLOCK_SIZE];
                    int bytes;
                    while (!abort && (bytes = inputStream.read(buffer)) != -1) {
                        stream.write(buffer, 0, bytes);
                        bytesSent += bytes;
                        int blocks = (int) (bytesSent / BLOCK_SIZE);
                        if (blocks > blocksReported) {
                            blocksReported = blocks;
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    if (!progressListener.setProgress(blocksReported)) {
                                        abort = true;
                                    }
                                }
                            });
                        }
                    }
                    stream.flush();
                } finally {
                    inputStream.close();
                }
            }

            /**
             * Tells that this entity is not streaming.
             *
             * @return <code>false</code>
             */
            public boolean isStreaming() {
                return false;
            }

            public Object clone() throws CloneNotSupportedException {
                // File instance is considered immutable
                // No need to make a copy of it
                return super.clone();
            }
        }
    }

    public interface UploadCallback {

        void onResponseReceived(DataSetResponse response);

    }

    /**
     * Uploads an XML Stream
     *
     * @author Manuel Bernhardt <bernhardt.manuel@gmail.com/>
     */
    public class XMLStreamUploader extends Uploader {

        final private String contenType;
        final private String contentName;
        final private InputStream stream;

        public XMLStreamUploader(InputStream stream, String spec, String uploadType, String contentType, String contentName, ProgressListener progressListener, UploadCallback callback) {
            super(spec, uploadType, progressListener, callback);
            this.stream = stream;
            this.contentName = contentName;
            this.contenType = contentType;
        }


        @Override
        protected void beforeUpload() throws Exception {
            log.info("Uploading XML stream...");
        }

        @Override
        protected String getContentType() {
            return contenType;
        }

        @Override
        protected String getContentName() {
            return contentName;
        }

        @Override
        protected HttpEntity createPayload() {
            InputStreamEntity payload = new InputStreamEntity(stream, -1);
            payload.setContentType(getContentType());
            payload.setChunked(true);
            return payload;
        }

        @Override
        protected void handleException(Exception e) {
            log.warn("Unable to upload stream", e);
        }

        @Override
        protected int getTotalSize() {
            return -1; // we don't know
        }
    }


    private class DataSetDownloader implements Runnable {
        private FileStore.DataSetStore dataSetStore;
        private ProgressListener progressListener;

        private DataSetDownloader(FileStore.DataSetStore dataSetStore, ProgressListener progressListener) {
            this.dataSetStore = dataSetStore;
            this.progressListener = progressListener;
        }

        @Override
        public void run() {
            HttpClient httpClient = new DefaultHttpClient();
            try {
                HttpGet method = new HttpGet(String.format(
                        "%s/fetch/%s-sip.zip?accessKey=%s",
                        context.getServerUrl(),
                        dataSetStore.getSpec(),
                        context.getAccessToken()
                ));
                HttpResponse httpResponse = httpClient.execute(method);
                if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    HttpEntity entity = httpResponse.getEntity();
                    ZipInputStream zipInputStream = new ZipInputStream(entity.getContent());
                    dataSetStore.acceptSipZip(zipInputStream, progressListener);
                } else {
                    log.warn("Unable to download source. HTTP response " + httpResponse.getStatusLine().getReasonPhrase());
                }
            } catch (Exception e) {
                log.warn("Unable to download source", e);
                context.tellUser("Unable to download source");
            }
        }
    }

    private DataSetResponse execute(HttpGet httpGet) {
        HttpClient httpClient = new DefaultHttpClient();
        try {
            return translate(httpClient.execute(httpGet));
        } catch (HttpHostConnectException e) {
            log.warn("Problem executing get (connecting)", e);
            forceDisconnect();
            return null;
        } catch (IOException e) {
            log.warn("Problem executing get (I/O)", e);
            forceDisconnect();
            return null;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private DataSetResponse execute(HttpPost httpPost) {
        log.info("POST: " + httpPost.getURI());
        HttpClient httpClient = new DefaultHttpClient();
        try {
            return translate(httpClient.execute(httpPost));
        } catch (IOException e) {
            log.warn("Problem executing post", e);
            forceDisconnect();
            return null;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private DataSetResponse translate(HttpResponse httpResponse) throws IOException {
        if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            HttpEntity entity = httpResponse.getEntity();
            Reader reader = new InputStreamReader(entity.getContent(), "UTF-8");
            DataSetResponse response = (DataSetResponse) xstream().fromXML(reader);
            entity.consumeContent();
            return response;
        } else {
            throw new IOException("Response not OK: " + httpResponse.getStatusLine());
        }
    }

    private void notifyUser(final Exception exception) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.warn("Problem communicating", exception);
                context.tellUser("Sorry, there was a problem communicating with Repository");
                forceDisconnect();
            }
        });
    }

    private void forceDisconnect() {
        setListFetchingEnabled(false);
        context.disconnected();
    }

    private void notifyUser(DataSetResponse response) {
        notifyUser(response.getResponseCode());
    }

    private void notifyUser(final DataSetResponseCode responseCode) {
        switch (responseCode) {
            case THANK_YOU:
            case GOT_IT_ALREADY:
            case READY_TO_RECEIVE:
                log.info("Received " + responseCode);
                break;
            case ACCESS_KEY_FAILURE:
                context.tellUser("The Access Key is not correct for this Repository");
                break;
            case STATE_CHANGE_FAILURE:
                context.tellUser("Could not execute state change"); // todo
                break;
            case DATA_SET_NOT_FOUND:
                context.tellUser("The Data Set was not found in the Repository");
                break;
            case NEWORK_ERROR:
                context.tellUser("There was a network error while communicating with the Repository");
                break;
            case SYSTEM_ERROR:
                context.tellUser("Sorry, there was a system error in the Repository");
                break;
            case UNKNOWN_RESPONSE:
                log.error(responseCode.toString());
                break;
            default:
                break;
        }
    }

    private XStream xstream() {
        XStream stream = new XStream();
        stream.processAnnotations(new Class<?>[]{DataSetResponse.class});
        return stream;
    }

}
