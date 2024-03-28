import { ListCard, StateContext } from "@/shared";
import { useContext, useEffect } from "react";
import { Helmet } from "react-helmet-async";

const Letter = () => {
  const { setHeaderTitle } = useContext(StateContext);
  useEffect(() => {
    setHeaderTitle("쪽지");
    return () => {
      setHeaderTitle("");
    };
  }, []);
  return (
    <div className="flex flex-col flex-1">
      <Helmet>
        <title>쪽지 페이지</title>
        <meta name="description" content="파인디어 쪽지 페이지" />
        <meta name="keywords" content="Findear, 파인디어, 쪽지" />
      </Helmet>
      <ListCard>쪽지 리스트 목록</ListCard>
    </div>
  );
};

export default Letter;
