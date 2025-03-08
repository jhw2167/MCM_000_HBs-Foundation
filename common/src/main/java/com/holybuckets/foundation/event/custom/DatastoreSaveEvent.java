package com.holybuckets.foundation.event.custom;

import com.holybuckets.foundation.datastore.DataStore;

public class DatastoreSaveEvent {
    private final DataStore dataStore;

    public DatastoreSaveEvent(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public DataStore getDataStore() {
        return dataStore;
    }
}
