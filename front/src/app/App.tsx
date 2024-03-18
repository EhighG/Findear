import { Main, Boarding, Signup, Signin, MyPage } from "@/pages";
import { Header, Footer } from "@/widgets";
import "./index.css";
import { DarkThemeToggle, Flowbite } from "flowbite-react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const queryClient = new QueryClient();

const App = () => {
  return (
    <Flowbite>
      <QueryClientProvider client={queryClient}>
        <div className="Container">
          <DarkThemeToggle className="absolute right-0 z-10" />
          <Header />
          <main className="flex flex-col flex-1">
            <Router>
              <Routes>
                <Route path="/" element={<Main />} />
                <Route path="/boarding" element={<Boarding />} />
                <Route path="/signup" element={<Signup />} />
                <Route path="/signin" element={<Signin />} />
                <Route path="/mypage" element={<MyPage />} />
              </Routes>
            </Router>
          </main>
          <Footer />
        </div>
      </QueryClientProvider>
    </Flowbite>
  );
};

export default App;
