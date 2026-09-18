package com.trendbench.store.repository;

import com.trendbench.store.entity.Store;
import com.trendbench.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {

	List<Store> findAllByUser(User user);

	List<Store> findAllByUser_UserId(Long userId);

	Optional<Store> findByStoreIdAndUser_UserId(Long storeId, Long userId);
}
