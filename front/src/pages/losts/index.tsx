import image1 from "@/shared/boardingImage/Findear.png";
import { Card, Text, useIntersectionObserver } from "@/shared";
import { SelectBox } from "@/shared";
import { IoIosOptions } from "react-icons/io";
import { AnimatePresence, motion } from "framer-motion";
import { useCallback, useRef, useState, useEffect } from "react";
import { IoCloseSharp } from "react-icons/io5";

const Loasts = () => {
  const [option, setOption] = useState(false);
  const [mobile, setMobile] = useState(false);
  const [render, setRender] = useState(20);
  const [isLoading, setIsLoading] = useState(false);
  const [observe, unobserve] = useIntersectionObserver(() => {
    setIsLoading(true);
    setTimeout(() => {
      setRender((prev) => prev + 20);
    }, 3000);
    setIsLoading(false);
  });

  const [total, setTotal] = useState(0);

  const target = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (render === 10) {
      observe(target.current as HTMLDivElement);
    }

    if (render >= total) {
      unobserve(target.current as HTMLDivElement);
    }
  }, [render]);

  useEffect(() => {
    if (isLoading) {
      unobserve(target.current as HTMLDivElement);
    } else {
      observe(target.current as HTMLDivElement);
    }
  }, [isLoading]);

  // 데이터를 패칭해오는 로직
  useEffect(() => {
    setTotal(100);
  }, []);

  const variants = {
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

  const checkDevice = useCallback(() => {
    if (window.innerWidth > 1280) {
      setMobile(false);
      return;
    }

    setMobile(true);
  }, []);

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

  return (
    <div className="flex flex-col flex-1">
      <div className="flex flex-col  flex-1 mx-[10px] my-[10px]">
        <div className="flex justify-between items-center">
          <Text className="font-bold text-[1.2rem]">
            소중한 것을 찾고 있어요!
          </Text>
          <SelectBox
            className="w-[140px]"
            options={[{ value: "파인디어" }, { value: "Lost112" }]}
            onChange={(e) => {
              console.log(e.target.value);
            }}
          ></SelectBox>
        </div>
        <div className="flex justify-between items-center my-[10px]">
          <div className="flex flex-wrap gap-[10px]">
            <SelectBox
              className="w-[140px]"
              options={[{ value: "data1" }, { value: "data2" }]}
              onChange={(e) => {
                console.log(e.target.value);
              }}
            ></SelectBox>
            <SelectBox
              className="w-[140px]"
              options={[{ value: "data1" }, { value: "data2" }]}
              onChange={(e) => {
                console.log(e.target.value);
              }}
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
                <motion.div
                  variants={variants}
                  initial={mobile ? "mobileInit" : "desktopInit"}
                  animate={mobile ? "mobileEnd" : "desktopEnd"}
                  exit={mobile ? "mobileInit" : "desktopInit"}
                  transition={{ ease: "easeOut", duration: 0.5 }}
                  className="absolute max-xl:w-[60%] xl:w-[40%] xl:h-[600px] right-0  max-xl:top-0 h-full z-[1] bg-A706LightGrey rounded-xl border-2 border-A706Grey2"
                >
                  <div className="flex items-center justify-between mx-[10px]">
                    <Text className="text-[1.5rem] font-bold p-[10px]">
                      상세 검색
                    </Text>
                    <div onClick={() => setOption(false)}>
                      <IoCloseSharp size="32" />
                    </div>
                  </div>
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
            {Array(render)
              .fill(null)
              .map((item, idx) => (
                <Card
                  key={item + idx}
                  date="2024-03-01"
                  image={image1}
                  locate="역삼역"
                  title="카드잃어버렸어요"
                />
              ))}
          </div>
          <div ref={target} className="w-full" />
        </div>
      </div>
    </div>
  );
};

export default Loasts;
