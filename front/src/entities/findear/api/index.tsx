import { FindearAxios } from "@/shared";
import { AxiosResponse } from "axios";

const axios = FindearAxios();

type AcquisitionsType = {
  productName: string;
  imgUrls: Array<string>;
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
};

type receiverType = {
  phoneNumber: string;
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
  categoryId?: string;
  sDate?: string;
  eDate?: string;
  keyword?: string;
  pageNo: number;
};

type registLostsType = {
  productName: string;
  content: string;
  memberId: number;
  color: string;
  categoryId: string;
  imageUrls: string[];
  lostAt: string;
  suspiciousPlace: string;
  xPos: number;
  yPos: number;
};

// 습득물 등록
const registAcquisitions = async (
  data: AcquisitionsType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.post("/acquisitions", data).then(success).catch(fail);
};

// 습득물 리스트 조회
const getAcquisitions = async (
  data: AcquistionsListType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.get("/acquisitions", { params: data }).then(success).catch(fail);
};

const getAcquisitionsDetail = async (
  boardId: number,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.get(`/acquisitions/${boardId}`).then(success).catch(fail);
};

const returnAcquisitions = async (
  data: returnAcquistionsType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .post(`/acquisitions/${data.boardId}/return`, data)
    .then(success)
    .catch(fail);
};

const acquistionRollBack = async (
  boardId: number,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .patch(`/acquisitions/${boardId}/rollback`)
    .then(success)
    .catch(fail);
};

const acquistionPatch = async (
  // data: AcquisitionsType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.patch(`/acquisitions/`).then(success).catch(fail);
};

const deleteAcquisitions = async (
  boardId: number,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .patch(`/acquisitions/${boardId}/delete`, boardId)
    .then(success)
    .catch(fail);
};

const registLosts = async (
  data: registLostsType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.post("/losts", data).then(success).catch(fail);
};

const getLosts = async (
  data: LostsListType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.get("/losts?", { params: data }).then(success).catch(fail);
};

const getLostsDetail = async (
  boardId: number,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.get(`/losts/${boardId}`).then(success).catch(fail);
};

const LostsPatch = async (
  data: LostsType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.patch(`/losts/${data.boardId}`, data).then(success).catch(fail);
};

const deleteLosts = async (
  boardId: number,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .patch(`/losts/${boardId}/delete`, boardId)
    .then(success)
    .catch(fail);
};

const scrapBoard = async (
  boardId: number,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.post(`/scraps?boardId=${boardId}`).then(success).catch(fail);
};

const cancelScarppedBoard = async (
  boardId: number,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.delete(`/scraps?boardId=${boardId}`).then(success).catch(fail);
};

export {
  registAcquisitions,
  getAcquisitions,
  getAcquisitionsDetail,
  returnAcquisitions,
  acquistionRollBack,
  registLosts,
  getLosts,
  getLostsDetail,
  deleteLosts,
  LostsPatch,
  acquistionPatch,
  deleteAcquisitions,
  scrapBoard,
  cancelScarppedBoard,
};

export type { receiverType };
