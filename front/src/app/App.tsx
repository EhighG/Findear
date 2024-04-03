import { useCallback, useEffect, useState } from "react";
import {
  BrowserRouter as Router,
  Route,
  Routes,
  Navigate,
} from "react-router-dom";
import { motion } from "framer-motion";
import { Flowbite, Toast } from "flowbite-react";
import { HelmetProvider } from "react-helmet-async";
import { EventSourcePolyfill } from "event-source-polyfill";
import { HiExclamation } from "react-icons/hi";
import { FaTelegramPlane } from "react-icons/fa";
import {
  Main,
  FoundItemWrite,
  AgencyRegist,
  Boarding,
  FoundItemDetail,
  Boards,
  Introduce,
  AcquireRegist,
  LostItemDetail,
  LostItemRegist,
  Letter,
  Alarm,
  MyBoard,
  MyPage,
  NaverLogin,
  UpdateInfo,
  MatchingList,
  CheckInfo,
  LetterRoomDetail,
} from "@/pages";
import { Header, Footer } from "@/widgets";
import { tokenCheck } from "@/entities";
import { useMemberStore, StateContext, SSEConnect } from "@/shared";
import "./index.css";

const App = () => {
  const {
    Authenticate,
    token,
    tokenInitialize,
    memberInitialize,
    authenticateInitialize,
    agencyInitialize,
  } = useMemberStore();

  const [headerTitle, setHeaderTitle] = useState<string>("");
  const [meta, setMeta] = useState(true);
  const [connected, setConnected] = useState(false);
  const [mobile, setMobile] = useState(false);
  const [openToast, setOpenToast] = useState(false);
  const [toastMessage, setToastMessage] = useState("메시지가 도착했어요");

  let eventSource: EventSourcePolyfill;

  const SSEConnection = () => {
    eventSource = SSEConnect();

    eventSource.onopen = () => {
      setConnected(true);
    };

    eventSource.onerror = () => {
      setConnected(false);
    };

    eventSource.addEventListener("message", (event) => {
      const data = JSON.parse(event.data);
      setToastMessage(data.content);
      setOpenToast(true);
    });
  };

  const checkDevice = useCallback(() => {
    if (window.innerWidth > 1280) {
      setMobile(false);
      return;
    }

    setMobile(true);
  }, []);

  useEffect(() => {
    if (!Authenticate || connected) return;
    SSEConnection();

    return () => {
      eventSource.close();
    };
  }, [Authenticate]);

  useEffect(() => {
    const handleResize = () => {
      checkDevice();
    };

    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
    };
  }, []);

  useEffect(() => {
    checkDevice();
  }, []);

  useEffect(() => {
    if (token.accessToken) {
      tokenCheck(
        () => {},
        () => {
          tokenInitialize();
          memberInitialize();
          authenticateInitialize();
          agencyInitialize();
          window.location.href = "/";
        }
      );
      return;
    }
  }, []);

  const handleToast = () => {
    return (
      <motion.div
        className="absolute w-[300px] self-center top-[20%] z-[10]"
        initial={{ opacity: 0, y: -100 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: 100 }}
        transition={{ duration: 0.5 }}
      >
        <Toast>
          <FaTelegramPlane className="h-5 w-5 text-cyan-600 dark:text-cyan-500" />
          <div className="pl-4 text-sm font-normal">{toastMessage}</div>
          <Toast.Toggle onClick={() => setOpenToast(false)} />
        </Toast>
      </motion.div>
    );
  };

  return (
    <HelmetProvider>
      <StateContext.Provider value={{ headerTitle, setHeaderTitle, setMeta }}>
        <Flowbite>
          <div className="Container">
            <Router>
              {meta && <Header />}
              {!mobile && (
                <Toast className="self-center mt-[80px]">
                  <div className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-orange-100 text-orange-500 dark:bg-orange-700 dark:text-orange-200">
                    <HiExclamation className="h-5 w-5" />
                  </div>
                  <div className="ml-3 text-sm font-normal">
                    Findear 서비스는 모바일 환경에 최적화되어 있습니다. PC
                    환경에서는 일부 기능이 제한되거나 정상 작동하지 않을 수
                    있습니다.
                  </div>
                  <Toast.Toggle />
                </Toast>
              )}

              {openToast ? handleToast() : ""}
              <main className="flex py-[80px] relative flex-col overflow-y-scroll scrollbar-hide flex-1  xl:mx-[10%]">
                <Routes>
                  <Route
                    path="/"
                    element={
                      Authenticate ? <Navigate to="/main" /> : <Boarding />
                    }
                  />
                  <Route path="/main" element={<Main />} />
                  <Route
                    path="/foundItemWrite"
                    element={
                      Authenticate ? <FoundItemWrite /> : <Navigate to="/" />
                    }
                  />
                  <Route
                    path="/agencyRegist"
                    element={
                      Authenticate ? <AgencyRegist /> : <Navigate to="/" />
                    }
                  />
                  <Route
                    path="/updateInfo"
                    element={
                      Authenticate ? <UpdateInfo /> : <Navigate to="/" />
                    }
                  />
                  <Route
                    path="/checkInfo"
                    element={Authenticate ? <CheckInfo /> : <Navigate to="/" />}
                  />
                  <Route
                    path="/losts"
                    element={<Boards boardType="분실물" />}
                  />
                  <Route
                    path="/acquire"
                    element={<Boards boardType="습득물" />}
                  />
                  <Route
                    path="/MyBoard"
                    element={Authenticate ? <MyBoard /> : <Navigate to="/" />}
                  />
                  <Route
                    path="/matchingList"
                    element={
                      Authenticate ? <MatchingList /> : <Navigate to="/" />
                    }
                  />

                  <Route
                    path="/letter/:roomId"
                    element={
                      Authenticate ? <LetterRoomDetail /> : <Navigate to="/" />
                    }
                  />
                  <Route path="/introduce" element={<Introduce />} />
                  <Route
                    path="/acquireRegist"
                    element={
                      Authenticate ? <AcquireRegist /> : <Navigate to="/" />
                    }
                  />
                  <Route
                    path="/myPage"
                    element={Authenticate ? <MyPage /> : <Navigate to="/" />}
                  />
                  <Route path="/members/login" element={<NaverLogin />} />
                  <Route
                    path="/foundItemDetail/:id"
                    element={
                      Authenticate ? <FoundItemDetail /> : <Navigate to="/" />
                    }
                  />
                  <Route
                    path="/lostItemDetail/:id"
                    element={
                      Authenticate ? <LostItemDetail /> : <Navigate to="/" />
                    }
                  />
                  <Route
                    path="/lostItemRegist"
                    element={
                      Authenticate ? <LostItemRegist /> : <Navigate to="/" />
                    }
                  />

                  <Route
                    path="/letter"
                    element={Authenticate ? <Letter /> : <Navigate to="/" />}
                  />
                  <Route
                    path="/alarm"
                    element={Authenticate ? <Alarm /> : <Navigate to="/" />}
                  />
                  <Route
                    path="/matchingList/:id"
                    element={
                      Authenticate ? <MatchingList /> : <Navigate to="/" />
                    }
                  ></Route>
                </Routes>
              </main>
              {meta && <Footer />}
            </Router>
          </div>
        </Flowbite>
      </StateContext.Provider>
    </HelmetProvider>
  );
};

export default App;
