import {
  Main,
  Boarding,
  Signup,
  Signin,
  FoundItemWrite,
  FoundItemDetail,
  AgencyRegist,
  LostItemRegist,
  Boards,
  Introduce,
  AcquireRegist,
  IntroduceDetail,
  Letter,
  Alarm,
} from "@/pages";
import { Header, Footer } from "@/widgets";
import "./index.css";
import { DarkThemeToggle, Flowbite } from "flowbite-react";
import {
  BrowserRouter as Router,
  Route,
  Routes,
  Navigate,
} from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useMemberStore } from "@/shared";
import { HelmetProvider } from "react-helmet-async";
import { StateContext } from "@/shared";
import { useEffect, useState } from "react";
import { tokenCheck } from "@/entities";
const queryClient = new QueryClient();

const App = () => {
  const {
    Authenticate,
    token,
    tokenInitialize,
    memberInitialize,
    authenticateInitialize,
  } = useMemberStore();
  const [headerTitle, setHeaderTitle] = useState<string>("");
  const [meta, setMeta] = useState(true);

  useEffect(() => {
    // 최초 진입 시 토큰이 있다면 체크, 토큰 유효 시 로그인 처리
    // 토큰이 만료되었을 경우 로그아웃 처리
    if (token.accessToken) {
      tokenCheck(
        () => {
          console.info("유효한 토큰");
        },
        () => {
          tokenInitialize();
          memberInitialize();
          authenticateInitialize();
        }
      );
      return;
    }
  }, []);

  return (
    <HelmetProvider>
      <StateContext.Provider value={{ headerTitle, setHeaderTitle, setMeta }}>
        <Flowbite>
          <QueryClientProvider client={queryClient}>
            <div className="Container">
              <Router>
                <DarkThemeToggle className="absolute right-0 z-10" />
                {meta && <Header />}
                <main className="flex relative flex-col flex-1 xl:mx-[10%]">
                  <Routes>
                    <Route
                      path="/"
                      element={
                        Authenticate ? <Navigate to="/main" /> : <Boarding />
                      }
                    />
                    <Route
                      path="/signup"
                      element={
                        !Authenticate ? <Signup /> : <Navigate to="/main" />
                      }
                    />
                    <Route
                      path="/signin"
                      element={
                        !Authenticate ? <Signin /> : <Navigate to="/main" />
                      }
                    />
                    <Route path="/main" element={<Main />} />

                    <Route
                      path="/foundItemWrite"
                      element={
                        Authenticate ? (
                          <FoundItemWrite />
                        ) : (
                          <Navigate to="/signin" />
                        )
                      }
                    />
                    <Route
                      path="/agencyRegist"
                      element={
                        Authenticate ? (
                          <AgencyRegist />
                        ) : (
                          <Navigate to="/signin" />
                        )
                      }
                    />
                    <Route
                      path="/losts"
                      element={<Boards boardType="분실물" />}
                    />
                    <Route
                      path="/acquire"
                      element={<Boards boardType="습득물" />}
                    />
                    <Route path="/introduce" element={<Introduce />} />
                    <Route
                      path="/acquireRegist"
                      element={
                        Authenticate ? (
                          <AcquireRegist />
                        ) : (
                          <Navigate to="/signin" />
                        )
                      }
                    />

                    <Route
                      path="/foundItemDetail/:id"
                      element={
                        Authenticate ? (
                          <FoundItemDetail />
                        ) : (
                          <Navigate to="/signin" />
                        )
                      }
                    />
                    <Route
                      path="/lostItemRegist"
                      element={
                        Authenticate ? (
                          <LostItemRegist />
                        ) : (
                          <Navigate to="/signin" />
                        )
                      }
                    />
                    <Route
                      path="/introduceDetail"
                      element={<IntroduceDetail />}
                    />
                    <Route
                      path="/letter"
                      element={
                        Authenticate ? <Letter /> : <Navigate to="/signin" />
                      }
                    />
                    <Route
                      path="/alarm"
                      element={
                        Authenticate ? <Alarm /> : <Navigate to="/signin" />
                      }
                    />
                  </Routes>
                </main>
                {meta && <Footer />}
              </Router>
            </div>
          </QueryClientProvider>
        </Flowbite>
      </StateContext.Provider>
    </HelmetProvider>
  );
};

export default App;
