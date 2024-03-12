import { useState, useEffect } from "react";
import { Label, TextInput } from "flowbite-react";
import { CustomButton } from "@/shared";
import { HiMail } from "react-icons/hi";
import { Text } from "@/shared";
import { Link } from "react-router-dom";

const Signin = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  useEffect(() => {
    console.log(email, password);
  }, [email, password]);
  return (
    <div className="flex flex-col flex-1 justify-center px-[10px]">
      <div className="flex flex-col min-w-[340px] mx-auto gap-[15px]">
        <div className="w-md ">
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
        <div className="max-w-md">
          <div className="mb-2 block">
            <Label htmlFor="password" color="success" value="비밀번호" />
          </div>
          <TextInput
            id="password"
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
      <div className="flex flex-col mt-[40px]">
        <CustomButton className="menubtn mt-[20px]">로그인</CustomButton>
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
