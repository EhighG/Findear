import { useState, useEffect } from "react";
import { Label, TextInput, Checkbox, Modal } from "flowbite-react";
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
  const [userNameValidate, setUserNameValidate] = useState(false); //닉네임 중복 체크 [true, false, null]
  const [password, setPassword] = useState("");
  const [password2, setPassword2] = useState("");
  const [passwordProblem, setPasswordProblem] = useState(false);
  const [username, setUsername] = useState("");
  const [tel, setTel] = useState("");
  const [manager, setManager] = useState(false);
  const [address, setAddress] = useState("");
  const [detailAddress, setDetailAddress] = useState("");
  const [facility, setFacility] = useState("");
  const [facilityNo, setFacilityNo] = useState("");
  const [passwordSame, setPasswordSame] = useState(false);
  const [openModal, setOpenModal] = useState(false);
  const debouncedEmail = useDebounce(email, 700);
  const debouncedUserName = useDebounce(username, 700);

  // 이메일 유효성 체크
  useEffect(() => {
    if (debouncedEmail.length > 10 && useEmailValidation(debouncedEmail)) {
      //TODO: 이메일 중복 체크 API 호출
      setEmailValidate(true);
      return;
    }
    setEmailValidate(false);
  }, [debouncedEmail]);

  // 닉네임 유효성 체크
  useEffect(() => {
    if (debouncedUserName.length >= 2) {
      //TODO: 닉네임 중복 체크 API 호출 검증
      setUserNameValidate(true);
      return;
    }
    setUserNameValidate(false);
  }, [debouncedUserName]);

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

  return (
    <div className="flex flex-col justify-center itmes-center p-[40px]">
      <div className="flex flex-col items-center gap-[8px]">
        <Text className="text-center text-4xl">회원가입</Text>
        <Modal show={openModal} onClose={() => setOpenModal(false)}>
          <Modal.Header>알림</Modal.Header>
          <Modal.Body>
            <div>
              <p>인증 코드가 전송되었습니다.</p>
            </div>
          </Modal.Body>
        </Modal>
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
                          onClick={() => setOpenModal(true)}
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
            <Label htmlFor="username" color="success" value="닉네임" />
          </div>
          <TextInput
            id="username"
            placeholder="닉네임을 입력해주세요"
            onChange={(e) => setUsername(e.target.value)}
            color={
              username.length >= 2
                ? userNameValidate
                  ? "success"
                  : "failure"
                : "gray"
            }
            helperText={
              username.length >= 2 ? (
                userNameValidate ? (
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
            value={tel}
            onChange={(e) => setTel(e.target.value)}
          />
        </div>
        <div className="flex flex-col gap-[10px]">
          <div className="flex gap-[10px] items-center">
            <Checkbox
              id="manager"
              name="group"
              value="manager"
              onChange={(e) => setManager(e.target.checked)}
            />
            <Label htmlFor="manager" className="text-xl">
              사장님이나 시설 관리자 이신가요?
            </Label>
          </div>
          {manager ? (
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
                    id="facility"
                    placeholder="시설명"
                    value={facility}
                    onChange={(e) => setFacility(e.target.value)}
                    required
                  />
                  <TextInput
                    type="tel"
                    id="facilityNo"
                    placeholder="시설연락처"
                    value={facilityNo}
                    onChange={(e) => setFacilityNo(e.target.value)}
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
