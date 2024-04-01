import { useContext, useEffect, useState } from "react";
import { Helmet } from "react-helmet-async";
import { MenuCard, StateContext } from "@/shared";
import { getAlarmList, readAlarm } from "@/entities";
import dayjs from "dayjs";
type AlarmListType = {
  alarmId: number;
  author: string;
  content: string;
  readYn: boolean;
  generatedAt: string;
};

const Alarm = () => {
  const [alarmList, setAlarmList] = useState<AlarmListType[]>([]);
  const [readTrigger, setReadTrigger] = useState<boolean>(false);
  const { setHeaderTitle } = useContext(StateContext);

  useEffect(() => {
    setHeaderTitle("알림");
    return () => {
      setHeaderTitle("");
    };
  }, []);

  useEffect(() => {
    getAlarmList(
      ({ data }) => setAlarmList(data.result),
      (error) => console.log(error)
    );
  }, [readTrigger]);
  return (
    <div className="flex flex-col flex-1 bg-gradient-to-b from-A706DarkGrey2 to-A706DarkGrey1 opacity-90 shadow-sm">
      <Helmet>
        <title>파인디어 알람 페이지</title>
        <meta name="description" content="파인디어 알람 확인 페이지" />
        <meta
          name="keywords"
          content="Findear, Alarm, 알람, 알림, Alarm, Notice"
        />
      </Helmet>
      <div className="flex flex-col m-[20px] gap-[15px]">
        {alarmList.map((alarm) => {
          if (!alarm.readYn) {
            return (
              <MenuCard
                key={alarm.alarmId}
                title={alarm.author}
                content={alarm.content}
                generatedAt={dayjs(alarm.generatedAt).format(
                  "YYYY-MM-DD HH:mm"
                )}
                onClick={() => {
                  readAlarm(
                    alarm.alarmId,
                    ({ data }) => {
                      console.log(data);
                      setReadTrigger((prev) => !prev);
                    },
                    (error) => console.log(error)
                  );
                }}
              />
            );
          }
        })}
      </div>
    </div>
  );
};

export default Alarm;
