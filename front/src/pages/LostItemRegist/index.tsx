import AddBoxOutlinedIcon from "@mui/icons-material/AddBoxOutlined";
import ArrowBackIosNewIcon from "@mui/icons-material/ArrowBackIosNew";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Textarea, TextInput } from "flowbite-react";
import { AxiosError } from "axios";
import { motion } from "framer-motion";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import { LocalizationProvider } from "@mui/x-date-pickers";
import { DemoContainer, DemoItem } from "@mui/x-date-pickers/internals/demo";
import { ProgressBar } from "@/widgets";
import { registLosts } from "@/entities";
import {
  CustomButton,
  SelectBox,
  useGenerateHexCode,
  useMemberStore,
} from "@/shared";
import AWS from "aws-sdk";

const LostItemRegist = () => {
  const colorList = [
    { name: "화이트", value: "FFFFFF" },
    { name: "블랙", value: "000000" },
    { name: "레드", value: "FF0000" },
    { name: "오렌지", value: "FFA500" },
    { name: "옐로우", value: "FFFF00" },
    { name: "그린", value: "008000" },
    { name: "블루", value: "0000FF" },
    { name: "브라운", value: "8B4513" },
    { name: "퍼플", value: "800080" },
    { name: "보라색", value: "FF1493" },
    { name: "그레이", value: "808080" },
    { name: "기타", value: "" },
  ];

  AWS.config.update({
    accessKeyId: import.meta.env.VITE_S3_ACCESS_KEY,
    secretAccessKey: import.meta.env.VITE_S3_SECRET_ACCESS_KEY,
  });

  const myBucket = new AWS.S3({
    params: { Bucket: "findearbucket" },
    region: "ap-northeast-2",
  });

  const { member } = useMemberStore();

  const [progress, setProgress] = useState<number>(0);
  const [isCompleted, setCompleted] = useState<boolean>(false);
  const [autoFilled, setAutoFilled] = useState<boolean>(false);
  const [question, setQuestion] = useState<JSX.Element>(<></>);
  const [inputForm, setInputForm] = useState<JSX.Element>(<></>);
  const [productName, setProductName] = useState<string>("");
  const [content, setContent] = useState<string>("");
  const [image, setImage] = useState<File>();
  const [thumbnailURL, setThumbnailURL] = useState<string>("");
  const [categoryId, setCategoryId] = useState<string>("");
  const [color, setColor] = useState<string>("");
  const [lostAt, setLostAt] = useState<string>("");
  const [suspiciousPlace, setSuspiciousPlace] = useState<string>("");

  const beforeProgress = () => {
    setProgress((progress) => (progress > 0 ? progress - 1 : progress));
  };

  const nextProgress = () => {
    setProgress((progress) => (progress < 6 ? progress + 1 : progress));
  };

  const completeRegist = (isSuccess: boolean, log?: AxiosError) => {
    setQuestion(() => (
      <p>{isSuccess ? "등록되었습니다" : "등록에 실패하였습니다"}</p>
    ));
    setInputForm(() => <p className="text-center">{log?.message}</p>);
    setProgress(7);
  };

  const uploadImageToS3 = () => {
    return new Promise((resolve, reject) => {
      const imageName = useGenerateHexCode(image?.name ?? "");
      const params = {
        Body: image,
        Bucket: "findearbucket",
        Key: imageName,
      };

      myBucket.putObject(params, (err: any) => {
        if (err) {
          console.error("이미지 업로드 중에 오류가 발생했습니다.", err);
          setThumbnailURL("");
          setImage(new FileList()[0]);
          reject(err);
        } else {
          resolve(
            myBucket
              .getSignedUrl("getObject", {
                Key: params.Key,
              })
              .split("?")[0]
          );
        }
      });
    });
  };

  const saveImageFile = (fileList: FileList) => {
    const uploadedFile = fileList[0];
    if (uploadedFile.type.split("/")[0] === "image") {
      const reader = new FileReader();
      reader.readAsDataURL(uploadedFile);
      reader.onloadend = () => {
        setThumbnailURL((reader.result ?? "").toString());
        setImage(uploadedFile);
      };
    }
  };

  const selectPostCode = () => {
    new daum.Postcode({
      oncomplete: (data: any) => {
        setSuspiciousPlace(data.address);
      },
    }).open();
  };

  const renderImageInputForm = () => {
    if (thumbnailURL && thumbnailURL.length > 0) {
      return (
        <div className="flex flex-row justify-center">
          <label htmlFor="file">
            <img
              className="bg-white border-dashed border border-black rounded-lg size-[240px]"
              src={thumbnailURL}
            />
          </label>
          <input
            id="file"
            type="file"
            hidden
            multiple
            onChange={(e) => saveImageFile(e.target.files ?? new FileList())}
          />
        </div>
      );
    } else {
      return (
        <div className="flex flex-row justify-center">
          <div className="flex justify-center">
            <label
              htmlFor="file"
              className="flex flex-col justify-center items-center cursor-pointer bg-white border-dashed border border-black rounded-lg size-[80px] mx-5"
            >
              <AddBoxOutlinedIcon />
            </label>
            <input
              id="file"
              type="file"
              hidden
              multiple
              onChange={(e) => saveImageFile(e.target.files ?? new FileList())}
            />
          </div>
        </div>
      );
    }
  };

  const renderAddressInputForm = () => {
    return (
      <>
        <TextInput
          readOnly
          placeholder={"장소를 입력해 주세요."}
          defaultValue={suspiciousPlace}
          onClick={() => selectPostCode()}
        />
      </>
    );
  };

  const handleRegist = async () => {
    await uploadImageToS3()
      .then((imageUrl: any) => {
        registLosts(
          {
            memberId: member.memberId,
            productName,
            content,
            color,
            categoryId,
            imageUrls: [imageUrl],
            lostAt,
            suspiciousPlace,
            xPos: 0,
            yPos: 0,
          },
          ({ data }) => {
            completeRegist(true, data);
          },
          (error) => {
            completeRegist(false, error);
          }
        );
      })
      .catch((error) => console.log(error));
  };

  const PAGE: Record<number, any> = {
    0: {
      question: "무엇을 잃어버리셨나요?",
      inputForm: (
        <TextInput
          placeholder={"분실한 물건의 이름을 적어주세요"}
          defaultValue={productName}
          onChange={(e) => setProductName(e.target.value)}
        />
      ),
    },
    1: {
      question: "어떤 종류의 물건인가요?",
      inputForm: (
        <div className="flex flex-col justify-center">
          {/* <Select id="mainCategory">
            <option value="">카테고리 선택</option>
            <option value="PRA000">가방</option>
            <option value="PRB000">도서용품</option>
            <option value="PRC000">서류</option>
            <option value="PRD000">산업용품</option>
            <option value="PRE000">스포츠용품</option>
            <option value="PRF000">자동차</option>
            <option value="PRG000">전자기기</option>
            <option value="PRH000">지갑</option>
            <option value="PRI000">컴퓨터</option>
            <option value="PRJ000">휴대폰</option>
            <option value="PRK000">의류</option>
            <option value="PRL000">현금</option>
            <option value="PRM000">유가증권</option>
            <option value="PRN000">증명서</option>
            <option value="PRO000">귀금속</option>
            <option value="PRP000">카드</option>
            <option value="PRQ000">쇼핑백</option>
            <option value="PRR000">악기</option>
            <option value="PRZ000">기타물품</option>
          </Select> */}
          <div className="flex flex-col flex-1">
            <ul className="grid grid-cols-4 grid-rows-4 gap-5">
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Magnifying%20Glass%20Tilted%20Right.png"
                  alt="Magnifying Glass Tilted Right"
                  width="50"
                  height="50"
                />
                전체
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Backpack.png"
                  alt="Backpack"
                  width="50"
                  height="50"
                />
                가방
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Gem%20Stone.png"
                  alt="Gem Stone"
                  width="50"
                  height="50"
                />
                귀금속
              </li>
              <li
                className=" flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Books.png"
                  alt="Books"
                  width="50"
                  height="50"
                />
                도서용품
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Card%20Index%20Dividers.png"
                  alt="Card Index Dividers"
                  width="50"
                  height="50"
                />
                서류
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Toolbox.png"
                  alt="Toolbox"
                  width="50"
                  height="50"
                />
                산업용품
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Shopping%20Bags.png"
                  alt="Shopping Bags"
                  width="50"
                  height="50"
                />{" "}
                쇼핑백
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Activities/Basketball.png"
                  alt="Basketball"
                  width="50"
                  height="50"
                />
                스포츠용품
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Violin.png"
                  alt="Violin"
                  width="50"
                  height="50"
                />
                악기
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Activities/Ticket.png"
                  alt="Ticket"
                  width="50"
                  height="50"
                />
                유가증권
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/T-Shirt.png"
                  alt="T-Shirt"
                  width="50"
                  height="50"
                />
                의류
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Pickup%20Truck.png"
                  alt="Pickup Truck"
                  width="50"
                  height="50"
                />
                자동차
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Headphone.png"
                  alt="Headphone"
                  width="50"
                  height="50"
                />
                전자기기
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Purse.png"
                  alt="Purse"
                  width="50"
                  height="50"
                />
                지갑
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Page%20Facing%20Up.png"
                  alt="Page Facing Up"
                  width="50"
                  height="50"
                />
                증명서
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Laptop.png"
                  alt="Laptop"
                  width="50"
                  height="50"
                />
                컴퓨터
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Credit%20Card.png"
                  alt="Credit Card"
                  width="50"
                  height="50"
                />
                카드
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Money%20Bag.png"
                  alt="Money Bag"
                  width="50"
                  height="50"
                />
                현금
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Mobile%20Phone.png"
                  alt="Mobile Phone"
                  width="50"
                  height="50"
                />
                휴대폰
              </li>
              <li
                className="flex flex-col items-center w-full h-20 justify-center"
                onClick={(e) => setCategoryId(e.currentTarget.innerText)}
              >
                <img
                  src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Link.png"
                  alt="Link"
                  width="50"
                  height="50"
                />
                기타물품
              </li>
            </ul>
          </div>
        </div>
      ),
    },
    2: {
      question: "물건을 대표하는 색상이 있을까요?",
      inputForm: (
        <SelectBox
          options={colorList.map((color) => ({ value: color.name }))}
          onChange={(e) =>
            setColor(
              [...colorList.entries()]
                .filter((entry) => entry[1].name === e.target.value)
                .map((entry) => entry[1].value)[0]
            )
          }
        ></SelectBox>
      ),
    },
    3: {
      question: "물건의 정보를 입력해주세요.",
      inputForm: (
        <Textarea
          className="resize-none"
          placeholder="자세한 정보를 입력해주시면 도움이 됩니다."
          defaultValue={content}
          onChange={(e) => setContent(e.target.value)}
          rows={4}
          required
        ></Textarea>
      ),
    },
    4: {
      question: "물건의 사진이 있다면 올려주세요. 찾는데 도움이 됩니다.",
      inputForm: <>{renderImageInputForm()}</>,
    },
    5: {
      question: "잃어버린 날짜를 기억하시나요?",
      inputForm: (
        <>
          <div className="flex justify-center">
            <LocalizationProvider dateAdapter={AdapterDayjs}>
              <DemoContainer components={["DatePicker"]}>
                <DemoItem
                  label={
                    <span>
                      <strong>날짜 선택</strong>
                    </span>
                  }
                >
                  <DatePicker
                    disableFuture
                    format={"YYYY/MM/DD"}
                    views={["year", "month", "day"]}
                    defaultValue={lostAt}
                    onChange={(value: string | null) => setLostAt(value ?? "")}
                  />
                </DemoItem>
              </DemoContainer>
            </LocalizationProvider>
          </div>
        </>
      ),
    },
    6: {
      question: "잃어버린 장소도 기억하시나요?",
      inputForm: <>{renderAddressInputForm()}</>,
    },
  } as const;
  type PAGE = (typeof PAGE)[keyof typeof PAGE]; // 'iOS' | 'Android'

  useEffect(() => {
    setInputForm(() => <>{renderImageInputForm()}</>);
  }, [thumbnailURL]);

  useEffect(() => {
    setInputForm(() => <>{renderAddressInputForm()}</>);
  }, [suspiciousPlace]);

  useEffect(() => {
    if (categoryId || color || lostAt) setAutoFilled(true);
  }, [categoryId, color, lostAt]);

  useEffect(() => {
    if (autoFilled) {
      setProgress((prev) => prev + 1);
      setAutoFilled(false);
    }
  }, [autoFilled]);

  useEffect(() => {
    console.log(progress);
    if (progress <= 6 && progress >= 0) {
      setQuestion(() => <p>{PAGE[progress].question}</p>);
      setInputForm(() => PAGE[progress].inputForm);
      if (progress == 6) {
        setCompleted(true);
      } else {
        setCompleted(false);
      }
    }
  }, [progress]);

  return (
    <div className="flex flex-col justify-center self-center w-[360px] flex-1">
      <div className="h-[48px]">
        {progress > 0 ? (
          <CustomButton>
            <ArrowBackIosNewIcon onClick={() => beforeProgress()} />
          </CustomButton>
        ) : (
          <></>
        )}
      </div>
      <div className="flex justify-center place-items-center h-[48px]">
        <ProgressBar progress={progress} />
      </div>
      <motion.div className="flex flex-col h-full">
        <div className="flex flex-col text-center justify-center align-middle h-[50%] text-2xl text-balance font-semibold">
          {question}
        </div>
        <div className="flex flex-col justify-start ">{inputForm}</div>
      </motion.div>
      <div className="h-[48px] my-5">
        {progress <= 6 ? (
          <div className="flex justify-center">
            <CustomButton
              className="w-[280px] h-[40px] rounded-xl bg-A706CheryBlue text-white disabled:bg-A706DarkGrey1 text-sm"
              onClick={() => (isCompleted ? handleRegist() : nextProgress())}
              children={isCompleted ? "분실물 등록" : "다음"}
            ></CustomButton>
          </div>
        ) : (
          <div className="flex justify-around">
            <Link
              className="w-[160px] h-[40px] rounded-xl bg-A706CheryBlue text-white dark:bg-A706DarkGrey2 text-center p-2"
              to={"/main"}
            >
              홈으로
            </Link>
            <Link
              className="w-[160px] h-[40px] rounded-xl bg-A706CheryBlue text-white dark:bg-A706DarkGrey2 text-center p-2"
              to={"/main"}
            >
              목록에서 찾아보기
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};

export default LostItemRegist;
