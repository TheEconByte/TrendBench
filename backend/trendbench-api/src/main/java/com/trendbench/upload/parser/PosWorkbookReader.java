package com.trendbench.upload.parser;

import java.io.IOException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

public final class PosWorkbookReader {

	private static final String POS_REPORT_PASSWORD = "0375";

	private PosWorkbookReader() {
	}

	public static Workbook open(MultipartFile file) throws IOException {
		try {
			return WorkbookFactory.create(file.getInputStream());
		} catch (EncryptedDocumentException exception) {
			return WorkbookFactory.create(file.getInputStream(), POS_REPORT_PASSWORD);
		}
	}
}
