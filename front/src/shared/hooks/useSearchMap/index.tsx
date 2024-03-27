type dataType = {
  address_name: string;
  category_group_code: string;
  category_group_name: string;
  category_name: string;
  distance: string;
  id: string;
  phone: string;
  place_name: string;
  place_url: string;
  road_address_name: string;
  x: string;
  y: string;
};

const useSearchMap = async (title: string) => {
  if (title) {
    // 장소 검색 객체를 생성합니다
    let ps = new window.kakao.maps.services.Places();

    return new Promise<dataType[]>((resolve, reject) => {
      // 키워드로 장소를 검색합니다
      ps.keywordSearch(title, (data: dataType[], status: any) => {
        if (status === "OK") {
          resolve(data);
        } else {
          reject(new Error("장소 검색에 실패했습니다."));
        }
      });
    });
  }
};

export default useSearchMap;
