import { FindearAxios } from "@/shared";
import { AxiosResponse } from "axios";

const axios = FindearAxios();

//  알람리스트 조회
const getAlarmList = async (
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.get("/alarm/alarm-list").then(success).catch(fail);
};

const readAlarm = async (
  alarmId: number,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.get(`/alarm/${alarmId}`).then(success).catch(fail);
};

export { getAlarmList, readAlarm };
