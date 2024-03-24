import { useState, useEffect, useCallback, useContext } from "react";
import { Link, useNavigate } from "react-router-dom";
import { CustomButton, StateContext, Text, cls } from "@/shared";
import { Label, TextInput } from "flowbite-react";
import { signIn } from "@/entities";
import { useMemberStore, usePhoneValidation } from "@/shared";

const Signin = () => {
  const { setMeta } = useContext(StateContext);

  useEffect(() => {
    setMeta(false);

    return () => {
      setMeta(true);
    };
  });
  const navigate = useNavigate();
  const [phoneNumber, setPhoneNumber] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const { setToken, setMember, setAuthenticate } = useMemberStore();
  const handleLogin = async (phoneNumber: string, password: string) => {
    if (!phoneNumber || !password) {
      return;
    }

    signIn(
      { phoneNumber, password },
      ({ data }) => {
        console.log(data.result.member);
        setToken({
          accessToken: data.result.accessToken,
          refreshToken: data.result.refreshToken,
        });
        setMember(data.result.member);
        setAuthenticate(true);
        navigate("/losts");
      },
      (error) => {
        console.error(error);
      }
    );
  };

  const handleKeyPress = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === "Enter") {
        handleLogin(phoneNumber, password);
      }
    },
    [phoneNumber, password]
  );

  useEffect(() => {
    window.addEventListener("keydown", handleKeyPress);
    return () => {
      window.removeEventListener("keydown", handleKeyPress);
    };
  }, [handleKeyPress]);

  return (
    <div className="flex flex-col w-full h-full justify-center items-center">
      <div className="flex flex-col w-full gap-[15px] items-center">
        <Text className="text-center text-4xl">로그인</Text>
        <div className="w-[340px]">
          <div className="mb-2 block">
            <Label htmlFor="phoneNumber" color="success" value="핸드폰 번호" />
          </div>
          <div className="flex w-full"></div>
          <TextInput
            id="phoneNumber"
            type="tel"
            placeholder="핸드폰 번호를 입력해주세요"
            required
            value={phoneNumber}
            onChange={(e) => setPhoneNumber(usePhoneValidation(e.target.value))}
          />
        </div>
        <div className="w-[340px]">
          <div className="mb-2 block">
            <Label htmlFor="password" color="success" value="비밀번호" />
          </div>
          <TextInput
            id="password"
            type="password"
            placeholder="비밀번호"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <div className="flex gap-[5px] items-center justify-center mt-[20px]">
          <Text className="faint text-[1rem] font-bold ">
            비밀번호를 잊으셨나요?
          </Text>
          <Text className="text-A706SubBlue text-[1.5rem] font-bold cursor-pointer">
            <Link to="/findpassword">비밀번호 찾기</Link>
          </Text>
        </div>
      </div>
      <div className="flex flex-col items-center mt-[40px]">
        <CustomButton
          className={cls(
            "menubtn mt-[20px]",
            phoneNumber && password ? "" : "bg-A706Grey"
          )}
          onClick={() => handleLogin(phoneNumber, password)}
          disabled={phoneNumber && password ? false : true}
        >
          로그인
        </CustomButton>
        <div className="flex gap-[5px] items-center justify-between w-full my-[10px]">
          <Text className="faint text-[1.5rem] ">계정이 없다면?</Text>
          <Text className="text-A706LightGrey dark:bg-A706DarkGrey2 text-[1.5rem] font-bold bg-A706CheryBlue cursor-pointer p-2 rounded-md">
            <Link to="/signup">회원가입</Link>
          </Text>
        </div>
        <div className="flex gap-[5px] items-center justify-between w-full ">
          <Text className="faint text-[1.5rem] ">가입전 둘러보세요</Text>
          <Text className="text-A706SlateGrey dark:text-A706Grey2 text-[1.5rem]  cursor-pointer rounded-md p-1">
            <Link to="/main">둘러보기</Link>
          </Text>
        </div>
      </div>
    </div>
  );
};

export default Signin;
