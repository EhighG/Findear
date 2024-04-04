import { useLocation } from "react-router-dom";
import { Text } from "@/shared";
import { Helmet } from "react-helmet-async";
const IntroduceDetail = () => {
  const { state } = useLocation();
  return (
    <div>
      <Helmet>
        <title>상세 소개</title>
        <meta name="description" content="파인디어 팁 상세 페이지" />
        <meta
          name="keywords"
          content="Findear, 파인디어, 팁, 소개, 상세, 안내, Detail, Tip"
        />
      </Helmet>
      <div>
        <Text className="text-[2rem] font-bold">제목 영역</Text>
      </div>
      <div>{state ? state : "소개 페이지입니다."}</div>
    </div>
  );
};

export default IntroduceDetail;
