import { CommercialAxios, PlaceAxios } from "@/shared/axios";
import { AxiosResponse } from "axios";

const axios = CommercialAxios();
const placeAxios = PlaceAxios();
const key = import.meta.env.VITE_COMMERCIAL_KEY;
const placeKey = import.meta.env.VITE_PLACE_SEARCH_KEY;

const getCommercialInfo = async (
  code: string,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .get(
      `storeListInPnu?serviceKey=${key}&key=${code}&numOfRows=100&pageNo=1&type=json`
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
      `storeListInRadius?serviceKey=${key}&radius=${radius}&cx=${longitude}&cy=${latitude}&numOfRows=10&pageNo=1&type=json`
    )
    .then(success)
    .catch(fail);
};

const getPlaceInfo = async (
  size: number,
  page: number,
  query: string,
  type: "PLACE" | "ADDRESS" | "DISTRICT" | "ROAD",
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await placeAxios
    .get(
      `search?service=search&request=search&version=2.0&crs=EPSG:900913&size=${size}&page=${page}&query=${query}&type=${type}&format=json&errorformat=json&key=${placeKey}`
    )
    .then(success)
    .catch(fail);
};

export { getCommercialInfo, getCommercialInfoByRadius, getPlaceInfo };
