package com.vivosys.test.persistence.ops.orientdb;

import com.orientechnologies.orient.client.remote.OEngineRemote;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectPool;
import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.exception.OQueryParsingException;
import com.orientechnologies.orient.core.index.OIndexes;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.vivosys.test.persistence.ops.OptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the OrientDB database connection pool, and allows operations to be executed against one of the
 * connections in the pool.
 *
 * // TODO profile and create indexes http://code.google.com/p/orient/wiki/PerformanceTuning#Use_Indexes
 */
public class DatabaseManager {

    private static Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private static final int DEFAULT_POOL_MIN_SIZE = 1;
    private static final int DEFAULT_POOL_MAX_SIZE = 10;

    private ODatabaseObjectPool objectConnectionPool;
    private ODatabaseDocumentPool documentConnectionPool;

    private int poolMinSize = DEFAULT_POOL_MIN_SIZE;
    private int poolMaxSize = DEFAULT_POOL_MAX_SIZE;
    private String dbUrl;
    private String username;
    private String password;

    public void setPoolMinSize(int poolMinSize) {

        this.poolMinSize = poolMinSize;

    }

    public void setPoolMaxSize(int poolMaxSize) {

        this.poolMaxSize = poolMaxSize;

    }

    public void setDbUrl(String dbUrl) {

        this.dbUrl = dbUrl;

    }

    public void setUsername(String username) {

        this.username = username;

    }

    public void setPassword(String password) {

        this.password = password;

    }

    public void init() {

        setupDbConnections();

    }

    public void shutdown(){

        tearDownDbConnections();

    }

    public synchronized void setupDbConnections(){

        log.info("Attempting to establish connection pools to orient db server at {} ", dbUrl);

        if(dbUrl.startsWith("local")) {

            ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl);
            if(! db.exists()) {
                db.create();
            }
            db.close();

        }

        // disable caches for now, need to do more investigation around caching
        // TODO enable caching for all or specific queries? http://code.google.com/p/orient/wiki/Caching
        OGlobalConfiguration.CACHE_LEVEL1_ENABLED.setValue(false);
        OGlobalConfiguration.CACHE_LEVEL2_ENABLED.setValue(false);

        //establish db connection and pool
        Orient.instance().registerEngine(new OEngineRemote());

        objectConnectionPool = new ODatabaseSizedObjectPool(poolMinSize, poolMaxSize);
        objectConnectionPool.setup();

        log.info("Attempting to register and create db classes");

//        createDbObjectType(Foo.class, true);

        documentConnectionPool = new ODatabaseSizedDocumentPool(poolMinSize, poolMaxSize);
        documentConnectionPool.setup();

        createDbDocumentType(KeyValueStoreOrientDbImpl.COLLECTION);

    }

    public synchronized void tearDownDbConnections() {

        objectConnectionPool.close();
        objectConnectionPool = null;

        documentConnectionPool.close();
        documentConnectionPool = null;

    }

    public <T> T executeObjCall(final DatabaseObjCall<T> call) {

        ODatabaseObjectTx db = null;
        ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();

        try {

            // Orient uses ServiceRegistry to load OIndexFactory. Set the TCCL so that the ServiceRegistry lookup
            // works inside of osgi
            ClassLoader orientClassLoader = OIndexes.class.getClassLoader();
            Thread.currentThread().setContextClassLoader(orientClassLoader);

            db = objectConnectionPool.acquire(dbUrl, username, password);

        } catch (Exception e){

            throw new RuntimeException("Unable to establish a connection to OrientDB. " +
                "Check to make sure the OrientDB Server is available.", e);

        } finally {

            Thread.currentThread().setContextClassLoader(origClassLoader);

        }

        try {

            return call.withDatabase(db);

        } catch (OConcurrentModificationException ex) {

            throw new OptimisticLockingFailureException("Object version is different than expected", ex);

        } catch (OQueryParsingException ex) {

            log.warn("Unable to run query against OrientDb", ex);

            return null;

        } finally {

            db.close();

        }

    }

    public <T> T executeDocCall(final DatabaseDocCall<T> call) {

        ODatabaseDocumentTx db = documentConnectionPool.acquire(dbUrl, username, password);

        try {

            return call.withDatabase(db);

        } catch (OConcurrentModificationException ex) {

            db.rollback();
            throw new OptimisticLockingFailureException("Object version is different than expected", ex);

        } finally {

            db.close();

        }

    }

    /**
     * Create database classes for schema-less document types managed in Orient DB Document Database
     * @param docType
     */
    private void createDbDocumentType(final String docType) {

        this.executeDocCall(new DatabaseDocCall<Void>() {
            @Override
            public Void withDatabase(ODatabaseDocumentTx db) {
                final OSchema schema = db.getMetadata().getSchema();
                if (schema.existsClass(docType)) {
                    log.debug("Verified that DB Document Class {} exists", docType);
                } else {
                    schema.createClass(docType);
                    log.debug("Created DB Document Class {}", docType);
                }
                return null;
            }
        });
    }

    /**
     * Create database classes for classes managed in Orient DB Object Database
     * @param clazz
     */
    private void createDbObjectType(final Class clazz, final boolean createClass) {

        this.executeObjCall(new DatabaseObjCall<Void>() {
            @Override
            public Void withDatabase(ODatabaseObjectTx db) {
                db.getEntityManager().registerEntityClass(clazz);
                if(! createClass) {
                    return null;
                }

                final OSchema schema = db.getMetadata().getSchema();
                if (schema.existsClass(clazz.getSimpleName())) {
                    log.debug("Verified that DB Object Class {} exists", clazz.getSimpleName());
                } else {
                    schema.createClass(clazz);
                    log.debug("Created DB Oject Class {}", clazz.getSimpleName());
                }

                return null;
            }
        });

    }

    private static class ODatabaseSizedObjectPool extends ODatabaseObjectPool {

        int minSize;
        int maxSize;

        private ODatabaseSizedObjectPool(int minSize, int maxSize) {
            this.minSize = minSize;
            this.maxSize = maxSize;
        }

        @Override
        public void setup() {
            super.setup(minSize, maxSize);
        }

    }

    private static class ODatabaseSizedDocumentPool extends ODatabaseDocumentPool {
        int minSize;
        int maxSize;

        private ODatabaseSizedDocumentPool(int minSize, int maxSize) {
            this.minSize = minSize;
            this.maxSize = maxSize;
        }

        @Override
        public void setup() {
            super.setup(minSize, maxSize);
        }

    }

}
