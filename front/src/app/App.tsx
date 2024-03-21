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
} from "@/pages";
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
          <Router>
            <DarkThemeToggle className="absolute right-0 z-10" />
            <Header />
            <main className="flex relative flex-col flex-1 xl:w-[1200px] xl:self-center">
              <Routes>
                <Route path="/" element={<Boarding />} />
                <Route path="/main" element={<Main />} />
                <Route path="/signup" element={<Signup />} />
                <Route path="/signin" element={<Signin />} />
                <Route path="/foundItemWrite" element={<FoundItemWrite />} />
                <Route path="/agencyRegist" element={<AgencyRegist />} />
                <Route path="/losts" element={<Losts />} />
                <Route path="/acquire" element={<Acquire />} />
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
  );
};

export default App;
