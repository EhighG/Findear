import SendOutlinedIcon from "@mui/icons-material/SendOutlined";
import FavoriteIcon from "@mui/icons-material/Favorite";
import FavoriteBorderOutlinedIcon from "@mui/icons-material/FavoriteBorderOutlined";
import { CustomButton, Text, useMemberStore } from "@/shared";
import { Carousel, FloatingLabel, Label, Modal } from "flowbite-react";
import { useEffect, useState } from "react";
import { getAcquisitionsDetail, returnAcquisitions } from "@/entities";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import {
  cancelScarppedBoard,
  scrapBoard,
  receiverType,
} from "@/entities/findear/api";
import { infoType } from "@/entities";

// type board = {
//   boardId: number;
//   memberId: number;
//   productName: string;
//   description: string;
//   status: string;
//   deleteYn: boolean;
//   registeredAt?: string;
//   thumbnailUrl: string;
//   categoryName: string;
//   color: string;
// };

// type AcquiredBoard = {
//   acquiredBoardId: number;
//   address: string;
//   name: string;
//   x_pos: number;
//   y_pos: number;
// };

const foundItemDetail = () => {
  const navigate = useNavigate();
  const { member } = useMemberStore();

  const [detailData, setDetailData] = useState<infoType>();

  // const [categoryName, setCategoryName] = useState<string>("");
  // const [founderId, setFounderId] = useState<string>("");
  // const [imgUrls, setImgUrls] = useState<string[]>([]);
  // const [productName, setProductName] = useState<string>("");
  // const [description, setDescription] = useState<string>("");
  // const [address, setAddress] = useState<string>("");
  // const [registeredAt, setRegisteredAt] = useState<string>(
  //   "2024.03.26 17:31:00"
  // );
  // const [agencyName, setAgencyName] = useState<string>("멀티캠퍼스");
  const [isScrapped, setScrapped] = useState<boolean>(false);
  const [isReturned, setReturned] = useState<boolean>(false);

  const boardId = parseInt(useParams().id ?? "0");
  const [query] = useSearchParams();
  const [receiver, setReceiver] = useState<receiverType>();
  const [receiverName, setReceiverName] = useState<string>();
  const [receiverEmail, setReceiverEmail] = useState<string>();
  const [receiverPhoneNumber, setReceiverPhoneNumber] = useState<string>();

  const [modalOptions, setModalOptions] = useState<boolean>(false);

  const initReceiver = () => {
    if (query) {
      setReceiver({
        phoneNumber: query.get("phoneNumber") ?? "",
      });
    }
  };

  useEffect(() => {
    initReceiver();
  }, [query]);

  useEffect(() => {
    getAcquisitionsDetail(
      boardId,
      ({ data }) => {
        console.log(data.result);
        setDetailData(data.result);
      },
      (error) => console.log(error)
    );
  }, []);

  const returnItem = () => {
    if (receiver) {
      returnAcquisitions(
        { boardId, receiver },
        ({ data }) => {
          console.log(data);
          alert(data.message);
          setReturned(true);
        },
        (error) => {
          alert(error.message);
          setReturned(false);
        }
      );
      setModalOptions(false);
    } else {
      alert("인계 대상자 덜 참");
    }
  };

  const sendMessage = () => {
    navigate("/letter", { state: { key: 18 } });
  };

  const scarpItem = (scrpping: boolean) => {
    if (scrpping) {
      scrapBoard(
        boardId,
        ({ data }) => {
          alert(data.message);
          setScrapped(scrpping);
        },
        (error) => {
          console.log(error);
          alert(error.message);
        }
      );
    } else {
      cancelScarppedBoard(
        boardId,
        ({ data }) => {
          alert(data.message);
          setScrapped(scrpping);
        },
        (error) => {
          console.log(error);
          alert(error.message);
        }
      );
    }
  };

  useEffect(() => {
    if (receiverName && receiverEmail && receiverPhoneNumber) {
      setReceiver({
        phoneNumber: receiverPhoneNumber,
      });
    }
  }, [receiverPhoneNumber]);

  return (
    <div className="flex flex-col justify-center items-center p-[40px]">
      <div className="flex flex-row justify-between w-[340px]">
        <span className="bg-A706Blue2 text-A706CheryBlue text-xs font-bold me-2 px-2.5 py-0.5 rounded dark:bg-blue-900 dark:text-blue-300">
          {detailData?.board.categoryName ?? "카테고리 없음"}
        </span>
        <Text className="text-md font-bold">
          보관장소 : {detailData?.agencyName ?? "시설명"}
        </Text>
      </div>
      <div className="flex flex-col justify-center p-[40px] gap-[20px]">
        <div className="flex  items-center justify-center size-[300px]">
          <Carousel>
            {detailData?.board.imgUrls.map((imgUrl, idx) => (
              <div
                className="flex justify-center w-full h-full border border-A706Grey2 rounded-xl"
                key={idx}
              >
                <img
                  src={imgUrl}
                  alt="이미지가 없습니다."
                  className="object-fill rounded-xl"
                />
              </div>
            ))}
          </Carousel>
        </div>
      </div>
      <div className="w-[340px] flex flex-col text-center">
        <Text className="text-md">
          {detailData?.address + ", " + detailData?.agencyName}
        </Text>
        <p className="text-md font-bold">{detailData?.board.registeredAt}</p>
      </div>
      <div className="flex flex-row justify-between mt-10 w-[340px]">
        <div className="w-full">
          <Label color="secondary" value="물품명" />
          <Text className="text-lg font-bold">
            {detailData?.board.productName ?? "물품명"}
          </Text>
          <div className="h-[1px] bg-A706DarkGrey2"></div>
        </div>
      </div>
      {/* <div className="w-[340px] mt-10">
          <Label color="secondary" value="설명" />
          <div className="bg-white border-2 min-h-[50px] p-5 rounded-md">
            {detailData?.board.description ?? "설명이 없습니다."}
          </div>
        </div> */}
      <div className="w-[340px] mt-10 flex flex-row justify-around">
        {member.memberId === detailData?.board.member.memberId ? (
          <>
            <CustomButton
              className="rounded-md bg-A706CheryBlue text-white text-sm w-full flex flex-row justify-around p-5 m-3"
              onClick={() => {
                sendMessage();
              }}
            >
              <>
                <SendOutlinedIcon className="self-center" />
                <p className="self-center">쪽지 보내기</p>
              </>
            </CustomButton>
            {isScrapped ? (
              <CustomButton
                className="rounded-md bg-A706Red text-white text-sm w-full flex flex-row justify-around p-5 m-3"
                onClick={() => scarpItem(false)}
              >
                <>
                  <FavoriteIcon />
                  <p>스크랩 완료</p>
                </>
              </CustomButton>
            ) : (
              <CustomButton
                className="rounded-md bg-A706Grey2 text-white text-sm w-full flex flex-row justify-around p-5 m-3"
                onClick={() => {
                  scarpItem(true);
                }}
              >
                <>
                  <FavoriteBorderOutlinedIcon />
                  <p>스크랩하기</p>
                </>
              </CustomButton>
            )}
          </>
        ) : (
          <>
            {isReturned ? (
              <CustomButton className="rounded-md w-[320px] h-[60px] bg-A706DarkGrey1 text-white">
                <p>인계 완료</p>
              </CustomButton>
            ) : (
              <CustomButton
                className="rounded-md bg-A706CheryBlue text-white text-base w-full p-3"
                onClick={() => setModalOptions(true)}
              >
                <p>인계하기</p>
              </CustomButton>
            )}
          </>
        )}
      </div>
      <Modal
        dismissible
        show={modalOptions}
        position={"center"}
        size="md"
        onClose={() => {
          setModalOptions(false);
        }}
      >
        <Modal.Header>
          <p>인계 대상자 정보</p>
        </Modal.Header>
        <Modal.Body className="flex flex-col justify-center">
          <FloatingLabel
            variant="outlined"
            label="이름을 입력해 주세요."
            sizing="sm"
            defaultValue={receiverName}
            onChange={(e) => setReceiverName(e.target.value)}
          />
          <FloatingLabel
            variant="outlined"
            label="이메일을 입력해 주세요."
            sizing="sm"
            defaultValue={receiverEmail}
            onChange={(e) => setReceiverEmail(e.target.value)}
          />
          <FloatingLabel
            variant="outlined"
            label="연락처를 입력해 주세요."
            sizing="sm"
            defaultValue={receiverPhoneNumber}
            onChange={(e) => setReceiverPhoneNumber(e.target.value)}
          />
          <CustomButton
            className="rounded-md w-full bg-A706CheryBlue text-white text-base p-3 mt-5 self-center"
            onClick={() => returnItem()}
          >
            <p>인계</p>
          </CustomButton>
        </Modal.Body>
      </Modal>
    </div>
  );
};

export default foundItemDetail;
