import AddBoxOutlinedIcon from "@mui/icons-material/AddBoxOutlined";
import CloudDownloadIcon from "@mui/icons-material/CloudDownload";
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
  KakaoMap,
  SelectBox,
  useGenerateHexCode,
  useMemberStore,
} from "@/shared";
import AWS from "aws-sdk";

const LostItemRegist = () => {
  // const categoryList = ["가방", "핸드폰"];
  const colorList = ["빨간색", "파란색"];

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
  const [categoryId] = useState<string>("");
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
    if (isSuccess) {
      setProgress(7);
    }
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
        <div className="flex justify-center">
          <KakaoMap className="size-[150px] m-5" keyword={suspiciousPlace} />
        </div>
      </>
    );
  };

  const handleRegist = async () => {
    await uploadImageToS3()
      .then((imageUrl: any) => {
        console.log("registered imageUrl", imageUrl);
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
        <div>
          <div className="flex flex-row">
            <button className="main-nav-button">
              <CloudDownloadIcon fontSize="large" />
              카테고리
            </button>
            <button className="main-nav-button">
              <CloudDownloadIcon fontSize="large" />
              카테고리
            </button>
            <button className="main-nav-button">
              <CloudDownloadIcon fontSize="large" />
              카테고리
            </button>
            <button className="main-nav-button">
              <CloudDownloadIcon fontSize="large" />
              카테고리
            </button>
          </div>
          <div className="flex flex-row">
            <button className="main-nav-button">
              <CloudDownloadIcon fontSize="large" />
              카테고리
            </button>
            <button className="main-nav-button">
              <CloudDownloadIcon fontSize="large" />
              카테고리
            </button>
            <button className="main-nav-button">
              <CloudDownloadIcon fontSize="large" />
              카테고리
            </button>
            <button className="main-nav-button">
              <CloudDownloadIcon fontSize="large" />
              카테고리
            </button>
          </div>
        </div>
      ),
    },
    2: {
      question: "물건을 대표하는 색상이 있을까요?",
      inputForm: (
        <SelectBox
          options={colorList.map((color) => ({ value: color }))}
          onChange={(e) => setColor(e.target.value)}
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
    <>
      <div className="flex flex-col justify-center self-center w-[360px]">
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
        <motion.div className="flex flex-col h-[550px]">
          <div className="flex flex-col text-center justify-center align-middle h-[250px] text-2xl text-balance font-semibold">
            {question}
          </div>
          <div className="flex flex-col justify-start h-[300px]">
            {inputForm}
          </div>
        </motion.div>
        <div className="h-[48px]">
          {progress < 7 ? (
            <div className="flex justify-center">
              <CustomButton
                className="w-[280px] h-[40px] rounded-xl bg-A706CheryBlue text-white disabled:bg-A706DarkGrey1"
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
    </>
  );
};

export default LostItemRegist;
