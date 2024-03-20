import image1 from "@/shared/boardingImage/Findear.png";
import { Card, Text } from "@/shared";
import { SelectBox } from "@/shared";
import { IoIosOptions } from "react-icons/io";
import { AnimatePresence, motion } from "framer-motion";
import { useState } from "react";
import { useEffect } from "react";
const Loasts = () => {
  const [option, setOption] = useState(false);
  const [mobile, setMobile] = useState(false);

  const variant = {
    desktopInit: {
      opacity: 0,
      y: 0,
    },
    desktopEnd: {
      opacity: 1,
      y: 0,
    },
    mobileInit: {
      x: 500,
    },
    mobileEnd: {
      x: 0,
    },
  };

  const checkDevice = () => {
    if (window.innerWidth > 1280) {
      setMobile(false);
      return;
    }
    setMobile(true);
  };

  useEffect(() => {
    window.addEventListener("resize", checkDevice);

    checkDevice();

    return window.removeEventListener("resize", checkDevice);
  }, []);
  return (
    <div className="flex flex-col flex-1">
      <div className="flex flex-col  flex-1 mx-[10px] my-[10px]">
        <div className="flex justify-between items-center">
          <Text className="font-bold text-[1.2rem]">
            소중한 것을 찾고 있어요!
          </Text>
          <SelectBox
            id="serviceType"
            className="w-[140px]"
            options={[{ value: "파인디어" }, { value: "Lost112" }]}
          ></SelectBox>
        </div>
        <div className="flex justify-between items-center my-[10px]">
          <div className="flex flex-wrap gap-[10px]">
            <SelectBox
              id="services"
              className="w-[140px]"
              options={[{ value: "data" }, { value: "data" }]}
            ></SelectBox>
            <SelectBox
              id="services2"
              className="w-[140px]"
              options={[{ value: "data" }, { value: "data" }]}
            ></SelectBox>
          </div>

          <div>
            <div
              onClick={() => {
                setOption((prev) => !prev);
              }}
            >
              <IoIosOptions size="24" />
            </div>
            <AnimatePresence>
              {option && (
                //   animate={{ y: 0 }}
                //   initial={{ y: -25 }}
                //   exit={{ y: -25, opacity: 0 }}
                <motion.div
                  variants={variant}
                  initial={mobile ? "mobileInit" : "desktopInit"}
                  animate={mobile ? "mobileEnd" : "desktopEnd"}
                  exit={mobile ? "mobileInit" : "desktopInit"}
                  transition={{ ease: "easeOut", duration: 0.5 }}
                  className="absolute max-xl:w-[60%] xl:w-[40%] xl:h-[600px] right-0  max-xl:top-0 h-full z-[1] bg-A706LightGrey rounded-xl border-2 border-A706Grey2"
                >
                  <Text className="text-[1.5rem] font-bold p-[10px]">
                    상세 검색
                  </Text>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
        <div className="flex flex-1 flex-col">
          <div className="grid max-md:grid-cols-2 max-lg:grid-cols-3 max-xl:grid-cols-4 max-2xl:grid-cols-5 grid-cols-6 justify-items-center gap-[10px]">
            <Card
              date="2024-03-01"
              image={image1}
              locate="멀티캠퍼스 역삼"
              title="카드잃어버렸어요"
              onClick={() => alert("클릭")}
            />
            <Card
              date="2024-03-01"
              image={image1}
              locate="역삼역"
              title="카드잃어버렸어요"
            />
            <Card
              date="2024-03-01"
              image={image1}
              locate="역삼역"
              title="카드잃어버렸어요"
            />
            <Card
              date="2024-03-01"
              image={image1}
              locate="역삼역"
              title="카드잃어버렸어요"
            />
            <Card
              date="2024-03-01"
              image={image1}
              locate="역삼역"
              title="카드잃어버렸어요"
            />
            <Card
              date="2024-03-01"
              image={image1}
              locate="역삼역"
              title="카드잃어버렸어요"
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default Loasts;
