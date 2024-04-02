import { Agency, getUserDetail } from "@/entities";
import {
  CustomButton,
  KakaoMap,
  MemberCard,
  Text,
  useMemberStore,
} from "@/shared";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Swal from "sweetalert2";

type MemberProps = {
  phoneNumber: string;
  joinedAt: string;
  role: string;
  memberId: number;
  agency: Agency;
};

const CheckInfo = () => {
  const navigate = useNavigate();
  const { member } = useMemberStore();
  const [memberData, setMemberData] = useState<MemberProps>();
  useEffect(() => {
    getUserDetail(
      member.memberId,
      ({ data }) => {
        setMemberData(data.result);
      },
      (error) => {
        console.log(error);
      }
    );
  }, []);
  return (
    <div className="flex flex-col flex-1 bg-gradient-to-b from-A706DarkGrey2 to-A706DarkGrey1 opacity-90 shadow-sm p-[10px] gap-[20px] dark:text-A706Dark">
      <MemberCard
        phoneNumber={memberData?.phoneNumber ?? "번호 정보 없음"}
        isManager={memberData?.role === "MANAGER" ? true : false}
        registeredAt={memberData?.joinedAt ?? "가입일 정보 없음"}
      />
      <div className="flex flex-col rounded-lg shadow-sm bg-white dark:bg-A706Blue2 gap-[10px] p-[10px] cursor-pointer animate-fade">
        <Text className="text-[1rem] text-center font-bold w-full bg-A706CheryBlue text-white rounded-md">
          시설 정보
        </Text>
        <Text className="text-[1rem] font-bold">
          시설 명 : {memberData?.agency.name ?? "시설 정보 없음"}
        </Text>
        <Text className="text-[1rem] font-bold">
          시설주소 : {memberData?.agency.address ?? "주소 없음"}
        </Text>
      </div>
      <div className="flex flex-col items-center rounded-lg shadow-sm bg-white dark:bg-A706Blue2 gap-[10px] p-[10px] cursor-pointer animate-fade">
        <KakaoMap
          className="size-[320px]"
          keyword={memberData?.agency.address}
        />
      </div>
      <CustomButton
        className="menubtn my-[10px] mx-auto"
        onClick={() => {
          Swal.fire({
            title: "정보 변경",
            text: "현재 정보 변경은 시설 정보만 가능합니다, 변경하시겠습니까?",
            icon: "question",
            showCancelButton: true,
          }).then((result) => {
            if (result.isConfirmed) {
              navigate("/updateInfo");
            }
          });
        }}
      >
        정보 변경
      </CustomButton>
    </div>
  );
};

export default CheckInfo;
