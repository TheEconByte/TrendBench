package com.trendbench.upload.parser;

import java.time.LocalDate;

public record PosDataBasis(
	LocalDate reportStartDate,
	LocalDate reportEndDate,
	String settlementBasis,
	String aggregationUnit
) {
}
