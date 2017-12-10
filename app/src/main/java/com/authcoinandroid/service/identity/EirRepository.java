package com.authcoinandroid.service.identity;

import android.app.Application;

import com.authcoinandroid.model.EntityIdentityRecord;
import com.authcoinandroid.ui.AuthCoinApplication;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;

/**
 * EIR database operations
 */
public class EirRepository {

    private static EirRepository repository;
    private final ReactiveEntityStore<Persistable> dataStore;

    private EirRepository(ReactiveEntityStore<Persistable> dataStore) {
        this.dataStore = dataStore;
    }

    public static EirRepository getInstance(Application app) {
        if (repository == null) {
            ReactiveEntityStore<Persistable> dataStore = ((AuthCoinApplication) app).getDataStore();
            repository = new EirRepository(dataStore);
        }
        return repository;
    }

    /**
     * Inserts or updates EIR based on EIR ID.
     */
    public Single<EntityIdentityRecord> save(EntityIdentityRecord eir) {
        byte[] id = eir.getId();
        EntityIdentityRecord result = dataStore.findByKey(EntityIdentityRecord.class, id).blockingGet();
        if(result == null) {
            return dataStore.insert(eir);
        }
        return dataStore.update(eir);
    }

    /**
     * Get all EIR values
     */
    public List<EntityIdentityRecord> findAll() {
        return dataStore.select(EntityIdentityRecord.class).orderBy(EntityIdentityRecord.ID.lower()).get().toList();
    }

    /**
     * Find EIR by primary key
     */
    public Observable<EntityIdentityRecord> find(byte[] id) {
        Maybe<EntityIdentityRecord> r = dataStore.findByKey(EntityIdentityRecord.class, id);
        return r.toObservable();
    }

}