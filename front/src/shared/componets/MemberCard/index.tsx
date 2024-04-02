import { Text } from "@/shared";

type memberProps = {
  phoneNumber: string;
  registeredAt: string;
  isManager: boolean;
};

const MemberCard = ({ phoneNumber, registeredAt, isManager }: memberProps) => {
  return (
    <div className="flex flex-col rounded-lg shadow-sm bg-white dark:bg-A706Blue2 gap-[10px] p-[10px] cursor-pointer animate-fade">
      <Text className="text-[1rem] text-center font-bold w-full bg-[#2DB400] text-white rounded-md">
        네이버 회원
      </Text>
      <Text className="text-[1rem] font-bold">전화번호 : {phoneNumber}</Text>
      <Text className="text-[1rem] font-bold">가입일 : {registeredAt}</Text>
      <Text className="text-[1rem] font-bold">
        {isManager ? "시설관리자" : ""}
      </Text>
    </div>
  );
};

export default MemberCard;
