import { FindearAxios } from "@/shared";
import { AxiosResponse } from "axios";
import { sendMessageType } from "../type";

const axios = FindearAxios();

// 습득물 등록
const getRoomList = async (
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.get(`/message`).then(success).catch(fail);
};

const getRoomDetail = async (
  RoomId: number,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.get(`/message/${RoomId}`).then(success).catch(fail);
};

const sendMessage = async (
  messageData: sendMessageType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.post(`/message`, messageData).then(success).catch(fail);
};

export { getRoomList, getRoomDetail, sendMessage };
