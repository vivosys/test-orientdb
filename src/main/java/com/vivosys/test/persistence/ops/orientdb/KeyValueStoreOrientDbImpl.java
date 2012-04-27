package com.vivosys.test.persistence.ops.orientdb;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.vivosys.test.persistence.ops.KeyValueStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>An implementation of KeyValueStore for Orient DB.</p>
 */
public class KeyValueStoreOrientDbImpl implements KeyValueStore<String, Object> {

    public static final String COLLECTION = "keyValue";
    private static final String FIELD_KEY = "key";
    private static final String FIELD_VALUE = "value";

    private static final String FETCH_PLAN_ALL_RECURSIVE = "*:-1";

    private static final String QUERY_DELETE_BY_KEY = "DELETE FROM " + COLLECTION + " WHERE " + FIELD_KEY + " = ?";

    private static final String QUERY_DELETE_BY_KEYS = "DELETE FROM " + COLLECTION + " WHERE " + FIELD_KEY + " IN ?";

    private static final String QUERY_ENTRY = "SELECT FROM " + COLLECTION + " WHERE " + FIELD_KEY + " = ? LIMIT 1";

    private DatabaseManager dbManager;

    public void setDbManager(DatabaseManager dbManager) {

        this.dbManager = dbManager;

    }

    //TODO: No upserts available in orientdb currently, but should probably update when it's available:
    // https://groups.google.com/forum/?fromgroups#!searchin/orient-database/upsert/orient-database/leMFWH8ZprU/boY9_cHEsR0J
    @Override
    public void put(final String key, final Object value) {

        dbManager.executeDocCall(new DatabaseDocCall<Void>() {
            @Override
            public Void withDatabase(ODatabaseDocumentTx db) {

                List<ODocument> results = db.command(new OSQLSynchQuery<ODocument>(QUERY_ENTRY)
                    .setFetchPlan(FETCH_PLAN_ALL_RECURSIVE))
                    .execute(key);

                ODocument document;

                //insert
                if (results == null || results.size() <= 0) {

                    document = new ODocument(COLLECTION);
                    document.field(FIELD_KEY, key);
                    document.setFieldType(FIELD_VALUE, OType.getTypeByClass(value.getClass()));
                    document.field(FIELD_VALUE, value);

                } else {

                    //update
                    document = results.get(0);
                    document.setFieldType(FIELD_VALUE, OType.getTypeByClass(value.getClass()));
                    document.field(FIELD_VALUE, value);

                }

                document.save();

                return null;

            }
        });
    }

    @Override
    public void putAll(final Map<String, Object> entries) {

        dbManager.executeDocCall(new DatabaseDocCall<Void>() {
            @Override
            public Void withDatabase(ODatabaseDocumentTx db) {

                for(Map.Entry<String, Object> entry : entries.entrySet()) {

                    put(entry.getKey(), entry.getValue());

                }

                return null;
            }
        });
    }

    @Override
    public Object get(final String key) {

        return dbManager.executeDocCall(new DatabaseDocCall<Object>() {
            @Override
            public Object withDatabase(ODatabaseDocumentTx db) {

                List<ODocument> results = db.command(new OSQLSynchQuery<ODocument>(QUERY_ENTRY)
                    .setFetchPlan(FETCH_PLAN_ALL_RECURSIVE))
                    .execute(key);

                return (results == null || results.size() <= 0 ? null : results.get(0).field(FIELD_VALUE));
            }
        });
    }

    @Override
    public void remove(final String key) {

        dbManager.executeDocCall(new DatabaseDocCall<Void>() {
            @Override
            public Void withDatabase(ODatabaseDocumentTx db) {
                db.command(new OCommandSQL(QUERY_DELETE_BY_KEY)).execute(key);
                return null;
            }
        });
    }

    @Override
    public void removeAll(final Collection<String> keys) {

        dbManager.executeDocCall(new DatabaseDocCall<Void>() {
            @Override
            public Void withDatabase(ODatabaseDocumentTx db) {
                db.command(new OCommandSQL(QUERY_DELETE_BY_KEYS)).execute(keys);
                return null;
            }
        });
    }
}
