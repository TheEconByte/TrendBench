package com.trendbench.store.entity;

import com.trendbench.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "stores")
public class Store {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "store_id")
	private Long storeId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "store_name", nullable = false, length = 100)
	private String storeName;

	@Column(name = "business_number", length = 30)
	private String businessNumber;

	@Column(name = "region_name", length = 50)
	private String regionName;

	@Column(name = "business_area_name", length = 50)
	private String businessAreaName;

	@Column(name = "industry_name", length = 50)
	private String industryName;

	@Column(name = "latitude")
	private Double latitude;

	@Column(name = "longitude")
	private Double longitude;

	@Column(name = "price_level", length = 30)
	private String priceLevel;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	protected Store() {
	}

	public Store(User user, String storeName) {
		this.user = user;
		this.storeName = storeName;
	}

	public Store(
			User user,
			String storeName,
			String businessNumber,
			String regionName,
			String businessAreaName,
			String industryName) {
		this.user = user;
		this.storeName = storeName;
		this.businessNumber = businessNumber;
		this.regionName = regionName;
		this.businessAreaName = businessAreaName;
		this.industryName = industryName;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}

	public Long getStoreId() {
		return storeId;
	}

	public User getUser() {
		return user;
	}

	public Long getUserId() {
		return user != null ? user.getUserId() : null;
	}

	public String getStoreName() {
		return storeName;
	}

	public String getBusinessNumber() {
		return businessNumber;
	}

	public String getRegionName() {
		return regionName;
	}

	public String getBusinessAreaName() {
		return businessAreaName;
	}

	public String getIndustryName() {
		return industryName;
	}

	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public String getPriceLevel() {
		return priceLevel;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void updateLocation(Double latitude, Double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public void updatePriceLevel(String priceLevel) {
		this.priceLevel = priceLevel;
	}
}
