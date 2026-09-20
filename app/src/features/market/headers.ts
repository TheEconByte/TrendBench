import { MarketSourceError } from './validation.ts';

// Column names recorded from the official files downloaded on 2026-09-09.
// 2024 and 2025 sales files share one schema; the store file changed from
// Korean to English column names in 2025, so each year gets its own mapping.
export const SALES_HEADERS: readonly string[] = [
  '기준_년분기_코드',
  '상권_구분_코드',
  '상권_구분_코드_명',
  '상권_코드',
  '상권_코드_명',
  '서비스_업종_코드',
  '서비스_업종_코드_명',
  '당월_매출_금액',
  '당월_매출_건수',
  '주중_매출_금액',
  '주말_매출_금액',
  '월요일_매출_금액',
  '화요일_매출_금액',
  '수요일_매출_금액',
  '목요일_매출_금액',
  '금요일_매출_금액',
  '토요일_매출_금액',
  '일요일_매출_금액',
  '시간대_00~06_매출_금액',
  '시간대_06~11_매출_금액',
  '시간대_11~14_매출_금액',
  '시간대_14~17_매출_금액',
  '시간대_17~21_매출_금액',
  '시간대_21~24_매출_금액',
  '남성_매출_금액',
  '여성_매출_금액',
  '연령대_10_매출_금액',
  '연령대_20_매출_금액',
  '연령대_30_매출_금액',
  '연령대_40_매출_금액',
  '연령대_50_매출_금액',
  '연령대_60_이상_매출_금액',
  '주중_매출_건수',
  '주말_매출_건수',
  '월요일_매출_건수',
  '화요일_매출_건수',
  '수요일_매출_건수',
  '목요일_매출_건수',
  '금요일_매출_건수',
  '토요일_매출_건수',
  '일요일_매출_건수',
  '시간대_건수~06_매출_건수',
  '시간대_건수~11_매출_건수',
  '시간대_건수~14_매출_건수',
  '시간대_건수~17_매출_건수',
  '시간대_건수~21_매출_건수',
  '시간대_건수~24_매출_건수',
  '남성_매출_건수',
  '여성_매출_건수',
  '연령대_10_매출_건수',
  '연령대_20_매출_건수',
  '연령대_30_매출_건수',
  '연령대_40_매출_건수',
  '연령대_50_매출_건수',
  '연령대_60_이상_매출_건수',
];

export const STORES_HEADERS_2024: readonly string[] = [
  '기준_년분기_코드',
  '상권_구분_코드',
  '상권_구분_코드_명',
  '상권_코드',
  '상권_코드_명',
  '서비스_업종_코드',
  '서비스_업종_코드_명',
  '점포_수',
  '유사_업종_점포_수',
  '개업_율',
  '개업_점포_수',
  '폐업_률',
  '폐업_점포_수',
  '프랜차이즈_점포_수',
];

export const STORES_HEADERS_2025: readonly string[] = [
  'stdr_yyqu_cd',
  'trdar_se_cd',
  'trdar_se_cd_nm',
  'trdar_cd',
  'trdar_cd_nm',
  'svc_induty_cd',
  'svc_induty_cd_nm',
  'stor_co',
  'similr_induty_stor_co',
  'opbiz_rt',
  'opbiz_stor_co',
  'clsbiz_rt',
  'clsbiz_stor_co',
  'frc_stor_co',
];

export type CanonicalColumn =
  | 'quarter'
  | 'areaType'
  | 'areaTypeName'
  | 'areaCode'
  | 'areaName'
  | 'industryCode'
  | 'industryName'
  | 'salesAmount'
  | 'storeCount'
  | 'similarIndustryStoreCount'
  | 'franchiseStoreCount'
  | 'openedStoreCount'
  | 'closedStoreCount';

export type SourceSchema = {
  id: string;
  label: string;
  expectedHeaders: readonly string[];
  columns: Readonly<Record<string, CanonicalColumn>>;
  requiredColumns: readonly CanonicalColumn[];
};

const SALES_COLUMNS: Readonly<Record<string, CanonicalColumn>> = {
  '기준_년분기_코드': 'quarter',
  '상권_구분_코드': 'areaType',
  '상권_구분_코드_명': 'areaTypeName',
  '상권_코드': 'areaCode',
  '상권_코드_명': 'areaName',
  '서비스_업종_코드': 'industryCode',
  '서비스_업종_코드_명': 'industryName',
  '당월_매출_금액': 'salesAmount',
};

const STORES_COLUMNS_2024: Readonly<Record<string, CanonicalColumn>> = {
  '기준_년분기_코드': 'quarter',
  '상권_구분_코드': 'areaType',
  '상권_구분_코드_명': 'areaTypeName',
  '상권_코드': 'areaCode',
  '상권_코드_명': 'areaName',
  '서비스_업종_코드': 'industryCode',
  '서비스_업종_코드_명': 'industryName',
  '점포_수': 'storeCount',
  '유사_업종_점포_수': 'similarIndustryStoreCount',
  '프랜차이즈_점포_수': 'franchiseStoreCount',
  '개업_점포_수': 'openedStoreCount',
  '폐업_점포_수': 'closedStoreCount',
};

const STORES_COLUMNS_2025: Readonly<Record<string, CanonicalColumn>> = {
  stdr_yyqu_cd: 'quarter',
  trdar_se_cd: 'areaType',
  trdar_se_cd_nm: 'areaTypeName',
  trdar_cd: 'areaCode',
  trdar_cd_nm: 'areaName',
  svc_induty_cd: 'industryCode',
  svc_induty_cd_nm: 'industryName',
  stor_co: 'storeCount',
  similr_induty_stor_co: 'similarIndustryStoreCount',
  frc_stor_co: 'franchiseStoreCount',
  opbiz_stor_co: 'openedStoreCount',
  clsbiz_stor_co: 'closedStoreCount',
};

const KEY_COLUMNS: readonly CanonicalColumn[] = ['quarter', 'areaType', 'areaCode', 'industryCode'];

export const SALES_SCHEMA_2024: SourceSchema = {
  id: 'sales-2024',
  label: '추정매출-상권 2024년',
  expectedHeaders: SALES_HEADERS,
  columns: SALES_COLUMNS,
  requiredColumns: [...KEY_COLUMNS, 'salesAmount'],
};

export const SALES_SCHEMA_2025: SourceSchema = {
  id: 'sales-2025',
  label: '추정매출-상권 2025년',
  expectedHeaders: SALES_HEADERS,
  columns: SALES_COLUMNS,
  requiredColumns: [...KEY_COLUMNS, 'salesAmount'],
};

export const STORES_SCHEMA_2024: SourceSchema = {
  id: 'stores-2024',
  label: '점포-상권 2024년',
  expectedHeaders: STORES_HEADERS_2024,
  columns: STORES_COLUMNS_2024,
  requiredColumns: [...KEY_COLUMNS, 'storeCount', 'similarIndustryStoreCount', 'franchiseStoreCount', 'openedStoreCount', 'closedStoreCount'],
};

export const STORES_SCHEMA_2025: SourceSchema = {
  id: 'stores-2025',
  label: '점포-상권 2025년',
  expectedHeaders: STORES_HEADERS_2025,
  columns: STORES_COLUMNS_2025,
  requiredColumns: [...KEY_COLUMNS, 'storeCount', 'similarIndustryStoreCount', 'franchiseStoreCount', 'openedStoreCount', 'closedStoreCount'],
};

export type ColumnIndex = Partial<Record<CanonicalColumn, number>>;

export function requireColumn(index: ColumnIndex, column: CanonicalColumn): number {
  const position = index[column];
  if (position === undefined) {
    throw new MarketSourceError('UNEXPECTED_HEADER', `열 '${column}'의 원본 열 위치를 찾지 못했습니다.`);
  }
  return position;
}

// Rejects a file whose headers are not the recorded ones, then resolves the
// canonical name to a column position by name rather than by ordinal.
export function resolveColumnIndex(schema: SourceSchema, actualHeaders: readonly string[]): ColumnIndex {
  const expected = new Set(schema.expectedHeaders);
  const seen = new Set<string>();
  for (const header of actualHeaders) {
    if (seen.has(header)) {
      throw new MarketSourceError('UNEXPECTED_HEADER', `${schema.label}: 열 이름 '${header}'이(가) 중복됩니다.`);
    }
    seen.add(header);
  }
  const missing = schema.expectedHeaders.filter((header) => !seen.has(header));
  const unexpected = actualHeaders.filter((header) => !expected.has(header));
  if (missing.length > 0 || unexpected.length > 0) {
    throw new MarketSourceError(
      'UNEXPECTED_HEADER',
      `${schema.label}: 예상한 열 구성과 다릅니다. 누락 ${missing.length}개[${missing.slice(0, 5).join(', ')}], 예상 밖 ${unexpected.length}개[${unexpected.slice(0, 5).join(', ')}]`,
    );
  }
  const index = {} as ColumnIndex;
  for (const [header, column] of Object.entries(schema.columns)) {
    const position = actualHeaders.indexOf(header);
    if (position < 0) {
      throw new MarketSourceError('UNEXPECTED_HEADER', `${schema.label}: 열 '${header}'을(를) 찾지 못했습니다.`);
    }
    index[column] = position;
  }
  for (const column of schema.requiredColumns) {
    if (index[column] === undefined) {
      throw new MarketSourceError('UNEXPECTED_HEADER', `${schema.label}: 필수 열 '${column}'의 원본 열 이름이 매핑에 없습니다.`);
    }
  }
  return index;
}
