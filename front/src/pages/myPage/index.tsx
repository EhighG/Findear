import { requestPermission } from "@/Firebase";
import { exitMember, signOut } from "@/entities";
import { ImageMenuCard, StateContext, Text, useMemberStore } from "@/shared";
import { useThemeMode } from "flowbite-react";
import { useContext, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Swal from "sweetalert2";
const MyPage = () => {
  const { mode, toggleMode } = useThemeMode();

  const { setHeaderTitle } = useContext(StateContext);
  const navigate = useNavigate();
  const { member, agencyInitialize, memberInitialize, authenticateInitialize } =
    useMemberStore();
  useEffect(() => {
    setHeaderTitle("마이페이지");
    return () => {
      setHeaderTitle("");
    };
  }, []);

  return (
    <div className="flex flex-col flex-1 bg-gradient-to-b from-A706DarkGrey2 to-A706DarkGrey1 opacity-90 shadow-sm">
      <div className="flex flex-col m-[20px] gap-[15px]">
        <ImageMenuCard
          render={1}
          title="안전하게 로그아웃"
          image="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Locked%20with%20Key.png"
          alt="logout"
          onClick={() => {
            signOut(
              () => {
                Swal.fire({
                  title: "로그아웃 완료",
                  text: "정상적으로 로그아웃 되었습니다",
                  icon: "success",
                }).then(() => {
                  agencyInitialize();
                  memberInitialize();
                  authenticateInitialize();
                  window.location.href = "/";
                });
              },
              () => {
                alert("로그아웃 실패");
              }
            );
          }}
        />
        {member.role === "MANAGER" && (
          <ImageMenuCard
            render={2}
            title="내가 작성한 습득물"
            alt="습득물"
            image="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Mobile%20Phone.png"
            onClick={() => {
              navigate("/myBoard", { state: { boardType: "습득물" } });
            }}
          />
        )}
        <ImageMenuCard
          render={3}
          title="내 정보 확인 및 변경"
          alt="정보 변경"
          image="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Locked%20with%20Pen.png"
          onClick={() => {
            navigate("/checkInfo");
          }}
        />

        <ImageMenuCard
          render={4}
          title={
            Notification.permission === "granted"
              ? "매칭 알림 ON"
              : "매칭 알림 신청"
          }
          alt="습득물"
          image={
            Notification.permission === "granted"
              ? "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Bell.png"
              : "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Bell%20with%20Slash.png"
          }
          onClick={() => {
            if (Notification.permission === "granted") {
              requestPermission();
              Swal.fire({
                title: "매칭 알림 수신 중",
                text: "매칭 알림이 정상적으로 수신 중 입니다.",
                icon: "info",
              });
              return;
            }
            if (Notification.permission === "denied") {
              Swal.fire({
                title: "매칭 알림 설정 거부",
                text: "매칭 알림 설정이 거부 상태입니다. 매칭알림을 받으시려면 설정에서 권한을 허용해주세요",
                icon: "warning",
              });
              return;
            }
          }}
        />

        <ImageMenuCard
          render={5}
          title="내가 작성한 분실물"
          alt="분실물"
          image="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Crayon.png"
          onClick={() => {
            navigate("/myBoard", { state: { boardType: "분실물" } });
          }}
        />
        <ImageMenuCard
          render={6}
          title={mode === "dark" ? "라이트모드" : "다크모드"}
          alt="다크모드 "
          image={
            mode === "dark"
              ? "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Bright%20Button.png"
              : "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Crescent%20Moon.png"
          }
          onClick={() => {
            toggleMode();
          }}
        />
        <ImageMenuCard
          render={7}
          title="매칭 리스트"
          alt="매칭 리스트"
          image="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Linked%20Paperclips.png"
          onClick={() => {
            navigate("/matchingList");
          }}
        />
        <Text
          className="self-end text-A706Grey"
          onClick={() => {
            Swal.fire({
              title: "회원 탈퇴",
              text: "정말 회원 탈퇴 하시겠습니까?",
              icon: "question",
              showCancelButton: true,
            }).then((result) => {
              if (result.isConfirmed) {
                exitMember(
                  member.memberId,
                  () => {
                    Swal.fire({
                      title: "회원 탈퇴 완료",
                      text: "정상적으로 회원탈퇴 되었습니다",
                      icon: "success",
                    }).then(() => {
                      agencyInitialize();
                      memberInitialize();
                      authenticateInitialize();
                      window.location.href = "/";
                    });
                  },
                  () => {
                    Swal.fire({
                      title: "회원 탈퇴 실패",
                      text: "회원탈퇴가 실패 되었습니다",
                      icon: "error",
                    });
                  }
                );
              } else {
                Swal.fire({
                  title: "회원 탈퇴 취소",
                  text: "회원탈퇴가 취소 되었습니다",
                  icon: "info",
                });
              }
            });
          }}
        >
          회원 탈퇴
        </Text>
      </div>
    </div>
  );
};

export default MyPage;
