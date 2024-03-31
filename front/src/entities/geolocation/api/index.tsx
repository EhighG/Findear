import axios, { AxiosResponse } from "axios";

const placeKey = import.meta.env.VITE_PLACE_SEARCH_KEY;

const getPlaceInfo = async (
  size: number,
  page: number,
  query: string,
  type: "PLACE" | "ADDRESS" | "DISTRICT" | "ROAD",
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .get(
      `/search?service=search&request=search&version=2.0&crs=EPSG:900913&size=${size}&page=${page}&query=${query}&type=${type}&format=json&errorformat=json&key=${placeKey}`
    )
    .then(success)
    .catch(fail);
};

export { getPlaceInfo };
