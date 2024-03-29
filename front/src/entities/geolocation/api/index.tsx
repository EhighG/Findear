import { CommercialAxios } from "@/shared/axios";
import { AxiosResponse } from "axios";

const axios = CommercialAxios();
const key = import.meta.env.VITE_COMMERCIAL_KEY;

const getCommercialInfo = async (
  code: string,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .get(
      `/storeListInPnu?serviceKey=${key}&key=${code}&numOfRows=100&pageNo=1&type=json`
    )
    .then(success)
    .catch(fail);
};

const getCommercialInfoByRadius = async (
  radius: string,
  longitude: string,
  latitude: string,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .get(
      `/storeListInRadius?serviceKey=${key}&radius=${radius}&cx=${longitude}&cy=${latitude}&numOfRows=10&pageNo=1&type=json`
    )
    .then(success)
    .catch(fail);
};

export { getCommercialInfo, getCommercialInfoByRadius };
