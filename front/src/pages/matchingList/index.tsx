import {
  getMatchingFindearTotal,
  getMatchingLost112Total,
} from "@/entities/findear/api";
import { Card, Text } from "@/shared";
import {
  CustomFlowbiteTheme,
  Label,
  Pagination,
  Tooltip,
} from "flowbite-react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

type FindearBoard = {
  acquiredAt: string;
  agencyAddress: string;
  agencyName: string;
  boardId: number;
  category: string;
  productName: string;
  status: string;
  thumbnailUrl: string;
};

type Lost112Board = {
  policeMatchingLogId: number;
  lostBoardId: number;
  similarityRate: number;
  matchingAt: string;
  id: number;
  atcId: string;
  depPlace: string;
  fdFilePathImg: string;
  fdPrdtNm: string;
  fdSbjt: string;
  clrNm: string;
  fdYmd: string;
  mainPrdtClNm: string;
};

const customPagination: CustomFlowbiteTheme["pagination"] = {
  base: "self-center my-5",
  layout: {
    table: {
      base: "text-gray-700 dark:text-gray-400",
      span: "font-semibold text-gray-900 dark:text-white",
    },
  },
  pages: {
    base: "xs:mt-0 mt-2 inline-flex items-center -space-x-px",
    showIcon: "inline-flex",
    previous: {
      base: "ml-0 rounded-l-lg border border-gray-300 bg-white px-3 py-2 leading-tight text-gray-500 enabled:hover:bg-gray-100 enabled:hover:text-gray-700 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 enabled:dark:hover:bg-gray-700 enabled:dark:hover:text-white",
      icon: "h-5 w-5",
    },
    next: {
      base: "rounded-r-lg border border-gray-300 bg-white px-3 py-2 leading-tight text-gray-500 enabled:hover:bg-gray-100 enabled:hover:text-gray-700 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 enabled:dark:hover:bg-gray-700 enabled:dark:hover:text-white",
      icon: "h-5 w-5",
    },
    selector: {
      base: "w-12 border border-gray-300 bg-white py-2 leading-tight text-gray-500 enabled:hover:bg-gray-100 enabled:hover:text-gray-700 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 enabled:dark:hover:bg-gray-700 enabled:dark:hover:text-white",
      active:
        "bg-cyan-50 text-cyan-600 hover:bg-cyan-100 hover:text-cyan-700 dark:border-gray-700 dark:bg-gray-700 dark:text-white",
      disabled: "cursor-not-allowed opacity-50",
    },
  },
};

const MatchingList = () => {
  const navigate = useNavigate();
  const lostBoardId = parseInt(useParams().id ?? "0");
  const [serviceType, setServiceType] = useState<string>("findear");
  const [findearBoardList, setFindearBoardList] = useState<FindearBoard[]>([]);
  const [lost112BoardList, setLost112BoardList] = useState<Lost112Board[]>([]);
  const [pageNo, setPageNo] = useState<number>(1);
  const [totalPageNum, setTotalPageNum] = useState<number>(1);
  const [Trigger, setTrigger] = useState<boolean>(true);

  const onPageChange = (page: number) => setPageNo(page);

  const handleDataFetching = () => {
    if (serviceType === "findear") {
      getMatchingFindearTotal(
        { lostBoardId, pageNo },
        ({ data }) => {
          console.log(data);
          let list: FindearBoard[] = [];
          data.result.matchingList.forEach((item: any) =>
            list.push(item.acquiredBoard)
          );
          setFindearBoardList(list);
          setTotalPageNum(data.result.totalPageNum);
        },
        (error) => console.log(error)
      );
      return;
    }

    if (serviceType === "Lost112") {
      getMatchingLost112Total(
        { lostBoardId, pageNo },
        ({ data }) => {
          console.log(data);
          let list: Lost112Board[] = [];
          data.result.matchingList.forEach((item: any) =>
            list.push(item.acquiredBoard)
          );
          setLost112BoardList(list);
          setTotalPageNum(data.result.totalPageNum);
        },
        (error) => console.log(error)
      );
      return;
    }
  };

  useEffect(() => {
    setFindearBoardList([]);
    setLost112BoardList([]);
    setPageNo(1);
    setTrigger(true);
  }, [serviceType]);

  useEffect(() => {
    if (Trigger) {
      handleDataFetching();
      setTrigger(false);
    }
  }, [Trigger]);

  // getMatchingLost112Total(
  //   { lostBoardId, pageNo, size: PAGE_SIZE },
  //   ({ data }) => console.log(data),
  //   (error) => console.log(error)
  // );

  return (
    <div className="w-[360px] flex flex-col">
      <div className="flex flex-col  flex-1 mx-[10px] my-[10px]">
        <div className="flex justify-between items-center">
          <Text className="font-bold text-[1.2rem]">
            {"매칭된 물건들이에요."}
          </Text>
          <form className="flex items-center gap-[10px]">
            <div>
              <Label className="flex items-center gap-1">
                <input
                  type="radio"
                  name="company"
                  value="findear"
                  checked={serviceType === "findear"}
                  onChange={() => setServiceType("findear")}
                  placeholder="파인디어"
                />
                파인디어
              </Label>
            </div>
            <div className="flex gap-[5px] items-center">
              <Label className="flex items-center gap-1">
                <input
                  type="radio"
                  name="company"
                  value="Lost112"
                  checked={serviceType === "Lost112"}
                  onChange={() => setServiceType("Lost112")}
                  placeholder="Lost112"
                />
                <Text>경찰청</Text>
              </Label>
            </div>
            <Tooltip content="파인디어가 아닌 외부 서비스(경찰청)에서 제공하는 정보에요">
              <img
                src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Information.png"
                alt="Information"
                width="20"
                height="20"
              />
            </Tooltip>
          </form>
        </div>
      </div>
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
            : lost112BoardList?.map((item) => {
                return (
                  <Card
                    key={item.id}
                    date={item.fdYmd}
                    category={item.mainPrdtClNm ?? ""}
                    image={item.fdFilePathImg}
                    locate={item.depPlace}
                    title={item.fdPrdtNm}
                    isLost={true}
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
      <Pagination
        showIcons
        nextLabel=""
        previousLabel=""
        theme={customPagination}
        currentPage={pageNo}
        totalPages={totalPageNum}
        onPageChange={onPageChange}
      />
    </div>
  );
};
export default MatchingList;
