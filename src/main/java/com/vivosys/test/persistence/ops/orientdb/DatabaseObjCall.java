package com.vivosys.test.persistence.ops.orientdb;

import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;

/**
 * Docs
 */
public interface DatabaseObjCall<T> {

    T withDatabase(ODatabaseObjectTx db);

}
