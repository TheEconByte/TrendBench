package com.trendbench.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreCreateRequest(

		@NotBlank(message = "매장명은 필수입니다.")
		@Size(max = 100, message = "매장명은 100자 이하여야 합니다.")
		String storeName,

		@Size(max = 30, message = "사업자등록번호는 30자 이하여야 합니다.")
		String businessNumber,

		@Size(max = 50, message = "지역명은 50자 이하여야 합니다.")
		String regionName,

		@Size(max = 50, message = "상권명은 50자 이하여야 합니다.")
		String businessAreaName,

		@Size(max = 50, message = "업종명은 50자 이하여야 합니다.")
		String industryName
) {
}
