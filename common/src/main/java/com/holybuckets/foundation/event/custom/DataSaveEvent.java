package com.holybuckets.foundation.event.custom;

import com.holybuckets.foundation.datastore.DataStore;

public class DataSaveEvent {
    private final DataStore dataStore;

    public DataSaveEvent(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public DataStore getDataStore() {
        return dataStore;
    }
}
