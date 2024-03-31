import { useContext, useEffect } from "react";
import { Helmet } from "react-helmet-async";
import { StateContext } from "@/shared";

const Alarm = () => {
  const { setHeaderTitle } = useContext(StateContext);

  useEffect(() => {
    setHeaderTitle("알림");
    return () => {
      setHeaderTitle("");
    };
  }, []);
  return (
    <div className="flex flex-col flex-1">
      <Helmet>
        <title>파인디어 알람 페이지</title>
        <meta name="description" content="파인디어 알람 확인 페이지" />
        <meta
          name="keywords"
          content="Findear, Alarm, 알람, 알림, Alarm, Notice"
        />
      </Helmet>
    </div>
  );
};

export default Alarm;
