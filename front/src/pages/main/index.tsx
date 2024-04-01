/* eslint-disable @typescript-eslint/no-explicit-any */
import SearchIcon from "@mui/icons-material/Search";
import ErrorOutlineIcon from "@mui/icons-material/ErrorOutline";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import ManageSearchOutlinedIcon from "@mui/icons-material/ManageSearchOutlined";
import NavigateNextOutlinedIcon from "@mui/icons-material/NavigateNextOutlined";
import KeyboardDoubleArrowRightIcon from "@mui/icons-material/KeyboardDoubleArrowRight";
import { ListTab } from "@/widgets";
import { CustomButton, Text, useMemberStore } from "@/shared";
import { useEffect, useState } from "react";
import { getAcquisitions } from "@/entities";
import { useNavigate } from "react-router-dom";
import {
  Alert,
  CustomFlowbiteTheme,
  Kbd,
  ListGroup,
  Progress,
  TextInput,
} from "flowbite-react";
import { getPlaceInfo } from "@/entities/geolocation";
import { HiInformationCircle } from "react-icons/hi";

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
};

const geocoder = new kakao.maps.services.Geocoder();

const customTheme: CustomFlowbiteTheme["progress"] = {
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

const Main = () => {
  const navigate = useNavigate();

  const { member, agency } = useMemberStore();

  const [selectedIndex, setSelectedIndex] = useState<number>(0);
  const [selectedCategory, setSelectedCategory] = useState<string>("");
  const [viewOptions, setViewOptions] = useState<boolean>(false);
  const [acquisitionThumbnailList, setAcquisitionThumbnailList] = useState<
    AcquisitionThumbnail[]
  >([]);
  const [latitude, setLatitude] = useState<number>(0);
  const [longitude, setLongitude] = useState<number>(0);
  const [placeAddresName, setPlaceAddressName] = useState<string>("");
  const [placeMap, setPlaceMap] = useState<Map<string, Place[]>>(
    new Map<string, Place[]>()
  );
  const [requireAddress, setRequireAddress] = useState<boolean>(
    agency ? false : true
  );

  const getCurrentPosition = () => {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLatitude(position.coords.latitude);
        setLongitude(position.coords.longitude);
      },
      () => {},
      { enableHighAccuracy: true, maximumAge: 0, timeout: 5000 }
    );
  };

  const isRequireAddress = (requireAddress: boolean) => {
    return requireAddress ? (
      <div className="w-full">
        <div className="my-5 rounded-lg border-2 p-5">
          <div className="mb-3 flex justify-between">
            <p className="text-lg self-center">관리자님의 현재 위치</p>
            {agency ? (
              <p
                className="text-sm self-center"
                onClick={() => setRequireAddress(false)}
              >
                변경 취소
              </p>
            ) : (
              <></>
            )}
          </div>
          <TextInput
            placeholder={"장소를 입력해 주세요."}
            value={placeAddresName}
            defaultValue={placeAddresName}
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
            <Text className="m-5">관리자님의 시설이 여기 있을까요?</Text>
            <div className="flex mx-3 text-sm overflow-x-scroll text-nowrap">
              {[...placeMap.keys()].map((category: string, index) => (
                <div className="w-full" key={index}>
                  <ListTab
                    text={category}
                    index={index}
                    selectedIndex={selectedIndex}
                    onClick={() => {
                      setSelectedIndex(index);
                      setSelectedCategory(category);
                    }}
                  />
                </div>
              ))}
            </div>
            <hr className="main-tab-hr mx-3 border-[1px]" />
          </div>
        </div>
        <div className="flex flex-col w-full h-[40%] overflow-scroll border-2 border-t-0 rounded-t-none rounded-md mb-5">
          <ListGroup className="border-0 p-3 rounded-none">
            {placeMap?.get(selectedCategory)?.map((place: Place, index) => (
              <ListGroup.Item
                key={index}
                onClick={() =>
                  navigate("/agencyRegist", { state: place.title })
                }
              >
                {place.title}
              </ListGroup.Item>
            ))}
          </ListGroup>
        </div>
      </div>
    ) : (
      <div className="w-full flex flex-col">
        <div className="border-2 p-5 rounded-lg my-5">
          <div className="flex justify-between mb-3">
            <p>시설 위치</p>
            <p
              className="text-sm text-A706Grey2"
              onClick={() => setRequireAddress(true)}
            >
              변경
            </p>
          </div>
          <p>{agency?.address + ", " + agency?.name}</p>
        </div>
        <div>요청이 왔습니다.</div>
      </div>
    );
  };

  useEffect(() => {
    getCurrentPosition();
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
            console.log(
              item.address.parcel.substring(0, item.address.parcel.length)
            );
            const title = item.title;
            const category = item.category.split(">")[0].trim();
            if (map.has(category)) {
              const titles: Place[] = map.get(category)!;
              titles.push({ category, title });
              map.set(category, titles);
            } else {
              map.set(category, [{ category, title }]);
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
    getAcquisitions(
      { pageNo: 1 },
      ({ data }) => {
        setAcquisitionThumbnailList(data.result.boardList);
      },
      (error) => console.log(error)
    );
  }, []);

  useEffect(() => {
    console.log("index selected: " + selectedIndex);
  }, [selectedIndex]);

  return viewOptions ? (
    <div className="flex flex-col self-center w-[360px]">
      <ArrowBackOutlinedIcon
        className="my-5"
        onClick={() => setViewOptions(false)}
      />
      <div className="flex flex-col">
        {
          <>
            <CustomButton
              className="flex flex-col justify-around p-5 rounded-lg border-2 my-5"
              onClick={() => navigate(`/acquireRegist`)}
            >
              <>
                <div className="flex w-full">
                  <Inventory2OutlinedIcon
                    className="self-center rounded-md"
                    fontSize="large"
                  />
                  <Text className="w-full font-bold text-lg text-center self-center">
                    습득물 등록
                  </Text>
                  <NavigateNextOutlinedIcon className="self-center" />
                </div>
              </>
            </CustomButton>
            <CustomButton
              className="flex flex-col justify-around p-5 rounded-lg border-2 my-5"
              onClick={() => navigate(`/acquire`)}
            >
              <>
                <div className="flex w-full">
                  <ManageSearchOutlinedIcon
                    className="self-center rounded-md"
                    fontSize="large"
                  />
                  <Text className="w-full font-bold text-lg text-center self-center">
                    습득물 목록
                  </Text>
                  <NavigateNextOutlinedIcon className="self-center" />
                </div>
              </>
            </CustomButton>
            <CustomButton
              className="flex flex-col justify-around p-5 rounded-lg border-2 my-5"
              onClick={() => navigate(`/lostItemRegist`)}
            >
              <>
                <div className="flex w-full">
                  <ErrorOutlineIcon
                    className="self-center rounded-md"
                    fontSize="large"
                  />
                  <Text className="w-full font-bold text-lg text-center self-center">
                    분실물 등록
                  </Text>
                  <NavigateNextOutlinedIcon className="self-center" />
                </div>
              </>
            </CustomButton>
            <CustomButton
              className="flex flex-col justify-around p-5 rounded-lg border-2 my-5"
              onClick={() => navigate(`/losts`)}
            >
              <>
                <div className="flex w-full">
                  <SearchIcon
                    className="self-center rounded-md"
                    fontSize="large"
                  />
                  <Text className="w-full font-bold text-lg text-center self-center">
                    분실물 목록
                  </Text>
                  <NavigateNextOutlinedIcon className="self-center" />
                </div>
              </>
            </CustomButton>
          </>
        }
      </div>
    </div>
  ) : (
    <div className="flex flex-col self-center w-[360px]">
      <div className="flex flex-col mt-5">
        {member.role === "MANAGER" ? (
          agency ? (
            <>
              <CustomButton
                className="border-2 rounded-lg flex flex-col justify-around p-5 my-5"
                onClick={() => navigate(`/acquireRegist`)}
              >
                <>
                  <div className="flex w-full">
                    <Inventory2OutlinedIcon
                      className="self-center"
                      fontSize="large"
                    />
                    <Text className="font-bold text-xl ml-5 text-start self-center">
                      누군가 물건을 놓고 갔나요?
                    </Text>
                  </div>
                  <Text className="text-sm text-right w-full mt-10">
                    <>
                      빠르게 돌려주기
                      <KeyboardDoubleArrowRightIcon fontSize="small" />
                    </>
                  </Text>
                </>
              </CustomButton>
              <Text
                className="text-center text-A706CheryBlue text-sm"
                onClick={() => setViewOptions(true)}
              >
                더보기
              </Text>
            </>
          ) : (
            <Alert
              className="p-5 my-5"
              icon={HiInformationCircle}
              color="warning"
            >
              <p className="text-center">
                처음이시라면 시설등록을 진행해주세요!
              </p>
            </Alert>
          )
        ) : (
          <>
            <CustomButton
              className="border-2 rounded-lg flex flex-col justify-around p-5 my-5"
              onClick={() => navigate(`/acquire`)}
            >
              <>
                <div className="flex w-full">
                  <SearchIcon className="self-center" fontSize="large" />
                  <Text className="font-bold text-xl ml-5 text-start self-center">
                    소중한 물건을 잃어버리셨나요?
                  </Text>
                </div>
                <Text className="text-sm text-A706DarkGrey2 text-right w-full mt-10">
                  <>
                    찾아보기
                    <KeyboardDoubleArrowRightIcon fontSize="small" />
                  </>
                </Text>
              </>
            </CustomButton>
            <Text
              className="text-center text-A706CheryBlue text-sm"
              onClick={() => setViewOptions(true)}
            >
              더보기
            </Text>
          </>
        )}
      </div>
      <hr className="my-5"></hr>
      <div className="flex flex-row justify-around w-full">
        {member.role === "MANAGER" ? (
          isRequireAddress(requireAddress)
        ) : (
          <>
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
          </>
        )}
      </div>
      {member.role === "MANAGER" ? (
        <></>
      ) : (
        <>
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
              <Text className="text-center">1개의 습득물을 찾았습니다.</Text>
              <div className="flex flex-col rounded-lg border-2 m-3 py-3 px-3">
                <div className="mx-3 flex justify-between">
                  <div>
                    <span className="bg-A706Blue2 text-A706CheryBlue text-xs font-bold me-2 px-2.5 py-0.5 rounded dark:bg-blue-900 dark:text-blue-300">
                      카테고리:
                    </span>
                    <span className="bg-blue-900 text-A706Yellow text-xs font-bold me-2 px-2.5 py-0.5 rounded dark:bg-blue-900 dark:text-blue-300">
                      Lost112
                    </span>
                  </div>
                  <CloseOutlinedIcon className="self-end" fontSize="small" />
                </div>
                <div className="flex flex-row">
                  <img
                    alt="No Image"
                    className="size-[64px] m-3 rounded-lg"
                    src="images/wallet.jpg"
                  ></img>
                  <div className="text-start items-center m-5 w-full">
                    <h3>갈색 지갑</h3>
                  </div>
                  <p className="text-left text-xs my-5 mx-3">
                    습득일: 2024.03.26 16:05:01{" "}
                  </p>
                </div>
                <hr></hr>
                <p className="p-3 center">
                  찾고 계신 <Kbd>갈색 지갑</Kbd> 하고 아주 유사합니다.
                </p>
                <div className="mx-3 mt-1">
                  <Progress
                    theme={customTheme}
                    labelProgress
                    progress={90}
                    progressLabelPosition="inside"
                    color="A706CheryBlue"
                    size={"lg"}
                  />
                </div>
                {/* <div className="flex">
              <CustomButton className="w-full bg-A706CheryBlue rounded-lg text-white text-sm py-2 m-2">
                쪽지 보내기
              </CustomButton>
              <CustomButton className="w-full bg-A706CheryBlue rounded-lg text-white text-sm py-2 m-2">
                자세히 보기
              </CustomButton>
            </div> */}
              </div>
              <div className="flex justify-end">
                <Text>매칭된 습득물 전체 보기</Text>
                <NavigateNextOutlinedIcon />
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default Main;
