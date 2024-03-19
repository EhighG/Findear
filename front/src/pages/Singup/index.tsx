import { useState, useEffect } from "react";
import { Label, TextInput, Checkbox } from "flowbite-react";
import { HiMail } from "react-icons/hi";
import {
  useDebounce,
  useEmailValidation,
  usePasswordValidation,
  Text,
  CustomButton,
} from "@/shared";
import { Link } from "react-router-dom";
import { KakaoMap } from "@/shared";
import { checkCode, checkEmail, nicknameCheck } from "@/entities";

declare global {
  interface Window {
    kakao: any;
    daum: any;
  }
  const kakao: any;
  const daum: any;
}

const Singup = () => {
  const [email, setEmail] = useState("");
  const [emailValidate, setEmailValidate] = useState(false); //이메일 중복 체크 [true, false, null
  const [message, setMessage] = useState("");
  const [nicknameValidate, setNicknameValidate] = useState(false); //닉네임 중복 체크 [true, false, null]
  const [password, setPassword] = useState("");
  const [password2, setPassword2] = useState("");
  const [passwordProblem, setPasswordProblem] = useState(false);
  const [nickname, setNickname] = useState("");
  const [code, setCode] = useState("");
  const [codeSend, setCodeSend] = useState(false); //인증번호 전송 여부 [true, false, null
  const [codeValidate, setCodeValidate] = useState(false);
  const [phoneNumber, setPhoneNumber] = useState("");
  const [role, setRole] = useState<"NORMAL" | "MANAGER">("NORMAL");
  const [address, setAddress] = useState("");
  const [detailAddress, setDetailAddress] = useState("");
  const [agencyName, setAgencyName] = useState("");
  const [agencyPhoneNumber, setAgencyPhoneNumber] = useState("");
  const [passwordSame, setPasswordSame] = useState(false);
  // const [xPos, setXPos] = useState<number>(0);
  // const [yPos, setYPos] = useState<number>(0);
  const debouncedEmail = useDebounce(email, 700);
  const debouncedNickname = useDebounce(nickname, 700);

  // 이메일 유효성 체크
  useEffect(() => {
    if (debouncedEmail.length > 10 && useEmailValidation(debouncedEmail)) {
      checkEmail(
        { email: debouncedEmail },
        () => {
          setEmailValidate(true);
        },
        () => {
          setEmailValidate(false);
        }
      );
      return;
    }
    setEmailValidate(false);
  }, [debouncedEmail]);

  // 닉네임 유효성 체크
  useEffect(() => {
    if (debouncedNickname.length >= 2) {
      nicknameCheck(
        debouncedNickname,
        () => {
          setNicknameValidate(true);
        },
        () => {
          setNicknameValidate(false);
        }
      );
    }
  }, [debouncedNickname]);

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

  const openAddressModal = () => {
    new daum.Postcode({
      oncomplete: function (data: any) {
        setAddress(data.address);
      },
    }).open();
  };

  const codeChecking = (code: string) => {
    checkCode(
      { email, code },
      () => {
        setCodeValidate(true);
      },
      (err) => {
        setMessage(err.respons?.data.message ?? "인증실패");
        (document.getElementById("my_modal_1") as HTMLFormElement).showModal();
      }
    );
  };

  const sendCode = () => {
    setMessage("인증 메시지를 전송하였습니다.");
    setCodeSend(true);
    (document.getElementById("my_modal_1") as HTMLFormElement).showModal();
  };

  return (
    <div className="flex flex-col flex-1 justify-center itmes-center p-[40px]">
      <div className="flex flex-col items-center gap-[8px]">
        <Text className="text-center text-4xl">회원가입</Text>
        <dialog id="my_modal_1" className="modal">
          <div className="modal-box">
            <h3 className="font-bold text-lg">알림</h3>
            <p className="py-4">{message}</p>
            <div className="modal-action">
              <form method="dialog">
                {/* if there is a button in form, it will close the modal */}
                <button className="btn">Close</button>
              </form>
            </div>
          </div>
        </dialog>
        <div className="w-[340px]">
          <div className="mb-2 block">
            <Label htmlFor="email" color="success" value="이메일" />
          </div>
          <div className="flex flex-col">
            <TextInput
              className="w-[340px]"
              id="email"
              type="email"
              icon={HiMail}
              // rightIcon={true ? IoIosSend : ""}
              placeholder="Findear@findear.com"
              required
              color={
                email.length > 10
                  ? emailValidate
                    ? "success"
                    : "failure"
                  : "gray"
              }
              onChange={(e) => setEmail(e.target.value)}
              helperText={
                email.length > 10 ? (
                  emailValidate ? (
                    <>
                      <span className="flex items-center  gap-[5px] font-medium">
                        사용가능 이메일, 인증하기를 눌러주세요
                        <CustomButton
                          className="text-xl border-2 border-A706DarkGrey1 rounded-md px-2 dark:text-A706LightGrey dark:border-A706LightGrey2"
                          onClick={() => sendCode()}
                        >
                          인증하기
                        </CustomButton>
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
        </div>
        {codeSend ? (
          <div className="w-[340px]">
            <div className="mb-2 block">
              <Label htmlFor="code" color="success" value="인증 코드 입력" />
            </div>
            <TextInput
              id="code"
              type="text"
              placeholder="인증 번호를 입력하세요"
              onChange={(e) => setCode(e.target.value)}
              required
              value={code}
              readOnly={codeValidate}
              helperText={
                codeValidate ? (
                  <span>인증성공</span>
                ) : (
                  <span className="flex items-center  gap-[5px] font-medium">
                    인증번호 입력 후 인증하기를 눌러주세요
                    <CustomButton
                      className="text-xl border-2 border-A706DarkGrey1 rounded-md px-2 dark:text-A706LightGrey dark:border-A706LightGrey2"
                      onClick={() => codeChecking(code)}
                    >
                      인증하기
                    </CustomButton>
                  </span>
                )
              }
            />
          </div>
        ) : (
          ""
        )}
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
                <span>
                  영어,숫자,특수문자 포함 8~16자리의 비밀번호를 입력해주세요
                </span>
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
        <div className="w-[340px]">
          <div className="mb-2 block">
            <Label htmlFor="password2" color="success" value="비밀번호 확인" />
          </div>
          <TextInput
            id="password2"
            type="password"
            placeholder="비밀번호를 한번 더 입력해주세요"
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
              password.length >= 8 && password2.length >= password.length ? (
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
        </div>
        <div className="w-[340px]">
          <div className="mb-2 block">
            <Label htmlFor="nickname" color="success" value="닉네임" />
          </div>
          <TextInput
            id="nickname"
            placeholder="닉네임을 입력해주세요"
            onChange={(e) => setNickname(e.target.value)}
            color={
              nickname.length >= 2
                ? nicknameValidate
                  ? "success"
                  : "failure"
                : "gray"
            }
            helperText={
              nickname.length >= 2 ? (
                nicknameValidate ? (
                  <>
                    <span className="font-medium">
                      사용가능한 닉네임 입니다.
                    </span>
                  </>
                ) : (
                  <>
                    <span className="font-medium">
                      사용불가능한 닉네임 입니다
                    </span>
                  </>
                )
              ) : (
                ""
              )
            }
            required
          />
        </div>
        <div className="w-[340px]">
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
            value={phoneNumber}
            onChange={(e) => setPhoneNumber(e.target.value)}
          />
        </div>
        <div className="flex flex-col gap-[10px]">
          <div className="flex gap-[10px] items-center">
            <Checkbox
              id="manager"
              name="group"
              value="MANAGER"
              onChange={(e) =>
                e.target.checked ? setRole("MANAGER") : setRole("NORMAL")
              }
            />
            <Label htmlFor="manager" className="text-xl">
              사장님이나 시설 관리자 이신가요?
            </Label>
          </div>
          {role === "MANAGER" ? (
            <div className="flex flex-col w-[340px] gap-[10px]">
              <CustomButton
                className="text-xl border-2 bg-A706white border-A706DarkGrey1 rounded-md px-1 dark:border-A706LightGrey dark:text-A706LightGrey"
                onClick={openAddressModal}
              >
                주소 검색
              </CustomButton>

              {address ? (
                <>
                  <TextInput
                    id="address"
                    placeholder="사용자 주소"
                    readOnly
                    value={address}
                    required
                  />
                  <TextInput
                    id="addressDetail"
                    placeholder="상세 주소"
                    value={detailAddress}
                    onChange={(e) => setDetailAddress(e.target.value)}
                    required
                  />
                  <TextInput
                    id="agencyName"
                    placeholder="시설명"
                    value={agencyName}
                    onChange={(e) => setAgencyName(e.target.value)}
                    required
                  />
                  <TextInput
                    type="tel"
                    id="agencyPhoneNumber"
                    placeholder="시설연락처"
                    value={agencyPhoneNumber}
                    onChange={(e) => setAgencyPhoneNumber(e.target.value)}
                    required
                  />
                  <KakaoMap className="size-[340px]" keyword={address} />
                </>
              ) : (
                ""
              )}
            </div>
          ) : (
            ""
          )}
        </div>
        <div className="flex w-full justify-center">
          <CustomButton
            className="menubtn mt-[20px]"
            onClick={() => alert("회원가입")}
          >
            회원가입
          </CustomButton>
        </div>
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
