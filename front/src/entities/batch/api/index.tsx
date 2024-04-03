import { BatchAxios } from "@/shared";
import { AxiosResponse } from "axios";
const axios = BatchAxios();

const getLost112AcquireList = async (
  size: number,
  page: number,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .get(`/search?size=${size}&page=${page}`)
    .then(success)
    .catch(fail);
};

export { getLost112AcquireList };
