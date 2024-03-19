import { Text, usePasswordValidation, CustomButton } from "@/shared";
import { useState, useEffect } from "react";
import { TextInput } from "flowbite-react";
import { KakaoMap, cls } from "@/shared";
const AgencyRegist = () => {
  type postionType = {
    xPos: number;
    yPos: number;
  };
  const [address, setAddress] = useState<string>("");
  const [agencyName, setAgencyName] = useState<string>("");
  const [position, setPosition] = useState<postionType>();

  const openAddressModal = () => {
    new daum.Postcode({
      oncomplete: function (data: any) {
        setAddress(data.address);
      },
    }).open();
  };

  const handleAgencyRegist = () => {
    alert("등록하기");
    console.log(position, address, agencyName);
  };

  return (
    <div className="flex flex-col flex-1 justify-center itmes-center p-[40px]">
      <div className="flex flex-col items-center gap-[8px]">
        <Text className="text-center text-4xl">시설 관리자 등록</Text>
        <div className="flex flex-col w-[340px] gap-[10px]">
          <CustomButton
            className="text-xl border-2 bg-A706white border-A706DarkGrey1 rounded-md px-1 dark:border-A706LightGrey dark:text-A706LightGrey"
            onClick={openAddressModal}
          >
            시설 주소 검색
          </CustomButton>
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
              address && agencyName ? "" : "bg-A706Grey"
            )}
            onClick={() => handleAgencyRegist()}
            disabled={address && agencyName ? false : true}
          >
            시설 등록하기
          </CustomButton>
        </div>
      </div>
    </div>
  );
};

export default AgencyRegist;
