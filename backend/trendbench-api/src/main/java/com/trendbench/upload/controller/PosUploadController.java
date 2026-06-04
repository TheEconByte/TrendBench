package com.trendbench.upload.controller;

import com.trendbench.upload.dto.SalesUploadResponse;
import com.trendbench.upload.service.PosUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pos/uploads")
public class PosUploadController {

	private final PosUploadService posUploadService;

	public PosUploadController(PosUploadService posUploadService) {
		this.posUploadService = posUploadService;
	}

	@PostMapping
	public ResponseEntity<SalesUploadResponse> createUpload(
		@RequestParam(value = "file", required = false) MultipartFile file,
		@RequestParam(value = "storeId", required = false) Long storeId
	) {
		SalesUploadResponse response = posUploadService.createUpload(file, storeId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
