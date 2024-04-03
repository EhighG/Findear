/* eslint-disable @typescript-eslint/no-explicit-any */
import MenuIcon from "@mui/icons-material/Menu";
import PriorityHighIcon from "@mui/icons-material/PriorityHigh";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import ArrowForwardOutlinedIcon from "@mui/icons-material/ArrowForwardOutlined";
import NavigateNextOutlinedIcon from "@mui/icons-material/NavigateNextOutlined";
import KeyboardDoubleArrowRightIcon from "@mui/icons-material/KeyboardDoubleArrowRight";
import { ListTab } from "@/widgets";
import { CustomButton, Text, useMemberStore } from "@/shared";
import { useEffect, useRef, useState } from "react";
import { getAcquisitions, userInfoPatch } from "@/entities";
import { useNavigate } from "react-router-dom";
import {
  CustomFlowbiteTheme,
  Kbd,
  ListGroup,
  Progress,
  TextInput,
} from "flowbite-react";
import { getPlaceInfo } from "@/entities/geolocation";
import { MysteryBox, FileSearching } from "@/shared";
import {
  getMatchedCount,
  getMatchingFindearBest,
  getMatchingLost112Best,
} from "@/entities/findear/api";

import Swal from "sweetalert2";

type AcquisitionThumbnail = {
  acquiredAt: string;
  agency: { id: number; name: string; address: string };
  boardId: number;
  category: string;
  productName: string;
  thumbnailUrl: string;
  writer: { memberId: number; phoneNumber: string; role: string };
};

type Place = {
  title: string;
  category: string;
  address: string;
  xpos: number;
  ypos: number;
};

type FindearBest = {
  lostBoard: { lostBoardId: number; boardId: number; productName: string };
  acquiredBoard: {
    boardId: number;
    productName: string;
    category: string;
    acquiredAt: string;
    agencyName: string;
    agencyAddress: string;
    thumbnailUrl: string;
  };
  similarityRate: number;
  matchedAt: string;
};

type Lost112Best = {
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

type Best = {
  findearBest: FindearBest | null;
  lost112Best: Lost112Best | null;
};

const geocoder = new kakao.maps.services.Geocoder();

const customProgress: CustomFlowbiteTheme["progress"] = {
  base: "w-full overflow-hidden rounded-full bg-A706Blue3 dark:bg-gray-700",
  label: "mb-1 flex justify-between font-medium dark:text-white",
  bar: "space-x-2 rounded-full text-center text-xs text-white dark:text-cyan-100",
  color: {
    dark: "bg-gray-600 dark:bg-gray-300",
    blue: "bg-blue-600",
    red: "bg-red-600 dark:bg-red-500",
    green: "bg-green-600 dark:bg-green-500",
    yellow: "bg-yellow-400",
    indigo: "bg-indigo-600 dark:bg-indigo-500",
    purple: "bg-purple-600 dark:bg-purple-500",
    cyan: "bg-cyan-600",
    gray: "bg-gray-500",
    lime: "bg-lime-600",
    pink: "bg-pink-500",
    teal: "bg-teal-600",
    A706CheryBlue: "bg-A706CheryBlue",
  },
  size: {
    sm: "h-1.5",
    md: "h-2.5",
    lg: "h-4",
    xl: "h-6",
  },
};

// const customPagination: CustomFlowbiteTheme["pagination"] = {
//   base: "",
//   layout: {
//     table: {
//       base: "text-sm text-gray-700 dark:text-gray-400",
//       span: "font-semibold text-gray-900 dark:text-white",
//     },
//   },
//   pages: {
//     base: "xs:mt-0 mt-2 inline-flex items-center -space-x-px",
//     showIcon: "inline-flex",
//     previous: {
//       base: "ml-0 rounded-l-lg bg-white px-3 py-2 leading-tight text-gray-500 enabled:hover:bg-A706Blue enabled:hover:text-A706CheryBlue dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 enabled:dark:hover:bg-gray-700 enabled:dark:hover:text-white",
//       icon: "h-5 w-5",
//     },
//     next: {
//       base: "rounded-r-lg bg-white px-3 py-2 leading-tight text-gray-500 enabled:hover:bg-A706Blue enabled:hover:text-A706CheryBlue dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 enabled:dark:hover:bg-gray-700 enabled:dark:hover:text-white",
//       icon: "h-5 w-5",
//     },
//     selector: {
//       base: "w-12 bg-white py-2 leading-tight text-gray-500 enabled:hover:bg-A706Blue enabled:hover:text-A706CheryBlue dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 enabled:dark:hover:bg-gray-700 enabled:dark:hover:text-white",
//       active:
//         "bg-A706Blue text-A706CheryBlue hover:bg-cyan-100 hover:text-cyan-700 dark:border-gray-700 dark:bg-gray-700 dark:text-white",
//       disabled: "cursor-not-allowed opacity-50",
//     },
//   },
// };

const Main = () => {
  const navigate = useNavigate();

  const addressCard = useRef<HTMLDivElement>(null);

  const { member, agency, setAgency } = useMemberStore();

  const [selectedIndex, setSelectedIndex] = useState<number>(0);
  const [selectedCategory, setSelectedCategory] = useState<string>("");
  const [viewOptions, setViewOptions] = useState<boolean>(false);
  const [mainViewState, setMainViewState] = useState<string>("NORMAL");
  const [acquisitionThumbnailList, setAcquisitionThumbnailList] = useState<
    AcquisitionThumbnail[]
  >([]);
  const [latitude, setLatitude] = useState<number>(0);
  const [longitude, setLongitude] = useState<number>(0);
  const [placeAddresName, setPlaceAddressName] = useState<string>("");
  const [placeMap, setPlaceMap] = useState<Map<string, Place[]>>(
    new Map<string, Place[]>()
  );
  const [registAddress, setRegistAddress] = useState<boolean>(false);
  const [, setCurrentPlace] = useState<Place>();
  const [todayMatchedCount, setTodayMatchedCount] = useState<number>(0);
  const [yesterdayMatchedCount, setYesterdayMatchedCount] = useState<number>(0);
  const [matchingFindearBestList, setMatchingFindearBestList] = useState<
    FindearBest[]
  >([]);
  const [matchingLost112BestList, setMatchingLost112BestList] = useState<
    Lost112Best[]
  >([]);
  const [bestList, setBestList] = useState<Best[]>([]);
  const [best, setBest] = useState<Best | null>(null);
  const [completeFindearBest, setCompleteFindearBest] =
    useState<boolean>(false);
  const [completeLost112Best, setCompleteLost112Best] =
    useState<boolean>(false);
  const [hideRefreshButton, setHideRefreshButton] = useState<boolean>(false);
  // const PAGE_SIZE = 5;
  // const [pages, setPages] = useState<Place[][]>([]);
  // const [currentPage, setCurrentPage] = useState(1);
  // const onPageChange = (page: number) => setCurrentPage(page);

  // const createPage = () => {
  //   let page: Place[] = [];
  //   placeMap?.get(selectedCategory)?.forEach((place: Place, index) => {
  //     if (index % PAGE_SIZE === 0) {
  //       page = [];
  //     }
  //     page.push(place);
  //     if (index % PAGE_SIZE === 0) {
  //       pages.push(page);
  //     }
  //   });
  // };

  const getCurrentPosition = () => {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLatitude(position.coords.latitude);
        setLongitude(position.coords.longitude);
        setHideRefreshButton(false);
      },
      () => {},
      { enableHighAccuracy: true, maximumAge: 0, timeout: 5000 }
    );
  };

  const handleAgencyRegist = (place: Place) => {
    if (place) {
      userInfoPatch(
        member.memberId.toString(),
        {
          memberId: member.memberId,
          role: member.role,
          phoneNumber: member.phoneNumber,
          agency: {
            name: place.title,
            address: place.address,
            xpos: place.xpos,
            ypos: place.ypos,
          },
        },
        ({ data }) => {
          console.log(data);
          setAgency(data.result.agency);
          setRegistAddress(false);
        },
        (error) => console.log(error)
      );
    }
  };

  useEffect(() => {
    getCurrentPosition();

    getMatchedCount(
      ({ data }) => {
        console.log(data);
        setTodayMatchedCount(data.result.today);
        setYesterdayMatchedCount(data.result.yesterday);
      },
      (error) => console.log(error)
    );

    getAcquisitions(
      { pageNo: 1 },
      ({ data }) => {
        setAcquisitionThumbnailList(data.result.boardList);
      },
      (error) => console.log(error)
    );

    getMatchingFindearBest(
      { pageNo: 1 },
      ({ data }) => {
        console.log(data);
        setMatchingFindearBestList(data.result.matchingList);
      },
      (error) => console.log(error)
    ).finally(() => setCompleteFindearBest(true));

    getMatchingLost112Best(
      { pageNo: 1 },
      ({ data }) => {
        console.log(data);
        setMatchingLost112BestList(data.result.matchingList);
      },
      (error) => console.log(error)
    ).finally(() => setCompleteLost112Best(true));
  }, []);

  useEffect(() => {
    if (latitude && longitude) {
      geocoder.coord2Address(longitude, latitude, (result: any) => {
        setPlaceAddressName(result[0].address.address_name);
      });
    }
  }, [latitude, longitude]);

  useEffect(() => {
    if (placeAddresName) {
      console.log(placeAddresName);
      getPlaceInfo(
        100,
        1,
        placeAddresName,
        "PLACE",
        ({ data }) => {
          console.log(data);
          const map: Map<string, Place[]> = new Map<string, Place[]>();
          data.response.result.items.forEach((item: any) => {
            const title = item.title;
            const category = item.category.split(">")[0].trim();
            if (
              category !== "철도시설" &&
              category !== "도로시설" &&
              category !== "진출입시설" &&
              category !== "기타정보서비스업"
            ) {
              if (map.has(category)) {
                const titles: Place[] = map.get(category)!;
                titles.push({
                  category,
                  title,
                  address: placeAddresName,
                  xpos: item.point.x,
                  ypos: item.point.y,
                });
                map.set(category, titles);
              } else {
                map.set(category, [
                  {
                    category,
                    title,
                    address: placeAddresName,
                    xpos: item.point.x,
                    ypos: item.point.y,
                  },
                ]);
              }
            }
          });
          setPlaceMap(map);
          setSelectedCategory(map.keys().next().value);
        },
        (error) => console.log(error)
      );
    }
  }, [placeAddresName]);

  useEffect(() => {
    if (completeFindearBest && completeLost112Best) {
      const list: Best[] = [];
      let best: Best | null = null;
      let maxsSimilarityRate = 0;
      const maxLength = Math.max(
        matchingFindearBestList.length,
        matchingLost112BestList.length
      );

      for (let i = 0; i < maxLength; i++) {
        const max = Math.max(
          matchingFindearBestList[i]?.similarityRate,
          matchingLost112BestList[i]?.similarityRate
        );

        if (max > maxsSimilarityRate) {
          maxsSimilarityRate = max;

          if (
            matchingFindearBestList[i]?.similarityRate >
            matchingLost112BestList[i]?.similarityRate
          ) {
            best = {
              findearBest: matchingFindearBestList[i] ?? null,
              lost112Best: null,
            };
          } else {
            best = {
              findearBest: null,
              lost112Best: matchingLost112BestList[i] ?? null,
            };
          }
        }

        list.push({
          findearBest: matchingFindearBestList[i] ?? null,
          lost112Best: matchingLost112BestList[i] ?? null,
        });
      }

      setBest(best);
      setBestList(list);
    }
  }, [completeFindearBest, completeLost112Best]);

  return viewOptions ? (
    <div className="flex flex-col self-center w-[360px]">
      <ArrowBackOutlinedIcon
        className="m-5"
        onClick={() => setViewOptions(false)}
      />
      <div className="flex flex-col">
        {
          <>
            {member.role === "MANAGER" ? (
              <CustomButton
                className="flex flex-col justify-around p-5 rounded-lg border-2 m-5"
                onClick={() => navigate(`/acquireRegist`)}
              >
                <div className="flex w-full">
                  <img
                    src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/New%20Button.png"
                    alt="New Button"
                    width="35"
                    height="35"
                  />
                  <Text className="w-full font-bold text-lg text-center self-center">
                    습득물 등록
                  </Text>
                  <NavigateNextOutlinedIcon className="self-center" />
                </div>
              </CustomButton>
            ) : (
              <CustomButton
                className="flex flex-col justify-around p-5 rounded-lg border-2 m-5"
                onClick={() => navigate(`/lostItemRegist`)}
              >
                <div className="flex w-full">
                  <img
                    src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Loudspeaker.png"
                    alt="Loudspeaker"
                    width="35"
                    height="35"
                  />
                  <Text className="w-full font-bold text-lg text-center self-center">
                    분실물 등록
                  </Text>
                  <NavigateNextOutlinedIcon className="self-center" />
                </div>
              </CustomButton>
            )}
            <CustomButton
              className="flex flex-col justify-around p-5 rounded-lg border-2 m-5"
              onClick={() => navigate(`/acquire`)}
            >
              <div className="flex w-full">
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/File%20Cabinet.png"
                  alt="File Cabinet"
                  width="35"
                  height="35"
                />
                <Text className="w-full font-bold text-lg text-center self-center">
                  습득물 목록
                </Text>
                <NavigateNextOutlinedIcon className="self-center" />
              </div>
            </CustomButton>
            <CustomButton
              className="flex flex-col justify-around p-5 rounded-lg border-2 m-5"
              onClick={() => navigate(`/losts`)}
            >
              <div className="flex w-full">
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Red%20Question%20Mark.png"
                  alt="Red Question Mark"
                  width="35"
                  height="35"
                />
                <Text className="w-full font-bold text-lg text-center self-center">
                  분실물 목록
                </Text>
                <NavigateNextOutlinedIcon className="self-center" />
              </div>
            </CustomButton>
          </>
        }
      </div>
    </div>
  ) : (
    <>
      <div className="flex flex-col self-center w-[360px] p-5">
        {member.role === "MANAGER" && mainViewState === "MANAGER" ? (
          <div
            className="flex"
            onClick={() => {
              setMainViewState("NORMAL");
              setSelectedIndex(0);
            }}
          >
            <ArrowBackOutlinedIcon />
            <p className="mx-2">분실물 보러 가기</p>
          </div>
        ) : (
          <div
            className="flex self-end"
            onClick={() => setMainViewState("MANAGER")}
          >
            <p className="mx-2">습득물 등록하러 가기</p>
            <ArrowForwardOutlinedIcon className="self-end" />
          </div>
        )}
        {/* {member.role === "MANAGER" && mainViewState === "NORMAL" ? (

        ) : (
          <></>
        )} */}
        <div className="flex flex-col">
          {mainViewState === "MANAGER" ? (
            <>
              <CustomButton
                className="border-2 rounded-lg flex flex-col justify-around p-5 my-5"
                onClick={() => {
                  if (agency) {
                    setRegistAddress(true);
                    addressCard.current?.scrollIntoView({ behavior: "smooth" });
                  } else {
                    navigate(`/acquireRegist`);
                  }
                }}
              >
                <>
                  <div className="flex w-full justify-center">
                    <Text className="font-bold text-xl text-start self-center">
                      누군가 물건을 놓고 갔나요?
                    </Text>
                  </div>
                  <MysteryBox />
                  <Text className="w-full text-center">
                    파인디어에 습득물을 등록하면
                  </Text>
                  <Text className="w-full text-center">
                    주인을 쉽게 찾아줄 수 있습니다.
                  </Text>
                  <Text className="text-sm text-right w-full mt-10">
                    <>
                      등록하러 가기
                      <KeyboardDoubleArrowRightIcon fontSize="small" />
                    </>
                  </Text>
                </>
              </CustomButton>
            </>
          ) : (
            <>
              <CustomButton
                className="border-2 rounded-lg flex flex-col justify-around p-5 my-5"
                onClick={() => navigate(`/acquire`)}
              >
                <>
                  <div className="flex w-full justify-center">
                    <Text className="font-bold text-xl text-start self-center">
                      소중한 물건을 잃어버리셨나요?
                    </Text>
                  </div>
                  <FileSearching />
                  <Text className="w-full text-center">
                    잃어버린 물건과 일치하는 습득물을
                  </Text>
                  <Text className="w-full text-center">
                    자동으로 찾아줄 수 있습니다.
                  </Text>
                  <Text className="text-sm text-A706DarkGrey2 text-right w-full mt-10">
                    <>
                      찾아보기
                      <KeyboardDoubleArrowRightIcon fontSize="small" />
                    </>
                  </Text>
                </>
              </CustomButton>
            </>
          )}
        </div>
        <div className="flex flex-row justify-around w-full">
          {mainViewState === "MANAGER" ? (
            <div className="flex flex-col w-full justify-center">
              <div className="flex flex-col w-full rounded-lg border-2 p-5">
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Left%20Arrow%20Curving%20Right.png"
                  alt="Left Arrow Curving Right"
                  width="35"
                  height="35"
                  className="m-1"
                />
                <p className="my-3 mx-2 text-lg font-bold">되돌려 준 습득물</p>
                {/* <hr className="mb-3" /> */}
                <div className="flex justify-between mt-3 mx-2">
                  <Text className="text-2xl font-bold self-center">
                    {todayMatchedCount}
                  </Text>
                  {todayMatchedCount < yesterdayMatchedCount ? (
                    <Text className="text-sm self-center text-green-500">
                      {`(+${yesterdayMatchedCount - todayMatchedCount}) 전날
                      대비`}
                    </Text>
                  ) : (
                    <></>
                  )}
                  {todayMatchedCount == yesterdayMatchedCount ? (
                    <Text className="text-sm self-center text-A">
                      {`전날과 동일`}
                    </Text>
                  ) : (
                    <></>
                  )}
                  {todayMatchedCount < yesterdayMatchedCount ? (
                    <Text className="text-sm self-center text-red-500">
                      {`(-${yesterdayMatchedCount - todayMatchedCount}) 전날
                      대비`}
                    </Text>
                  ) : (
                    <></>
                  )}
                </div>
              </div>
              <div className="w-full flex flex-col">
                <div className="border-2 p-5 rounded-lg my-5">
                  <img
                    src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Office%20Building.png"
                    alt="Office Building"
                    width="35"
                    height="35"
                    className="m-1"
                  />
                  <div
                    ref={addressCard}
                    className="flex justify-between my-3 mx-2"
                  >
                    <p className="font-bold text-lg">시설 주소</p>
                    {!registAddress ? (
                      <></>
                    ) : (
                      <p
                        className="text-sm text-A706Grey2 self-center"
                        onClick={() => setRegistAddress(false)}
                      >
                        변경 취소
                      </p>
                    )}
                  </div>
                  {!registAddress ? (
                    <>
                      <div className="flex justify-center my-5">
                        {agency?.address || agency?.name ? (
                          <div className="flex flex-col w-full mx-2">
                            <p className="text-lg">{agency.name}</p>
                            <p className="text-sm">{agency.address}</p>
                          </div>
                        ) : (
                          <Kbd className="px-5 py-2">
                            등록 된 시설이 없습니다.
                          </Kbd>
                        )}
                      </div>
                      <CustomButton
                        className="w-full rounded-lg bg-A706CheryBlue p-2 text-white"
                        onClick={() => setRegistAddress(true)}
                      >
                        {agency?.address || agency?.name
                          ? "시설 변경"
                          : "시설 등록"}
                      </CustomButton>
                    </>
                  ) : (
                    <div className="w-full">
                      <div className="my-5 rounded-lg border-2 p-5">
                        <div className="mb-3 flex justify-between">
                          <p className="text-lg self-center">시설 위치</p>
                          {hideRefreshButton ? (
                            <></>
                          ) : (
                            <img
                              src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Counterclockwise%20Arrows%20Button.png"
                              alt="Counterclockwise Arrows Button"
                              width="30"
                              height="30"
                              onClick={() => {
                                setHideRefreshButton(true);
                                getCurrentPosition();
                              }}
                            />
                          )}
                        </div>
                        <TextInput
                          placeholder={"장소를 입력해 주세요."}
                          readOnly
                          value={placeAddresName}
                          onClick={() => {
                            new daum.Postcode({
                              oncomplete: (data: any) => {
                                console.log(data);
                                setPlaceAddressName(data.jibunAddress);
                              },
                            }).open();
                          }}
                        />
                      </div>
                      <div className="flex flex-col w-full overflow-hidden border-2 border-b-0 rounded-b-none rounded-md">
                        <div className="sticky">
                          {placeMap.keys.length > 0 ? (
                            <Text className="text-sm m-5">
                              * 관리자님의 현재 위치를 기준으로 검색한 주변의
                              시설들입니다.
                            </Text>
                          ) : (
                            <></>
                          )}
                          <div className="flex mx-3 text-sm overflow-x-scroll text-nowrap">
                            {[...placeMap.keys()].length > 0 ? (
                              [...placeMap.keys()].map(
                                (category: string, index) => (
                                  <div className="w-full mx-1" key={index}>
                                    <ListTab
                                      text={category}
                                      index={index}
                                      selectedIndex={selectedIndex}
                                      onClick={() => {
                                        setSelectedCategory(category);
                                      }}
                                    />
                                  </div>
                                )
                              )
                            ) : (
                              <div className="flex m-2 mt-7">
                                <PriorityHighIcon />
                                <p className="self-center">
                                  주변 시설이 검색되지 않습니다.
                                </p>
                              </div>
                            )}
                          </div>
                          {[...placeMap.keys()].length > 0 ? (
                            <hr className="main-tab-hr mx-3 border-[1px]" />
                          ) : (
                            <></>
                          )}
                        </div>
                      </div>
                      <div className="flex flex-col w-full overflow-scroll border-2 border-t-0 rounded-t-none rounded-md mb-5">
                        <ListGroup className="border-0 p-3 rounded-none w-full">
                          {placeMap
                            ?.get(selectedCategory)
                            ?.map((place: Place, index) => (
                              <ListGroup.Item
                                className="w-full"
                                key={index}
                                onClick={() => {
                                  setCurrentPlace(place);
                                  Swal.fire({
                                    title: "등록하시겠습니까?",
                                    text: place.title + ", " + place.address,
                                    confirmButtonText: "확인",
                                    showCancelButton: true,
                                    cancelButtonText: "취소",
                                  }).then((result) => {
                                    if (result.isConfirmed) {
                                      handleAgencyRegist(place);
                                    }
                                  });
                                }}
                              >
                                {place.title}
                              </ListGroup.Item>
                            ))}
                          {/* <div className="w-full">
                          <Pagination
                            nextLabel=""
                            previousLabel=""
                            showIcons
                            theme={customPagination}
                            currentPage={currentPage}
                            totalPages={100}
                            onPageChange={onPageChange}
                          />
                        </div> */}
                        </ListGroup>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ) : bestList.length > 0 && best ? (
            <div className="flex flex-col w-full justify-center">
              <div className="flex flex-col w-full rounded-lg border-2 p-5">
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Check%20Mark%20Button.png"
                  alt="Check Mark Button"
                  width="25"
                  height="25"
                />
                <p className="my-3 text-lg font-bold">
                  Findear에서 분실물을 찾았습니다!
                </p>
                <div className="mx-2 my-3 flex justify-between">
                  <span className="bg-A706Blue2 text-A706CheryBlue text-xs font-bold mr-2 px-2.5 py-0.5 rounded dark:bg-blue-900 dark:text-blue-300 h-fit">
                    {best.findearBest
                      ? best.findearBest.acquiredBoard.category
                      : best.lost112Best!.mainPrdtClNm}
                  </span>
                  <span className="bg-blue-900 text-A706Yellow text-xs font-bold mr-2 px-2.5 py-0.5 rounded dark:bg-blue-900 dark:text-blue-300 h-fit">
                    Lost112
                  </span>
                  <p className="text-right text-xs w-fit">
                    {best.findearBest
                      ? best.findearBest.acquiredBoard.acquiredAt
                      : best.lost112Best!.fdYmd}
                  </p>
                </div>
                <div className="flex flex-row">
                  <img
                    alt="No Image"
                    className="size-[64px] m-2 rounded-lg"
                    src={
                      best.findearBest
                        ? best.findearBest.acquiredBoard.thumbnailUrl
                        : best.lost112Best!.fdFilePathImg
                    }
                  ></img>
                  <div className="text-start items-center h-fit m-2 w-full">
                    <p className="self-center">
                      {best.findearBest
                        ? best.findearBest.acquiredBoard.productName
                        : best.lost112Best!.fdPrdtNm}
                    </p>
                  </div>
                </div>
                <div className="my-3 mx-2">
                  <Progress
                    theme={customProgress}
                    labelProgress
                    progress={
                      best.findearBest
                        ? best.findearBest.similarityRate
                        : best.lost112Best!.similarityRate
                    }
                    progressLabelPosition="inside"
                    color="A706CheryBlue"
                    size={"lg"}
                  />
                </div>
              </div>
            </div>
          ) : (
            <></>
          )}
        </div>
        {mainViewState === "MANAGER" ? (
          <></>
        ) : (
          <>
            <div className="flex w-full justify-around mt-5">
              <ListTab
                text={"실시간 습득물"}
                index={0}
                selectedIndex={selectedIndex}
                onClick={() => setSelectedIndex(0)}
              />
              <ListTab
                text={"매칭"}
                index={1}
                selectedIndex={selectedIndex}
                onClick={() => setSelectedIndex(1)}
              />
            </div>
            <hr className="main-tab-hr" />
            {selectedIndex == 0 ? (
              <div className="py-5">
                {acquisitionThumbnailList.map(
                  (acquisitionThumbnail: AcquisitionThumbnail, index) => (
                    <div
                      className="flex flex-col rounded-lg border-2 m-3 py-3 px-3"
                      key={index}
                      onClick={() =>
                        navigate(
                          `/foundItemDetail/${acquisitionThumbnail.boardId}`
                        )
                      }
                    >
                      <div className="mx-3 flex justify-between">
                        <span className="bg-A706Blue2 text-A706CheryBlue text-xs font-bold me-2 px-2.5 py-0.5 rounded dark:bg-blue-900 dark:text-blue-300">
                          {acquisitionThumbnail.category ?? "카테고리 없음"}
                        </span>
                        {/* <CloseOutlinedIcon className="self-end" fontSize="small" /> */}
                      </div>
                      <div className="flex flex-row">
                        <img
                          alt="No Image"
                          className="size-[64px] m-3 rounded-lg"
                          src={acquisitionThumbnail.thumbnailUrl}
                        />
                        <div className="w-full flex flex-col justify-between">
                          <div className="text-start items-center m-3">
                            <h3>
                              {acquisitionThumbnail.productName.length < 12
                                ? acquisitionThumbnail.productName
                                : acquisitionThumbnail.productName
                                    .substring(0, 10)
                                    .concat("..")}
                            </h3>
                          </div>
                          <p className="text-end text-xs m-3 ">
                            습득일: {acquisitionThumbnail.acquiredAt}
                          </p>
                        </div>
                      </div>
                    </div>
                  )
                )}
                <div className="flex justify-end">
                  <button className="flex" onClick={() => navigate("/acquire")}>
                    <Text>전체 보기</Text>
                    <NavigateNextOutlinedIcon />
                  </button>
                </div>
              </div>
            ) : (
              <div className="py-5">
                {bestList && bestList.length > 0 ? (
                  bestList.map((best) => (
                    <>
                      <div className="flex flex-col rounded-lg border-2 m-3 py-3 px-3">
                        <div className="mx-3 m-2 flex">
                          <Kbd className="w-fit">
                            {best.findearBest
                              ? best.findearBest.lostBoard.productName
                              : best.lost112Best!.fdPrdtNm}
                          </Kbd>
                        </div>
                        <hr className="my-2" />
                        {best.findearBest ? (
                          <>
                            <div className="mx-2 my-3 flex justify-between">
                              <span className="bg-A706Blue2 text-A706CheryBlue text-xs font-bold mr-2 px-2.5 py-0.5 rounded dark:bg-blue-900 dark:text-blue-300 h-fit">
                                {best.findearBest.acquiredBoard.category}
                              </span>
                              <p className="text-right text-xs w-fit">
                                {best.findearBest.acquiredBoard.acquiredAt}
                              </p>
                            </div>
                            <div className="flex flex-row">
                              <img
                                alt="No Image"
                                className="size-[64px] m-2 rounded-lg"
                                src={
                                  best.findearBest.acquiredBoard.thumbnailUrl
                                }
                              ></img>
                              <div className="text-start items-center h-fit m-2 w-full">
                                <p className="self-center">
                                  {best.findearBest.acquiredBoard.productName}
                                </p>
                              </div>
                            </div>
                            <div className="my-3 mx-2">
                              <Progress
                                theme={customProgress}
                                labelProgress
                                progress={best.findearBest.similarityRate}
                                progressLabelPosition="inside"
                                color="A706CheryBlue"
                                size={"lg"}
                              />
                            </div>
                            <hr className="my-2" />
                          </>
                        ) : (
                          <></>
                        )}
                        {best.lost112Best ? (
                          <>
                            <div className="mx-2 my-3 flex justify-between">
                              <span className="bg-A706Blue2 text-A706CheryBlue text-xs font-bold mr-2 px-2.5 py-0.5 rounded dark:bg-blue-900 dark:text-blue-300 h-fit">
                                {best.lost112Best.mainPrdtClNm}
                              </span>
                              <span className="bg-blue-900 text-A706Yellow text-xs font-bold mr-2 px-2.5 py-0.5 rounded dark:bg-blue-900 dark:text-blue-300 h-fit">
                                Lost112
                              </span>
                              <p className="text-right text-xs w-fit">
                                {best.lost112Best.fdYmd}
                              </p>
                            </div>
                            <div className="flex flex-row">
                              <img
                                alt="No Image"
                                className="size-[64px] m-2 rounded-lg"
                                src={best.lost112Best.fdFilePathImg}
                              ></img>
                              <div className="text-start items-center h-fit m-2 w-full">
                                <p className="self-center">
                                  {best.lost112Best.fdPrdtNm}
                                </p>
                              </div>
                            </div>
                            <div className="my-3 mx-2">
                              <Progress
                                theme={customProgress}
                                labelProgress
                                progress={best.lost112Best.similarityRate}
                                progressLabelPosition="inside"
                                color="A706CheryBlue"
                                size={"lg"}
                              />
                            </div>
                          </>
                        ) : (
                          <></>
                        )}
                      </div>
                      <div className="m-3 flex justify-end">
                        <div
                          className="flex"
                          onClick={() => {
                            navigate(
                              `/matchingList/${
                                best.findearBest
                                  ? best.findearBest.lostBoard.lostBoardId
                                  : best.lost112Best!.lostBoardId
                              }`
                            );
                          }}
                        >
                          <Text>매칭된 습득물 전체 보기</Text>
                          <NavigateNextOutlinedIcon />
                        </div>
                      </div>
                    </>
                  ))
                ) : (
                  <div className="text-center">
                    아직 매칭 된 물건이 없습니다.
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </div>
      <div className="sticky bottom-[110px] mx-3">
        <div className="flex flex-col items-end">
          <CustomButton
            className="text-2xl font-bold rounded-3xl shadow-md bg-A706Yellow text-A706DarkGrey2 fixed bottom-28 px-3 py-2"
            onClick={() => {
              setViewOptions(true);
            }}
          >
            <MenuIcon />
          </CustomButton>
        </div>
      </div>
    </>
  );
};

export default Main;
