import { useContext, useEffect } from "react";
import { Helmet } from "react-helmet-async";
import { Carousel } from "flowbite-react";
import {
  boardImage1,
  boardImage2,
  boardImage3,
  CustomButton,
  StateContext,
  Text,
  naver_login,
} from "@/shared";

const Boarding = () => {
  const { setMeta } = useContext(StateContext);

  useEffect(() => {
    setMeta(false);

    return () => {
      setMeta(true);
    };
  });
  return (
    <div className="flex flex-col w-full h-full justify-center items-center">
      <Helmet>
        <title>파인디어 온보딩 페이지</title>
        <meta name="description" content="파인디어 온보딩 페이지" />
        <meta name="keywords" content="Findear, onboarding, boarding" />
      </Helmet>
      <div className="flex flex-col mx-auto gap-[15px] text-center">
        <Carousel>
          <>
            <div>
              <Text className="slogan">물건을 분실 하셨나요?</Text>
              <Text className="slogan">Findear에서 찾아보세요!</Text>
            </div>
            <img
              src={boardImage1}
              alt="LostCard"
              className="mx-auto size-[340px]"
            />
          </>
          <>
            <div>
              <Text className="slogan">습득물을 보관중이신가요?</Text>
              <Text className="slogan">Findear에서 관리하세요!</Text>
            </div>
            <img
              src={boardImage2}
              alt="Saving"
              className="mx-auto size-[340px]"
            />
          </>
          <>
            <div>
              <Text className="slogan">Find your dear</Text>
              <Text className="slogan">당신의 홈즈, 파인디어</Text>
            </div>
            <img
              src={boardImage3}
              alt="Findear"
              className="mx-auto size-[340px]"
            />
          </>
        </Carousel>
      </div>
      <div className="flex flex-col items-center mt-[40px]">
        <CustomButton
          className="w-[340px] "
          onClick={() => {
            window.location.href = `${
              import.meta.env.VITE_NAVER_LOGIN
            }?redirect_uri=${
              import.meta.env.VITE_REDIRECT_URI
            }&state=test&response_type=code&client_id=${
              import.meta.env.VITE_CLIENT_ID
            }&client_secret=${
              import.meta.env.VITE_CLIENT_SECRET
            }&response_type=token`;
          }}
        >
          <img
            src={naver_login}
            alt="naver_login"
            className="size-full object-fill"
          />
        </CustomButton>
        {/* <div className="flex gap-[5px] items-center justify-around w-full ">
          <Text className="faint text-[1.5rem] ">가입전 둘러보세요</Text>
          <Text className="text-A706SlateGrey dark:text-A706Grey2 text-[1.5rem]  cursor-pointer rounded-md p-1">
            <Link to="/main">둘러보기</Link>
          </Text>
        </div> */}
        {/* <div className="flex gap-[5px] items-center justify-between w-full my-[10px]">
          <Text className="faint text-[1.5rem] ">이미 계정이 있나요?</Text>
          <Text className="text-A706LightGrey dark:bg-A706DarkGrey2 text-[1.5rem] font-bold bg-A706CheryBlue cursor-pointer p-2 rounded-md">
            <Link to="/signin">로그인</Link>
          </Text>
        </div> */}
      </div>
    </div>
  );
};

export default Boarding;
