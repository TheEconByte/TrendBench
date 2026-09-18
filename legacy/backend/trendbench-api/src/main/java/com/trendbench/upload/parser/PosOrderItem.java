package com.trendbench.upload.parser;

import java.time.LocalDate;

public record PosOrderItem(
	LocalDate orderDate,
	String orderTime,
	String orderNo,
	String orderChannel,
	String paymentStatus,
	String productName,
	String productCode,
	String productCategory,
	String optionName,
	int quantity,
	long productPrice,
	long optionPrice,
	long productDiscountAmount,
	long orderDiscountAmount,
	long netSales,
	long vatAmount
) {
}
