import SendOutlinedIcon from "@mui/icons-material/SendOutlined";
import FavoriteIcon from "@mui/icons-material/Favorite";
import FavoriteBorderOutlinedIcon from "@mui/icons-material/FavoriteBorderOutlined";
import { CustomButton, Text, useMemberStore } from "@/shared";
import { FloatingLabel, Kbd, Label, Modal } from "flowbite-react";
import { useEffect, useState } from "react";
import { getAcquisitionsDetail, returnAcquisitions } from "@/entities";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import {
  cancelScarppedBoard,
  scrapBoard,
  receiverType,
} from "@/entities/findear/api";

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

  const [categoryName, setCategoryName] = useState<string>("지갑");
  const [founderId, setFounderId] = useState<string>("ztrl");
  const [imgUrls, setImgUrls] = useState<string[]>([]);
  const [productName, setProductName] = useState<string>("갈색 지갑");
  const [description, setDescription] = useState<string>("설명이 없습니다.");
  const [address, setAddress] = useState<string>("서울시 강남구 역삼동");
  const [registeredAt, setRegisteredAt] = useState<string>(
    "2024.03.26 17:31:00"
  );
  const [agencyName, setAgencyName] = useState<string>("멀티캠퍼스");
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
    if (
      query &&
      query.get("name") &&
      query.get("email") &&
      query.get("phoneNumber")
    ) {
      setReceiver({
        name: query.get("name") ?? "",
        email: query.get("email") ?? "",
        phoneNumber: query.get("phoneNumber") ?? "",
      });
    }
  };

  initReceiver();

  useEffect(() => {
    getAcquisitionsDetail(
      boardId,
      ({ data }) => {
        console.log(data);
        setFounderId(data.result.board.member.memberId);
        setProductName(data.result.board.productName);
        if (data.result.description) {
          setDescription(data.result.description);
        }
        setCategoryName(data.result.board.categoryName);
        setImgUrls(data.result.board.imgUrls);
        setRegisteredAt(data.result.registeredAt);
        setAddress(data.result.address);
        setAgencyName(data.result.agencyName);
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
    navigate("/letter", { state: { key: founderId } });
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
        name: receiverName,
        email: receiverEmail,
        phoneNumber: receiverPhoneNumber,
      });
    }
  }, [receiverName, receiverEmail, receiverPhoneNumber]);

  return (
    <>
      <div className="flex flex-col justify-center items-center p-[40px]">
        <div className="flex flex-row justify-between w-[340px]">
          {categoryName ? (
            <span className="bg-A706Blue2 text-A706CheryBlue text-xs font-bold me-2 px-2.5 py-0.5 rounded dark:bg-blue-900 dark:text-blue-300">
              카테고리: {categoryName}
            </span>
          ) : (
            <Kbd>카테고리 없음</Kbd>
          )}
          <Text className="text-sm">{agencyName}</Text>
        </div>
        <div className="flex flex-col justify-center p-[40px]">
          <div className="flex justify-center mx-5">
            <img
              src={imgUrls[0]}
              alt="이미지가 없습니다."
              className="size-[170px] self-center"
            />
          </div>
        </div>
        <div className="w-[340px] flex flex-col text-center">
          <p className="text-sm">{address + ", " + agencyName}</p>
          <p className="text-xs">{registeredAt}</p>
        </div>
        <div className="flex flex-row justify-between mt-10 w-[340px]">
          <div className="w-full">
            <Label color="secondary" value="이름" />
            <Text children={productName} className="text-lg font-bold" />
            <div className="h-[1px] bg-A706DarkGrey2"></div>
          </div>
        </div>
        <div className="w-[340px] mt-10">
          <Label color="secondary" value="설명" />
          <div className="bg-white border-2 min-h-[50px] p-5 rounded-md">
            {description}
          </div>
        </div>
        <div className="w-[340px] mt-10 flex flex-row justify-around">
          {member.role === "NORMAL" ? (
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
    </>
  );
};

export default foundItemDetail;
