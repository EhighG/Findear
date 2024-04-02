import { ListType, Lost112ListType } from "@/entities";
import {
  getMatchingFindearTotal,
  getMatchingLost112Total,
} from "@/entities/findear/api";
import { Card } from "@/shared";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

type acquiredBoard = {
  acquiredAt: string;
  agencyAddress: string;
  agencyName: string;
  boardId: number;
  category: string;
  productName: string;
  status: string;
  thumbnailUrl: string;
};

const MatchingList = () => {
  const navigate = useNavigate();
  const lostBoardId = parseInt(useParams().id ?? "0");
  const [serviceType] = useState<string>("findear");

  const [findearBoardList, setFindearBoardList] = useState<acquiredBoard[]>([]);
  const [lostBoardList] = useState<Lost112ListType[]>([]);
  const [pageNo] = useState<number>(1);
  const PAGE_SIZE = 6;

  useEffect(() => {
    getMatchingFindearTotal(
      { lostBoardId, pageNo, size: PAGE_SIZE },
      ({ data }) => {
        console.log(data);
        let list: acquiredBoard[] = [];
        data.result.matchingList.forEach((item: any) =>
          list.push(item.acquiredBoard)
        );
        setFindearBoardList(list);
      },
      (error) => console.log(error)
    );
  }, []);

  // getMatchingLost112Total(
  //   { lostBoardId, pageNo, size: PAGE_SIZE },
  //   ({ data }) => console.log(data),
  //   (error) => console.log(error)
  // );

  return (
    <div className="w-[360px] flex flex-col">
      <div className="self-center">매칭리스트임니다</div>
      <div className="flex flex-1 flex-col">
        <div className="grid max-sm:grid-cols-2 max-md:grid-cols-3 max-lg:grid-cols-4 max-xl:grid-cols-6 max-2xl:grid-cols-7 grid-cols-8 justify-items-center gap-[10px]">
          {serviceType === "findear"
            ? findearBoardList &&
              findearBoardList?.map((findearBoard) => {
                return (
                  <Card
                    key={findearBoard.boardId}
                    category={findearBoard.category ?? ""}
                    date={findearBoard.acquiredAt ?? ""}
                    image={findearBoard.thumbnailUrl}
                    locate={findearBoard.agencyName ?? ""}
                    title={findearBoard.productName}
                    isLost={false}
                    onClick={() =>
                      navigate(`/foundItemDetail/${findearBoard.boardId}`)
                    }
                  />
                );
              })
            : lostBoardList?.map((item) => {
                return (
                  <Card
                    key={item.id}
                    date={item.fdYmd}
                    category={item.mainPrdtClNm ?? ""}
                    image={item.fdFilePathImg}
                    locate={item.depPlace}
                    title={item.fdPrdtNm}
                    isLost={false}
                    onClick={() =>
                      navigate(`/founditemDetail/${item.atcId}`, {
                        state: { ...item, serviceType: "Lost112" },
                      })
                    }
                  />
                );
              })}
        </div>
      </div>
    </div>
  );
};
export default MatchingList;
