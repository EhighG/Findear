import { FindearAxios } from "@/shared";
import { AxiosResponse } from "axios";
import { inRoomMessageType, sendMessageType } from "../type";

const axios = FindearAxios();

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

const sendMessageInRoom = async (
  messageData: inRoomMessageType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.post(`/message/reply`, messageData).then(success).catch(fail);
};

export { getRoomList, getRoomDetail, sendMessage, sendMessageInRoom };
