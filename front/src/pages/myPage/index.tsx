import { signOut } from "@/entities";
import { CustomButton, StateContext } from "@/shared";
import { useContext, useEffect } from "react";

const MyPage = () => {
  const { setHeaderTitle } = useContext(StateContext);
  useEffect(() => {
    setHeaderTitle("마이페이지");
    return () => {
      setHeaderTitle("");
    };
  }, []);
  return (
    <div className="flex flex-col flex-1 items-center justify-center">
      <CustomButton
        className="menubtn"
        onClick={() => {
          signOut(
            () => {
              console.info("로그아웃 성공");
            },
            () => {
              console.error("로그아웃 실패");
            }
          );
        }}
      >
        로그아웃
      </CustomButton>
    </div>
  );
};

export default MyPage;
