import { Text, CustomButton, useDebounce } from "@/shared";
import { useEffect, useState } from "react";
import { Label, TextInput } from "flowbite-react";
import { KakaoMap, cls } from "@/shared";
import { useMemberStore, useSearchMap } from "@/shared";
import { agencyReigst } from "@/entities";
import { Helmet } from "react-helmet-async";
import { useNavigate } from "react-router-dom";

const AgencyRegist = () => {
  const navigate = useNavigate();
  type postionType = {
    xPos: number;
    yPos: number;
  };

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

  const [title, setTitle] = useState<string>("");
  const [address, setAddress] = useState<string>("");
  const [list, setList] = useState<dataType[]>([]);
  const [agencyName, setAgencyName] = useState<string>("");
  const [position, setPosition] = useState<postionType>();
  const { getMember, setAgency, setMember } = useMemberStore();

  const debouncedTitle = useDebounce(title, 500);

  const fetchData = async () => {
    const searchData = await useSearchMap(debouncedTitle);
    if (searchData) setList(searchData);
  };
  useEffect(() => {
    if (debouncedTitle) {
      fetchData();
    }
  }, [debouncedTitle]);

  // const openAddressModal = () => {
  //   new daum.Postcode({
  //     oncomplete: function (data: any) {
  //       setAddress(data.address);
  //     },
  //   }).open();
  // };

  const handleAgencyRegist = () => {
    if (!position || !title || !agencyName) return;
    console.log(agencyName, address, position.xPos, position.yPos);
    agencyReigst(
      getMember().memberId,
      {
        name: agencyName,
        address: address,
        xpos: position.xPos,
        ypos: position.yPos,
      },
      ({ data }) => {
        console.log(data);
        setAgency(data.result.agency);
        setMember({
          memberId: data.result.memberId,
          role: data.result.role,
          phoneNumber: data.result.phoneNumber,
        });
        alert("등록이 완료되었습니다");
        navigate("/main");
      },
      (error) => {
        console.error(error);
      }
    );
  };

  return (
    <div className="flex flex-col flex-1 justify-center itmes-center p-[40px]">
      <Helmet>
        <title>관리자 등록</title>
        <meta name="description" content="파인디어 시설 관리자 등록 페이지" />
        <meta name="keywords" content="Findear, 시설, 관리자, Manager, 등록" />
      </Helmet>
      <div className="flex flex-col items-center gap-[8px]">
        <Text className="text-center text-4xl">시설 관리자 등록</Text>
        <div className="flex flex-col w-[340px] gap-[10px]">
          {/* <CustomButton
            className="text-xl border-2 bg-A706white border-A706DarkGrey1 rounded-md px-1 dark:border-A706LightGrey dark:text-A706LightGrey"
            onClick={openAddressModal}
          >
            시설명 검색
          </CustomButton> */}
          <form className="flex flex-col">
            <Label htmlFor="address" className="text-[1rem] font-bold">
              시설명 검색
              <div>
                <TextInput
                  id="address"
                  placeholder="시설명을 검색해주세요 ex) 멀티캠퍼스 역삼"
                  value={title}
                  onChange={(e) => {
                    setTitle(e.target.value);
                    setAddress("");
                  }}
                  required
                  helperText="시설명을 입력하시면 상위 15개 시설 목록을 조회 합니다"
                />
              </div>
            </Label>
          </form>
          {!address && (
            <div className="flex flex-col">
              {list.map((item, i) => {
                return (
                  <div
                    key={i}
                    className="border border-A706Grey rounded-lg h-[60px] p-2 cursor-pointer"
                    onClick={() => {
                      setAddress(item.address_name);
                      setAgencyName(item.place_name);
                      setPosition({
                        xPos: Number(item.x),
                        yPos: Number(item.y),
                      });
                    }}
                  >
                    <Text className="font-bold text-[1rem]">
                      {item.place_name}
                    </Text>
                    <Text>{item.address_name}</Text>
                  </div>
                );
              })}
            </div>
          )}
          {address ? (
            <>
              <TextInput
                id="address"
                placeholder="사용자 주소"
                readOnly
                value={address}
                required
              />
              <TextInput
                id="agencyName"
                placeholder="시설명을 입력해주세요"
                value={agencyName}
                readOnly
                onChange={(e) => setAgencyName(e.target.value)}
                required
              />
              <KakaoMap
                className="size-[340px]"
                keyword={address}
                setPosition={setPosition}
              />
            </>
          ) : (
            ""
          )}
          <CustomButton
            className={cls(
              "menubtn mt-[20px]",
              title && agencyName && position ? "" : "bg-A706Grey"
            )}
            onClick={() => handleAgencyRegist()}
            disabled={title && agencyName ? false : true}
          >
            시설 등록하기
          </CustomButton>
        </div>
      </div>
    </div>
  );
};

export default AgencyRegist;
