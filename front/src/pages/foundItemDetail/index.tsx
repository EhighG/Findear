import SendOutlinedIcon from "@mui/icons-material/SendOutlined";
import FavoriteIcon from "@mui/icons-material/Favorite";
import FavoriteBorderOutlinedIcon from "@mui/icons-material/FavoriteBorderOutlined";
import ColorLensIcon from "@mui/icons-material/ColorLens";
import { CustomButton, KakaoMap, Text, useMemberStore } from "@/shared";
import { Breadcrumb, FloatingLabel, Label, Modal } from "flowbite-react";
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

  const [category, setCategory] = useState<string>("지갑");
  const [founderId, setFounderId] = useState<string>("습득자1");
  const [imageUrlList, setImageUrlList] = useState<string[]>([
    "../images/Findear-ls.png",
  ]);
  const [color, setColor] = useState<string>("빨간색");
  const [productName, setProductName] = useState<string>("");
  const [description, setDescription] = useState<string>("");
  const [address, setAddress] = useState<string>("");
  const [acquiredAt, setAcquiredAt] = useState<string>("");
  const [placeName, setPlaceName] = useState<string>("");
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

  getAcquisitionsDetail(
    boardId,
    ({ data }) => {
      console.log(data);
      setFounderId(data.result.acquisitionId);
      setColor(data.result.color);
      setProductName(data.result.productName);
      setDescription(data.result.description);
      setCategory(data.result.categoryName);
      setImageUrlList(data.result.imgUrlList);
      setAcquiredAt(data.result.acquiredAt);
      setAddress(data.result.address);
      setPlaceName(data.result.name);
    },
    (error) => console.log(error)
  );

  const returnItem = () => {
    if (!receiver) {
      alert("인계 대상자 정보를 입력해주세요.");
    } else {
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

  const checkReturnState = (isReturned: boolean) => {
    return isReturned ? (
      <CustomButton className="rounded-md w-[320px] h-[60px] bg-A706DarkGrey1 text-white">
        <p>인계 완료</p>
      </CustomButton>
    ) : (
      <CustomButton
        className="rounded-md w-[320px] h-[60px] bg-A706CheryBlue text-white"
        onClick={() => setModalOptions(true)}
      >
        <p>인계하기</p>
      </CustomButton>
    );
  };

  const showSendButton = (role: string) => {
    return role === "NORMAL" ? (
      <CustomButton
        className="main-nav-button"
        onClick={() => {
          sendMessage();
        }}
      >
        <>
          <SendOutlinedIcon />
          쪽지 보내기
        </>
      </CustomButton>
    ) : (
      <></>
    );
  };

  const showScrapbutton = (isScrapped: boolean) => {
    return isScrapped ? (
      <CustomButton
        className="main-nav-button"
        onClick={() => scarpItem(false)}
      >
        <>
          <FavoriteIcon />
          <p>스크랩 완료</p>
        </>
      </CustomButton>
    ) : (
      <CustomButton
        className="main-nav-button"
        onClick={() => {
          scarpItem(true);
        }}
      >
        <>
          <FavoriteBorderOutlinedIcon />
          <p>스크랩하기</p>
        </>
      </CustomButton>
    );
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
        <div className="flex flex-row justify-between w-[340px] mb-10">
          <Breadcrumb>
            <Breadcrumb.Item>카테고리</Breadcrumb.Item>
            <Breadcrumb.Item>{category}</Breadcrumb.Item>
          </Breadcrumb>
          <Text>{"습득자: " + founderId}</Text>
        </div>
        <div className="flex flex-row justify-between p-[40px]">
          <div className="mx-5">
            <img
              src={imageUrlList[0]}
              alt="이미지가 없습니다."
              className="size-[170px] self-center"
            />
          </div>
          <div className="mx-5">
            <Label color="secondary" value={placeName} />
            <KakaoMap
              className="size-[170px] self-center rounded-md shadow-md"
              keyword={address}
            ></KakaoMap>
          </div>
        </div>
        <div className="w-[340px] flex flex-row justify-end text-A706DarkGrey1">
          <p>습득일: {acquiredAt}</p>
        </div>
        <div className="flex flex-row justify-between mt-10 w-[340px]">
          <div className="w-[200px]">
            <Label color="secondary" value="이름" />
            <Text children={productName} className="text-lg font-bold" />
            <div className="h-[1px] bg-A706DarkGrey2"></div>
          </div>
          <div className="flex justify-start">
            <ColorLensIcon />
            <Text className="mx-5">{color}</Text>
          </div>
        </div>
        <div className="w-[340px] mt-10">
          <Label color="secondary" value="설명" />
          <div className="bg-white shadow-md min-h-[50px] p-5 rounded-md">
            {description}
          </div>
        </div>
        {/* <div className="w-[340px] mt-10">
          <Label color="secondary" value="장소 이름" />
          <Text children={placeName} className="w-[340px] text-lg font-bold" />
          <div className="h-[1px] bg-A706DarkGrey2"></div>
        </div> */}
        <div className="w-[340px] mt-10 flex flex-row justify-around">
          {showSendButton(member.role)}
          {member.role === "NORMAL"
            ? showScrapbutton(isScrapped)
            : checkReturnState(isReturned)}
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
              className="rounded-md w-full h-[60px] bg-A706CheryBlue text-white self-center"
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
