import ArrowBackIosNewIcon from "@mui/icons-material/ArrowBackIosNew";
import CloudDownloadIcon from "@mui/icons-material/CloudDownload";
import { CustomButton } from "@/shared";
import { useEffect, useState } from "react";
import { Select, Textarea, TextInput } from "flowbite-react";
import { ProgressBar } from "@/widgets";
import AddBoxOutlinedIcon from "@mui/icons-material/AddBoxOutlined";

const LostItemRegist = () => {
  const imageReader = new FileReader();
  const [progress, setProgress] = useState<number>(0);
  //   const [autoFilled, setAutoFilled] = useState<boolean>(false);
  const [question, setQuestion] = useState<JSX.Element>();
  const [inputForm, setInputForm] = useState<JSX.Element>();
  const [files, setFiles] = useState<File[]>([]);
  const [uploadedFiles, setUploadedFiles] = useState<FileList>();
  const [images, setImages] = useState<string[]>([]);

  const PAGE_ELEMENT: Record<number, any> = {
    0: {
      question: "무엇을 잃어버리셨나요?",
      inputForm: <TextInput placeholder="분실한 물건의 이름을 적어주세요" />,
    },
    1: {
      question: "어떤 물건인가요?",
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
        <Select>
          <option>빨간색</option>
          <option>파란색</option>
        </Select>
      ),
    },
    3: {
      question: "물건의 정보를 입력해주세요.",
      inputForm: (
        <Textarea
          className="resize-none"
          placeholder="자세한 정보를 입력해주시면 도움이 됩니다."
          rows={4}
          required
        ></Textarea>
      ),
    },
    4: {
      question: "물건의 사진이 있다면 올려주세요. 찾는데 도움이 됩니다.",
      inputForm: (
        <>
          {images.map((image, index) => (
            <img key={index} src={image} />
          ))}
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
            onChange={(e) => setUploadedFiles(e.target.files ?? new FileList())}
          ></input>
        </>
      ),
    },
    5: {},
    6: {},
    7: {},
  } as const;
  type PAGE_ELEMENT = (typeof PAGE_ELEMENT)[keyof typeof PAGE_ELEMENT]; // 'iOS' | 'Android'

  imageReader.onloadend = () => {
    const result = imageReader.result ?? "";
    setImages((images) => [...images, result.toString()]);
  };

  useEffect(() => {
    setInputForm(() => (
      <>
        {images.map((image, index) => (
          <img key={index} src={image} />
        ))}
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
          onChange={(e) => setUploadedFiles(e.target.files ?? new FileList())}
        ></input>
      </>
    ));
  }, [images]);

  useEffect(() => {
    files
      .filter((file) => file.type.split("/")[0] === "image")
      .forEach((file) => imageReader.readAsDataURL(file));
  }, [files]);

  useEffect(() => {
    setFiles((files) => [...files, ...(uploadedFiles ?? [])]);
  }, [uploadedFiles]);

  useEffect(() => {
    setQuestion(() => <p>{PAGE_ELEMENT[progress].question}</p>);
    setInputForm(() => PAGE_ELEMENT[progress].inputForm);
  }, [progress]);

  return (
    <>
      <div className="flex flex-col justify-center self-center w-[360px]">
        <div className="h-[48px]">
          <CustomButton
            children={
              <ArrowBackIosNewIcon
                onClick={() =>
                  setProgress((progress) =>
                    progress > 0 ? progress - 1 : progress
                  )
                }
              />
            }
          ></CustomButton>
        </div>
        <div className="flex justify-center place-items-center h-[48px]">
          <ProgressBar progress={progress} />
        </div>
        <div className="flex flex-col h-[704px]">
          <div className="flex flex-col text-center justify-center align-middle h-[352px]">
            {question}
          </div>
          <div className="flex justify-center h-auto">{inputForm}</div>
        </div>
        <div className="flex justify-center h-[48px]">
          <CustomButton
            className="w-[280px] h-[40px] rounded-xl bg-A706CheryBlue text-white"
            onClick={() =>
              setProgress((progress) =>
                progress < 7 ? progress + 1 : progress
              )
            }
            children={progress < 7 ? "다음" : "분실물 등록"}
          ></CustomButton>
        </div>
      </div>
    </>
  );
};

export default LostItemRegist;
