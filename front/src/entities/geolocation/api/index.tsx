// import PlaceAxios from "@/shared/axios/PlaceAxios";
import { PlaceAxios } from "@/shared/axios";
import { AxiosError, AxiosResponse } from "axios";

const placeAxios = PlaceAxios();
const placeKey = import.meta.env.VITE_PLACE_SEARCH_KEY;

const getPlaceInfo = async (
  size: number,
  page: number,
  query: string,
  type: "PLACE" | "ADDRESS" | "DISTRICT" | "ROAD",
  success: (response: AxiosResponse) => void,
  fail: (error: AxiosError) => void,
  category?: string
) => {
  await placeAxios
    .get(
      `/search?service=search&request=search&version=2.0&crs=epsg:4326&size=${size}&page=${page}&query=${query}&type=${type}&format=json&errorformat=json&key=${placeKey}` +
        (category ? `&category=${category}` : "")
    )
    .then(success)
    .catch(fail);
};

const getCoordinateInfo = async (
  address: string,
  success: (response: AxiosResponse) => void,
  fail: (error: AxiosError) => void
) => {
  await placeAxios
    .get(
      `/address?service=address&request=getcoord&version=2.0&crs=epsg:4326&address=${address}&refine=true&simple=false&format=json&type=road&key=${placeKey}`
    )
    .then(success)
    .catch(fail);
};

export { getPlaceInfo, getCoordinateInfo };
