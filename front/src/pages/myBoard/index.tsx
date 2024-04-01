import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";

const MyBoard = () => {
  const [boardType, setBoardType] = useState<"습득물" | "분실물">("분실물");
  const { state } = useLocation();

  useEffect(() => {
    if (state) {
      setBoardType(state.boardType);
    }
    console.log(boardType);
  }, [state]);
  useEffect(() => {
    // 사용자가 선택한 게시판 타입에 따라
    // 사용자가 작성한 게시판 데이터를 가져온다.
  }, []);

  return <div>index</div>;
};

export default MyBoard;
