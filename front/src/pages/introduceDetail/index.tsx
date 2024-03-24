import { useLocation } from "react-router-dom";
import { Text } from "@/shared";
const IntroduceDetail = () => {
  const { state } = useLocation();
  return (
    <div>
      <div>
        <Text className="text-[2rem] font-bold">제목 영역</Text>
      </div>
      <div>{state ? state : "소개 페이지입니다."}</div>
    </div>
  );
};

export default IntroduceDetail;
