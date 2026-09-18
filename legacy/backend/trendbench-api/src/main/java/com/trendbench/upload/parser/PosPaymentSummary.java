package com.trendbench.upload.parser;

import java.time.LocalDate;

public record PosPaymentSummary(
	LocalDate salesDate,
	long grossSales,
	long vatAmount,
	long paymentCount,
	long cashSales,
	long cardSales,
	long qrSales,
	long bankTransferSales,
	long prepaidSales,
	long otherSales
) {
}
