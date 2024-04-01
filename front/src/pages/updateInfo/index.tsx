import { getUserDetail } from "@/entities";
import { MemberCard, Text, useMemberStore } from "@/shared";
import { useEffect } from "react";

const UpdateInfo = () => {
  const { member } = useMemberStore();
  useEffect(() => {
    getUserDetail(
      member.memberId,
      ({ data }) => {
        console.log(data);
      },
      (error) => {
        console.log(error);
      }
    );
  }, []);
  return (
    <div className="flex flex-col flex-1 bg-gradient-to-b from-A706DarkGrey2 to-A706DarkGrey1 opacity-90 shadow-sm p-[10px] gap-[20px]">
      <MemberCard />
      <div className="flex flex-col rounded-lg shadow-sm bg-white dark:bg-A706Blue2 gap-[10px] p-[10px] cursor-pointer animate-fade">
        <Text className="text-[1rem] text-center font-bold w-full bg-A706CheryBlue text-white rounded-md">
          시설 정보
        </Text>
        <Text className="text-[1rem] font-bold">시설 명 :</Text>
        <Text className="text-[1rem] font-bold">시설주소 :</Text>
        <Text className="text-[1rem] font-bold">지도</Text>
      </div>
    </div>
  );
};

export default UpdateInfo;
