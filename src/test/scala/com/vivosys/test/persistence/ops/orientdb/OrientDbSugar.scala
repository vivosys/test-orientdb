package com.vivosys.test.persistence.ops.orientdb

import com.orientechnologies.orient.core.command.OCommandRequest
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.db.`object`.ODatabaseObjectTx
import com.orientechnologies.orient.core.sql.OCommandSQL

trait OrientDbSugar {

  def cleanDb(dbManager: DatabaseManager, clusterNames : String*) {

    def truncate(clazz: String) {
      dbManager.executeDocCall(new DatabaseDocCall[Unit] {
        def withDatabase(db: ODatabaseDocumentTx) {
          if(db.getMetadata.getSchema.existsClass(clazz)) {
            db.command(new OCommandSQL("TRUNCATE CLASS " + clazz)).asInstanceOf[OCommandRequest].execute()
          }
        }
      })
    }

    def deleteAll(clazz: String) {
      dbManager.executeObjCall(new DatabaseObjCall[Unit] {
        @Override
        def withDatabase(db: ODatabaseObjectTx) {
          if(db.getMetadata.getSchema.existsClass(clazz)) {
            db.command(new OCommandSQL("DELETE FROM " + clazz)).asInstanceOf[OCommandRequest].execute()
          }
        }
      });
    }

    def rebuildIndexes() {
      dbManager.executeObjCall(new DatabaseObjCall[Unit] {
        @Override
        def withDatabase(db: ODatabaseObjectTx) {
          db.command(new OCommandSQL("REBUILD INDEX *")).asInstanceOf[OCommandRequest].execute()
        }
      });
    }

    for (clusterName <- clusterNames){
      deleteAll(clusterName)
      truncate(clusterName)
    }

    rebuildIndexes()

  }
}
