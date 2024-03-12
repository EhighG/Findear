import { DarkThemeToggle, Flowbite } from "flowbite-react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import { Main, Boarding, Signup, Signin } from "@/pages";
import { Header, Footer } from "@/widgets";
import "./index.css";

const App = () => {
  return (
    <Flowbite>
      <div className="Container">
        <DarkThemeToggle className="absolute right-0" />
        <div className="flex flex-row justify-center z-0">
          <main className="flex flex-col justify-between w-[360px] h-[800px] bg-[#FAFAFA]">
            <Header />
            <Router>
              <Routes>
                <Route path="/" element={<Main />} />
                <Route path="/boarding" element={<Boarding />} />
                <Route path="/signup" element={<Signup />} />
                <Route path="/signin" element={<Signin />} />
              </Routes>
            </Router>
            <Footer />
          </main>
        </div>

      </div>
    </Flowbite>
  );
};

export default App;
