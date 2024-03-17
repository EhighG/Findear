import { Text } from "@/shared";
import { Carousel } from "flowbite-react";
import { boardImage1, boardImage2, boardImage3, CustomButton } from "@/shared";
import { Link } from "react-router-dom";
const Boarding = () => {
  return (
    <div className="flex flex-col w-full h-full justify-center items-center">
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
      <CustomButton
        className="menubtn my-[20px]"
        onClick={() => {
          window.location.href = "/signup";
        }}
      >
        시작하기
      </CustomButton>
      <div className="flex gap-[5px] items-center justify-center">
        <Text className="faint text-[1.5rem]">이미 계정이 있나요?</Text>
        <Text className="text-A706CheryBlue text-[2rem] font-bold cursor-pointer">
          <Link to="/signin">로그인</Link>
        </Text>
      </div>
      <div className="flex gap-[5px] items-center justify-center">
        <Text className="faint text-[1.5rem] ">계정이 없다면?</Text>
        <Text className="text-A706Grey2 text-[2rem] font-bold cursor-pointer">
          <Link to="/main">둘러보기</Link>
        </Text>
      </div>
    </div>
  );
};

export default Boarding;
