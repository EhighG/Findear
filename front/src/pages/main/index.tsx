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
import { getCommercialInfo } from "@/entities";
import { ListGroup, TextInput } from "flowbite-react";

type AcquisitionThumbnail = {
  acquiredAt: string;
  agency: { id: number; name: string; address: string };
  boardId: number;
  category: string;
  productName: string;
  thumbnailUrl: string;
  writer: { memberId: number; phoneNumber: string; role: string };
};

const geocoder = new kakao.maps.services.Geocoder();

const Main = () => {
  const { member } = useMemberStore();
  const navigate = useNavigate();
  const [selectedIndex, setSelectedIndex] = useState<number>(0);
  const [selectedBuilding, setSelectedBuilding] = useState<string>("");
  const [viewOptions, setViewOptions] = useState<boolean>(false);
  const [acquisitionThumbnailList, setAcquisitionThumbnailList] = useState<
    AcquisitionThumbnail[]
  >([]);

  const [latitude, setLatitude] = useState<number>(0);
  const [longitude, setLongitude] = useState<number>(0);

  const [mainAddress, setMainAddress] = useState<string>("");
  const [subAddress, setSubAddress] = useState<string>("");
  const [code, setCode] = useState<string>("");
  const [bjCode, setBjCode] = useState<string>("");
  const [addressName, setAddressName] = useState<string>("");
  const [, setRoadAddressName] = useState<string>("");
  const [, setBuildingName] = useState<string>("");

  const [commerceMap, setCommerceMap] = useState<Map<string, string[]>>(
    new Map<string, string[]>()
  );

  // const [commerceList] = useState<Map<string, string[]>>(
  //   new Map([
  //     ["멀티캠퍼스", ["SSAFY", "웰스토리"]],
  //     ["바나프레소", ["바나프레소", "GS25", "CU"]],
  //     [
  //       "63빌딩",
  //       ["한화", "아쿠아리움", "코엑스", "국회의사당", "롯데타워", "삼성"],
  //     ],
  //   ])
  // );

  const getCurrentPosition = () => {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLatitude(position.coords.latitude);
        setLongitude(position.coords.longitude);
        // getCommercialInfoByRadius(
        //   position.coords.accuracy.toString(),
        //   longitude.toString(),
        //   latitude.toString(),
        //   ({ data }) => {
        //     setCommerceListRadius(data.body ? data.body.items : []);
        //   },
        //   (error) => console.log(error)
        // );
      },
      () => {},
      { enableHighAccuracy: true, maximumAge: 0, timeout: 5000 }
    );
  };

  useEffect(() => {
    getCurrentPosition();
  }, []);

  useEffect(() => {
    if (latitude && longitude) {
      geocoder.coord2Address(longitude, latitude, (result: any) => {
        setAddressName(result[0].address.address_name);
        if (result[0].road_address) {
          setRoadAddressName(result[0].road_address.address_name);
          setBuildingName(result[0].road_address.building_name);
        }
        setMainAddress(
          result[0].address.main_address_no.length === 0
            ? "0000"
            : result[0].address.main_address_no
        );
        setSubAddress(
          result[0].address.sub_address_no.length === 0
            ? "0000"
            : result[0].address.sub_address_no
        );
      });
      geocoder.coord2RegionCode(longitude, latitude, (result: any) => {
        setCode(result[0].code);
      });
    }
  }, [latitude, longitude]);

  useEffect(() => {
    if (mainAddress && subAddress && code) {
      setBjCode(
        code + "1" + mainAddress.padStart(4, "0") + subAddress.padStart(4, "0")
      );
    }
  }, [mainAddress, subAddress, code]);

  useEffect(() => {
    if (bjCode) {
      getCommercialInfo(
        bjCode,
        ({ data }) => {
          setCommerceMap(() => {
            const map = new Map<string, string[]>();
            data.body?.items.forEach((item: any) => {
              if (map.has(item.bldNm)) {
                let buildings: string[] = map.get(item.bldNm)!;
                buildings.push(item.bizesNm);
                map.set(item.bldNm, buildings);
              } else {
                map.set(item.bldNm, [item.bizesNm]);
              }
            });
            console.log(map);
            setSelectedBuilding(map.keys().next().value);
            return map;
          });
        },
        (error) => console.log(error)
      );
    }
  }, [bjCode]);

  const renderOptionsButton = () => {
    return (
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
              <SearchIcon className="self-center rounded-md" fontSize="large" />
              <Text className="w-full font-bold text-lg text-center self-center">
                분실물 목록
              </Text>
              <NavigateNextOutlinedIcon className="self-center" />
            </div>
          </>
        </CustomButton>
      </>
    );
  };

  const renderMainButton = () => {
    return member.role !== "MANAGER" ? (
      <CustomButton
        className=" border-2 rounded-lg flex flex-col justify-around p-5 my-5"
        onClick={() => navigate(`/acquireRegist`)}
      >
        <>
          <div className="flex w-full">
            <Inventory2OutlinedIcon className="self-center" fontSize="large" />
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
    ) : (
      <CustomButton
        className="bg-A706Blue2 rounded-3xl flex flex-col justify-around p-5 my-5"
        onClick={() => navigate(`/acquire`)}
      >
        <>
          <div className="flex w-full">
            <SearchIcon className="self-center" fontSize="large" />
            <Text className="font-bold text-xl text-A706DarkGrey2 ml-5 text-start">
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
    );
  };

  const renderTabItem = () => {
    if (selectedIndex == 0) {
      return (
        <div className="py-5">
          {acquisitionThumbnailList.map(
            (acquisitionThumbnail: AcquisitionThumbnail, index) => (
              <div
                className="flex flex-col rounded-lg border-2 m-3 py-3 px-3"
                key={index}
              >
                <div className="mx-3 flex justify-between">
                  <span className="bg-A706Blue2 text-A706CheryBlue text-xs font-bold me-2 px-2.5 py-0.5 rounded dark:bg-blue-900 dark:text-blue-300">
                    카테고리: {acquisitionThumbnail.category}
                  </span>
                  <CloseOutlinedIcon className="self-end" fontSize="small" />
                </div>
                <div className="flex flex-row">
                  <img
                    className="size-[64px] m-3 rounded-lg"
                    src={acquisitionThumbnail.thumbnailUrl}
                  />
                  <div className="w-full">
                    <div className="text-start items-center m-3">
                      <h3>{acquisitionThumbnail.productName}</h3>
                    </div>
                    <p className="text-end text-xs m-3 ">
                      습득일: {acquisitionThumbnail.acquiredAt}
                    </p>
                  </div>
                </div>
                {/* <CustomButton
                  className="bg-A706CheryBlue rounded-lg text-white text-sm py-2 m-2"
                  onClick={() =>
                    navigate(`/foundItemDetail/${acquisitionThumbnail.boardId}`)
                  }
                >
                  자세히 보기
                </CustomButton> */}
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
      );
    } else {
      return (
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
                className="size-[64px] m-3 rounded-lg"
                src="images/wallet.jpg"
              ></img>
              <div className="text-start items-center m-5 w-full">
                <h3>갈색 지갑</h3>
                <Text className="text-xs">'갈색 지갑' 과 매칭</Text>
              </div>
              <p className="text-left text-xs my-5 mx-3">
                습득일: 2024.03.26 16:05:01{" "}
              </p>
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
      );
    }
  };

  const renderOptions = (state: boolean) => {
    setViewOptions(state);
  };

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
        onClick={() => renderOptions(false)}
      />
      <div className="flex flex-col">{renderOptionsButton()}</div>
    </div>
  ) : (
    <div className="flex flex-col self-center w-[360px]">
      <div className="flex flex-col my-5">
        {renderMainButton()}
        <Text
          className="text-center text-A706CheryBlue text-sm"
          onClick={() => {
            renderOptions(true);
          }}
        >
          더보기
        </Text>
      </div>
      <hr className="my-5"></hr>
      <div className="flex flex-row justify-around w-full">
        {member.role !== "MANAGER" ? (
          <>
            <div className="w-full">
              <div className="mb-5">
                <p className="mb-3 text-lg">관리자님의 현재 위치</p>
                <TextInput defaultValue={addressName} />
                {/* <TextInput defaultValue={roadAddressName} />
              <TextInput defaultValue={buildingName} /> */}
              </div>
              <div className="flex flex-col w-full overflow-hidden border-2 border-b-0 rounded-b-none rounded-md">
                <div className="sticky">
                  <Text className="m-5">관리자님의 시설이 여기 있을까요?</Text>
                  <div className="mx-3 text-sm">
                    {[...commerceMap.keys()].map((building: string, index) => (
                      <ListTab
                        key={index}
                        text={building}
                        index={index}
                        selectedIndex={selectedIndex}
                        onClick={() => {
                          setSelectedIndex(index);
                          setSelectedBuilding(building);
                        }}
                      />
                    ))}
                  </div>
                  <hr className="main-tab-hr mx-3 border-[1px]" />
                </div>
              </div>
              <div className="flex flex-col w-full h-[40%] overflow-scroll border-2 border-t-0 rounded-t-none rounded-md mb-5">
                <ListGroup className="border-0 p-3 rounded-none">
                  {commerceMap
                    ?.get(selectedBuilding)
                    ?.map((commerceName: string, index) => (
                      <ListGroup.Item key={index}>
                        {commerceName}
                      </ListGroup.Item>
                    ))}
                </ListGroup>
              </div>
            </div>
          </>
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
      {member.role !== "MANAGER" ? (
        <></>
      ) : (
        <>
          <hr className="main-tab-hr" />
          {renderTabItem()}
        </>
      )}
    </div>
  );
};

export default Main;
