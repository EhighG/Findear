import { requestPermission } from "@/Firebase";
import { exitMember, signOut } from "@/entities";
import {
  CustomButton,
  ImageMenuCard,
  StateContext,
  Text,
  useMemberStore,
} from "@/shared";
import { useThemeMode } from "flowbite-react";
import { useContext, useEffect } from "react";
import { useNavigate } from "react-router-dom";

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
                alert("로그아웃 성공");
                agencyInitialize();
                memberInitialize();
                authenticateInitialize();
                window.location.href = "/";
              },
              () => {
                alert("로그아웃 실패");
              }
            );
          }}
        />
        {member.role === "Manager" && (
          <ImageMenuCard
            render={2}
            title="내가 작성한 분실물"
            alt="습득물"
            image="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/People/Detective.png"
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
            navigate("/updateInfo");
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
              return;
            }
            if (Notification.permission === "denied") {
              alert(
                "알림이 거부되었습니다, 알림을 원하시면 설정에서 알림을 허용해주세요."
              );
              return;
            }
            requestPermission();
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

        <Text
          className="self-end text-A706Grey"
          onClick={() => {
            if (confirm("정말로 탈퇴하시겠습니까?")) {
              exitMember(
                member.memberId,
                () => {
                  alert("회원 탈퇴 성공");
                  navigate("/");
                },
                () => {
                  alert("회원 탈퇴 실패");
                }
              );
            }
          }}
        >
          회원 탈퇴
        </Text>
      </div>
      <CustomButton
        className="menubtn"
        onClick={() => {
          signOut(
            () => {
              console.info("로그아웃 성공");
            },
            () => {
              console.error("로그아웃 실패");
            }
          );
        }}
      >
        로그아웃
      </CustomButton>
    </div>
  );
};

export default MyPage;
