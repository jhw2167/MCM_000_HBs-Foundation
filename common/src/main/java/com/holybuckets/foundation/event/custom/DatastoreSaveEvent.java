package com.holybuckets.foundation.event.custom;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.datastore.DataStore;

public class DatastoreSaveEvent {
    private final DataStore dataStore;

    private DatastoreSaveEvent(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public static DatastoreSaveEvent create() {
        return new DatastoreSaveEvent(GeneralConfig.getInstance().getDataStore());
    }
}
