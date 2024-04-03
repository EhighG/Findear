import { FindearAxios } from "@/shared";
import { AxiosResponse } from "axios";
import type {
  AcquisitionsType,
  AcquistionsListType,
  returnAcquistionsType,
  registLostsType,
  LostsListType,
  LostsType,
  matchingTotalType,
  matchingBestType,
  AcquisitionPatchType,
} from "../type";

const axios = FindearAxios();

const registAcquisitions = async (
  data: AcquisitionsType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.post("/acquisitions", data).then(success).catch(fail);
};

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
  boardId: number,
  data: AcquisitionPatchType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.patch(`/acquisitions/${boardId}`, data).then(success).catch(fail);
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

const getLost112Acquire = async (
  data: AcquistionsListType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .get("/acquisitions/lost112", { params: data })
    .then(success)
    .catch(fail);
};

const getLost112AcquisitionsDetail = async (
  boardId: number,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.get(`/acquisitions/lost112/${boardId}`).then(success).catch(fail);
};

const getMatchingFindearBest = async (
  pageNo: matchingBestType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .get(`/matchings/findear/bests`, { params: pageNo })
    .then(success)
    .catch(fail);
};

const getMatchingLost112Best = async (
  pageNo: matchingBestType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .get(`/matchings/lost112/bests`, { params: pageNo })
    .then(success)
    .catch(fail);
};

const getMatchingFindearTotal = async (
  data: matchingTotalType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .get(`/matchings/findear/total`, { params: data })
    .then(success)
    .catch(fail);
};

const getMatchingLost112Total = async (
  data: matchingTotalType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .get(`/matchings/lost112/total`, { params: data })
    .then(success)
    .catch(fail);
};

const getMatchedCount = async (
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.get(`/acquisitions/returns/count`).then(success).catch(fail);
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
  getLost112Acquire,
  getLost112AcquisitionsDetail,
  getMatchingFindearBest,
  getMatchingLost112Best,
  getMatchingFindearTotal,
  getMatchingLost112Total,
  getMatchedCount,
};
