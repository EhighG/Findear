import { DarkThemeToggle, Flowbite } from "flowbite-react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import {
  Main,
  Boarding,
  Signup,
  Signin,
  FoundItemWrite,
  FoundItemDetail,
} from "@/pages";
import { Header, Footer, Dropdown } from "@/widgets";
import "./index.css";

const App = () => {
  return (
    <Flowbite>
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
              <Route path="/foundItemWrite" element={<FoundItemWrite />} />
              <Route
                path="/foundItemDetail/:id"
                element={<FoundItemDetail />}
              />
              <Route path="/dropdown" element={<Dropdown />}></Route>
            </Routes>
          </Router>
        </main>
        <Footer />
      </div>
    </Flowbite>
  );
};

export default App;
