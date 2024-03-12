import { useState, useEffect } from "react";
import { Label, TextInput, Checkbox, Button } from "flowbite-react";
import { HiMail } from "react-icons/hi";
import { IoIosSend } from "react-icons/io";
import { useDebounce, useEmailValidation, Text } from "@/shared";

const Singup = () => {
  const [email, setEmail] = useState("");
  const [emailValidate, setEmailValidate] = useState(false); //이메일 중복 체크 [true, false, null
  const [password, setPassword] = useState("");
  const [password2, setPassword2] = useState("");
  const [username, setUsername] = useState("");
  const [tel, setTel] = useState("");
  const [manager, setManager] = useState(false);
  const debouncedEmail = useDebounce(email, 700);

  useEffect(() => {
    if (debouncedEmail.length > 0 && useEmailValidation(debouncedEmail)) {
      //TODO: 이메일 중복 체크 API 호출 검증
      setEmailValidate(true);
      console.log(debouncedEmail);
      // 가입된 이메일이 없다면 setEmailValidate(true);
    }
  }, [debouncedEmail]);

  useEffect(() => {
    if (password.length > 0 && password2.length > 0) {
      if (password === password2) {
        console.log("비밀번호 일치");
      } else {
        console.log("비밀번호 불일치");
      }
    }
  }, [password, password2]);

  useEffect(() => {
    console.log(username, tel, manager);
  }, [username, manager, tel]);

  return (
    <div className="flex flex-col  w-full ">
      <div className="flex flex-col px-[2%] gap-[8px] ">
        <div className="max-w-md ">
          <div className="mb-2 block">
            <Label htmlFor="email" color="success" value="이메일" />
          </div>
          <div className="flex w-full"></div>
          <TextInput
            id="email"
            type="email"
            icon={HiMail}
            // rightIcon={true ? IoIosSend : ""}
            placeholder="FindHere@findear.com"
            required
            color={
              email.length > 0
                ? emailValidate
                  ? "success"
                  : "failure"
                : "gray"
            }
            onChange={(e) => setEmail(e.target.value)}
            helperText={
              email.length > 0 ? (
                emailValidate ? (
                  <>
                    <span className="flex items-center  gap-[5px] font-medium">
                      사용가능 이메일, 인증하기를 눌러주세요
                      {<IoIosSend onClick={() => alert("까꿍")} />}
                    </span>
                  </>
                ) : (
                  <>
                    <span className="font-medium">
                      사용불가능한 이메일 입니다
                    </span>
                  </>
                )
              ) : (
                ""
              )
            }
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
            //   color="success"
          />
        </div>
        <div className="max-w-md">
          <div className="mb-2 block">
            <Label htmlFor="password2" color="success" value="비밀번호 확인" />
          </div>
          <TextInput
            id="password2"
            placeholder="비밀번호를 한번 더 입력해주세요"
            required
            onChange={(e) => setPassword2(e.target.value)}
            color={true ? "failure" : "success"}
            helperText={
              true ? (
                <>
                  <span className="font-medium">
                    사용불가능한 비밀번호 입니다
                  </span>
                </>
              ) : (
                <>
                  <span className="font-medium">
                    사용가능한 비밀번호 입니다
                  </span>
                </>
              )
            }
          />
        </div>
        <div className="max-w-md">
          <div className="mb-2 block">
            <Label htmlFor="username" color="success" value="닉네임" />
          </div>
          <TextInput
            id="username"
            placeholder="닉네임을 입력해주세요"
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>
        <div className="max-w-md">
          <div className="mb-2 block">
            <Label
              htmlFor="tel"
              color="success"
              value="전화번호를 입력해주세요"
            />
          </div>
          <TextInput
            id="tel"
            type="tel"
            placeholder="010-1234-5678"
            required
            onChange={(e) => setTel(e.target.value)}

            //   color="success"
          />
        </div>
        <div className="flex flex-col">
          <div className="flex items-center ml-[5%] gap-2">
            <Checkbox
              id="manager"
              name="group"
              value="manager"
              onChange={(e) => setManager(e.target.checked)}
            />
            <Label htmlFor="manager">사장님이나 시설 관리자 이신가요?</Label>
          </div>
          {true ? <Text>관리자인 경우 랜더링</Text> : ""}
        </div>
        <div className="flex w-full">
          <Button className="w-full" disabled>
            로그인
          </Button>
        </div>
        <div className="flex">
          <Text>이미 계정이 있나요? 로그인</Text>
        </div>
      </div>
    </div>
  );
};

export default Singup;
