import type { Member, Agency } from "@/entities";

type AcquisitionsType = {
  productName: string;
  imgUrls: Array<string>;
};

type AcquisitionPatchType = {
  category: string;
};

type AcquistionsListType = {
  sidoId?: string;
  sigunguId?: string;
  dongId?: string;
  categoryId?: string;
  sDate?: string;
  eDate?: string;
  subCategoryId?: number;
  keyword?: string;
  pageNo: number;
  memberId?: number;
  sortBy?: string;
};

type returnAcquistionsType = {
  boardId: number;
  receiver: receiverType;
};
type LostsType = {
  boardId: number;
  productName: string;
  content: string;
  memberId: number;
  color: Array<string>;
  categoryId: number;
  subCategoryId: number;
  imgUrls: Array<string>;
  lostAt: string;
  suspiciousPlace: string;
  suspiciousRadius: number;
  xPos: number;
  yPos: number;
};

type LostsListType = {
  memberId?: number;
  categoryId?: string;
  sDate?: string;
  eDate?: string;
  keyword?: string;
  pageNo: number;
  sortBy?: string;
};

type registLostsType = {
  productName: string;
  content: string;
  memberId: number;
  color: string;
  category: string;
  imgUrls: string[];
  lostAt: string;
  suspiciousPlace: string;
  xpos: number;
  ypos: number;
};

type boardType = {
  id: number;
  color?: string;
  description?: string;
  categoryName?: string;
  productName: string;
  registeredAt: string;
  imgUrls: string[];
  isLost: boolean;
  member: Member; // memberType
  status: "ONGOING" | "DONE";
};

type infoType = {
  address: string;
  agencyName: string;
  suspiciousPlace: string;
  id: number;
  xpos: number;
  ypos: number;
  board: boardType;
};

type ListType = {
  acquiredAt?: string; //습득물
  agency?: Agency; // 습득물
  boardId: number;
  category?: string;
  status: "DONE" | "ONGOING";
  isLost: boolean;
  productName: string;
  thumbnailUrl: string;
  lostAt?: string; // 분실물
  suspiciousPlace?: string; // 분실물
  writer: Member;
};

type BoardCategoryProps = {
  boardType: "분실물" | "습득물";
};

type searchType = {
  pageNo: number;
  category?: string;
  sDate?: string;
  eDate?: string;
  keyword?: string;
  sortBy?: string;
};

type Lost112ListType = {
  addr: string;
  atcId: string;
  clrNm: string;
  depPlace: string;
  fdFilePathImg: string;
  fdPrdtNm: string;
  fdSbjt: string;
  fdYmd: string;
  id: string;
  mainPrdtClNm: string;
  prdtClNm: string;
  subPrdtClNm: string;
  serviceType?: string;
};

type receiverType = {
  phoneNumber: string;
};

type matchingBestType = {
  pageNo: number;
};

type matchingTotalType = {
  lostBoardId: number;
  pageNo: number;
  size?: number;
};

export type {
  infoType,
  ListType,
  BoardCategoryProps,
  searchType,
  Lost112ListType,
  AcquisitionsType,
  AcquistionsListType,
  returnAcquistionsType,
  LostsType,
  LostsListType,
  registLostsType,
  receiverType,
  matchingBestType,
  matchingTotalType,
  AcquisitionPatchType,
};
