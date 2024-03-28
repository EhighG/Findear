import { StateContext } from "@/shared";
import { useContext, useEffect } from "react";

const MyPage = () => {
  const { setHeaderTitle } = useContext(StateContext);
  useEffect(() => {
    setHeaderTitle("마이페이지");
    return () => {
      setHeaderTitle("");
    };
  }, []);
  return <div className="flex flex-col flex-1">마이페이지</div>;
};

export default MyPage;
