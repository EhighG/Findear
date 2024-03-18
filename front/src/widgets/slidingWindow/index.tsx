import { CustomButton, Text } from "@/shared";
import Dropdown from "../dropdown";

const SlidingWindow = () => {
  const name = "인계 대상자 이름";
  const contact = "인계 대상자 연락처";
  const email = "인계 대상자 이메일";
  const authenticationState = "READY";
  const isRegisteredUser = true;

  const checkAuthenticationState = (
    authenticationState: string,
    isRegisteredUser: boolean
  ) => {
    switch (authenticationState) {
      case "READY":
        return (
          <CustomButton className="rounded-md w-[320px] h-[60px] bg-black text-white">
            <p>인증 요청하기</p>
          </CustomButton>
        );
      case "AUTHENTICATIONING":
        return (
          <CustomButton className="rounded-md w-[320px] h-[60px] bg-A706Yellow text-black">
            <p>인증 중</p>
          </CustomButton>
        );
      case "AUTHENTICATED":
        if (isRegisteredUser) {
          return (
            <CustomButton className="rounded-md w-[320px] h-[60px] bg-A706CheryBlue text-white">
              <p>인계 하기</p>
            </CustomButton>
          );
        } else {
          return (
            <CustomButton className="rounded-md w-[320px] h-[60px] bg-A706CheryBlue text-white">
              <p>비회원 인계 하기</p>
            </CustomButton>
          );
        }
      case "RETURNED":
        if (isRegisteredUser) {
          return (
            <CustomButton className="rounded-md w-[320px] h-[60px] bg-A706Green text-white">
              <p>인계 완료</p>
            </CustomButton>
          );
        } else {
          return (
            <CustomButton className="rounded-md w-[320px] h-[60px] bg-A706Green text-white">
              <p>비회원 인계 완료</p>
            </CustomButton>
          );
        }
    }
  };

  return (
    <>
      <Dropdown></Dropdown>
    </>
  );
};

export default SlidingWindow;
