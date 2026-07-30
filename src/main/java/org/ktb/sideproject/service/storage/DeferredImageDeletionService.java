package org.ktb.sideproject.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeferredImageDeletionService {

    private static final Object REGISTERED_STORAGE_KEYS_RESOURCE = DeferredImageDeletionService.class;

    private final ImageStorageService imageStorageService;

    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteNow(storageKey);
            return;
        }

        if (!registerStorageKey(storageKey)) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteNow(storageKey);
            }

            @Override
            public void afterCompletion(int status) {
                if (TransactionSynchronizationManager.hasResource(REGISTERED_STORAGE_KEYS_RESOURCE)) {
                    TransactionSynchronizationManager.unbindResource(REGISTERED_STORAGE_KEYS_RESOURCE);
                }
            }
        });
    }

    private boolean registerStorageKey(String storageKey) {
        Set<String> storageKeys = getOrCreateRegisteredStorageKeys();
        return storageKeys.add(storageKey);
    }

    @SuppressWarnings("unchecked")
    private Set<String> getOrCreateRegisteredStorageKeys() {
        if (TransactionSynchronizationManager.hasResource(REGISTERED_STORAGE_KEYS_RESOURCE)) {
            return (Set<String>) TransactionSynchronizationManager.getResource(REGISTERED_STORAGE_KEYS_RESOURCE);
        }

        Set<String> storageKeys = new LinkedHashSet<>();
        TransactionSynchronizationManager.bindResource(REGISTERED_STORAGE_KEYS_RESOURCE, storageKeys);
        return storageKeys;
    }

    private void deleteNow(String storageKey) {
        try {
            imageStorageService.delete(storageKey);
        } catch (RuntimeException e) {
            log.warn("이미지 파일 삭제에 실패했습니다. storageKey={}", storageKey, e);
        }
    }
}
