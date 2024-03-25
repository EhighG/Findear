import { Helmet } from "react-helmet-async";

const Alarm = () => {
  return (
    <div>
      <Helmet>
        <title>파인디어 알람 페이지</title>
        <meta name="description" content="파인디어 알람 확인 페이지" />
        <meta
          name="keywords"
          content="Findear, Alarm, 알람, 알림, Alarm, Notice"
        />
      </Helmet>
      알림 페이지
    </div>
  );
};

export default Alarm;
