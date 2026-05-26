package com.trendbench.upload.repository;

import com.trendbench.upload.entity.SalesUpload;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesUploadRepository extends JpaRepository<SalesUpload, Long> {

	Optional<SalesUpload> findByUploadIdAndStoreId(Long uploadId, Long storeId);
}
