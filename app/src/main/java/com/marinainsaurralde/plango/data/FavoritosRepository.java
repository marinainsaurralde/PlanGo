package com.marinainsaurralde.plango.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.marinainsaurralde.plango.model.Plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class FavoritosRepository {

    public interface Listener {
        void onFavoritosChanged();
        void onFavoritosError(Exception error);
    }

    public interface OperationCallback {
        default void onSuccess() { }
        void onError(Exception error);
    }

    private static final FavoritosRepository INSTANCE = new FavoritosRepository();

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Map<String, Plan> favoritos = new LinkedHashMap<>();
    private final Map<String, Boolean> pendingTargets = new HashMap<>();
    private ListenerRegistration snapshotRegistration;
    private String activeUid;
    private boolean loaded;

    private FavoritosRepository() {
        auth.addAuthStateListener(firebaseAuth -> switchUser(firebaseAuth.getCurrentUser()));
        switchUser(auth.getCurrentUser());
    }

    public static FavoritosRepository getInstance() {
        return INSTANCE;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        listener.onFavoritosChanged();
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public synchronized boolean isFavorito(String eventId) {
        return eventId != null && favoritos.containsKey(eventId);
    }

    public synchronized boolean isPending(String eventId) {
        return eventId != null && pendingTargets.containsKey(eventId);
    }

    public synchronized Set<String> getFavoriteIds() {
        return Collections.unmodifiableSet(new HashSet<>(favoritos.keySet()));
    }

    public synchronized Set<String> getPendingIds() {
        return Collections.unmodifiableSet(new HashSet<>(pendingTargets.keySet()));
    }

    public synchronized List<Plan> getFavoritos() {
        return Collections.unmodifiableList(new ArrayList<>(favoritos.values()));
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public void toggle(Plan evento, OperationCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        String eventId = evento == null ? null : evento.getFavoriteDocumentId();
        if (user == null || eventId == null || eventId.trim().isEmpty()) {
            callback.onError(new IllegalStateException("No hay una sesión o evento válido"));
            return;
        }

        final boolean targetFavorite;
        synchronized (this) {
            if (!user.getUid().equals(activeUid) || pendingTargets.containsKey(eventId)) {
                return;
            }
            targetFavorite = !favoritos.containsKey(eventId);
            pendingTargets.put(eventId, targetFavorite);
        }
        notifyChanged();

        CollectionReference collection = favoritesCollection(user.getUid());
        com.google.android.gms.tasks.Task<Void> task;
        if (targetFavorite) {
            Map<String, Object> document = FavoritoMapper.toMap(evento);
            document.put("guardadoEn", FieldValue.serverTimestamp());
            task = collection.document(eventId).set(document);
        } else {
            task = collection.document(eventId).delete();
        }
        task.addOnSuccessListener(unused -> callback.onSuccess());
        task.addOnFailureListener(error -> {
            synchronized (FavoritosRepository.this) {
                pendingTargets.remove(eventId);
            }
            notifyChanged();
            callback.onError(error);
        });
    }

    private synchronized void switchUser(FirebaseUser user) {
        String newUid = user == null ? null : user.getUid();
        if (newUid == null ? activeUid == null : newUid.equals(activeUid)) {
            return;
        }
        if (snapshotRegistration != null) {
            snapshotRegistration.remove();
            snapshotRegistration = null;
        }
        activeUid = newUid;
        favoritos.clear();
        pendingTargets.clear();
        loaded = newUid == null;
        notifyChanged();
        if (newUid == null) {
            return;
        }
        snapshotRegistration = favoritesCollection(newUid).addSnapshotListener(
                MetadataChanges.INCLUDE, (snapshot, error) -> {
            if (!newUid.equals(activeUid)) {
                return;
            }
            if (error != null) {
                notifyError(error);
                return;
            }
            if (snapshot != null && snapshot.getMetadata().hasPendingWrites()) {
                return;
            }
            Map<String, Plan> updated = new LinkedHashMap<>();
            if (snapshot != null) {
                snapshot.getDocuments().forEach(document -> updated.put(
                        document.getId(),
                        FavoritoMapper.fromMap(document.getId(), document.getData())
                ));
            }
            synchronized (FavoritosRepository.this) {
                favoritos.clear();
                favoritos.putAll(updated);
                loaded = true;
                pendingTargets.entrySet().removeIf(entry ->
                        favoritos.containsKey(entry.getKey()) == entry.getValue());
            }
            notifyChanged();
        });
    }

    private CollectionReference favoritesCollection(String uid) {
        return firestore.collection("usuarios").document(uid).collection("favoritos");
    }

    private void notifyChanged() {
        for (Listener listener : listeners) {
            listener.onFavoritosChanged();
        }
    }

    private void notifyError(Exception error) {
        for (Listener listener : listeners) {
            listener.onFavoritosError(error);
        }
    }
}
