import { useCallback, useRef, useState, useEffect, useContext } from "react";
import { useNavigate } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { Helmet } from "react-helmet-async";
import {
  Button,
  Label,
  Modal,
  TextInput,
  Datepicker,
  Tooltip,
} from "flowbite-react";
import { IoIosOptions } from "react-icons/io";
import { IoCloseSharp } from "react-icons/io5";
import dayjs from "dayjs";
import type {
  ListType,
  BoardCategoryProps,
  searchType,
  Lost112ListType,
} from "@/entities";
import {
  Card,
  CategoryList,
  CustomButton,
  StateContext,
  Text,
  cls,
  useIntersectionObserver,
  useMemberStore,
} from "@/shared";
import { getAcquisitions, getLost112Acquire, getLosts } from "@/entities";

const Boards = ({ boardType }: BoardCategoryProps) => {
  const { setHeaderTitle } = useContext(StateContext);
  const { member, Authenticate } = useMemberStore();

  const [option, setOption] = useState(false);
  const [openCategory, setOpenCategory] = useState(false);
  const [openModal, setOpenModal] = useState(false);
  const [mobile, setMobile] = useState(false);

  // 검색조건
  const [category, setCategory] = useState("");
  const [dateSearch, setDateSearch] = useState(false); // 날짜 검색 적용 여부
  const [sDate, setSDate] = useState(dayjs(new Date()).format("YYYY-MM-DD"));
  const [eDate, setEDate] = useState(dayjs(new Date()).format("YYYY-MM-DD"));
  const [serviceType, setServiceType] = useState<"findear" | "Lost112">(
    "findear"
  );
  const [Trigger, setTrigger] = useState(false); // 데이터 패칭 트리거
  const [keywordTrigger, setKeywordTrigger] = useState(false); // 키워드 검색 트리거
  const [keyword, setKeyword] = useState("");

  // boardList -> Findear 데이터, lostBoardList -> Lost112 데이터
  const [boardList, setBoardList] = useState<ListType[]>([]);
  const [lostBoardList, setLostBoardList] = useState<Lost112ListType[]>([]);

  //무한 스크롤 조건
  const [isLoading, setIsLoading] = useState(false);
  const [pageNo, setPageNo] = useState(1);
  const [total, setTotal] = useState(1);

  const navigate = useNavigate();
  const [observe, unobserve] = useIntersectionObserver(() => {
    setIsLoading(true);
    setPageNo((prev) => prev + 1);
  });

  const target = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setHeaderTitle(boardType);
    setCategory("");
    setServiceType("findear");
    return () => setHeaderTitle("");
  }, [boardType]);

  useEffect(() => {
    setBoardList([]);
    setLostBoardList([]);
    setPageNo(1);
    setTotal(1);
    setTrigger(true);
  }, [boardType, category, dateSearch, keywordTrigger, serviceType]);

  useEffect(() => {
    if (isLoading || pageNo >= total) {
      unobserve(target.current as HTMLDivElement);
      return;
    }

    observe(target.current as HTMLDivElement);
  }, [isLoading, pageNo, total]);

  // 데이터를 패칭해오는 로직
  const handleDataFetching = () => {
    const requestData: searchType = { pageNo };

    if (category) {
      requestData.category = category;
    }

    if (keyword) {
      requestData.keyword = keyword;
    }

    if (dateSearch && sDate) {
      requestData.sDate = sDate;
    }

    if (dateSearch && eDate) {
      requestData.eDate = eDate;
    }

    if (boardType === "습득물" && serviceType === "findear") {
      getAcquisitions(
        requestData,
        ({ data }) => {
          if (!data.result?.boardList) {
            console.log("데이터가 없습니다");
            setIsLoading(false);
            return;
          }
          setBoardList((prev) => [...prev, ...data.result.boardList]);
          setTotal(data.result.totalPageNum);
          setIsLoading(false);
        },
        (error) => console.error(error)
      );
      return;
    }

    if (boardType === "분실물" && serviceType === "findear") {
      getLosts(
        requestData,
        ({ data }) => {
          if (!data.result?.boardList) {
            console.log("데이터가 없습니다");
            setIsLoading(false);
            return;
          }
          setBoardList((prev) => [...prev, ...data.result.boardList]);
          setTotal(data.result.totalPageNum);
          setIsLoading(false);
        },
        (error) => console.error(error)
      );
      return;
    }

    if (boardType === "습득물" && serviceType === "Lost112") {
      getLost112Acquire(
        requestData,
        ({ data }) => {
          if (data.result.length === 0) {
            console.log("데이터가 없습니다");
            setIsLoading(false);
            return;
          }
          setLostBoardList((prev) => [...prev, ...data.result]);
          setTotal(1000);
          setIsLoading(false);
        },
        (error) => console.error(error)
      );
      return;
    }
  };
  useEffect(() => {
    if (Trigger) {
      handleDataFetching();
      setTrigger(false);
    }
  }, [Trigger]);

  useEffect(() => {
    if (pageNo > 1) handleDataFetching();
  }, [pageNo]);

  const cartegoryVariants = {
    desktopInit: {
      opacity: 0,
      y: 0,
    },
    desktopEnd: {
      opacity: 1,
      y: 0,
    },
    mobileInit: {
      y: 800,
    },
    mobileEnd: {
      y: 0,
    },
  };

  const variants = {
    desktopInit: {
      opacity: 0,
      y: 0,
    },
    desktopEnd: {
      opacity: 1,
      y: 0,
    },
    mobileInit: {
      y: 80,
      x: 500,
    },
    mobileEnd: {
      x: 0,
      y: 80,
    },
  };

  const checkDevice = useCallback(() => {
    if (window.innerWidth > 1280) {
      setMobile(false);
      return;
    }

    setMobile(true);
  }, []);

  useEffect(() => {
    const handleResize = () => {
      checkDevice();
    };

    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
    };
  }, []);

  useEffect(() => {
    checkDevice();
  }, []);

  const requestAgency = () => {
    setOpenModal(true);
  };

  return (
    <div className="flex flex-col flex-1">
      <Helmet>
        <title>리스트 조회 페이지</title>
        <meta
          name="description"
          content="파인디어 분실, 습득 리스트 확인 페이지"
        />
        <meta
          name="keywords"
          content="Findear, 파인디어, 분실, 습득, Losts, Acquire, 리스트, list"
        />
      </Helmet>
      <Modal show={openModal} onClose={() => setOpenModal(false)}>
        <Modal.Header>안내</Modal.Header>
        <Modal.Body>
          <div className="space-y-6">
            <p className="text-base leading-relaxed text-gray-500 dark:text-gray-400">
              습득물 등록 기능은 시설 관리자만 사용 가능합니다. 관리자 시라면
              시설 관리자 등록을 해주세요
            </p>
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button color="warning" onClick={() => navigate("/agencyRegist")}>
            등록하기
          </Button>
          <Button color="gray" onClick={() => setOpenModal(false)}>
            닫기
          </Button>
        </Modal.Footer>
      </Modal>
      <div className="flex flex-col  flex-1 mx-[10px] my-[10px]">
        <div className="flex justify-between items-center">
          <Text className="font-bold text-[1.2rem]">
            {boardType === "분실물"
              ? "찾고있는 물건들이에요"
              : "보관중인 물건들이에요"}
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
            <div
              className={cls(
                "flex gap-[5px] items-center",
                boardType === "분실물" ? "hidden" : ""
              )}
            >
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
                className={boardType === "습득물" ? "" : "hidden"}
              />
            </Tooltip>
          </form>
          {/* className="w-[140px]"
            options={[{ value: "파인디어" }, { value: "Lost112" }]}
            onChange={(e) => {
              console.log(e.target.value);
            }}
          ></SelectBox> */}
        </div>
        <div className="flex justify-between items-center my-[10px]">
          <div className="flex flex-wrap h-full gap-[10px] ">
            <CustomButton
              className="border border-A706DarkGrey1 dark:bg-A706DarkGrey1 p-2 rounded-lg text-[1rem] font-bold bg-A706LightGrey"
              onClick={() => setOpenCategory(true)}
            >
              {category ? category : "카테고리"}
            </CustomButton>
            <AnimatePresence>
              {openCategory && (
                <motion.div
                  variants={cartegoryVariants}
                  initial={mobile ? "mobileInit" : "desktopInit"}
                  animate={mobile ? "mobileEnd" : "desktopEnd"}
                  exit={mobile ? "mobileInit" : "desktopInit"}
                  transition={{ ease: "easeOut", duration: 0.3 }}
                  className="absolute dark:bg-A706DarkGrey1 left-0 w-full h-full rounded-lg bg-A706LightGrey z-[10] overflow-hidden"
                >
                  <div className="flex items-center justify-between mx-[10px]">
                    <Text className="text-[1.5rem] font-bold p-[10px]">
                      카테고리 검색
                    </Text>
                    <div onClick={() => setOpenCategory(false)}>
                      <IoCloseSharp size="32" />
                    </div>
                  </div>
                  <CategoryList
                    setCategory={setCategory}
                    setModal={setOpenCategory}
                    className="grid grid-cols-4 grid-rows-4 gap-5"
                  />
                </motion.div>
              )}
            </AnimatePresence>

            {/* <SelectBox
              className="w-[140px]"
              options={[{ value: "data1" }, { value: "data2" }]}
              onChange={(e) => {
                console.log(e.target.value);
              }}
            ></SelectBox> */}
            {/* <SelectBox
              className="w-[140px]"
              options={[{ value: "data1" }, { value: "data2" }]}
              onChange={(e) => {
                console.log(e.target.value);
              }}
            ></SelectBox> */}
          </div>

          <div className="flex items-center justify-between z-[5] gap-2">
            <TextInput
              className="w-[180px]"
              placeholder="검색어를 입력하세요"
              onChange={(e) => setKeyword(e.target.value)}
              value={keyword}
            />
            <CustomButton
              className="border border-A706LightGrey2 rounded-lg p-2"
              onClick={() => {
                setKeywordTrigger((prev) => !prev);
              }}
            >
              검색
            </CustomButton>

            <div
              onClick={() => {
                setOption((prev) => !prev);
              }}
            >
              <IoIosOptions size="24" />
            </div>
            <AnimatePresence>
              {option && (
                <motion.div
                  variants={variants}
                  initial={mobile ? "mobileInit" : "desktopInit"}
                  animate={mobile ? "mobileEnd" : "desktopEnd"}
                  exit={mobile ? "mobileInit" : "desktopInit"}
                  transition={{ ease: "easeOut", duration: 0.5 }}
                  className="absolute max-xl:w-[85%] xl:w-[40%] xl:h-[600px] right-0  max-xl:top-0 h-full  bg-A706LightGrey dark:bg-A706DarkGrey1 rounded-xl border-2 border-A706Grey2 p-[10px]"
                >
                  <div className="flex items-center justify-between">
                    <Text className="text-[1.5rem] font-bold">상세 검색</Text>
                    <div onClick={() => setOption(false)}>
                      <IoCloseSharp size="32" />
                    </div>
                  </div>
                  <div className="flex flex-col gap-[10px] mt-[20px]">
                    <Label>시작일자 </Label>
                    <Datepicker
                      language="ko-kr"
                      helperText="검색 시작 기준일"
                      value={sDate}
                      onSelectedDateChanged={(date) => {
                        setSDate(dayjs(date).format("YYYY-MM-DD"));
                      }}
                    />
                    <Label>종료일자 </Label>
                    <Datepicker
                      language="ko-kr"
                      helperText="검색 종료 기준일"
                      value={eDate}
                      onSelectedDateChanged={(date) => {
                        dayjs(date).isAfter(dayjs(sDate))
                          ? setEDate(dayjs(date).format("YYYY-MM-DD"))
                          : (alert("시작일보다 종료일이 빠릅니다"),
                            setEDate(sDate));
                      }}
                    />
                    <CustomButton
                      className={cls(
                        "w-full h-[40px] rounded-lg text-A706LightGrey",
                        !dateSearch ? "bg-A706CheryBlue" : "bg-A706Blue3"
                      )}
                      onClick={() => setDateSearch((prev) => !prev)}
                    >
                      {!dateSearch
                        ? "설정한 범위로 검색"
                        : "범위 검색중.. 눌러서 종료"}
                    </CustomButton>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
        <div className="flex flex-1 flex-col">
          <div
            className={cls(
              "grid max-sm:grid-cols-2 max-md:grid-cols-3 max-lg:grid-cols-4 max-xl:grid-cols-6 max-2xl:grid-cols-7 grid-cols-8 justify-items-center gap-[10px]",
              openCategory ? "hidden" : ""
            )}
          >
            {serviceType === "findear"
              ? boardList &&
                boardList?.map((item) => {
                  return (
                    <Card
                      key={item.boardId}
                      category={item.category ?? ""}
                      date={
                        boardType === "습득물"
                          ? item.acquiredAt ?? ""
                          : item.lostAt ?? ""
                      }
                      image={item.thumbnailUrl}
                      locate={
                        boardType === "습득물"
                          ? item.agency?.name ?? ""
                          : item.suspiciousPlace ?? ""
                      }
                      title={item.productName}
                      isLost={item.isLost}
                      onClick={() =>
                        boardType === "습득물"
                          ? navigate(`/foundItemDetail/${item.boardId}`)
                          : navigate(`/lostItemDetail/${item.boardId}`)
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
            <div ref={target} className="w-full" />
          </div>
        </div>
      </div>
      <div className="sticky bottom-[110px]">
        <div className="flex flex-col items-end">
          <CustomButton
            className={cls(
              "text-2xl font-bold rounded-3xl shadow-md ",
              "bg-A706CheryBlue text-A706LightGrey px-3 py-2"
            )}
            onClick={() => {
              Authenticate
                ? boardType === "분실물"
                  ? navigate("/lostItemRegist")
                  : member?.role === "NORMAL"
                  ? requestAgency()
                  : navigate("/acquireRegist")
                : navigate("/");
            }}
          >
            + 글 쓰기
          </CustomButton>
        </div>
      </div>
    </div>
  );
};

export default Boards;
