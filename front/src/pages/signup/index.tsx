import {
  Text,
  usePasswordValidation,
  CustomButton,
  cls,
  StateContext,
} from "@/shared";
import { useState, useEffect, useCallback, useContext } from "react";
import { Label, TextInput } from "flowbite-react";
import { useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { usePhoneValidation, useDebounce } from "@/shared";
import { checkPhone, signUp } from "@/entities";
import { Helmet } from "react-helmet-async";

const Singup = () => {
  const navigate = useNavigate();
  const [phoneNumber, setPhoneNumber] = useState<string>("");
  const [password, setPassword] = useState("");
  const [password2, setPassword2] = useState("");
  const [passwordProblem, setPasswordProblem] = useState(false);
  const [passwordSame, setPasswordSame] = useState(false);
  const [phoneCheck, setPhoneCheck] = useState(false);
  const debouncedPhoneNumber = useDebounce(phoneNumber, 500);
  const { setMeta } = useContext(StateContext);

  useEffect(() => {
    setMeta(false);

    return () => {
      setMeta(true);
    };
  });
  // const [showMessage, setShowMessage] = useState(false);
  // const [message, setMessage] = useState("");

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

  useEffect(() => {
    if (debouncedPhoneNumber.length > 10) {
      checkPhone(
        debouncedPhoneNumber,
        ({ data }) => {
          setPhoneCheck(!data.result);
        },
        (error) => {
          console.log(error);
        }
      );
    }
  }, [debouncedPhoneNumber]);

  const handleKeyPress = useCallback((e: KeyboardEvent) => {
    if (e.key === "Enter") {
      handleSignup();
    }
  }, []);

  useEffect(() => {
    window.addEventListener("keydown", handleKeyPress);
    return () => {
      window.removeEventListener("keydown", handleKeyPress);
    };
  }, [handleKeyPress]);

  const handleSignup = () => {
    if (!phoneCheck || !phoneNumber || !passwordSame) {
      return;
    }
    signUp(
      {
        phoneNumber,
        password,
      },
      () => {
        // setShowMessage(true);
        // setMessage("회원가입이 완료되었습니다.");
        alert("회원 가입이 완료되었습니다.");
        navigate("/signin");
      },
      (error) => {
        console.log(error);
      }
    );
  };

  return (
    <div className="flex flex-col flex-1 justify-center itmes-center p-[40px]">
      <Helmet>
        <title>파인디어 회원가입 페이지</title>
        <meta name="description" content="파인디어 회원가입 페이지" />
        <meta
          name="keywords"
          content="Findear, 파인디어, register, signup, 회원가입, 가입"
        />
      </Helmet>
      <div className="flex flex-col items-center gap-[8px]">
        <Text className="text-center text-4xl">회원가입</Text>
        <form>
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
                autoComplete="off"
                onChange={(e) =>
                  setPhoneNumber(usePhoneValidation(e.target.value))
                }
                color={
                  debouncedPhoneNumber.length > 10
                    ? phoneCheck
                      ? "success"
                      : "warning"
                    : "gray"
                }
                helperText={
                  debouncedPhoneNumber.length > 10
                    ? phoneCheck
                      ? "사용 가능한 전화번호"
                      : "사용 불가능한 전화번호"
                    : "전화번호를 입력해주세요"
                }
                placeholder="전화번호를 입력해주세요"
                required
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
              placeholder="비밀번호를 입력해주세요"
              autoComplete="off"
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
                  autoComplete="off"
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
        </form>
        <div className="w-[340px]">
          <CustomButton
            className={cls(
              "menubtn mt-[20px]",
              phoneCheck && phoneNumber && passwordSame ? "" : "bg-A706Grey"
            )}
            disabled={phoneCheck && phoneNumber && passwordSame ? false : true}
            onClick={() => handleSignup()}
          >
            회원가입
          </CustomButton>
          <div className="flex gap-[5px] items-center justify-between w-full my-[10px]">
            <Text className="faint text-[1.5rem] ">이미 계정이 있나요?</Text>
            <CustomButton
              className="text-A706LightGrey dark:bg-A706DarkGrey2 text-[1.5rem] font-bold bg-A706CheryBlue cursor-pointer p-2 rounded-md"
              onClick={() => navigate("/signin")}
            >
              로그인
            </CustomButton>
          </div>
          <div className="flex gap-[5px] items-center justify-between w-full ">
            <Text className="faint text-[1.5rem] ">가입전 둘러보세요</Text>
            <CustomButton
              className="text-A706SlateGrey dark:text-A706Grey2 text-[2rem]  cursor-pointer rounded-md p-1"
              onClick={() => navigate("/main")}
            >
              둘러보기
            </CustomButton>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Singup;
