package com.vivosys.test.persistence.ops.orientdb;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

/**
 * Docs
 */
public interface DatabaseDocCall<T> {

    T withDatabase(ODatabaseDocumentTx db);

}
