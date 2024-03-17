import { useState } from "react";
import { Link } from "react-router-dom";
import { CustomButton, Text } from "@/shared";
import { Label, TextInput } from "flowbite-react";
import { HiMail } from "react-icons/hi";

const Signin = () => {
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");

  const handleLogin = (email: string, password: string) => {
    console.log(Object.entries({ email, password }));
  };

  return (
    <div className="flex flex-col w-full h-full justify-center items-center">
      <div className="flex flex-col w-full gap-[15px] items-center">
        <Text className="text-center text-4xl">로그인</Text>
        <div className="w-[340px]">
          <div className="mb-2 block">
            <Label htmlFor="email" color="success" value="이메일" />
          </div>
          <div className="flex w-full"></div>
          <TextInput
            id="email"
            type="email"
            icon={HiMail}
            placeholder="FindHere@findear.com"
            required
            onChange={(e) => setEmail(e.target.value)}
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
          className="menubtn mt-[20px]"
          onClick={() => handleLogin(email, password)}
        >
          로그인
        </CustomButton>
        <div className="flex gap-[5px] items-center justify-center mt-[20px]">
          <Text className="text-[2rem] ">계정이 없다면?</Text>
          <Text className="text-A706CheryBlue text-[2rem] font-bold cursor-pointer">
            <Link to="/main">둘러보기</Link>
          </Text>
        </div>
      </div>
    </div>
  );
};

export default Signin;
