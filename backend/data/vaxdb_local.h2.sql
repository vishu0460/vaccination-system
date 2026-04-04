-- MVStore
CREATE ALIAS IF NOT EXISTS READ_BLOB_MAP FOR 'org.h2.tools.Recover.readBlobMap';
CREATE ALIAS IF NOT EXISTS READ_CLOB_MAP FOR 'org.h2.tools.Recover.readClobMap';
// error: org.h2.mvstore.MVStoreException: File is corrupted - unable to recover a valid set of chunks [2.3.232/6]
