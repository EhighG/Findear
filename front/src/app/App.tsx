import {
  Main,
  Boarding,
  Signup,
  Signin,
  FoundItemWrite,
  FoundItemDetail,
  AgencyRegist,
  LostItemRegist,
  Losts,
  Acquire,
  Introduce,
  AcquireRegist,
} from "@/pages";
import { Header, Footer } from "@/widgets";
import "./index.css";
import { DarkThemeToggle, Flowbite } from "flowbite-react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useMemberStore } from "@/shared";
import { HelmetProvider } from "react-helmet-async";
const queryClient = new QueryClient();

const App = () => {
  const { Authenticate } = useMemberStore();
  //TODO: 최초 진입 시 토큰 체크 로직, 토큰 유효 시 로그인 처리
  // const checkToken = () => {};

  return (
    <HelmetProvider>
      <Flowbite>
        <QueryClientProvider client={queryClient}>
          <div className="Container">
            <Router>
              <DarkThemeToggle className="absolute right-0 z-10" />
              <Header />
              <main className="flex relative flex-col flex-1 xl:mx-[10%]">
                <Routes>
                  <Route
                    path="/"
                    element={Authenticate ? <Losts /> : <Boarding />}
                  />
                  <Route
                    path="/signup"
                    element={!Authenticate ? <Signup /> : <Losts />}
                  />
                  <Route
                    path="/signin"
                    element={!Authenticate ? <Signin /> : <Losts />}
                  />
                  <Route path="/main" element={<Main />} />

                  <Route path="/foundItemWrite" element={<FoundItemWrite />} />
                  <Route path="/agencyRegist" element={<AgencyRegist />} />
                  <Route path="/losts" element={<Losts />} />
                  <Route path="/acquire" element={<Acquire />} />
                  <Route path="/introduce" element={<Introduce />} />
                  <Route path="/acquireRegist" element={<AcquireRegist />} />

                  <Route
                    path="/foundItemDetail/:id"
                    element={<FoundItemDetail />}
                  />
                  <Route path="/lostItemRegist" element={<LostItemRegist />} />
                </Routes>
              </main>
              <Footer />
            </Router>
          </div>
        </QueryClientProvider>
      </Flowbite>
    </HelmetProvider>
  );
};

export default App;
