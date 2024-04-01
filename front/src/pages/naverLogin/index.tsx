import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { oauthSignin } from "@/entities";
import { useMemberStore } from "@/shared";
import Swal from "sweetalert2";

const NaverLogin = () => {
  const navigate = useNavigate();
  const [param] = useSearchParams();
  const { setToken, setMember, setAgency, setAuthenticate } = useMemberStore();
  useEffect(() => {
    const code = param.get("code");
    console.log("code", code);
    if (code) {
      oauthSignin(
        code,
        ({ data }) => {
          console.log(data);
          setToken({
            accessToken: data.accessToken,
            refreshToken: data.refreshToken,
          });
          setMember(data.member);
          setAgency(data.agency);
          setAuthenticate(true);
          Swal.fire({
            icon: "success",
            title: "로그인 성공",
          }).then(() => {
            navigate("/main");
          });
        },
        (error) => {
          console.log(error);
        }
      );
    }
  }, [param]);

  return <div></div>;
};

export default NaverLogin;
