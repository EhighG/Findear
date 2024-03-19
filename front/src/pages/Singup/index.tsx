import { Text, usePasswordValidation, CustomButton } from "@/shared";
import { useState, useEffect } from "react";
import { Label, TextInput } from "flowbite-react";
import { Link } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";

declare global {
  interface Window {
    kakao: any;
    daum: any;
  }
  const kakao: any;
  const daum: any;
}

const Singup = () => {
  const [phoneNumber, setPhoneNumber] = useState<string>("");
  const [password, setPassword] = useState("");
  const [password2, setPassword2] = useState("");
  const [passwordProblem, setPasswordProblem] = useState(false);
  const [passwordSame, setPasswordSame] = useState(false);

  // 비밀번호 검증
  useEffect(() => {
    if (password.length >= 8 && usePasswordValidation(password)) {
      setPasswordProblem(false);
      return;
    }
    setPasswordProblem(true);
  }, [password]);

  // 비밀번호 일치여부
  useEffect(() => {
    if (
      password.length >= 8 &&
      password2.length >= 8 &&
      password === password2
    ) {
      setPasswordSame(true);
      return;
    }
    setPasswordSame(false);
  }, [password, password2]);

  return (
    <div className="flex flex-col flex-1 justify-center itmes-center p-[40px]">
      <div className="flex flex-col items-center gap-[8px]">
        <Text className="text-center text-4xl">회원가입</Text>
        <div className="w-[340px]">
          <div className="mb-2 block">
            <Label htmlFor="phoneNumber" color="success" value="전화번호" />
          </div>
          <div className="flex flex-col">
            <TextInput
              className="w-[340px]"
              id="phoneNumber"
              type="tel"
              value={phoneNumber}
              placeholder="010-1212-1212"
              required
              onChange={(e) => setPhoneNumber(e.target.value)}
            />
          </div>
        </div>
        <div className="w-[340px]">
          <div className="mb-2 block">
            <Label htmlFor="password" color="success" value="비밀번호" />
          </div>
          <TextInput
            id="password"
            type="password"
            placeholder="비밀번호"
            helperText={
              password.length >= 8 ? (
                passwordProblem ? (
                  <span>사용 불가능한 비밀번호</span>
                ) : (
                  <span>사용 가능한 비밀번호</span>
                )
              ) : (
                <span>영어,숫자,특수문자 포함 8~16자리의 비밀번호 입력</span>
              )
            }
            required
            onChange={(e) => setPassword(e.target.value)}
            color={
              password.length >= 8
                ? !passwordProblem
                  ? "success"
                  : "failure"
                : "gray"
            }
          />
        </div>
        <AnimatePresence>
          {!passwordProblem && password.length >= 8 && (
            <motion.div
              animate={{ y: 0 }}
              initial={{ y: -25 }}
              exit={{ y: -25, opacity: 0 }}
              transition={{ ease: "easeOut", duration: 0.3 }}
              className="w-[340px]"
            >
              <div className="mb-2 block">
                <Label
                  htmlFor="password2"
                  color="success"
                  value="비밀번호 확인"
                />
              </div>
              <TextInput
                id="password2"
                type="password"
                placeholder="비밀번호 재입력"
                required
                onChange={(e) => setPassword2(e.target.value)}
                color={
                  password.length >= 8 && password2.length >= password.length
                    ? passwordProblem || !passwordSame
                      ? "failure"
                      : "success"
                    : "gray"
                }
                helperText={
                  password.length >= 8 &&
                  password2.length >= password.length ? (
                    passwordProblem ? (
                      <span>사용 불가능한 비밀번호</span>
                    ) : passwordSame ? (
                      <span>비밀번호가 일치합니다.</span>
                    ) : (
                      <span>비밀번호가 일치하지 않습니다.</span>
                    )
                  ) : (
                    <span>비밀번호를 한번 더 입력해주세요</span>
                  )
                }
              />
            </motion.div>
          )}
        </AnimatePresence>
        <AnimatePresence>
          {phoneNumber && passwordSame && (
            <motion.div
              animate={{ opacity: 1 }}
              initial={{ opacity: 0 }}
              exit={{ opacity: 0 }}
              transition={{ ease: "easeOut", duration: 0.3 }}
              className="flex w-full justify-center"
            >
              <CustomButton
                className="menubtn mt-[20px]"
                onClick={() => alert("회원가입")}
              >
                회원가입
              </CustomButton>
            </motion.div>
          )}
        </AnimatePresence>

        <div className="flex items-center justify-center gap-[5px] mt-[20px]">
          <Text className="text-[1rem] ">이미 계정이 있나요?</Text>
          <Link
            to="/signin"
            className="text-A706CheryBlue text-[1rem] font-bold cursor-pointer"
          >
            로그인
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Singup;
