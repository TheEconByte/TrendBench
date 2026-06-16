package com.trendbench.store.dto;

import com.trendbench.store.entity.Store;

public record StoreCreateResponse(
		Long storeId,
		String storeName
) {
	public static StoreCreateResponse from(Store store) {
		return new StoreCreateResponse(store.getStoreId(), store.getStoreName());
	}
}
